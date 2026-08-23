/**
 * Ktorm 关系查询计划测试 / Ktorm relational query plan tests
 */
package fuookami.ospf.kotlin.framework.persistence.query

import java.nio.file.Files
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.ktorm.database.Database
import org.ktorm.schema.Table
import org.ktorm.schema.int
import org.ktorm.schema.varchar
import org.ktorm.support.sqlite.SQLiteDialect
import fuookami.ospf.kotlin.framework.persistence.expression.resolveColumnWithDiagnostics
import fuookami.ospf.kotlin.framework.persistence.expression.translator.KtormScalarBinding
import fuookami.ospf.kotlin.framework.persistence.expression.translator.KtormTargetConstantBinder
import fuookami.ospf.kotlin.math.symbol.expression.*
import fuookami.ospf.kotlin.utils.functional.Failed

@DisplayName("KtormRelationalQueryCompiler Tests / Ktorm 关系查询编译器测试")
class KtormRelationalQueryCompilerTest {
    @JvmInline
    private value class MaterialCode(val value: String)

    private object UnsupportedConstant

    private object Orders : Table<Nothing>("orders") {
        val id = int("id")
        val status = varchar("status")
        val secret = varchar("secret")
    }

    private object Items : Table<Nothing>("order_items") {
        val id = int("id")
        val orderId = int("order_id")
        val material = varchar("material").transform(::MaterialCode, MaterialCode::value)
    }

    private fun database(): Database {
        val file = Files.createTempFile("ospf-query-join", ".db").toFile().apply { deleteOnExit() }
        val database = Database.connect(
            url = "jdbc:sqlite:${file.absolutePath}",
            dialect = SQLiteDialect()
        )
        database.useConnection { connection ->
            connection.createStatement().use { statement ->
                statement.execute("create table orders(id integer primary key, status text, secret text)")
                statement.execute("create table order_items(id integer primary key, order_id integer, material text)")
                statement.execute("insert into orders(id, status) values (1, 'confirmed')")
                statement.execute("insert into orders(id, status) values (2, 'confirmed')")
                statement.execute("insert into orders(id, status) values (3, 'draft')")
                statement.execute("insert into order_items(id, order_id, material) values (11, 1, 'M-001')")
                statement.execute("insert into order_items(id, order_id, material) values (12, 1, 'M-001')")
                statement.execute("insert into order_items(id, order_id, material) values (13, 2, null)")
            }
        }
        return database
    }

    private fun sources(): Map<String, KtormQuerySource> {
        return mapOf(
            "orders" to KtormQuerySource(
                source = QuerySource("orders"),
                table = Orders,
                resolveColumnDetailed = resolveColumnWithDiagnostics {
                    map("id", Orders.id)
                    map("status", Orders.status)
                },
                defaultColumns = listOf("id", "status")
            ),
            "items" to KtormQuerySource(
                source = QuerySource("items"),
                table = Items,
                resolveColumnDetailed = resolveColumnWithDiagnostics {
                    map("id", Items.id)
                    map("orderId", Items.orderId)
                    map("material", Items.material)
                },
                defaultColumns = listOf("id", "orderId", "material")
            )
        )
    }

    private fun targetConstantBinder(): KtormTargetConstantBinder {
        return { value, target ->
            if (value is MaterialCode && target == Items.material.sqlType) {
                KtormScalarBinding.forTarget(value, target)
            } else {
                null
            }
        }
    }

    private fun compilerWithBindings(): KtormRelationalQueryCompiler {
        return KtormRelationalQueryCompiler(
            database = database(),
            sources = sources(),
            targetConstantBinder = targetConstantBinder()
        )
    }

    private fun reference(path: String): ScalarReference<Any?> {
        return ScalarReference(PropertyPath.parse(path))
    }

    private fun equals(left: String, right: ScalarExpression<*>): Comparison<Any?> {
        return Comparison(
            operator = ComparisonOperator.Eq,
            left = reference(left),
            right = right
        )
    }

