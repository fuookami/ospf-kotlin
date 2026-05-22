/**
 * MongoDB 标量表达式翻译器
 * MongoDB Scalar Expression Translator
 *
 * 将通用 ScalarExpression 翻译为 MongoDB $expr 可用的表达式值。
 * Translates generic ScalarExpression to values usable in MongoDB $expr.
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
import org.bson.Document

/**
 * MongoDB 标量表达式翻译器
 * MongoDB scalar expression translator
 */
class MongoScalarTranslator(
    private val resolveFieldName: MongoFieldNameResolver,
    private val unsupportedPredicatePolicy: UnsupportedPredicatePolicy = UnsupportedPredicatePolicy.AlwaysFalse
) {
    /**
     * 翻译标量表达式
     * Translate scalar expression
     */
    fun translate(expr: ScalarExpression<*>): Any? {
        return when (expr) {
            is ScalarReference<*> -> {
                val field = resolveFieldName(expr.path.value)
                    ?: return unsupported("Unresolved path: ${expr.path.value}")
                "\$$field"
            }
            is ScalarConstant<*> -> expr.value
            is ScalarUnary<*> -> translateUnary(expr)
            is ScalarBinary<*> -> translateBinary(expr)
            else -> unsupported("Unsupported scalar expression: ${expr.typeName}")
        }
    }

    private fun translateUnary(expr: ScalarUnary<*>): Any? {
        val operand = translate(expr.operand) ?: return null
        return when (expr.operator) {
            UnaryOperator.Negate -> Document("\$multiply", listOf(-1, operand))
            UnaryOperator.Positive -> operand
            UnaryOperator.Abs -> unsupported("ABS unary scalar expression is not supported")
        }
    }

    private fun translateBinary(expr: ScalarBinary<*>): Any? {
        val left = translate(expr.left) ?: return null
        val right = translate(expr.right) ?: return null
        val operator = when (expr.operator) {
            BinaryOperator.Add -> "\$add"
            BinaryOperator.Subtract -> "\$subtract"
            BinaryOperator.Multiply -> "\$multiply"
            BinaryOperator.Divide -> "\$divide"
            BinaryOperator.Modulo -> "\$mod"
            BinaryOperator.Power -> return unsupported("POWER scalar expression is not supported")
        }
        return Document(operator, listOf(left, right))
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
