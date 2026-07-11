/**
 * MyBatis 排序翻译器
 * MyBatis Order By Translator
 *
 * 将 SortBy 翻译为 MyBatis-Plus ORDER BY 子句。
 * Translates SortBy to MyBatis-Plus ORDER BY clause.
*/
package fuookami.ospf.kotlin.framework.persistence.expression.translator

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper
import fuookami.ospf.kotlin.framework.persistence.expression.*

/**
 * MyBatis 排序翻译器
 * MyBatis Order By Translator
 *
 * 将 SortBy 模型翻译为 MyBatis-Plus 排序表达式。
 * Translates SortBy model to MyBatis-Plus order expressions.
 *
 * @param T 实体类型 / Entity type
 * @property resolveColumnName 列名解析函数 / Column name resolver function
 * @property nullsOrderSupport 空值排序支持检测 / Nulls order support detection
*/
class MybatisOrderByTranslator<T : Any>(
    private val resolveColumnName: MybatisColumnNameResolver,
    private val nullsOrderSupport: NullsOrderSupport = NullsOrderSupport.Auto
) {

    /**
     * 应用排序到 Wrapper
     * Apply sort to wrapper
     *
     * @param wrapper MyBatis-Plus 查询 Wrapper / MyBatis-Plus query wrapper
     * @param sortBy 排序条件 / Sort conditions
     * @return 应用排序后的 QueryWrapper / QueryWrapper with sort applied
    */
    fun apply(wrapper: QueryWrapper<T>, sortBy: SortBy): QueryWrapper<T> {
        if (sortBy.isEmpty()) return wrapper

        var result = wrapper
        for (item in sortBy.items) {
            result = applyItem(result, item)
        }
        return result
    }

    /**
     * 应用单个排序项到 Wrapper
     * Apply a single sort item to wrapper
     *
     * @param wrapper MyBatis-Plus 查询 Wrapper / MyBatis-Plus query wrapper
     * @param item 排序项 / Sort item
     * @return 应用排序后的 QueryWrapper / QueryWrapper with sort item applied
    */
    private fun applyItem(wrapper: QueryWrapper<T>, item: SortItem): QueryWrapper<T> {
        val column = resolveColumnName(item.path) ?: return wrapper

        // 基础排序
        // Basic sorting
        return when (item.direction) {
            SortDirection.Asc -> wrapper.orderByAsc(column)
            SortDirection.Desc -> wrapper.orderByDesc(column)
        }
    }
}