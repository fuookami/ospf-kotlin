/**
 * Ktorm 更新翻译器
 * Ktorm Update Translator
 *
 * 将 UpdateAssignment 翻译为 Ktorm UPDATE SET 子句。
 * Translates UpdateAssignment to Ktorm UPDATE SET clause.
 */
package fuookami.ospf.kotlin.framework.persistence.expression.translator

import fuookami.ospf.kotlin.framework.persistence.expression.*
import fuookami.ospf.kotlin.math.symbol.expression.*
import org.ktorm.dsl.*
import org.ktorm.schema.ColumnDeclaring

/**
 * Ktorm 更新翻译器
 * Ktorm Update Translator
 *
 * 将 UpdateAssignments 翻译为 Ktorm 更新构建器调用。
 * Translates UpdateAssignments to Ktorm update builder calls.
 *
 * @property meta 实体元数据 / Entity metadata
 */
class KtormUpdateTranslator(
    private val meta: EntityMeta<*>
) {
    /**
     * 应用更新赋值到构建器
     * Apply update assignments to builder
     *
     * @param builder 更新构建器 / Update builder
     * @param assignments 更新赋值集合 / Update assignments
     */
    fun apply(builder: UpdateBuilder, assignments: UpdateAssignments) {
        for (item in assignments.items) {
            applyItem(builder, item)
        }
    }

    private fun applyItem(builder: UpdateBuilder, item: UpdateAssignment) {
        when (item) {
            is SetValue -> {
                val column = resolveColumn(item.path)
                builder.set(column, item.value)
            }
            is SetNull -> {
                val column = resolveColumn(item.path)
                builder.set(column, null)
            }
            is SetFromExpression -> {
                val column = resolveColumn(item.path)
                val exprValue = translateScalarExpression(item.expression)
                if (exprValue != null) {
                    builder.set(column, exprValue)
                } else {
                    throw IllegalArgumentException(
                        "Cannot translate expression for path: ${item.path}"
                    )
                }
            }
        }
    }

    private fun resolveColumn(path: String): ColumnDeclaring<*> {
        return meta.resolveColumn(path) ?: throw IllegalArgumentException(
            "Cannot resolve path: $path"
        )
    }

    /**
     * 翻译标量表达式
     * Translate scalar expression
     *
     * @param expr 标量表达式 / Scalar expression
     * @return 翻译后的值，无法翻译时返回 null / Translated value, null if cannot translate
     */
    fun translateScalarExpression(expr: ScalarExpression<*>): Any? {
        return when (expr) {
            is ScalarConstant<*> -> expr.value
            is ScalarReference<*> -> {
                // 引用需要特殊处理，使用列引用
                // Reference needs special handling, use column reference
                null  // 暂不支持字段引用作为更新值 / Field reference as update value not supported yet
            }
            is ScalarUnary<*> -> null
            is ScalarBinary<*> -> null
            is ScalarFunction<*> -> null
            is ScalarCustom<*> -> null
        }
    }
}

// ========== 便捷扩展函数 / Convenience Extension Functions ==========

/**
 * 应用 UpdateAssignments 到 UpdateBuilder
 * Apply UpdateAssignments to UpdateBuilder
 */
fun UpdateBuilder.set(
    assignments: UpdateAssignments,
    meta: EntityMeta<*>
): UpdateBuilder {
    KtormUpdateTranslator(meta).apply(this, assignments)
    return this
}