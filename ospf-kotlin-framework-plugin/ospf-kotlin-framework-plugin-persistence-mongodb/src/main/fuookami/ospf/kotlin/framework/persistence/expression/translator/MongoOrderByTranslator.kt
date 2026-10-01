/**
 * MongoDB 排序翻译器
 * MongoDB Order By Translator
 *
 * 将 SortBy 翻译为 MongoDB Bson 排序。
 * Translates SortBy to MongoDB Bson sort.
*/
package fuookami.ospf.kotlin.framework.persistence.expression.translator

import java.util.concurrent.CancellationException
import com.mongodb.client.model.Sorts
import org.bson.conversions.Bson
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
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
     * 翻译排序为 Bson / Translate sort to Bson
     *
     * @param sortBy 排序条件（可选）/ Sort conditions (optional)
     * @return Bson 排序表达式，为空时返回 null；字段未解析时返回失败 / Bson sort expression, null when empty, or failure for an unresolved field
    */
    fun translate(sortBy: SortBy?): Ret<Bson?> {
        return try {
            translateInternal(sortBy)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            persistenceFailure("MongoDB sort translation", error)
        }
    }

    private fun translateInternal(sortBy: SortBy?): Ret<Bson?> {
        if (sortBy == null || sortBy.isEmpty()) return Ok(null)

        val sorts = mutableListOf<Bson>()
        for (item in sortBy.items) {
            val translated = translateItem(item)
            translated.propagateFailure<Bson?>()?.let { return it }
            val sort = translated.value
                ?: return sortFailure(item.path, "sort did not produce an expression")
            sorts += sort
        }

        return Ok(Sorts.orderBy(sorts))
    }

    /**
     * 翻译单个排序项为 Bson / Translate single sort item to Bson
     *
     * @param item 排序项 / Sort item
     * @return Bson 排序表达式，字段未解析时返回 null / Bson sort expression, or null if field unresolved
    */
    private fun translateItem(item: SortItem): Ret<Bson?> {
        val field = resolveFieldName(item.path)
            ?: return sortFailure(item.path, "field was not resolved")

        return Ok(when (item.direction) {
            SortDirection.Asc -> Sorts.ascending(field)
            SortDirection.Desc -> Sorts.descending(field)
        })
    }

    private fun sortFailure(path: String, reason: String): Ret<Bson?> {
        return Failed(
            ErrorCode.IllegalArgument,
            "排序字段翻译失败：字段 $path：$reason / Sort field translation failed: field $path: $reason"
        )
    }
}
