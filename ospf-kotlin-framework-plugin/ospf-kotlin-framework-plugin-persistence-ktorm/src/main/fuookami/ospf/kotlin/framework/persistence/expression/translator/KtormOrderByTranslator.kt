/**
 * Ktorm 排序翻译器
 * Ktorm Order By Translator
 *
 * 将 SortBy 翻译为 Ktorm ORDER BY 子句。
 * Translates SortBy to Ktorm ORDER BY clause.
 */
package fuookami.ospf.kotlin.framework.persistence.expression.translator

import fuookami.ospf.kotlin.framework.persistence.expression.*
import org.ktorm.expression.OrderByExpression
import org.ktorm.dsl.*

/**
 * Ktorm 排序翻译器
 * Ktorm Order By Translator
 *
 * 将 SortBy 模型翻译为 Ktorm 排序表达式。
 * Translates SortBy model to Ktorm order expressions.
 *
 * @property resolveColumn 列解析函数 / Column resolver function
 * @property nullsOrderSupport 空值排序支持检测 / Nulls order support detection
 */
class KtormOrderByTranslator(
    private val resolveColumn: KtormColumnResolver,
    private val nullsOrderSupport: NullsOrderSupport = NullsOrderSupport.Auto
) {
    /**
     * 应用排序到查询
     * Apply sort to query
     */
    fun apply(query: Query, sortBy: SortBy): Query {
        if (sortBy.isEmpty()) return query

        val orders = mutableListOf<OrderByExpression>()
        for (item in sortBy.items) {
            orders.addAll(buildOrders(item))
        }
        if (orders.isEmpty()) return query
        return query.orderBy(*orders.toTypedArray())
    }

    private fun buildOrders(item: SortItem): List<OrderByExpression> {
        val column = resolveColumn(item.path) ?: throw IllegalArgumentException(
            "Cannot resolve path: ${item.path}"
        )

        val orders = mutableListOf<OrderByExpression>()

        // 当数据库不支持 NULLS FIRST/LAST 时，使用布尔排序降级：
        // false < true，因此 nulls last 使用 isNull asc；nulls first 使用 isNull desc。
        // When NULLS FIRST/LAST is not supported, fallback to boolean ordering:
        // false < true, so nulls last uses isNull asc; nulls first uses isNull desc.
        if (item.nulls != null && !nullsOrderSupport.isSupported(item)) {
            val nullOrderExpr = when (item.nulls) {
                NullsOrder.NullsFirst -> column.isNull().desc()
                NullsOrder.NullsLast -> column.isNull().asc()
                else -> column.isNull().asc()
            }
            orders.add(nullOrderExpr)
        }

        val orderExpr = when (item.direction) {
            SortDirection.Asc -> column.asc()
            SortDirection.Desc -> column.desc()
        }
        orders.add(orderExpr)
        return orders
    }
}
