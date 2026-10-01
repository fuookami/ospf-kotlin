/**
 * Ktorm 仓储实现
 * Ktorm Repository Implementation
 *
 * 提供基于 Ktorm 的仓储实现。 / Provides Ktorm-based repository implementation.
*/
package fuookami.ospf.kotlin.framework.persistence.expression

import java.util.concurrent.CancellationException
import org.ktorm.database.Database
import org.ktorm.dsl.*
import org.ktorm.schema.ColumnDeclaring
import org.ktorm.schema.Table
import fuookami.ospf.kotlin.framework.persistence.KtormBackend
import fuookami.ospf.kotlin.framework.persistence.expression.translator.*
import fuookami.ospf.kotlin.framework.persistence.query.ColumnRef
import fuookami.ospf.kotlin.framework.persistence.query.KtormCompiledQuery
import fuookami.ospf.kotlin.framework.persistence.query.KtormQuerySource
import fuookami.ospf.kotlin.framework.persistence.query.KtormRelationalQueryCompiler
import fuookami.ospf.kotlin.framework.persistence.query.NullsOrder as RelationalNullsOrder
import fuookami.ospf.kotlin.framework.persistence.query.OrderSpec
import fuookami.ospf.kotlin.framework.persistence.query.PageSpec
import fuookami.ospf.kotlin.framework.persistence.query.QuerySource
import fuookami.ospf.kotlin.framework.persistence.query.RelationalQueryDialect
import fuookami.ospf.kotlin.framework.persistence.query.RelationalQueryPlan
import fuookami.ospf.kotlin.framework.persistence.query.SortDirection as RelationalSortDirection
import fuookami.ospf.kotlin.math.symbol.expression.BooleanExpression
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*

/**
 * 列名解析器 / Column Name Resolver
*/
fun interface ColumnNameResolver {
    operator fun invoke(path: String): String?
}