    private fun joinCondition(): BooleanExpression {
        return Comparison(
            operator = ComparisonOperator.Eq,
            left = reference("o.id"),
            right = reference("i.orderId")
        )
    }

    @Test
    @DisplayName("inner join binds value object and paginates root projection / Inner Join 应绑定值对象并分页根投影")
    fun innerJoinBindsValueObjectAndPaginatesRootProjection() {
        val compiler = compilerWithBindings()
        val plan = RelationalQueryPlan(
            root = QuerySource("orders", "o"),
            joins = listOf(
                JoinSpec(
                    type = JoinType.Inner,
                    source = QuerySource("items", "i"),
                    condition = joinCondition(),
                    cardinality = JoinCardinality.OneToMany
                )
            ),
            predicate = equals("i.material", ScalarConstant(MaterialCode("M-001"))),
            projections = listOf(ProjectionSpec(ColumnRef("o", "id"))),
            distinct = true,
            orderBy = listOf(OrderSpec(ColumnRef("o", "id"))),
            page = PageSpec(limit = 1),
            rootKey = listOf(ColumnRef("o", "id"))
        )

        val compiled = compiler.compile(plan).value!!
        assertTrue(compiled.audit.sqlTemplate.contains("join", ignoreCase = true))
        assertTrue(compiled.audit.parameterTypes.isNotEmpty())
        assertEquals(listOf(1), compiled.query.iterator().asSequence().map { it.getInt(1) }.toList())
    }

    @Test
    @DisplayName("exists keeps root cardinality and count / Exists 保持根粒度并正确计数")
    fun existsKeepsRootCardinalityAndCount() {
        val compiler = compilerWithBindings()
        val plan = RelationalQueryPlan(
            root = QuerySource("orders", "o"),
            joins = listOf(
                JoinSpec(
                    type = JoinType.Exists,
                    source = QuerySource("items", "i"),
                    condition = AndExpression(
                        listOf(
                            joinCondition(),
                            equals("i.material", ScalarConstant(MaterialCode("M-001")))
                        )
                    ),
                    cardinality = JoinCardinality.OneToMany
                )
            ),
            predicate = equals("o.status", ScalarConstant("confirmed")),
            projections = listOf(ProjectionSpec(ColumnRef("o", "id"))),
            rootKey = listOf(ColumnRef("o", "id"))
        )

        val compiled = compiler.compile(plan).value!!
        assertTrue(compiled.audit.sqlTemplate.contains("exists", ignoreCase = true))
        assertEquals(listOf(1), compiled.query.iterator().asSequence().map { it.getInt(1) }.toList())

        val countQuery = compiler.compileCount(plan).value!!
        assertEquals(1L, countQuery.iterator().next().getLong(1))
    }

    @Test
    @DisplayName("invalid join and root key declarations fail structurally / 非法 Join 与关联表根键结构化拒绝")
    fun invalidJoinAndAssociatedRootKeyFailStructurally() {
        val compiler = KtormRelationalQueryCompiler(database(), sources())
        val invalidExists = RelationalQueryPlan(
            root = QuerySource("orders", "o"),
            joins = listOf(
                JoinSpec(
                    type = JoinType.Exists,
                    source = QuerySource("items", "i"),
                    condition = joinCondition(),
                    cardinality = JoinCardinality.OneToOne
                )
            )
        )
        val invalidExistsResult = compiler.compile(invalidExists)
        assertTrue(invalidExistsResult.failed)
        val invalidExistsFailure = (invalidExistsResult as Failed<*, *, *>).error.value
            as RelationalQueryFailure
        assertEquals(QueryExecutionErrorCategory.InvalidJoin, invalidExistsFailure.category)

        val associatedRootKey = RelationalQueryPlan(
            root = QuerySource("orders", "o"),
            joins = listOf(
                JoinSpec(
                    type = JoinType.Inner,
                    source = QuerySource("items", "i"),
                    condition = joinCondition(),
                    cardinality = JoinCardinality.OneToMany
                )
            ),
            rootKey = listOf(ColumnRef("i", "id"))
        )
        val associatedRootKeyResult = compiler.compileCount(associatedRootKey)
        assertTrue(associatedRootKeyResult.failed)
        val associatedRootKeyFailure = (associatedRootKeyResult as Failed<*, *, *>).error.value
            as RelationalQueryFailure
        assertEquals(QueryExecutionErrorCategory.SqlGeneration, associatedRootKeyFailure.category)
        assertEquals("rootKey", associatedRootKeyFailure.field)
    }

