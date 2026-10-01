/**
 * Ktorm 排序翻译器 / Ktorm Order By Translator
 *
 * 将 SortBy 翻译为 Ktorm ORDER BY 子句。
 * Translates SortBy to Ktorm ORDER BY clause.
*/
package fuookami.ospf.kotlin.framework.persistence.expression.translator

import java.util.concurrent.CancellationException
import org.ktorm.dsl.*
import org.ktorm.expression.OrderByExpression
import fuookami.ospf.kotlin.framework.persistence.expression.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*

/**
 * Ktorm 排序翻译器 / Ktorm Order By Translator
 *
 * 将 SortBy 模型翻译为 Ktorm 排序表达式。 / Translates SortBy model to Ktorm order expressions.
 *
 * @property resolveColumn 列解析函数 / Column resolver function
 * @param nullsOrderSupport Compatibility hint retained for callers; explicit null placement uses rank expressions because Ktorm's order AST cannot encode it.
*/
class KtormOrderByTranslator(
    private val resolveColumn: KtormColumnResolver,
    @Suppress("UNUSED_PARAMETER")
    nullsOrderSupport: NullsOrderSupport = NullsOrderSupport.Auto
) {

    /**
     * 应用排序到查询 / Apply sort to query
     *
     * @param query Ktorm 查询对象 / Ktorm query object
     * @param sortBy 排序条件 / Sort conditions
     * @return 应用排序后的查询对象 / Query object with sort applied
    */
    fun apply(query: Query, sortBy: SortBy): Ret<Query> {
        if (sortBy.isEmpty()) return Ok(query)

        return try {
            val orders = mutableListOf<OrderByExpression>()
            for (item in sortBy.items) {
                val result = buildOrders(item)
                result.propagateFailure<Query>()?.let { return it }
                orders.addAll(result.value ?: return Failed(
                    ErrorCode.ApplicationError,
                    "Ktorm 排序翻译未返回结果 / Ktorm sort translation returned no value"
                ))
            }
            if (orders.isEmpty()) Ok(query) else Ok(query.orderBy(*orders.toTypedArray()))
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            persistenceFailure("Ktorm order translation", error)
        }
    }

    /**
     * 构建排序表达式列表 / Build order by expression list
     *
     * @param item 排序项 / Sort item
     * @return 排序表达式列表 / List of order by expressions
    */
    private fun buildOrders(item: SortItem): Ret<List<OrderByExpression>> {
        val column = resolveColumn(item.path) ?: return Failed(
            ErrorCode.IllegalArgument,
            "Cannot resolve path: ${item.path}"
        )

        val orders = mutableListOf<OrderByExpression>()

        // Ktorm's order expression cannot carry explicit NULL placement, so explicit requests use the portable rank expression.
        // 当 Ktorm 排序表达式无法携带显式空值位置时，显式请求都使用可移植的优先级表达式：
        // false < true，因此 nulls last 使用 isNull asc；nulls first 使用 isNull desc。
        // false < true, so nulls last uses isNull asc; nulls first uses isNull desc.
        if (item.nulls != null) {
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
        return Ok(orders)
    }
}
