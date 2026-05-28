/**
 * 仓储 API
 * Repository API
 *
 * 提供基于表达式的统一查询和更新接口�?
 * Provides unified query and update interface based on expressions.
 */
package fuookami.ospf.kotlin.framework.persistence.expression

import fuookami.ospf.kotlin.math.symbol.expression.BooleanExpression

/**
 * 表达式仓储接�?
 * Expression Repository Interface
 *
 * 定义基于 BooleanExpression 的查询和更新接口�?
 * Defines query and update interfaces based on BooleanExpression.
 *
 * @param E 实体类型 / Entity type
 */
interface ExpressionRepository<E : Any> {
    /**
     * 查询实体
     * Find entities
     */
    fun find(where: BooleanExpression): List<E>

    /**
     * 查询实体（带排序和分页）
     * Find entities with sort and pagination
     */
    fun find(
        where: BooleanExpression,
        sortBy: SortBy?,
        limit: Int?,
        offset: Int?
    ): List<E>

    /**
     * 计数
     * Count
     */
    fun count(where: BooleanExpression): Long

    /**
     * 更新
     * Update
     */
    fun update(where: BooleanExpression, assignments: UpdateAssignments): Int

    /**
     * 删除
     * Delete
     */
    fun delete(where: BooleanExpression): Int

    /**
     * 检查是否存�?
     * Check if exists
     */
    fun exists(where: BooleanExpression): Boolean = count(where) > 0
}
