/**
 * 仓储 API
 * Repository API
 *
 * 提供基于表达式的统一查询和更新接口。
 * Provides unified query and update interface based on expressions.
*/
package fuookami.ospf.kotlin.framework.persistence.expression

import fuookami.ospf.kotlin.math.symbol.expression.BooleanExpression

/**
 * 表达式仓储接口
 * Expression Repository Interface
 *
 * 定义基于 BooleanExpression 的查询和更新接口。
 * Defines query and update interfaces based on BooleanExpression.
 *
 * @param E 实体类型 / Entity type
*/
interface ExpressionRepository<E : Any> {

    /**
     * 查询实体
     * Find entities
     *
     * @param where 查询条件表达式 / Query condition expression
     * @return 匹配的实体列表 / List of matching entities
    */
    fun find(where: BooleanExpression): List<E>

    /**
     * 查询实体（带排序和分页）
     * Find entities with sort and pagination
     *
     * @param where 查询条件表达式 / Query condition expression
     * @param sortBy 排序规则，可为 null / Sort rules, nullable
     * @param limit 返回数量限制，可为 null / Limit, nullable
     * @param offset 偏移量，可为 null / Offset, nullable
     * @return 匹配的实体列表 / List of matching entities
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
     *
     * @param where 查询条件表达式 / Query condition expression
     * @return 匹配实体数量 / Count of matching entities
    */
    fun count(where: BooleanExpression): Long

    /**
     * 更新
     * Update
     *
     * @param where 更新条件表达式 / Update condition expression
     * @param assignments 更新赋值集合 / Update assignments
     * @return 受影响的行数 / Number of affected rows
    */
    fun update(where: BooleanExpression, assignments: UpdateAssignments): Int

    /**
     * 删除
     * Delete
     *
     * @param where 删除条件表达式 / Delete condition expression
     * @return 受影响的行数 / Number of affected rows
    */
    fun delete(where: BooleanExpression): Int

    /**
     * 检查是否存在
     * Check if exists
     *
     * @param where 查询条件表达式 / Query condition expression
     * @return 是否存在匹配实体 / Whether matching entities exist
    */
    fun exists(where: BooleanExpression): Boolean = count(where) > 0
}
