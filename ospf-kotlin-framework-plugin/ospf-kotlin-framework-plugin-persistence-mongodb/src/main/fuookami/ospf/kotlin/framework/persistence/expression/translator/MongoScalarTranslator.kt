/**
 * MongoDB 标量表达式翻译器
 * MongoDB Scalar Expression Translator
 *
 * 将通用 ScalarExpression 翻译为 MongoDB $expr 可用的表达式值。
 * Translates generic ScalarExpression to values usable in MongoDB $expr.
 */
package fuookami.ospf.kotlin.framework.persistence.expression.translator

import org.bson.Document
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.symbol.expression.*
import fuookami.ospf.kotlin.framework.persistence.expression.*

/**
 * MongoDB 标量表达式翻译器
 * MongoDB scalar expression translator
 *
 * @property resolveFieldName 字段名解析函数 / Field name resolver function
 * @property unsupportedPredicatePolicy 不支持谓词时的策略 / Policy for unsupported predicates
 */
class MongoScalarTranslator(
    private val resolveFieldName: MongoFieldNameResolver,
    private val unsupportedPredicatePolicy: UnsupportedPredicatePolicy = UnsupportedPredicatePolicy.AlwaysFalse
) {
    /**
     * 翻译标量表达式为 MongoDB 可用的值
     * Translate scalar expression to MongoDB-compatible value
     *
     * @param expr 标量表达式 / Scalar expression
     * @return MongoDB 可用的值，不支持时返回 null / MongoDB-compatible value, or null if unsupported
     */
    fun translate(expr: ScalarExpression<*>): Ret<Any?> {
        return when (expr) {
            is ScalarReference<*> -> {
                val field = resolveFieldName(expr.path.value)
                    ?: return unsupported("Unresolved path: ${expr.path.value}")
                Ok("\$$field")
            }
            is ScalarConstant<*> -> Ok(expr.value)
            is ScalarUnary<*> -> translateUnary(expr)
            is ScalarBinary<*> -> translateBinary(expr)
            is ScalarFunction<*> -> translateFunction(expr)
            else -> unsupported("Unsupported scalar expression: ${expr.typeName}")
        }
    }

    private fun translateUnary(expr: ScalarUnary<*>): Ret<Any?> {
        val operand = translate(expr.operand).value ?: return Ok(null)
        return when (expr.operator) {
            UnaryOperator.Negate -> Ok(Document("\$multiply", listOf(-1, operand)))
            UnaryOperator.Positive -> Ok(operand)
            UnaryOperator.Abs -> unsupported("ABS unary scalar expression is not supported")
        }
    }

    private fun translateBinary(expr: ScalarBinary<*>): Ret<Any?> {
        val left = translate(expr.left).value ?: return Ok(null)
        val right = translate(expr.right).value ?: return Ok(null)
        val operator = when (expr.operator) {
            BinaryOperator.Add -> "\$add"
            BinaryOperator.Subtract -> "\$subtract"
            BinaryOperator.Multiply -> "\$multiply"
            BinaryOperator.Divide -> "\$divide"
            BinaryOperator.Modulo -> "\$mod"
            BinaryOperator.Power -> return unsupported("POWER scalar expression is not supported")
        }
        return Ok(Document(operator, listOf(left, right)))
    }

    private fun translateFunction(expr: ScalarFunction<*>): Ret<Any?> {
        val arguments = expr.arguments.map { translate(it).value ?: return Ok(null) }
        return when (expr.name.lowercase()) {
            ScalarFunctionNames.Abs -> translateUnaryFunction(expr.name, "\$abs", arguments)
            ScalarFunctionNames.Lower -> translateUnaryFunction(expr.name, "\$toLower", arguments)
            ScalarFunctionNames.Upper -> translateUnaryFunction(expr.name, "\$toUpper", arguments)
            ScalarFunctionNames.Trim -> translateUnaryFunction(expr.name, "\$trim", arguments) { arg ->
                Document("input", arg)
            }
            ScalarFunctionNames.Length -> translateUnaryFunction(expr.name, "\$strLenCP", arguments)
            ScalarFunctionNames.Coalesce -> {
                if (arguments.isEmpty()) return unsupported("Function coalesce expects at least one argument")
                Ok(arguments.drop(1).fold(arguments.first()) { acc, value ->
                    Document("\$ifNull", listOf(acc, value))
                })
            }
            else -> unsupported("Unsupported scalar function: ${expr.name}")
        }
    }

    private fun translateUnaryFunction(
        logicalName: String,
        mongoName: String,
        arguments: List<Any?>,
        argumentMapper: (Any?) -> Any? = { it }
    ): Ret<Any?> {
        if (arguments.size != 1) {
            return unsupported("Function $logicalName expects exactly one argument")
        }
        return Ok(Document(mongoName, argumentMapper(arguments[0])))
    }

    private fun unsupported(reason: String): Ret<Any?> {
        return when (unsupportedPredicatePolicy) {
            UnsupportedPredicatePolicy.FailFast -> {
                val detail = UnsupportedPredicateDetail.failFast(
                    expressionType = "ScalarExpression",
                    reason = reason,
                    backendName = "MongoDB"
                )
                Failed(detail.toError())
            }
            UnsupportedPredicatePolicy.AlwaysFalse -> Ok(null)
            UnsupportedPredicatePolicy.ClientFilter -> {
                val detail = UnsupportedPredicateDetail.clientFilter(
                    expressionType = "ScalarExpression",
                    reason = reason,
                    backendName = "MongoDB"
                )
                Failed(detail.toError())
            }
        }
    }
}
