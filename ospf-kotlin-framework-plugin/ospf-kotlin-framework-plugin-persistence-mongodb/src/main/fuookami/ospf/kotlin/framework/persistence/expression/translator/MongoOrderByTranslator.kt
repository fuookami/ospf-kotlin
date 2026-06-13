/**
 * MongoDB 排序翻译器
 * MongoDB Order By Translator
 *
 * 将 SortBy 翻译为 MongoDB Bson 排序。
 * Translates SortBy to MongoDB Bson sort.
 */
package fuookami.ospf.kotlin.framework.persistence.expression.translator

import com.mongodb.client.model.Sorts
import org.bson.conversions.Bson
import fuookami.ospf.kotlin.framework.persistence.expression.*

/**
 * MongoDB 排序翻译器
 * MongoDB Order By Translator
 *
 * 将 SortBy 模型翻译为 MongoDB 排序表达式。
 * Translates SortBy model to MongoDB sort expressions.
 *
 * @property resolveFieldName 字段名解析函数 / Field name resolver function
 */
class MongoOrderByTranslator(
    private val resolveFieldName: MongoFieldNameResolver
) {
    /**
     * 翻译排序为 Bson
     * Translate sort to Bson
     *
     * @param sortBy 排序条件（可选）/ Sort conditions (optional)
     * @return Bson 排序表达式，为空时返回 null / Bson sort expression, or null if empty
     */
    fun translate(sortBy: SortBy?): Bson? {
        if (sortBy == null || sortBy.isEmpty()) return null

        val sorts = sortBy.items.mapNotNull { translateItem(it) }
        if (sorts.isEmpty()) return null

        return Sorts.orderBy(sorts)
    }

    /**
     * 翻译单个排序项为 Bson
     * Translate single sort item to Bson
     *
     * @param item 排序项 / Sort item
     * @return Bson 排序表达式，字段未解析时返回 null / Bson sort expression, or null if field unresolved
     */
    private fun translateItem(item: SortItem): Bson? {
        val field = resolveFieldName(item.path) ?: return null

        return when (item.direction) {
            SortDirection.Asc -> Sorts.ascending(field)
            SortDirection.Desc -> Sorts.descending(field)
        }
    }
}