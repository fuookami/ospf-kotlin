/**
 * 关系查询编译器契约测试 / Relational query compiler contract tests
 */
package fuookami.ospf.kotlin.framework.persistence.query

import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.Ret
import fuookami.ospf.kotlin.math.symbol.expression.AndExpression
import fuookami.ospf.kotlin.math.symbol.expression.BooleanCustom
import fuookami.ospf.kotlin.math.symbol.expression.Comparison
import fuookami.ospf.kotlin.math.symbol.expression.ComparisonOperator
import fuookami.ospf.kotlin.math.symbol.expression.PropertyPath
import fuookami.ospf.kotlin.math.symbol.expression.ScalarConstant
import fuookami.ospf.kotlin.math.symbol.expression.ScalarReference

class RelationalQueryCompilerTest {
    @Test
    fun compilerDeclaresSupportedRelationalDialectsAndReturnsResult() {
        val plan = RelationalQueryPlan(root = QuerySource("orders"))

        RelationalQueryDialect.entries.forEach { dialect ->
            val compiler = object : RelationalQueryCompiler<String> {
                override val dialect: RelationalQueryDialect = dialect

                override fun compile(plan: RelationalQueryPlan): Ret<String> = Ok(plan.hash())
            }

            val result = compiler.compile(plan)

            assertEquals(dialect, compiler.dialect)
            assertEquals(plan.hash(), result.value)
        }
    }

    @Test
    fun compilerBuildsParameterizedSqlForEachDialect() {
        val plan = RelationalQueryPlan(
            root = QuerySource("orders", "o"),
            predicate = AndExpression(
                listOf(
                    Comparison(
                        ComparisonOperator.Eq,
                        ScalarReference<Any?>(PropertyPath.parse("o.status")),
                        ScalarConstant<Any?>("OPEN")
                    ),
                    Comparison(
                        ComparisonOperator.Gt,
                        ScalarReference<Any?>(PropertyPath.parse("o.total")),
                        ScalarConstant<Any?>(100)
                    )
                )
            ),
            projections = listOf(
                ProjectionSpec(ColumnRef("o", "id")),
                ProjectionSpec(ColumnRef("o", "status"), "state")
            ),
            page = PageSpec(limit = 10, offset = 20)
        )

        val mysql = SqlRelationalQueryCompiler(RelationalQueryDialect.MySQL).compile(plan)
        val postgres = SqlRelationalQueryCompiler(RelationalQueryDialect.PostgreSQL).compile(plan)
        val sqlite = SqlRelationalQueryCompiler(RelationalQueryDialect.SQLite).compile(plan)
        val oracle = SqlRelationalQueryCompiler(RelationalQueryDialect.Oracle).compile(plan)

        assertTrue(mysql.ok)
        assertTrue(postgres.ok)
        assertTrue(sqlite.ok)
        assertTrue(oracle.ok)
        assertEquals(listOf("OPEN", 100), mysql.value?.parameters)
        assertEquals(listOf("OPEN", 100), postgres.value?.parameters)
        assertEquals(listOf("OPEN", 100), sqlite.value?.parameters)
        assertEquals(listOf("OPEN", 100), oracle.value?.parameters)
        assertTrue(mysql.value?.sql?.contains("SELECT `o`.`id`, `o`.`status` AS `state`") == true)
        assertTrue(mysql.value?.sql?.endsWith("LIMIT 20, 10") == true)
        assertTrue(postgres.value?.sql?.contains("SELECT \"o\".\"id\", \"o\".\"status\" AS \"state\"") == true)
        assertTrue(postgres.value?.sql?.endsWith("LIMIT 10 OFFSET 20") == true)
        assertTrue(sqlite.value?.sql?.contains("SELECT \"o\".\"id\", \"o\".\"status\" AS \"state\"") == true)
        assertTrue(sqlite.value?.sql?.endsWith("LIMIT 10 OFFSET 20") == true)
        assertTrue(oracle.value?.sql?.contains("SELECT \"o\".\"id\", \"o\".\"status\" AS \"state\"") == true)
        assertTrue(oracle.value?.sql?.endsWith("OFFSET 20 ROWS FETCH NEXT 10 ROWS ONLY") == true)
        assertFalse(mysql.value?.sql?.contains("OPEN") == true)
    }

