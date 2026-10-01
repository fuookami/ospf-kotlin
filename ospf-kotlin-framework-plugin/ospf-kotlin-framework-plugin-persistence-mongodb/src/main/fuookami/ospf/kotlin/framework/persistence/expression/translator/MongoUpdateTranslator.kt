/**
 * MongoDB 更新翻译器
 * MongoDB Update Translator
 *
 * 将 UpdateAssignment 翻译为 MongoDB Bson 更新操作。
 * Translates UpdateAssignment to MongoDB Bson update operations.
*/
package fuookami.ospf.kotlin.framework.persistence.expression.translator

import java.util.concurrent.CancellationException
import com.mongodb.client.model.Updates
import org.bson.conversions.Bson
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.symbol.expression.*
import fuookami.ospf.kotlin.framework.persistence.expression.*

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
     * 翻译更新为 Bson / Translate update to Bson
     *
     * @param assignments 更新赋值列表 / Update assignment list
     * @return Bson 更新表达式，为空时返回 null；任何赋值无法翻译时返回失败 / Bson update expression, null if empty, or failure when an assignment cannot be translated
    */
    fun translate(assignments: UpdateAssignments): Ret<Bson?> {
        return try {
            translateInternal(assignments)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            persistenceFailure("MongoDB update translation", error)
        }
    }

    private fun translateInternal(assignments: UpdateAssignments): Ret<Bson?> {
        if (assignments.isEmpty()) return Ok(null)

        val updates = mutableListOf<Bson>()
        for (item in assignments.items) {
            val translated = translateItem(item)
            translated.propagateFailure<Bson?>()?.let { return it }
            val update = translated.value
                ?: return assignmentFailure(item.path, "assignment did not produce an update")
            updates += update
        }

        return Ok(Updates.combine(updates))
    }

    /**
     * 翻译单个更新赋值项为 Bson / Translate single update assignment item to Bson
     *
     * @param item 更新赋值项 / Update assignment item
     * @return Bson 更新表达式 / Bson update expression
    */
    private fun translateItem(item: UpdateAssignment): Ret<Bson?> {
        return when (item) {
            is SetValue -> translateSetValue(item)
            is SetNull -> translateSetNull(item)
            is SetFromExpression -> translateSetFromExpression(item)
        }
    }

    /**
     * 翻译设置值赋值项 / Translate set-value assignment item
     *
     * @param item 设置值赋值项 / Set-value assignment item
     * @return Bson 更新表达式 / Bson update expression
    */
    private fun translateSetValue(item: SetValue): Ret<Bson?> {
        val field = resolveFieldName(item.path)
            ?: return assignmentFailure(item.path, "field was not resolved")
        return Ok(Updates.set(field, item.value))
    }

    /**
     * 翻译设置空值赋值项 / Translate set-null assignment item
     *
     * @param item 设置空值赋值项 / Set-null assignment item
     * @return Bson 更新表达式 / Bson update expression
    */
    private fun translateSetNull(item: SetNull): Ret<Bson?> {
        val field = resolveFieldName(item.path)
            ?: return assignmentFailure(item.path, "field was not resolved")
        return Ok(Updates.set(field, null))
    }

    /**
     * 翻译表达式赋值项 / Translate set-from-expression assignment item
     *
     * @param item 表达式赋值项 / Set-from-expression assignment item
     * @return Bson 更新表达式 / Bson update expression
    */
    private fun translateSetFromExpression(item: SetFromExpression): Ret<Bson?> {
        val field = resolveFieldName(item.path)
            ?: return assignmentFailure(item.path, "field was not resolved")
        val expression = item.expression as? ScalarConstant<*>
            ?: return assignmentFailure(item.path, "expression is not supported")
        return Ok(Updates.set(field, expression.value))
    }

    private fun assignmentFailure(path: String, reason: String): Ret<Bson?> {
        return Failed(
            ErrorCode.IllegalArgument,
            "更新赋值翻译失败：字段 $path：$reason / Update assignment translation failed: field $path: $reason"
        )
    }
}
