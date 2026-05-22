/**
 * MyBatis 标量表达式翻译器
 * MyBatis Scalar Expression Translator
 *
 * 将通用 ScalarExpression 翻译为参数化 SQL 片段。
 * Translates generic ScalarExpression to parameterized SQL fragments.
 */
package fuookami.ospf.kotlin.framework.persistence.expression.translator

import fuookami.ospf.kotlin.framework.persistence.expression.UnsupportedPredicatePolicy
import fuookami.ospf.kotlin.math.symbol.expression.BinaryOperator
import fuookami.ospf.kotlin.math.symbol.expression.ScalarBinary
import fuookami.ospf.kotlin.math.symbol.expression.ScalarConstant
import fuookami.ospf.kotlin.math.symbol.expression.ScalarExpression
import fuookami.ospf.kotlin.math.symbol.expression.ScalarReference
import fuookami.ospf.kotlin.math.symbol.expression.ScalarUnary
import fuookami.ospf.kotlin.math.symbol.expression.UnaryOperator

/**
 * MyBatis 标量 SQL 片段
 * MyBatis scalar SQL fragment
 */
data class MybatisScalarSql(
    val sql: String,
    val params: List<Any?> = emptyList(),
    val isColumnOnly: Boolean = false
) {
    /**
     * 将参数占位符整体右移
     * Shift parameter placeholders by offset
     */
    fun shifted(offset: Int): MybatisScalarSql {
        if (offset == 0 || params.isEmpty()) return this
        var shiftedSql = sql
        for (index in params.indices.reversed()) {
            shiftedSql = shiftedSql.replace("{$index}", "{${index + offset}}")
        }
        return copy(sql = shiftedSql)
    }
}

/**
 * MyBatis 标量表达式翻译器
 * MyBatis scalar expression translator
 */
class MybatisScalarTranslator(
    private val resolveColumnName: MybatisColumnNameResolver,
    private val unsupportedPredicatePolicy: UnsupportedPredicatePolicy = UnsupportedPredicatePolicy.AlwaysFalse
) {
    /**
     * 翻译标量表达式
     * Translate scalar expression
     */
    fun translate(expr: ScalarExpression<*>): MybatisScalarSql? {
        return when (expr) {
            is ScalarReference<*> -> {
                val column = resolveColumnName(expr.path.value)
                    ?: return unsupported("Unresolved path: ${expr.path.value}")
                MybatisScalarSql(column, isColumnOnly = true)
            }
            is ScalarConstant<*> -> MybatisScalarSql("{0}", listOf(expr.value), isColumnOnly = false)
            is ScalarUnary<*> -> translateUnary(expr)
            is ScalarBinary<*> -> translateBinary(expr)
            else -> unsupported("Unsupported scalar expression: ${expr.typeName}")
        }
    }

    private fun translateUnary(expr: ScalarUnary<*>): MybatisScalarSql? {
        val operand = translate(expr.operand) ?: return null
        return when (expr.operator) {
            UnaryOperator.Negate -> operand.copy(sql = "(-${operand.sql})", isColumnOnly = false)
            UnaryOperator.Positive -> operand.copy(sql = "(+${operand.sql})", isColumnOnly = false)
            UnaryOperator.Abs -> unsupported("ABS unary scalar expression is not supported")
        }
    }

    private fun translateBinary(expr: ScalarBinary<*>): MybatisScalarSql? {
        val left = translate(expr.left) ?: return null
        val right = translate(expr.right)?.shifted(left.params.size) ?: return null
        val operator = when (expr.operator) {
            BinaryOperator.Add -> "+"
            BinaryOperator.Subtract -> "-"
            BinaryOperator.Multiply -> "*"
            BinaryOperator.Divide -> "/"
            BinaryOperator.Modulo -> "%"
            BinaryOperator.Power -> return unsupported("POWER scalar expression is not supported")
        }
        return MybatisScalarSql(
            sql = "(${left.sql} $operator ${right.sql})",
            params = left.params + right.params,
            isColumnOnly = false
        )
    }

    private fun unsupported(reason: String): Nothing? {
        when (unsupportedPredicatePolicy) {
            UnsupportedPredicatePolicy.FailFast -> throw IllegalArgumentException(reason)
            UnsupportedPredicatePolicy.AlwaysFalse -> return null
            UnsupportedPredicatePolicy.ClientFilter -> throw IllegalArgumentException(
                "ClientFilter is not implemented for unsupported predicate: $reason"
            )
        }
    }
}