    @Test
    fun compilerPreservesOffsetOnlyPaginationByDialect() {
        val plan = RelationalQueryPlan(
            root = QuerySource("orders", "o"),
            projections = listOf(ProjectionSpec(ColumnRef("o", "id"))),
            page = PageSpec(limit = null, offset = 20)
        )

        val postgres = SqlRelationalQueryCompiler(RelationalQueryDialect.PostgreSQL).compile(plan)
        val sqlite = SqlRelationalQueryCompiler(RelationalQueryDialect.SQLite).compile(plan)
        val oracle = SqlRelationalQueryCompiler(RelationalQueryDialect.Oracle).compile(plan)
        val mysql = SqlRelationalQueryCompiler(RelationalQueryDialect.MySQL).compile(plan)

        assertTrue(postgres.ok)
        assertEquals(plan.hash(), postgres.value?.canonicalHash)
        assertTrue(postgres.value?.sql?.endsWith("OFFSET 20") == true)
        assertFalse(postgres.value?.sql?.contains("LIMIT") == true)
        assertTrue(sqlite.ok)
        assertTrue(sqlite.value?.sql?.endsWith("LIMIT -1 OFFSET 20") == true)
        assertTrue(oracle.ok)
        assertTrue(oracle.value?.sql?.endsWith("OFFSET 20 ROWS") == true)
        assertFalse(oracle.value?.sql?.contains("FETCH NEXT") == true)
        assertTrue(mysql.failed)
        val mysqlFailure = (mysql as Failed<*, *, *>).error.value
            as RelationalQueryCompilationError
        assertEquals(RelationalQueryCompilationErrorCategory.UnsupportedDialect, mysqlFailure.category)
    }

    @Test
    fun compilerBuildsJoinsAndRejectsCustomExpressionsAsResultFailure() {
        val plan = RelationalQueryPlan(
            root = QuerySource("orders", "o"),
            joins = listOf(
                JoinSpec(
                    type = JoinType.Left,
                    source = QuerySource("items", "i"),
                    condition = Comparison(
                        ComparisonOperator.Eq,
                        ScalarReference<Any?>(PropertyPath.parse("o.id")),
                        ScalarReference<Any?>(PropertyPath.parse("i.order_id"))
                    ),
                    cardinality = JoinCardinality.OneToMany
                )
            ),
            projections = listOf(ProjectionSpec(ColumnRef("o", "id")))
        )
        val compiled = SqlRelationalQueryCompiler(RelationalQueryDialect.PostgreSQL).compile(plan)

        assertTrue(compiled.ok)
        assertTrue(compiled.value?.sql?.contains("LEFT JOIN \"items\" AS \"i\" ON") == true)
        assertTrue(
            compiled.value?.sql?.contains("\"o\".\"id\" = \"i\".\"order_id\"") == true,
            compiled.value?.sql ?: "compilation failed"
        )

        val custom = SqlRelationalQueryCompiler(RelationalQueryDialect.PostgreSQL).compile(
            RelationalQueryPlan(
                root = QuerySource("orders"),
                predicate = BooleanCustom("unsupported"),
                projections = listOf(ProjectionSpec(ColumnRef("orders", "id")))
            )
        )
        assertTrue(custom.failed)
        val failure = (custom as Failed<*, *, *>).error.value
        assertTrue(failure is RelationalQueryCompilationError)
        assertEquals(
            RelationalQueryCompilationErrorCategory.UnsupportedExpression,
            (failure as RelationalQueryCompilationError).category
        )
    }

