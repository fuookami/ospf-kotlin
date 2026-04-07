/**
 * MongoDB 排序翻译器
 * MongoDB Order By Translator
 *
 * 将 SortBy 翻译为 MongoDB Bson 排序。
 * Translates SortBy to MongoDB Bson sort.
 */
package fuookami.ospf.kotlin.framework.persistence.expression.translator

import fuookami.ospf.kotlin.framework.persistence.expression.*
import org.bson.conversions.Bson
import com.mongodb.client.model.Sorts

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
     */
    fun translate(sortBy: SortBy?): Bson? {
        if (sortBy == null || sortBy.isEmpty()) return null

        val sorts = sortBy.items.mapNotNull { translateItem(it) }
        if (sorts.isEmpty()) return null

        return Sorts.orderBy(sorts)
    }

    private fun translateItem(item: SortItem): Bson? {
        val field = resolveFieldName(item.path) ?: return null

        return when (item.direction) {
            SortDirection.Asc -> Sorts.ascending(field)
            SortDirection.Desc -> Sorts.descending(field)
        }
    }
}