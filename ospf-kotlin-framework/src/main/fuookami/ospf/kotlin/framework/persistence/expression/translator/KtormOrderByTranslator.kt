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
import org.ktorm.schema.ColumnDeclaring

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
     *
     * @param query 查询源 / Query source
     * @param sortBy 排序定义 / Sort definition
     * @return 带排序的查询 / Query with order by
     */
    fun apply(query: QuerySource, sortBy: SortBy): QuerySource {
        if (sortBy.isEmpty()) return query

        var result = query
        for (item in sortBy.items) {
            result = applyItem(result, item)
        }
        return result
    }

    private fun applyItem(query: QuerySource, item: SortItem): QuerySource {
        val column = meta.resolveColumn(item.path) ?: throw IllegalArgumentException(
            "Cannot resolve path: ${item.path}"
        )

        val orderExpression = when (item.direction) {
            SortDirection.Asc -> column.asc()
            SortDirection.Desc -> column.desc()
        }

        // 处理空值排序
        // Handle nulls order
        val nullsOrder = if (nullsOrderSupport.isSupported(item)) {
            item.nulls
        } else {
            // 降级：忽略空值排序
            // Fallback: ignore nulls order
            null
        }

        return when (nullsOrder) {
            NullsOrder.NullsFirst -> query.orderBy(orderExpression.nullsFirst())
            NullsOrder.NullsLast -> query.orderBy(orderExpression.nullsLast())
            null -> query.orderBy(orderExpression)
        }
    }
}

// ========== 便捷扩展函数 / Convenience Extension Functions ==========

/**
 * 应用 SortBy 到 QuerySource
 * Apply SortBy to QuerySource
 */
fun QuerySource.orderBy(
    sortBy: SortBy,
    meta: EntityMeta<*>,
    nullsOrderSupport: NullsOrderSupport = NullsOrderSupport.Auto
): QuerySource {
    return KtormOrderByTranslator(meta, nullsOrderSupport).apply(this, sortBy)
}

/**
 * 创建排序表达式
 * Create order expression
 */
fun ColumnDeclaring<*>.ascWithNulls(nulls: NullsOrder? = null): OrderByExpression {
    val base = this.asc()
    return when (nulls) {
        NullsOrder.NullsFirst -> base.nullsFirst()
        NullsOrder.NullsLast -> base.nullsLast()
        null -> base
    }
}

/**
 * 创建降序排序表达式
 * Create descending order expression
 */
fun ColumnDeclaring<*>.descWithNulls(nulls: NullsOrder? = null): OrderByExpression {
    val base = this.desc()
    return when (nulls) {
        NullsOrder.NullsFirst -> base.nullsFirst()
        NullsOrder.NullsLast -> base.nullsLast()
        null -> base
    }
}