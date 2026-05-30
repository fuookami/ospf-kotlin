/**
 * MyBatis 更新翻译器
 * MyBatis Update Translator
 *
 * 将 UpdateAssignment 翻译为 MyBatis-Plus UPDATE SET 子句。
 * Translates UpdateAssignment to MyBatis-Plus UPDATE SET clause.
 */
package fuookami.ospf.kotlin.framework.persistence.expression.translator

import fuookami.ospf.kotlin.framework.persistence.expression.*
import fuookami.ospf.kotlin.math.symbol.expression.*
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper

/**
 * MyBatis 更新翻译器
 * MyBatis Update Translator
 *
 * 将 UpdateAssignments 翻译为 MyBatis-Plus 更新构建器调用。
 * Translates UpdateAssignments to MyBatis-Plus update builder calls.
 *
 * @param T 实体类型 / Entity type
 * @property resolveColumnName 列名解析函数 / Column name resolver function
 */
class MybatisUpdateTranslator<T : Any>(
    private val resolveColumnName: MybatisColumnNameResolver
) {
    /**
     * 应用更新到 Wrapper
     * Apply update to wrapper
     *
     * @param wrapper MyBatis-Plus 更新 Wrapper / MyBatis-Plus update wrapper
     * @param assignments 更新赋值列表 / Update assignment list
     * @return 应用更新后的 UpdateWrapper / UpdateWrapper with updates applied
     */
    fun apply(wrapper: UpdateWrapper<T>, assignments: UpdateAssignments): UpdateWrapper<T> {
        if (assignments.isEmpty()) return wrapper

        var result = wrapper
        for (item in assignments.items) {
            result = when (item) {
                is SetValue -> applySetValue(result, item)
                is SetNull -> applySetNull(result, item)
                is SetFromExpression -> applySetFromExpression(result, item)
            }
        }
        return result
    }

    private fun applySetValue(wrapper: UpdateWrapper<T>, item: SetValue): UpdateWrapper<T> {
        val column = resolveColumnName(item.path) ?: return wrapper
        return wrapper.set(column, item.value)
    }

    private fun applySetNull(wrapper: UpdateWrapper<T>, item: SetNull): UpdateWrapper<T> {
        val column = resolveColumnName(item.path) ?: return wrapper
        return wrapper.set(column, null)
    }

    private fun applySetFromExpression(wrapper: UpdateWrapper<T>, item: SetFromExpression): UpdateWrapper<T> {
        val column = resolveColumnName(item.path) ?: return wrapper

        // 目前只支持 ScalarConstant
        // Currently only supports ScalarConstant
        val exprValue = (item.expression as? ScalarConstant<*>)?.value
        return if (exprValue != null) {
            wrapper.set(column, exprValue)
        } else {
            // 对于复杂表达式，可以使用 setSql
            // For complex expressions, can use setSql
            wrapper.setSql("$column = ${item.expression}")
        }
    }
}