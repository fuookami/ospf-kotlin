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

    /**
     * 根据条件查询实体列表
     * Find entity list by condition
     *
     * @param where 查询条件 / Query condition
     * @return 实体列表 / Entity list
     */
    override fun find(where: BooleanExpression): List<E> {
        return find(where, null, null, null)
    }

    /**
     * 根据条件查询实体列表（支持排序和分页）
     * Find entity list by condition with sorting and pagination
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

    /**
     * 统计满足条件的实体数量
     * Count entities matching condition
     *
     * @param where 查询条件 / Query condition
     * @return 实体数量 / Entity count
     */
    override fun count(where: BooleanExpression): Long {
        val condition = booleanTranslator.translate(where)
        if (condition == null) return 0L

        val totalRecords = database.from(table).select().where(condition).totalRecordsInAllPages
        return totalRecords.toLong()
    }

    /**
     * 更新满足条件的实体
     * Update entities matching condition
     *
     * @param where 更新条件 / Update condition
     * @param assignments 更新赋值列表 / Update assignment list
     * @return 受影响的行数 / Number of affected rows
     */
    override fun update(where: BooleanExpression, assignments: UpdateAssignments): Int {
        if (assignments.isEmpty()) return 0

        val condition = booleanTranslator.translate(where)
        if (condition == null) return 0

        return updateTranslator.executeUpdate(database, condition, assignments)
    }

    /**
     * 删除满足条件的实体
     * Delete entities matching condition
     *
     * @param where 删除条件 / Delete condition
     * @return 受影响的行数 / Number of affected rows
     */
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
         *
         * @param table Ktorm 表定义 / Ktorm table definition
         * @return 列解析器函数 / Column resolver function
         */
        fun tableColumnResolver(table: Table<*>): KtormColumnResolver = { path: String ->
            val columnName = path.substringAfterLast(".")
            table.columns.find { it.name == columnName } as? ColumnDeclaring<*>
        }
    }
}
