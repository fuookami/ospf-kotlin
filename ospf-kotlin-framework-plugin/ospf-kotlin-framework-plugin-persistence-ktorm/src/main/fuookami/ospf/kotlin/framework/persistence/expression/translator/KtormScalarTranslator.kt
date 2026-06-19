/**
 * Ktorm 标量表达式翻译器
 * Ktorm Scalar Expression Translator
 *
 * 将通用 ScalarExpression 翻译为 Ktorm 标量 SQL 表达式。
 * Translates generic ScalarExpression to Ktorm scalar SQL expressions.
 */
package fuookami.ospf.kotlin.framework.persistence.expression.translator

import org.ktorm.expression.ArgumentExpression
import org.ktorm.expression.BinaryExpression
import org.ktorm.expression.BinaryExpressionType
import org.ktorm.expression.FunctionExpression
import org.ktorm.expression.ScalarExpression as KtormScalarExpression
import org.ktorm.expression.UnaryExpression
import org.ktorm.expression.UnaryExpressionType
import org.ktorm.schema.BooleanSqlType
import org.ktorm.schema.DoubleSqlType
import org.ktorm.schema.IntSqlType
import org.ktorm.schema.LongSqlType
import org.ktorm.schema.SqlType
import org.ktorm.schema.VarcharSqlType
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.symbol.expression.*
import fuookami.ospf.kotlin.framework.persistence.expression.UnsupportedPredicatePolicy

/**
 * Ktorm 标量表达式翻译器
 * Ktorm scalar expression translator
 *
 * @property resolveColumn 列解析函数 / Column resolver function
 * @property unsupportedPredicatePolicy 不支持谓词时的策略 / Policy for unsupported predicates
 */
class KtormScalarTranslator(
    private val resolveColumn: KtormColumnResolver,
    private val unsupportedPredicatePolicy: UnsupportedPredicatePolicy = UnsupportedPredicatePolicy.AlwaysFalse
) {
    /**
     * 翻译标量表达式为 Ktorm 标量 SQL 表达式
     * Translate scalar expression to Ktorm scalar SQL expression
     *
     * @param expr 标量表达式 / Scalar expression
     * @return Ktorm 标量表达式，不支持时返回 null / Ktorm scalar expression, or null if unsupported
     */
    fun translate(expr: ScalarExpression<*>): Ret<KtormScalarExpression<*>?> {
        return when (expr) {
            is ScalarReference<*> -> {
                val column = resolveColumn(expr.path.value)
                    ?: return unsupported("Unresolved path: ${expr.path.value}")
                Ok(column.asExpression())
            }
            is ScalarConstant<*> -> translateConstant(expr.value)
            is ScalarUnary<*> -> translateUnary(expr)
            is ScalarBinary<*> -> translateBinary(expr)
            is ScalarFunction<*> -> translateFunction(expr)
            else -> unsupported("Unsupported scalar expression: ${expr.typeName}")
        }
    }

    private fun translateConstant(value: Any?): Ret<KtormScalarExpression<*>?> {
        if (value == null) {
            return unsupported("Null scalar constants are not supported in predicates")
        }
        @Suppress("UNCHECKED_CAST")
        val sqlType = inferSqlType(value) as SqlType<Any>
        return Ok(ArgumentExpression(value, sqlType))
    }

    private fun translateUnary(expr: ScalarUnary<*>): Ret<KtormScalarExpression<*>?> {
        val operand = translate(expr.operand).value ?: return Ok(null)
        val type = when (expr.operator) {
            UnaryOperator.Negate -> UnaryExpressionType.UNARY_MINUS
            UnaryOperator.Positive -> UnaryExpressionType.UNARY_PLUS
            UnaryOperator.Abs -> return unsupported("ABS unary scalar expression is not supported")
        }
        @Suppress("UNCHECKED_CAST")
        val sqlType = operand.sqlType as SqlType<Any>
        return Ok(UnaryExpression(type, operand, sqlType))
    }

    private fun translateBinary(expr: ScalarBinary<*>): Ret<KtormScalarExpression<*>?> {
        val left = translate(expr.left).value ?: return Ok(null)
        val right = translate(expr.right).value ?: return Ok(null)
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
        return Ok(BinaryExpression(type, left, right, sqlType))
    }

    private fun translateFunction(expr: ScalarFunction<*>): Ret<KtormScalarExpression<*>?> {
        val arguments = expr.arguments.map { translate(it).value ?: return Ok(null) }
        return when (expr.name.lowercase()) {
            ScalarFunctionNames.Abs -> {
                if (arguments.size != 1) {
                    return unsupported("Function ${expr.name} expects exactly one argument")
                }
                @Suppress("UNCHECKED_CAST")
                Ok(FunctionExpression("ABS", arguments, arguments[0].sqlType as SqlType<Any>))
            }
            ScalarFunctionNames.Lower -> {
                if (arguments.size != 1) {
                    return unsupported("Function ${expr.name} expects exactly one argument")
                }
                Ok(FunctionExpression("LOWER", arguments, VarcharSqlType))
            }
            ScalarFunctionNames.Upper -> {
                if (arguments.size != 1) {
                    return unsupported("Function ${expr.name} expects exactly one argument")
                }
                Ok(FunctionExpression("UPPER", arguments, VarcharSqlType))
            }
            ScalarFunctionNames.Trim -> {
                if (arguments.size != 1) {
                    return unsupported("Function ${expr.name} expects exactly one argument")
                }
                Ok(FunctionExpression("TRIM", arguments, VarcharSqlType))
            }
            ScalarFunctionNames.Length -> {
                if (arguments.size != 1) {
                    return unsupported("Function ${expr.name} expects exactly one argument")
                }
                Ok(FunctionExpression("LENGTH", arguments, IntSqlType))
            }
            ScalarFunctionNames.Coalesce -> {
                if (arguments.isEmpty()) {
                    return unsupported("Function coalesce expects at least one argument")
                }
                @Suppress("UNCHECKED_CAST")
                Ok(FunctionExpression("COALESCE", arguments, arguments[0].sqlType as SqlType<Any>))
            }
            else -> unsupported("Unsupported scalar function: ${expr.name}")
        }
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

    private fun unsupported(reason: String): Ret<KtormScalarExpression<*>?> {
        return when (unsupportedPredicatePolicy) {
            UnsupportedPredicatePolicy.FailFast -> Failed(ErrorCode.IllegalArgument, reason)
            UnsupportedPredicatePolicy.AlwaysFalse -> Ok(null)
            UnsupportedPredicatePolicy.ClientFilter -> Failed(
                ErrorCode.IllegalArgument,
                "ClientFilter is not implemented for unsupported predicate: $reason"
            )
        }
    }
}
