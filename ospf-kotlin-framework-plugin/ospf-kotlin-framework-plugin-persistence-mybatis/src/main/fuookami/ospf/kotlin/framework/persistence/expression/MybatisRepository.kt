/**
 * MyBatis 仓储实现
 * MyBatis Repository Implementation
 *
 * 提供基于 MyBatis-Plus 的仓储实现。
 * Provides MyBatis-Plus-based repository implementation.
*/
package fuookami.ospf.kotlin.framework.persistence.expression

import java.util.concurrent.CancellationException
import fuookami.ospf.kotlin.framework.persistence.expression.translator.*
import fuookami.ospf.kotlin.framework.persistence.mybatis.MybatisDialect
import fuookami.ospf.kotlin.math.symbol.expression.BooleanExpression
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper
import com.baomidou.mybatisplus.core.mapper.BaseMapper

/**
 * MyBatis 仓储实现
 * MyBatis Repository Implementation
 *
 * 提供基于 MyBatis-Plus 的仓储基类实现。 / Provides base repository implementation based on MyBatis-Plus.
 *
 * @param E 实体类型 / Entity type
 * @param M Mapper 类型 / Mapper type
 * @property mapper MyBatis-Plus Mapper / MyBatis-Plus Mapper
 * @property resolveColumnName 列名解析函数 / Column name resolver function
 * @property nullsOrderSupport 空值排序支持 / Nulls order support
 * @property unsupportedPredicatePolicy 不支持谓词策略 / Unsupported predicate policy
 * @property dialect SQL 方言 / SQL dialect
*/
abstract class MybatisRepository<E : Any, M : BaseMapper<E>>(
    protected val mapper: M,
    protected val resolveColumnName: MybatisColumnNameResolver,
    protected val nullsOrderSupport: NullsOrderSupport = NullsOrderSupport.Auto,
    protected val unsupportedPredicatePolicy: UnsupportedPredicatePolicy = UnsupportedPredicatePolicy.FailFast,
    protected val dialect: MybatisDialect = MybatisDialect.Portable
) : ExpressionRepository<E> {

    private val booleanTranslator = MybatisBooleanTranslator<E>(
        resolveColumnName = resolveColumnName,
        unsupportedPredicatePolicy = unsupportedPredicatePolicy,
        dialect = dialect
    )
    private val orderByTranslator = MybatisOrderByTranslator<E>(
        resolveColumnName = resolveColumnName,
        nullsOrderSupport = nullsOrderSupport,
        dialect = dialect
    )
    private val updateTranslator = MybatisUpdateTranslator<E>(resolveColumnName, dialect)

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
        return try {
            val translated = booleanTranslator.translate(QueryWrapper<E>(), where)
            translated.propagateFailure<List<E>>()?.let { return it }
            var wrapper = translated.value
                ?: return Failed(ErrorCode.ApplicationError, "MyBatis 查询条件翻译未返回结果 / MyBatis predicate translation returned no value")

            if (limit == 0) return Ok(emptyList())

            val suffixes = mutableListOf<String>()
            if (sortBy != null && sortBy.isNotEmpty()) {
                when (val sorted = orderByTranslator.translate(sortBy)) {
                    is Ok -> sorted.value.sql.takeIf { it.isNotBlank() }?.let { suffixes += "ORDER BY $it" }
                    is Failed -> return Failed(sorted.error)
                    is Fatal -> return Fatal(sorted.errors)
                }
            }

            val pagination = when (val result = dialect.pagination(limit, offset)) {
                is Ok -> result.value.sql
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
            if (pagination.isNotBlank()) {
                suffixes += pagination
            }
            if (suffixes.isNotEmpty()) {
                wrapper = wrapper.last(suffixes.joinToString(" "))
            }

            Ok(mapper.selectList(wrapper))
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            persistenceFailure("MyBatis find", error)
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
            val translated = booleanTranslator.translate(QueryWrapper<E>(), where)
            translated.propagateFailure<Long>()?.let { return it }
            val wrapper = translated.value
                ?: return Failed(ErrorCode.ApplicationError, "MyBatis 计数条件翻译未返回结果 / MyBatis count predicate translation returned no value")
            Ok(mapper.selectCount(wrapper))
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            persistenceFailure("MyBatis count", error)
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
            val whereResult = booleanTranslator.translate(UpdateWrapper<E>(), where)
            whereResult.propagateFailure<Int>()?.let { return it }
            val updateWrapper = whereResult.value
                ?: return Failed(ErrorCode.ApplicationError, "MyBatis 更新条件翻译未返回结果 / MyBatis update predicate translation returned no value")
            val updateResult = updateTranslator.translate(updateWrapper, assignments)
            updateResult.propagateFailure<Int>()?.let { return it }
            val translatedWrapper = updateResult.value
                ?: return Failed(ErrorCode.ApplicationError, "MyBatis 更新赋值翻译未返回结果 / MyBatis update assignment translation returned no value")
            Ok(mapper.update(null, translatedWrapper))
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            persistenceFailure("MyBatis update", error)
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
            val translated = booleanTranslator.translate(QueryWrapper<E>(), where)
            translated.propagateFailure<Int>()?.let { return it }
            val wrapper = translated.value
                ?: return Failed(ErrorCode.ApplicationError, "MyBatis 删除条件翻译未返回结果 / MyBatis delete predicate translation returned no value")
            Ok(mapper.delete(wrapper))
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            persistenceFailure("MyBatis delete", error)
        }
    }

    companion object {
        /**
         * 简单列名解析器：直接使用路径最后一部分作为列名 / Simple column resolver: use last part of path as column name
         *
         * @return 列名解析器函数 / Column name resolver function
        */
        fun simpleColumnResolver(): MybatisColumnNameResolver = MybatisColumnNameResolver { path: String ->
            path.substringAfterLast(".")
        }
    }
}
