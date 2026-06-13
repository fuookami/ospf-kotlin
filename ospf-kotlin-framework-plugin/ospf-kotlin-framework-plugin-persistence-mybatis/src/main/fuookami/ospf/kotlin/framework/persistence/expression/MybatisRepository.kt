/**
 * MyBatis 仓储实现
 * MyBatis Repository Implementation
 *
 * 提供基于 MyBatis-Plus 的仓储实现。
 * Provides MyBatis-Plus-based repository implementation.
 */
package fuookami.ospf.kotlin.framework.persistence.expression

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper
import com.baomidou.mybatisplus.core.mapper.BaseMapper
import fuookami.ospf.kotlin.math.symbol.expression.BooleanExpression
import fuookami.ospf.kotlin.framework.persistence.expression.translator.*

/**
 * MyBatis 仓储实现
 * MyBatis Repository Implementation
 *
 * 提供基于 MyBatis-Plus 的仓储基类实现。
 * Provides base repository implementation based on MyBatis-Plus.
 *
 * @param E 实体类型 / Entity type
 * @param M Mapper 类型 / Mapper type
 * @property mapper MyBatis-Plus Mapper / MyBatis-Plus Mapper
 * @property resolveColumnName 列名解析函数 / Column name resolver function
 * @property nullsOrderSupport 空值排序支持 / Nulls order support
 * @property unsupportedPredicatePolicy 不支持谓词策略 / Unsupported predicate policy
 */
abstract class MybatisRepository<E : Any, M : BaseMapper<E>>(
    protected val mapper: M,
    protected val resolveColumnName: MybatisColumnNameResolver,
    protected val nullsOrderSupport: NullsOrderSupport = NullsOrderSupport.Auto,
    protected val unsupportedPredicatePolicy: UnsupportedPredicatePolicy = UnsupportedPredicatePolicy.AlwaysFalse
) : ExpressionRepository<E> {

    private val booleanTranslator = MybatisBooleanTranslator<E>(resolveColumnName, unsupportedPredicatePolicy)
    private val orderByTranslator = MybatisOrderByTranslator<E>(resolveColumnName, nullsOrderSupport)
    private val updateTranslator = MybatisUpdateTranslator<E>(resolveColumnName)

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
        var wrapper = QueryWrapper<E>()
        wrapper = booleanTranslator.translate(wrapper, where)

        // 应用排序
        // Apply order by
        if (sortBy != null && sortBy.isNotEmpty()) {
            wrapper = orderByTranslator.apply(wrapper, sortBy)
        }

        // 应用分页
        // Apply pagination
        val limitOffsetClause = when {
            limit != null && offset != null -> "LIMIT $limit OFFSET $offset"
            limit != null -> "LIMIT $limit"
            offset != null -> "OFFSET $offset"
            else -> null
        }
        if (limitOffsetClause != null) {
            wrapper = wrapper.last(limitOffsetClause)
        }

        return mapper.selectList(wrapper)
    }

    /**
     * 统计满足条件的实体数量
     * Count entities matching condition
     *
     * @param where 查询条件 / Query condition
     * @return 实体数量 / Entity count
     */
    override fun count(where: BooleanExpression): Long {
        val wrapper = QueryWrapper<E>()
        booleanTranslator.translate(wrapper, where)
        return mapper.selectCount(wrapper)
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

        var updateWrapper = UpdateWrapper<E>()
        updateWrapper = booleanTranslator.translate(updateWrapper, where)
        updateWrapper = updateTranslator.apply(updateWrapper, assignments)

        return mapper.update(null, updateWrapper)
    }

    /**
     * 删除满足条件的实体
     * Delete entities matching condition
     *
     * @param where 删除条件 / Delete condition
     * @return 受影响的行数 / Number of affected rows
     */
    override fun delete(where: BooleanExpression): Int {
        val wrapper = QueryWrapper<E>()
        booleanTranslator.translate(wrapper, where)
        return mapper.delete(wrapper)
    }

    companion object {
        /**
         * 简单列名解析器：直接使用路径最后一部分作为列名
         * Simple column resolver: use last part of path as column name
         *
         * @return 列名解析器函数 / Column name resolver function
         */
        fun simpleColumnResolver(): MybatisColumnNameResolver = { path: String ->
            path.substringAfterLast(".")
        }
    }
}
