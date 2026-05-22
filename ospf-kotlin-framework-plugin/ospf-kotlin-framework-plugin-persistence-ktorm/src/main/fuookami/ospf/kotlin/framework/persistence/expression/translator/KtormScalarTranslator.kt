/**
 * Ktorm 标量表达式翻译器
 * Ktorm Scalar Expression Translator
 *
 * 将通用 ScalarExpression 翻译为 Ktorm 标量 SQL 表达式。
 * Translates generic ScalarExpression to Ktorm scalar SQL expressions.
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
import org.ktorm.expression.ArgumentExpression
import org.ktorm.expression.BinaryExpression
import org.ktorm.expression.BinaryExpressionType
import org.ktorm.expression.UnaryExpression
import org.ktorm.expression.UnaryExpressionType
import org.ktorm.schema.BooleanSqlType
import org.ktorm.schema.DoubleSqlType
import org.ktorm.schema.IntSqlType
import org.ktorm.schema.LongSqlType
import org.ktorm.schema.SqlType
import org.ktorm.schema.VarcharSqlType
import org.ktorm.expression.ScalarExpression as KtormScalarExpression

/**
 * Ktorm 标量表达式翻译器
 * Ktorm scalar expression translator
 */
class KtormScalarTranslator(
    private val resolveColumn: KtormColumnResolver,
    private val unsupportedPredicatePolicy: UnsupportedPredicatePolicy = UnsupportedPredicatePolicy.AlwaysFalse
) {
    /**
     * 翻译标量表达式
     * Translate scalar expression
     */
    fun translate(expr: ScalarExpression<*>): KtormScalarExpression<*>? {
        return when (expr) {
            is ScalarReference<*> -> {
                resolveColumn(expr.path.value)?.asExpression()
                    ?: unsupported("Unresolved path: ${expr.path.value}")
            }
            is ScalarConstant<*> -> translateConstant(expr.value)
            is ScalarUnary<*> -> translateUnary(expr)
            is ScalarBinary<*> -> translateBinary(expr)
            else -> unsupported("Unsupported scalar expression: ${expr.typeName}")
        }
    }

    private fun translateConstant(value: Any?): KtormScalarExpression<*>? {
        if (value == null) {
            return unsupported("Null scalar constants are not supported in predicates")
        }
        @Suppress("UNCHECKED_CAST")
        val sqlType = inferSqlType(value) as SqlType<Any>
        return ArgumentExpression(value, sqlType)
    }

    private fun translateUnary(expr: ScalarUnary<*>): KtormScalarExpression<*>? {
        val operand = translate(expr.operand) ?: return null
        val type = when (expr.operator) {
            UnaryOperator.Negate -> UnaryExpressionType.UNARY_MINUS
            UnaryOperator.Positive -> UnaryExpressionType.UNARY_PLUS
            UnaryOperator.Abs -> return unsupported("ABS unary scalar expression is not supported")
        }
        @Suppress("UNCHECKED_CAST")
        val sqlType = operand.sqlType as SqlType<Any>
        return UnaryExpression(type, operand, sqlType)
    }

    private fun translateBinary(expr: ScalarBinary<*>): KtormScalarExpression<*>? {
        val left = translate(expr.left) ?: return null
        val right = translate(expr.right) ?: return null
        val type = when (expr.operator) {
            BinaryOperator.Add -> BinaryExpressionType.PLUS
            BinaryOperator.Subtract -> BinaryExpressionType.MINUS
            BinaryOperator.Multiply -> BinaryExpressionType.TIMES
            BinaryOperator.Divide -> BinaryExpressionType.DIV
            BinaryOperator.Modulo -> BinaryExpressionType.REM
            BinaryOperator.Power -> return unsupported("POWER scalar expression is not supported")
        }
        @Suppress("UNCHECKED_CAST")
        val sqlType = left.sqlType as SqlType<Any>
        return BinaryExpression(type, left, right, sqlType)
    }

    private fun inferSqlType(value: Any): SqlType<*> {
        return when (value) {
            is Int -> IntSqlType
            is Long -> LongSqlType
            is Float, is Double -> DoubleSqlType
            is Boolean -> BooleanSqlType
            else -> VarcharSqlType
        }
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
