/**
 * MyBatis 更新翻译器
 * MyBatis Update Translator
 *
 * 将 UpdateAssignments 翻译为 MyBatis-Plus UPDATE SET 子句。
 * Translates UpdateAssignments to MyBatis-Plus UPDATE SET clauses.
*/
package fuookami.ospf.kotlin.framework.persistence.expression.translator

import java.util.concurrent.CancellationException
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper
import fuookami.ospf.kotlin.framework.persistence.expression.*
import fuookami.ospf.kotlin.framework.persistence.mybatis.MybatisDialect
import fuookami.ospf.kotlin.math.symbol.expression.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*

/**
 * MyBatis 更新翻译器 / MyBatis update translator
 *
 * @param T 实体类型 / Entity type
 * @property resolveColumnName 列名解析函数 / Column name resolver function
 * @param dialect SQL 方言 / SQL dialect
 */
class MybatisUpdateTranslator<T : Any>(
    private val resolveColumnName: MybatisColumnNameResolver,
    dialect: MybatisDialect = MybatisDialect.Portable
) {

    private data class TranslatedAssignment(
        val column: String,
        val value: Any? = null,
        val expression: MybatisScalarSql? = null
    )

    private val scalarTranslator = MybatisScalarTranslator(
        resolveColumnName = resolveColumnName,
        unsupportedPredicatePolicy = UnsupportedPredicatePolicy.FailFast,
        dialect = dialect
    )

    /**
     * 翻译更新并应用到 Wrapper / Translate updates and apply them to the wrapper
     *
     * @param wrapper MyBatis-Plus 更新 Wrapper / MyBatis-Plus update wrapper
     * @param assignments 更新赋值列表 / Update assignment list
     * @return 应用更新后的结果 / Result containing the updated wrapper
     */
    fun translate(wrapper: UpdateWrapper<T>, assignments: UpdateAssignments): Ret<UpdateWrapper<T>> {
        return try {
            translateInternal(wrapper, assignments)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            persistenceFailure("MyBatis update translation", error)
        }
    }

    private fun translateInternal(
        wrapper: UpdateWrapper<T>,
        assignments: UpdateAssignments
    ): Ret<UpdateWrapper<T>> {
        if (assignments.isEmpty()) return Ok(wrapper)

        val translatedAssignments = mutableListOf<TranslatedAssignment>()
        for (item in assignments.items) {
            val column = resolveColumnName(item.path)
                ?: return assignmentFailure(item.path, "column was not resolved")
            when (item) {
                is SetValue -> translatedAssignments += TranslatedAssignment(
                    column = column,
                    value = MybatisValueConverter.convert(item.value)
                )
                is SetNull -> translatedAssignments += TranslatedAssignment(column = column)
                is SetFromExpression -> {
                    val translated = scalarTranslator.translate(item.expression)
                    translated.propagateFailure<UpdateWrapper<T>>()?.let { return it }
                    val expression = translated.value
                        ?: return assignmentFailure(item.path, "expression is not supported")
                    translatedAssignments += TranslatedAssignment(
                        column = column,
                        expression = expression
                    )
                }
            }
        }

        var result = wrapper
        for (assignment in translatedAssignments) {
            val expression = assignment.expression
            result = if (expression == null) {
                result.set(assignment.column, assignment.value)
            } else {
                result.setSql(
                    "${assignment.column} = ${expression.sql}",
                    *expression.params.toTypedArray()
                )
            }
        }
        return Ok(result)
    }

    private fun assignmentFailure(path: String, reason: String): Ret<UpdateWrapper<T>> {
        return Failed(
            ErrorCode.IllegalArgument,
            "更新赋值翻译失败：字段 $path：$reason / Update assignment translation failed: field $path: $reason"
        )
    }
}
