/**
 * Ktorm 排序翻译器
 * Ktorm Order By Translator
 *
 * 将 SortBy 翻译为 Ktorm ORDER BY 子句。
 * Translates SortBy to Ktorm ORDER BY clause.
 */
package fuookami.ospf.kotlin.framework.persistence.expression.translator

import fuookami.ospf.kotlin.framework.persistence.expression.*
import org.ktorm.dsl.*

/**
 * Ktorm 排序翻译器
 * Ktorm Order By Translator
 *
 * 将 SortBy 模型翻译为 Ktorm 排序表达式。
 * Translates SortBy model to Ktorm order expressions.
 *
 * @property meta 实体元数据 / Entity metadata
 * @property nullsOrderSupport 空值排序支持检测 / Nulls order support detection
 */
class KtormOrderByTranslator(
    private val meta: EntityMeta<*>,
    private val nullsOrderSupport: NullsOrderSupport = NullsOrderSupport.Auto
) {
    /**
     * 应用排序到查询
     * Apply sort to query
     */
    fun apply(query: Query, sortBy: SortBy): Query {
        if (sortBy.isEmpty()) return query

        var result = query
        for (item in sortBy.items) {
            result = applyItem(result, item)
        }
        return result
    }

    private fun applyItem(query: Query, item: SortItem): Query {
        val column = meta.resolveColumn(item.path) ?: throw IllegalArgumentException(
            "Cannot resolve path: ${item.path}"
        )

        // 基础排序不支持 NULLS FIRST/LAST（需要方言扩展）
        // Basic sorting doesn't support NULLS FIRST/LAST (requires dialect extension)
        val orderExpr = when (item.direction) {
            SortDirection.Asc -> column.asc()
            SortDirection.Desc -> column.desc()
        }

        return query.orderBy(orderExpr)
    }
}