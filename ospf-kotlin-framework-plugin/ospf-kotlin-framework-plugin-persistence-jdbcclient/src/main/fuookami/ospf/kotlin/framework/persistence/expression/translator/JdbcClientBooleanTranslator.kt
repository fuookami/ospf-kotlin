/**
 * JdbcClient 布尔表达式翻译器 / JdbcClient boolean expression translator
 *
 * 先翻译整棵谓词树，再在根节点应用 unsupported 策略，避免 NOT 改变失败条件的含义。
 * Translates the complete predicate tree before applying the unsupported policy at its root, so NOT cannot invert a failed condition.
 */
package fuookami.ospf.kotlin.framework.persistence.expression.translator

import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.Trivalent
import fuookami.ospf.kotlin.math.symbol.expression.*
import fuookami.ospf.kotlin.framework.persistence.expression.*
import fuookami.ospf.kotlin.framework.persistence.jdbcclient.*

private data class JdbcClientBooleanTranslation(
    val fragment: JdbcClientSqlFragment? = null,
    val unsupported: UnsupportedPredicateDetail? = null
)

/**
 * 布尔表达式翻译器 / Boolean expression translator
 *
 * @property resolveColumnName 属性路径解析器 / Property path resolver
 * @property dialect SQL 方言 / SQL dialect
 * @property unsupportedPredicatePolicy 不支持谓词的处理策略 / Unsupported predicate policy
 */