    @Test
    @DisplayName("uncorrelated constant joins are rejected / 无关联常量 Join 应被拒绝")
    fun uncorrelatedConstantJoinsAreRejected() {
        val compiler = KtormRelationalQueryCompiler(database(), sources())
        val plan = RelationalQueryPlan(
            root = QuerySource("orders", "o"),
            joins = listOf(
                JoinSpec(
                    type = JoinType.Inner,
                    source = QuerySource("items", "i"),
                    condition = BooleanConstant(fuookami.ospf.kotlin.math.Trivalent.True),
                    cardinality = JoinCardinality.OneToMany
                )
            )
        )

        val result = compiler.compile(plan)
        assertTrue(result.failed)
        val failure = (result as Failed<*, *, *>).error.value as RelationalQueryFailure
        assertEquals(QueryExecutionErrorCategory.InvalidJoin, failure.category)
    }

    @Test
    @DisplayName("arithmetic join operands are rejected / Join 关联端不能使用算术表达式")
    fun arithmeticJoinOperandsAreRejected() {
        val compiler = KtormRelationalQueryCompiler(database(), sources())
        val result = compiler.compile(
            RelationalQueryPlan(
                root = QuerySource("orders", "o"),
                joins = listOf(
                    JoinSpec(
                        type = JoinType.Inner,
                        source = QuerySource("items", "i"),
                        condition = Comparison(
                            operator = ComparisonOperator.Eq,
                            left = reference("o.id"),
                            right = ScalarBinary(
                                operator = BinaryOperator.Add,
                                left = reference("i.orderId"),
                                right = ScalarConstant(1)
                            )
                        ),
                        cardinality = JoinCardinality.OneToMany
                    )
                )
            )
        )

        assertTrue(result.failed)
        val failure = (result as Failed<*, *, *>).error.value as RelationalQueryFailure
        assertEquals(QueryExecutionErrorCategory.InvalidJoin, failure.category)
    }

    @Test
    @DisplayName("left join preserves null semantics / Left Join 保留 NULL 语义")
    fun leftJoinPreservesNullSemantics() {
        val compiler = KtormRelationalQueryCompiler(database(), sources())
        val plan = RelationalQueryPlan(
            root = QuerySource("orders", "o"),
            joins = listOf(
                JoinSpec(
                    type = JoinType.Left,
                    source = QuerySource("items", "i"),
                    condition = joinCondition(),
                    cardinality = JoinCardinality.OneToMany
                )
            ),
            predicate = NullCheck(PropertyPath.parse("i.material"), NullCheckType.IsNull),
            projections = listOf(ProjectionSpec(ColumnRef("o", "id"))),
            distinct = true,
            orderBy = listOf(OrderSpec(ColumnRef("o", "id")))
        )

        val compiled = compiler.compile(plan).value!!
        assertEquals(listOf(2, 3), compiled.query.iterator().asSequence().map { it.getInt(1) }.toList())
    }

    @Test
    @DisplayName("ambiguous short path fails fast / 未限定歧义字段快速失败")
    fun ambiguousShortPathFailsFast() {
        val compiler = KtormRelationalQueryCompiler(database(), sources())
        val plan = RelationalQueryPlan(
            root = QuerySource("orders"),
            joins = listOf(
                JoinSpec(JoinType.Inner, QuerySource("items"), joinCondition(), JoinCardinality.OneToMany)
            ),
            predicate = equals("id", ScalarConstant(1))
        )

        val result = compiler.compile(plan)
        assertTrue(result.failed)
        assertFalse(result.ok)
    }

