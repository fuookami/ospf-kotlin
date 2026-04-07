/**
 * MyBatis 仓储实现
 * MyBatis Repository Implementation
 *
 * 提供基于 MyBatis-Plus 的仓储实现。
 * Provides MyBatis-Plus-based repository implementation.
 */
package fuookami.ospf.kotlin.framework.persistence.expression

import fuookami.ospf.kotlin.math.symbol.expression.BooleanExpression
import fuookami.ospf.kotlin.framework.persistence.expression.translator.*
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper
import com.baomidou.mybatisplus.core.mapper.BaseMapper

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
 */
abstract class MybatisRepository<E : Any, M : BaseMapper<E>>(
    protected val mapper: M,
    protected val resolveColumnName: MybatisColumnNameResolver,
    protected val nullsOrderSupport: NullsOrderSupport = NullsOrderSupport.Auto
) : ExpressionRepository<E> {

    private val booleanTranslator = MybatisBooleanTranslator<E>(resolveColumnName)
    private val orderByTranslator = MybatisOrderByTranslator<E>(resolveColumnName, nullsOrderSupport)
    private val updateTranslator = MybatisUpdateTranslator<E>(resolveColumnName)

    override fun find(where: BooleanExpression): List<E> {
        return find(where, null, null, null)
    }

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
        if (limit != null) {
            wrapper = wrapper.last("LIMIT $limit")
        }
        if (offset != null) {
            wrapper = wrapper.last("OFFSET $offset")
        }

        return mapper.selectList(wrapper)
    }

    override fun count(where: BooleanExpression): Long {
        val wrapper = QueryWrapper<E>()
        booleanTranslator.translate(wrapper, where)
        return mapper.selectCount(wrapper)
    }

    override fun update(where: BooleanExpression, assignments: UpdateAssignments): Int {
        if (assignments.isEmpty()) return 0

        val queryWrapper = QueryWrapper<E>()
        booleanTranslator.translate(queryWrapper, where)

        var updateWrapper = UpdateWrapper<E>()
        updateWrapper = updateTranslator.apply(updateWrapper, assignments)

        return mapper.update(null, updateWrapper)
    }

    override fun delete(where: BooleanExpression): Int {
        val wrapper = QueryWrapper<E>()
        booleanTranslator.translate(wrapper, where)
        return mapper.delete(wrapper)
    }

    companion object {
        /**
         * 简单列名解析器：直接使用路径最后一部分作为列名
         * Simple column resolver: use last part of path as column name
         */
        fun simpleColumnResolver(): MybatisColumnNameResolver = { path ->
            path.substringAfterLast(".")
        }
    }
}