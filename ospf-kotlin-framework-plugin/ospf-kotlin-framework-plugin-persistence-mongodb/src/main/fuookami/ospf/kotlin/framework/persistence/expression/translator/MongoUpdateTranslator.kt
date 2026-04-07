/**
 * MongoDB 更新翻译器
 * MongoDB Update Translator
 *
 * 将 UpdateAssignment 翻译为 MongoDB Bson 更新操作。
 * Translates UpdateAssignment to MongoDB Bson update operations.
 */
package fuookami.ospf.kotlin.framework.persistence.expression.translator

import fuookami.ospf.kotlin.framework.persistence.expression.*
import fuookami.ospf.kotlin.math.symbol.expression.*
import org.bson.conversions.Bson
import com.mongodb.client.model.Updates

/**
 * MongoDB 更新翻译器
 * MongoDB Update Translator
 *
 * 将 UpdateAssignments 翻译为 MongoDB 更新操作。
 * Translates UpdateAssignments to MongoDB update operations.
 *
 * @property resolveFieldName 字段名解析函数 / Field name resolver function
 */
class MongoUpdateTranslator(
    private val resolveFieldName: MongoFieldNameResolver
) {
    /**
     * 翻译更新为 Bson
     * Translate update to Bson
     */
    fun translate(assignments: UpdateAssignments): Bson? {
        if (assignments.isEmpty()) return null

        val updates = assignments.items.mapNotNull { translateItem(it) }
        if (updates.isEmpty()) return null

        return Updates.combine(updates)
    }

    private fun translateItem(item: UpdateAssignment): Bson? {
        return when (item) {
            is SetValue -> translateSetValue(item)
            is SetNull -> translateSetNull(item)
            is SetFromExpression -> translateSetFromExpression(item)
        }
    }

    private fun translateSetValue(item: SetValue): Bson? {
        val field = resolveFieldName(item.path) ?: return null
        return Updates.set(field, item.value)
    }

    private fun translateSetNull(item: SetNull): Bson? {
        val field = resolveFieldName(item.path) ?: return null
        return Updates.set(field, null)
    }

    private fun translateSetFromExpression(item: SetFromExpression): Bson? {
        val field = resolveFieldName(item.path) ?: return null

        // 目前只支持 ScalarConstant
        // Currently only supports ScalarConstant
        val exprValue = (item.expression as? ScalarConstant<*>)?.value
        return if (exprValue != null) {
            Updates.set(field, exprValue)
        } else {
            // 对于复杂表达式，可能需要使用 $expr
            // For complex expressions, may need to use $expr
            null
        }
    }
}