    @Test
    @DisplayName("invalid aliases are rejected structurally / 非法别名结构化拒绝")
    fun invalidAliasesAreRejectedStructurally() {
        val result = RelationalQueryPlan(
            root = QuerySource("orders", "orders")
        ).validate()

        assertTrue(result.failed)
    }

    @Test
    @DisplayName("unknown source returns structured failure / 未知数据源返回结构化失败")
    fun unknownSourceReturnsStructuredFailure() {
        val compiler = KtormRelationalQueryCompiler(database(), sources())
        val result = compiler.compile(RelationalQueryPlan(root = QuerySource("missing")))

        assertTrue(result.failed)
        val failure = (result as Failed<*, *, *>).error.value as RelationalQueryFailure
        assertEquals(QueryExecutionErrorCategory.UnknownSource, failure.category)
        assertEquals("root", failure.field)
    }

    @Test
    @DisplayName("source diagnostics are preserved / 数据源诊断应被保留")
    fun sourceDiagnosticsArePreserved() {
        val diagnosticResolver = resolveColumnWithDiagnostics {
            map("id", Orders.id)
            map("id", Orders.status)
        }
        val diagnosticSources = sources().toMutableMap().apply {
            this["orders"] = KtormQuerySource(
                source = QuerySource("orders"),
                table = Orders,
                resolveColumnDetailed = diagnosticResolver,
                defaultColumns = listOf("id", "status")
            )
        }
        val compiler = KtormRelationalQueryCompiler(database(), diagnosticSources)
        val plan = RelationalQueryPlan(
            root = QuerySource("orders", "o"),
            predicate = equals("o.id", ScalarConstant(1))
        )

        val result = compiler.compile(plan)

        assertTrue(result.failed)
        val failure = (result as Failed<*, *, *>).error.value as RelationalQueryFailure
        assertEquals(QueryExecutionErrorCategory.UnknownColumn, failure.category)
        assertTrue(failure.reason.contains("Ambiguous"))
    }

    @Test
    @DisplayName("invalid source configuration returns SQL generation failure / 非法数据源配置返回 SQL 生成失败")
    fun invalidSourceConfigurationReturnsSqlGenerationFailure() {
        val diagnosticResolver = resolveColumnWithDiagnostics {
            map("o", "id", Orders.id)
            map("o", "id", Orders.status)
        }
        val diagnosticSources = sources().toMutableMap().apply {
            this["orders"] = KtormQuerySource(
                source = QuerySource("orders"),
                table = Orders,
                resolveColumnDetailed = diagnosticResolver,
                defaultColumns = listOf("id", "status")
            )
        }
        val compiler = KtormRelationalQueryCompiler(database(), diagnosticSources)
        val plan = RelationalQueryPlan(
            root = QuerySource("orders", "o"),
            predicate = equals("o.id", ScalarConstant(1))
        )

        val result = compiler.compile(plan)

        assertTrue(result.failed)
        val failure = (result as Failed<*, *, *>).error.value as RelationalQueryFailure
        assertEquals(QueryExecutionErrorCategory.SqlGeneration, failure.category)
        assertTrue(failure.reason.contains("Duplicate column mapping"))
    }

    @Test
    @DisplayName("unsupported parameter binding is classified structurally / 不支持的参数绑定应结构化分类")
    fun unsupportedParameterBindingIsClassifiedStructurally() {
        val compiler = KtormRelationalQueryCompiler(database(), sources())
        val plan = RelationalQueryPlan(
            root = QuerySource("orders", "o"),
            predicate = equals("o.status", ScalarConstant(UnsupportedConstant))
        )

        val result = compiler.compile(plan)

        assertTrue(result.failed)
        val failure = (result as Failed<*, *, *>).error.value as RelationalQueryFailure
        assertEquals(QueryExecutionErrorCategory.ParameterBinding, failure.category)
    }