    @Test
    fun compilerCollectsExistsParametersAfterRootPredicateParameters() {
        val plan = RelationalQueryPlan(
            root = QuerySource("orders", "o"),
            joins = listOf(
                JoinSpec(
                    type = JoinType.Exists,
                    source = QuerySource("items", "i"),
                    condition = AndExpression(
                        listOf(
                            Comparison(
                                ComparisonOperator.Eq,
                                ScalarReference<Any?>(PropertyPath.parse("o.id")),
                                ScalarReference<Any?>(PropertyPath.parse("i.order_id"))
                            ),
                            Comparison(
                                ComparisonOperator.Eq,
                                ScalarReference<Any?>(PropertyPath.parse("i.status")),
                                ScalarConstant<Any?>("READY")
                            )
                        )
                    ),
                    cardinality = JoinCardinality.OneToMany
                )
            ),
            predicate = Comparison(
                ComparisonOperator.Eq,
                ScalarReference<Any?>(PropertyPath.parse("o.status")),
                ScalarConstant<Any?>("OPEN")
            ),
            projections = listOf(ProjectionSpec(ColumnRef("o", "id")))
        )

        val compiled = SqlRelationalQueryCompiler(RelationalQueryDialect.PostgreSQL).compile(plan)

        assertTrue(compiled.ok)
        assertEquals(listOf("OPEN", "READY"), compiled.value?.parameters)
        assertTrue(compiled.value?.sql?.contains("WHERE (\"o\".\"status\" = ?) AND (EXISTS") == true)
    }

    @Test
    fun sourceRegistryFreezesExternalSourceMapsAndDefaultColumns() {
        val columns = linkedMapOf("id" to "order_id")
        val defaultColumns = mutableListOf("id")
        val definitions = linkedMapOf(
            "orders" to RelationalQuerySource(
                name = "orders",
                table = "orders_table",
                columns = columns,
                defaultColumns = defaultColumns
            )
        )
        val registry = RelationalQuerySourceRegistry(definitions)

        columns["status"] = "order_status"
        defaultColumns += "status"
        definitions.clear()

        val frozen = registry["orders"]
        assertEquals(mapOf("id" to "order_id"), frozen?.columns)
        assertEquals(listOf("id"), frozen?.defaultColumns)
    }

    @Test
    fun compilerPropagatesGroupAndOrderColumnFailures() {
        val plan = RelationalQueryPlan(
            root = QuerySource("orders"),
            projections = listOf(ProjectionSpec(ColumnRef("orders", "id"))),
            groupBy = listOf(ColumnRef("orders", "missing"))
        )

        val result = SqlRelationalQueryCompiler(
            dialect = RelationalQueryDialect.MySQL,
            sources = listOf(
                RelationalQuerySource(
                    name = "orders",
                    table = "orders",
                    columns = mapOf("id" to "id")
                )
            )
        ).compile(plan)

        assertTrue(result.failed)
        val failure = (result as Failed<*, *, *>).error.value
        assertTrue(failure is RelationalQueryCompilationError)
        assertEquals("groupBy[0]", (failure as RelationalQueryCompilationError).field)
    }

    @Test
    fun executionStatsRecorderIsThreadSafeAndRetainsPlanAuditData() {
        val plan = RelationalQueryPlan(
            root = QuerySource("orders"),
            projections = listOf(ProjectionSpec(ColumnRef("orders", "id")))
        )
        val recorder = RelationalQueryExecutionStatsRecorder()
        val executor = Executors.newFixedThreadPool(4)
        repeat(100) { index ->
            executor.submit {
                if (index % 2 == 0) {
                    recorder.recordSuccess(plan, duration = index.toLong(), rowCount = index.toLong())
                } else {
                    recorder.recordFailure(plan, duration = index.toLong(), failure = "failure")
                }
            }
        }
        executor.shutdown()
        assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS))

        val snapshot = recorder.snapshot()
        assertEquals(100, snapshot.size)
        assertTrue(snapshot.all { it.planHash == plan.hash() })
        assertTrue(snapshot.all { it.canonical == plan.canonical() })
        assertEquals(50, snapshot.count { !it.failed })
        assertEquals(50, snapshot.count { it.failed })
        assertEquals(50, snapshot.count { it.rowCount == null })
    }
}