/**
 * Ktorm 仓储实现
 * Ktorm Repository Implementation
 *
 * 提供基于 Ktorm 的仓储基类实现。 / Provides base repository implementation based on Ktorm.
 *
 * @param E 实体类型 / Entity type
 * @property database Ktorm 数据库实例 / Ktorm database instance
 * @property table Ktorm 表定义 / Ktorm table definition
 * @property resolveColumn Ktorm 列解析函数 / Ktorm column resolver function
 * @property patternMatchPolicy 模式匹配策略 / Pattern match policy
 * @property nullsOrderSupport 空值排序支持 / Nulls order support
 * @property unsupportedPredicatePolicy 不支持谓词策略 / Unsupported predicate policy
 * @property relationalQueryDialect Ktorm 数据库使用的关系查询方言 / Relational query dialect used by the Ktorm database
*/
abstract class KtormRepository<E : Any>(
    protected val database: Database,
    protected val table: Table<*>,
    protected val resolveColumn: KtormColumnResolver,
    protected val patternMatchPolicy: PatternMatchPolicy = DefaultPatternMatchPolicy,
    protected val nullsOrderSupport: NullsOrderSupport = NullsOrderSupport.Auto,
    protected val unsupportedPredicatePolicy: UnsupportedPredicatePolicy = UnsupportedPredicatePolicy.FailFast,
    protected val targetConstantBinder: KtormTargetConstantBinder? = null,
    protected val relationalQueryDialect: RelationalQueryDialect = RelationalQueryDialect.SQLite
) : ExpressionRepository<E> {

    /**
     * Creates a repository from a database backend supplied by a persistence plugin.
     * 使用持久化插件提供的数据库后端创建仓储。
     */
    constructor(
        backend: KtormBackend,
        table: Table<*>,
        resolveColumn: KtormColumnResolver,
        patternMatchPolicy: PatternMatchPolicy = DefaultPatternMatchPolicy,
        nullsOrderSupport: NullsOrderSupport = NullsOrderSupport.Auto,
        unsupportedPredicatePolicy: UnsupportedPredicatePolicy = UnsupportedPredicatePolicy.FailFast,
        targetConstantBinder: KtormTargetConstantBinder? = null
    ) : this(
        database = backend.database,
        table = table,
        resolveColumn = resolveColumn,
        patternMatchPolicy = patternMatchPolicy,
        nullsOrderSupport = nullsOrderSupport,
        unsupportedPredicatePolicy = unsupportedPredicatePolicy,
        targetConstantBinder = targetConstantBinder,
        relationalQueryDialect = backend.dialect
    )

    private val relationalQuerySource = QuerySource(
        table::class.simpleName ?: "root"
    )
    private val relationalQueryCompiler = KtormRelationalQueryCompiler(
        database = database,
        sources = mapOf(
            relationalQuerySource.name to KtormQuerySource(
                source = relationalQuerySource,
                table = table,
                resolveColumnDetailed = object : DiagnosticPersistenceFieldResolver<ColumnDeclaring<*>> {
                    override fun resolveDetailed(path: String): PersistenceFieldResolution<ColumnDeclaring<*>> {
                        val normalizedPath = path.trim()
                        val column = resolveColumn(normalizedPath)
                        return if (column == null) {
                            PersistenceFieldResolution.Missing(path)
                        } else {
                            PersistenceFieldResolution.Resolved(column)
                        }
                    }
                },
                defaultColumnExpressions = table.columns.map { it as ColumnDeclaring<*> }
            )
        ),
        patternMatchPolicy = patternMatchPolicy,
        nullsOrderSupport = nullsOrderSupport,
        unsupportedPredicatePolicy = unsupportedPredicatePolicy,
        targetConstantBinder = targetConstantBinder,
        dialect = relationalQueryDialect
    )
    private val booleanTranslator = KtormBooleanTranslator(
        resolveColumn = resolveColumn,
        patternMatchPolicy = patternMatchPolicy,
        unsupportedPredicatePolicy = unsupportedPredicatePolicy,
        targetConstantBinder = targetConstantBinder
    )
    private val updateTranslator = KtormUpdateTranslator(resolveColumn, table)

    /**
     * 根据条件查询实体列表 / Find entity list by condition
     *
     * @param where 查询条件 / Query condition
     * @return 实体列表 / Entity list
    */
    override fun find(where: BooleanExpression): Ret<List<E>> {
        return find(where, null, null, null)
    }

    /**
     * 根据条件查询实体列表（支持排序和分页） / Find entity list by condition with sorting and pagination
     *
     * @param where 查询条件 / Query condition
     * @param sortBy 排序条件（可选）/ Sort conditions (optional)
     * @param limit 返回数量限制（可选）/ Limit (optional)
     * @param offset 偏移量（可选）/ Offset (optional)
     * @return 实体列表 / Entity list
    */
    override fun find(
        where: BooleanExpression,
        sortBy: SortBy?,
        limit: Int?,
        offset: Int?
    ): Ret<List<E>> {
        if (limit != null && limit < 0 || offset != null && offset < 0) {
            return Failed(
                ErrorCode.IllegalArgument,
                "分页参数不能为负数 / Pagination values cannot be negative"
            )
        }
        if (limit == 0) return Ok(emptyList())

        return try {
            val compiledResult = compileQuery(
                queryPlan(
                    where = where,
                    sortBy = sortBy,
                    limit = limit,
                    offset = offset
                )
            )
            compiledResult.propagateFailure<List<E>>()?.let { return it }
            val compiled = compiledResult.value
                ?: return Failed(ErrorCode.ApplicationError, "Ktorm 查询编译未返回结果 / Ktorm query compilation returned no value")
            Ok(compiled.query.mapNotNull { mapToEntity(it) })
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            persistenceFailure("Ktorm find", error)
        }
    }

    /**
     * 统计满足条件的实体数量 / Count entities matching condition
     *
     * @param where 查询条件 / Query condition
     * @return 实体数量 / Entity count
    */
    override fun count(where: BooleanExpression): Ret<Long> {
        return try {
            val compiledResult = compileQuery(queryPlan(where = where))
            compiledResult.propagateFailure<Long>()?.let { return it }
            val compiled = compiledResult.value
                ?: return Failed(ErrorCode.ApplicationError, "Ktorm 计数查询编译未返回结果 / Ktorm count query compilation returned no value")
            Ok(compiled.query.totalRecordsInAllPages.toLong())
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            persistenceFailure("Ktorm count", error)
        }
    }

    /**
     * 更新满足条件的实体 / Update entities matching condition
     *
     * @param where 更新条件 / Update condition
     * @param assignments 更新赋值列表 / Update assignment list
     * @return 受影响的行数 / Number of affected rows
    */
    override fun update(where: BooleanExpression, assignments: UpdateAssignments): Ret<Int> {
        if (assignments.isEmpty()) return Ok(0)

        return try {
            val translatedCondition = booleanTranslator.translate(where)
            translatedCondition.propagateFailure<Int>()?.let { return it }
            val condition = translatedCondition.value
                ?: return Failed(ErrorCode.ApplicationError, "Ktorm 更新条件翻译未返回结果 / Ktorm update predicate translation returned no value")
            updateTranslator.executeUpdate(database, condition, assignments)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            persistenceFailure("Ktorm update", error)
        }
    }

    /**
     * 删除满足条件的实体 / Delete entities matching condition
     *
     * @param where 删除条件 / Delete condition
     * @return 受影响的行数 / Number of affected rows
    */
    override fun delete(where: BooleanExpression): Ret<Int> {
        return try {
            val translatedCondition = booleanTranslator.translate(where)
            translatedCondition.propagateFailure<Int>()?.let { return it }
            val condition = translatedCondition.value
                ?: return Failed(ErrorCode.ApplicationError, "Ktorm 删除条件翻译未返回结果 / Ktorm delete predicate translation returned no value")
            Ok(database.delete(table) { condition })
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            persistenceFailure("Ktorm delete", error)
        }
    }

    /**
     * 将行映射为实体 / Map row to entity
     *
     * 子类需要实现此方法以进行实体映射。 / Subclasses must implement this method for entity mapping.
     *
     * @param row 查询结果行 / Query result row
     * @return 映射后的实体，若无法映射则返回 null / Mapped entity, or null if mapping fails
    */
    protected abstract fun mapToEntity(row: QueryRowSet): E?

    /**
     * 编译仓储读取计划 / Compile repository read plan
     *
     * 子类可覆写该边界以记录审计信息或替换执行策略。 / Subclasses may override this boundary to record audit information or replace the execution strategy.
     *
     * @param plan 关系查询计划 / Relational query plan
     * @return Ktorm 编译结果 / Ktorm compiled result
     */
    protected open fun compileQuery(plan: RelationalQueryPlan): Ret<KtormCompiledQuery> {
        return relationalQueryCompiler.compile(plan)
    }

    private fun queryPlan(
        where: BooleanExpression,
        sortBy: SortBy? = null,
        limit: Int? = null,
        offset: Int? = null
    ): RelationalQueryPlan {
        val orders = sortBy?.items.orEmpty().map { item ->
            OrderSpec(
                column = ColumnRef(relationalQuerySource.name, item.path),
                direction = when (item.direction) {
                    SortDirection.Asc -> RelationalSortDirection.Ascending
                    SortDirection.Desc -> RelationalSortDirection.Descending
                },
                nulls = when (item.nulls) {
                    NullsOrder.NullsFirst -> RelationalNullsOrder.First
                    NullsOrder.NullsLast -> RelationalNullsOrder.Last
                    null -> RelationalNullsOrder.Unspecified
                }
            )
        }
        val page = if (limit != null || offset != null) {
            PageSpec(limit = limit, offset = offset ?: 0)
        } else {
            null
        }
        return RelationalQueryPlan(
            root = relationalQuerySource,
            predicate = where,
            orderBy = orders,
            page = page
        )
    }

    companion object {
        /**
         * 从 Table 自动创建列解析器 / Create column resolver from Table
         *
         * @param table Ktorm 表定义 / Ktorm table definition
         * @return 列解析器函数 / Column resolver function
        */
        fun tableColumnResolver(table: Table<*>): KtormColumnResolver = KtormColumnResolver { path: String ->
            table.columns.find { it.name == path.trim() } as? ColumnDeclaring<*>
        }
    }
}