    @Test
    @DisplayName("binder exceptions are classified structurally / 绑定器异常应结构化分类")
    fun binderExceptionsAreClassifiedStructurally() {
        val compiler = KtormRelationalQueryCompiler(
            database = database(),
            sources = sources(),
            targetConstantBinder = { _, _ ->
                throw IllegalArgumentException("binder rejected value")
            }
        )
        val plan = RelationalQueryPlan(
            root = QuerySource("orders", "o"),
            predicate = equals("o.status", ScalarConstant("confirmed"))
        )

        val result = compiler.compile(plan)

        assertTrue(result.failed)
        val failure = (result as Failed<*, *, *>).error.value as RelationalQueryFailure
        assertEquals(QueryExecutionErrorCategory.ParameterBinding, failure.category)
        assertTrue(failure.reason.contains("No SQL binding registered"))
    }

    @Test
    @DisplayName("incompatible transformed values fail during compilation / 不兼容转换值应在编译阶段失败")
    fun incompatibleTransformedValuesFailDuringCompilation() {
        val compiler = KtormRelationalQueryCompiler(database(), sources())
        val plan = RelationalQueryPlan(
            root = QuerySource("orders", "o"),
            predicate = equals("o.id", ScalarConstant("not-an-int"))
        )

        val result = compiler.compile(plan)

        assertTrue(result.failed)
        val failure = (result as Failed<*, *, *>).error.value as RelationalQueryFailure
        assertEquals(QueryExecutionErrorCategory.ParameterBinding, failure.category)
    }

    @Test
    @DisplayName("invalid transformed-column values fail during compilation / 转换列的非法对象应在编译阶段失败")
    fun invalidTransformedColumnValuesFailDuringCompilation() {
        val compiler = KtormRelationalQueryCompiler(database(), sources())
        val plan = RelationalQueryPlan(
            root = QuerySource("orders", "o"),
            joins = listOf(
                JoinSpec(
                    type = JoinType.Inner,
                    source = QuerySource("items", "i"),
                    condition = joinCondition(),
                    cardinality = JoinCardinality.OneToMany
                )
            ),
            predicate = equals("i.material", ScalarConstant(123))
        )

        val result = compiler.compile(plan)

        assertTrue(result.failed)
        val failure = (result as Failed<*, *, *>).error.value as RelationalQueryFailure
        assertEquals(QueryExecutionErrorCategory.ParameterBinding, failure.category)
    }

    @Test
    @DisplayName("unregistered physical columns are not resolved / 未注册物理列不可解析")
    fun unregisteredPhysicalColumnsAreNotResolved() {
        val compiler = KtormRelationalQueryCompiler(database(), sources())
        val result = compiler.compile(
            RelationalQueryPlan(
                root = QuerySource("orders", "o"),
                predicate = equals("o.secret", ScalarConstant(1))
            )
        )

        assertTrue(result.failed)
        val failure = (result as Failed<*, *, *>).error.value as RelationalQueryFailure
        assertEquals(QueryExecutionErrorCategory.UnknownColumn, failure.category)
    }

    @Test
    @DisplayName("implicit projections use the registered allowlist / 隐式投影只能使用注册白名单")
    fun implicitProjectionsUseRegisteredAllowlist() {
        val compiler = KtormRelationalQueryCompiler(database(), sources())
        val result = compiler.compile(RelationalQueryPlan(root = QuerySource("orders")))

        assertTrue(result.ok)
        val compiled = result.value!!
        assertFalse(compiled.audit.sqlTemplate.contains("secret", ignoreCase = true))
        val row = compiled.query.iterator().next()
        assertEquals(1, row.getInt(1))
        assertEquals("confirmed", row.getString(2))
    }

    @Test
    @DisplayName("execution reports row truncation / 执行达到上限时报告截断")
    fun executionReportsRowTruncation() {
        val compiler = KtormRelationalQueryCompiler(database(), sources())
        val compiled = compiler.compile(
            RelationalQueryPlan(
                root = QuerySource("orders", "o"),
                projections = listOf(ProjectionSpec(ColumnRef("o", "id")))
            )
        ).value!!

        val result = compiled.execute(maxReturnedRows = 1) { row -> row.getInt(1) }

        assertTrue(result.ok)
        assertEquals(listOf(1), result.value!!.value)
        assertTrue(result.value!!.stats.truncated)
    }
}