class JdbcClientBooleanTranslator(
    private val resolveColumnName: JdbcClientColumnNameResolver,
    private val dialect: JdbcClientDialect,
    private val unsupportedPredicatePolicy: UnsupportedPredicatePolicy = UnsupportedPredicatePolicy.FailFast
) {
    private val scalarTranslator = JdbcClientScalarTranslator(resolveColumnName, dialect)

    /**
     * 翻译布尔表达式 / Translate a boolean expression
     *
     * @param expression 布尔表达式 / Boolean expression
     * @return 参数化谓词 SQL / Parameterized predicate SQL
     */
    fun translate(expression: BooleanExpression): Ret<JdbcClientSqlFragment> {
        return when (val result = translateInternal(expression)) {
            is Failed -> Failed(result.error)
            is Fatal -> Fatal(result.errors)
            is Ok -> {
                val translation = result.value
                if (translation.unsupported == null) {
                    Ok(translation.fragment ?: JdbcClientSqlFragment("1 = 1"))
                } else if (unsupportedPredicatePolicy == UnsupportedPredicatePolicy.AlwaysFalse) {
                    Ok(JdbcClientSqlFragment("1 = 0"))
                } else {
                    Failed(translation.unsupported.toError())
                }
            }
        }
    }

    private fun translateInternal(expression: BooleanExpression): Ret<JdbcClientBooleanTranslation> {
        return when (expression) {
            is BooleanConstant -> Ok(
                JdbcClientBooleanTranslation(
                    fragment = JdbcClientSqlFragment(
                        when (expression.value) {
                            Trivalent.True -> "1 = 1"
                            Trivalent.False -> "1 = 0"
                            Trivalent.Unknown -> "1 = NULL"
                        }
                    )
                )
            )
            is Comparison<*> -> translateComparison(expression)
            is InExpression<*> -> translateIn(expression)
            is PatternMatch<*> -> translatePattern(expression)
            is NullCheck -> translateNullCheck(expression)
            is AndExpression -> translateLogical(expression.operands, "AND")
            is OrExpression -> translateLogical(expression.operands, "OR")
            is NotExpression -> translateNot(expression)
            is BooleanCustom -> unsupported(expression, "自定义布尔表达式不支持 / Custom boolean expressions are not supported")
        }
    }

    private fun translateComparison(expression: Comparison<*>): Ret<JdbcClientBooleanTranslation> {
        val leftExpression = expression.left
        val rightExpression = expression.right
        val nullSide = when {
            leftExpression is ScalarConstant<*> && leftExpression.value == null -> rightExpression
            rightExpression is ScalarConstant<*> && rightExpression.value == null -> leftExpression
            else -> null
        }
        if (nullSide != null && expression.operator in setOf(ComparisonOperator.Eq, ComparisonOperator.Ne)) {
            val value = when (val result = scalarTranslator.translate(nullSide)) {
                is Ok -> result.value ?: return unsupported(expression, "比较包含不支持的标量表达式 / Comparison contains an unsupported scalar expression")
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
            val op = if (expression.operator == ComparisonOperator.Eq) "IS NULL" else "IS NOT NULL"
            return Ok(JdbcClientBooleanTranslation(JdbcClientSqlFragment("${value.sql} $op", value.parameters)))
        }

        val leftResult = scalarTranslator.translate(expression.left)
        val rightResult = scalarTranslator.translate(expression.right)
        if (leftResult is Failed) return Failed(leftResult.error)
        if (leftResult is Fatal) return Fatal(leftResult.errors)
        if (rightResult is Failed) return Failed(rightResult.error)
        if (rightResult is Fatal) return Fatal(rightResult.errors)
        val leftFragment = (leftResult as Ok).value
            ?: return unsupported(expression, "比较左侧标量表达式不支持 / Unsupported left scalar expression in comparison")
        val rightFragment = (rightResult as Ok).value
            ?: return unsupported(expression, "比较右侧标量表达式不支持 / Unsupported right scalar expression in comparison")
        val operator = when (expression.operator) {
            ComparisonOperator.Eq -> "="
            ComparisonOperator.Ne -> "<>"
            ComparisonOperator.Lt -> "<"
            ComparisonOperator.Le -> "<="
            ComparisonOperator.Gt -> ">"
            ComparisonOperator.Ge -> ">="
        }
        return Ok(
            JdbcClientBooleanTranslation(
                JdbcClientSqlFragment(
                    "${leftFragment.sql} $operator ${rightFragment.sql}",
                    leftFragment.parameters + rightFragment.parameters
                )
            )
        )
    }

    private fun translateIn(expression: InExpression<*>): Ret<JdbcClientBooleanTranslation> {
        if (expression.candidates.isEmpty()) {
            return Ok(JdbcClientBooleanTranslation(JdbcClientSqlFragment(if (expression.negated) "1 = 1" else "1 = 0")))
        }
        val valueResult = scalarTranslator.translate(expression.value)
        val candidates = mutableListOf<JdbcClientSqlFragment>()
        var unsupportedCandidate = false
        for (candidate in expression.candidates) {
            when (val result = scalarTranslator.translate(candidate)) {
                is Ok -> {
                    val translated = result.value
                    if (candidate !is ScalarConstant<*> || translated == null) {
                        unsupportedCandidate = true
                    } else {
                        candidates.add(translated)
                    }
                }
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
        }
        if (valueResult is Failed) return Failed(valueResult.error)
        if (valueResult is Fatal) return Fatal(valueResult.errors)
        if (unsupportedCandidate) {
            return unsupported(expression, "IN 候选项必须是可翻译常量 / IN candidates must be translatable constants")
        }
        val value = (valueResult as Ok).value
            ?: return unsupported(expression, "IN 左侧标量表达式不支持 / Unsupported IN value expression")
        val keyword = if (expression.negated) "NOT IN" else "IN"
        val sql = "${value.sql} $keyword (${candidates.joinToString(", ") { it.sql }})"
        val parameters = value.parameters + candidates.flatMap { it.parameters }
        return Ok(JdbcClientBooleanTranslation(JdbcClientSqlFragment(sql, parameters)))
    }

    private fun translatePattern(expression: PatternMatch<*>): Ret<JdbcClientBooleanTranslation> {
        val value = when (val result = scalarTranslator.translate(expression.value)) {
            is Ok -> result.value ?: return unsupported(expression, "模式匹配左侧标量表达式不支持 / Unsupported pattern value expression")
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        val pattern = (expression.pattern as? ScalarConstant<*>)?.value?.toString()
            ?: return unsupported(expression, "模式必须是非空常量 / Pattern must be a non-null constant")
        val sqlPattern = when (expression.mode) {
            PatternMatchMode.Exact -> pattern
            PatternMatchMode.Prefix -> "$pattern%"
            PatternMatchMode.Suffix -> "%$pattern"
            PatternMatchMode.Contains -> "%$pattern%"
            PatternMatchMode.Like -> pattern
            PatternMatchMode.Regex -> return unsupported(expression, "正则模式不支持 / Regex patterns are not supported")
        }
        val operator = if (expression.negated) "NOT LIKE" else "LIKE"
        return Ok(
            JdbcClientBooleanTranslation(
                JdbcClientSqlFragment("${value.sql} $operator ?", value.parameters + JdbcClientValueConverter.convert(sqlPattern))
            )
        )
    }

    private fun translateNullCheck(expression: NullCheck): Ret<JdbcClientBooleanTranslation> {
        val column = resolveColumnName(expression.path.value)
            ?: return unsupported(expression, "无法解析空值检查字段：${expression.path.value} / Cannot resolve null-check field: ${expression.path.value}")
        return quoteIdentifierPath(column, dialect).map { quoted ->
            JdbcClientBooleanTranslation(
                JdbcClientSqlFragment("$quoted ${if (expression.isNull) "IS NULL" else "IS NOT NULL"}")
            )
        }
    }

    private fun translateLogical(operands: List<BooleanExpression>, operator: String): Ret<JdbcClientBooleanTranslation> {
        val translations = mutableListOf<JdbcClientSqlFragment>()
        var unsupported: UnsupportedPredicateDetail? = null
        for (operand in operands) {
            when (val result = translateInternal(operand)) {
                is Ok -> {
                    if (result.value.unsupported != null) {
                        if (unsupported == null) unsupported = result.value.unsupported
                    } else {
                        translations.add(result.value.fragment ?: JdbcClientSqlFragment("1 = 1"))
                    }
                }
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
        }
        if (unsupported != null) return Ok(JdbcClientBooleanTranslation(unsupported = unsupported))
        if (translations.isEmpty()) {
            return Ok(JdbcClientBooleanTranslation(JdbcClientSqlFragment(if (operator == "AND") "1 = 1" else "1 = 0")))
        }
        return Ok(
            JdbcClientBooleanTranslation(
                JdbcClientSqlFragment(
                    sql = translations.joinToString(" $operator ", "(", ")") { it.sql },
                    parameters = translations.flatMap { it.parameters }
                )
            )
        )
    }

    private fun translateNot(expression: NotExpression): Ret<JdbcClientBooleanTranslation> {
        return when (val result = translateInternal(expression.operand)) {
            is Ok -> if (result.value.unsupported != null) {
                Ok(result.value)
            } else {
                val fragment = result.value.fragment ?: JdbcClientSqlFragment("1 = 1")
                Ok(JdbcClientBooleanTranslation(JdbcClientSqlFragment("NOT (${fragment.sql})", fragment.parameters)))
            }
            is Failed -> Failed(result.error)
            is Fatal -> Fatal(result.errors)
        }
    }

    private fun unsupported(expression: BooleanExpression, reason: String): Ret<JdbcClientBooleanTranslation> {
        return Ok(
            JdbcClientBooleanTranslation(
                unsupported = UnsupportedPredicateDetail(
                    expressionType = expression.typeName,
                    reason = reason,
                    policy = unsupportedPredicatePolicy,
                    backendName = "JdbcClient"
                )
            )
        )
    }
}
