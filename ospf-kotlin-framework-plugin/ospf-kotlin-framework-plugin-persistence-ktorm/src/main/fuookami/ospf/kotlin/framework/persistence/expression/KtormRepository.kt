/**
 * Ktorm 仓储实现
 * Ktorm Repository Implementation
 *
 * 提供基于 Ktorm 的仓储实现。
 * Provides Ktorm-based repository implementation.
 */
package fuookami.ospf.kotlin.framework.persistence.expression

import fuookami.ospf.kotlin.framework.persistence.expression.translator.*
import fuookami.ospf.kotlin.math.symbol.expression.BooleanExpression
import org.ktorm.database.Database
import org.ktorm.dsl.*
import org.ktorm.schema.ColumnDeclaring
import org.ktorm.schema.Table

/**
 * 列名解析器
 * Column Name Resolver
 */
typealias ColumnNameResolver = (String) -> String?

/**
 * Ktorm 仓储实现
 * Ktorm Repository Implementation
 *
 * 提供基于 Ktorm 的仓储基类实现。
 * Provides base repository implementation based on Ktorm.
 *
 * @param E 实体类型 / Entity type
 * @property database Ktorm 数据库实例 / Ktorm database instance
 * @property table Ktorm 表定义 / Ktorm table definition
 * @property resolveColumn Ktorm 列解析函数 / Ktorm column resolver function
 * @property patternMatchPolicy 模式匹配策略 / Pattern match policy
 * @property nullsOrderSupport 空值排序支持 / Nulls order support
 * @property unsupportedPredicatePolicy 不支持谓词策略 / Unsupported predicate policy
 */
abstract class KtormRepository<E : Any>(
    protected val database: Database,
    protected val table: Table<*>,
    protected val resolveColumn: KtormColumnResolver,
    protected val patternMatchPolicy: PatternMatchPolicy = DefaultPatternMatchPolicy,
    protected val nullsOrderSupport: NullsOrderSupport = NullsOrderSupport.Auto,
    protected val unsupportedPredicatePolicy: UnsupportedPredicatePolicy = UnsupportedPredicatePolicy.AlwaysFalse
) : ExpressionRepository<E> {

    private val booleanTranslator = KtormBooleanTranslator(resolveColumn, patternMatchPolicy, unsupportedPredicatePolicy)
    private val orderByTranslator = KtormOrderByTranslator(resolveColumn, nullsOrderSupport)
    private val updateTranslator = KtormUpdateTranslator(resolveColumn, table)

    override fun find(where: BooleanExpression): List<E> {
        return find(where, null, null, null)
    }

    override fun find(
        where: BooleanExpression,
        sortBy: SortBy?,
        limit: Int?,
        offset: Int?
    ): List<E> {
        val condition = booleanTranslator.translate(where)
        if (condition == null) return emptyList()

        var query = database.from(table).select().where(condition)

        // 应用排序
        // Apply order by
        if (sortBy != null && sortBy.isNotEmpty()) {
            query = orderByTranslator.apply(query, sortBy)
        }

        // 应用分页
        // Apply pagination
        if (limit != null) {
            query = query.limit(limit)
        }
        if (offset != null) {
            query = query.offset(offset)
        }

        return query.mapNotNull { mapToEntity(it) }
    }

    override fun count(where: BooleanExpression): Long {
        val condition = booleanTranslator.translate(where)
        if (condition == null) return 0L

        val totalRecords = database.from(table).select().where(condition).totalRecordsInAllPages
        return totalRecords.toLong()
    }

    override fun update(where: BooleanExpression, assignments: UpdateAssignments): Int {
        if (assignments.isEmpty()) return 0

        val condition = booleanTranslator.translate(where)
        if (condition == null) return 0

        return updateTranslator.executeUpdate(database, condition, assignments)
    }

    override fun delete(where: BooleanExpression): Int {
        val condition = booleanTranslator.translate(where)
        if (condition == null) return 0

        return database.delete(table) { condition }
    }

    /**
     * 将行映射为实体
     * Map row to entity
     *
     * 子类需要实现此方法以进行实体映射。
     * Subclasses must implement this method for entity mapping.
     */
    protected abstract fun mapToEntity(row: QueryRowSet): E?

    companion object {
        /**
         * 从 Table 自动创建列解析器
         * Create column resolver from Table
         */
        fun tableColumnResolver(table: Table<*>): KtormColumnResolver = { path: String ->
            val columnName = path.substringAfterLast(".")
            table.columns.find { it.name == columnName } as? ColumnDeclaring<*>
        }
    }
}
