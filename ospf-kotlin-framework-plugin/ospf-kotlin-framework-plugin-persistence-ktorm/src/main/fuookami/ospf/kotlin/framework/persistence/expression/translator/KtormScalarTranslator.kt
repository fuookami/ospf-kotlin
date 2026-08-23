/**
 * Ktorm 标量表达式翻译器 / Ktorm Scalar Expression Translator
 *
 * 将通用 ScalarExpression 翻译为 Ktorm 标量 SQL 表达式。
 * Translates generic ScalarExpression to Ktorm scalar SQL expressions.
*/
package fuookami.ospf.kotlin.framework.persistence.expression.translator

import kotlinx.coroutines.CancellationException
import org.ktorm.expression.ArgumentExpression
import org.ktorm.expression.BinaryExpression
import org.ktorm.expression.BinaryExpressionType
import org.ktorm.expression.FunctionExpression
import org.ktorm.expression.ScalarExpression as KtormScalarExpression
import org.ktorm.expression.UnaryExpression
import org.ktorm.expression.UnaryExpressionType
import org.ktorm.schema.BooleanSqlType
import org.ktorm.schema.ColumnDeclaring
import org.ktorm.schema.DecimalSqlType
import org.ktorm.schema.DoubleSqlType
import org.ktorm.schema.IntSqlType
import org.ktorm.schema.LongSqlType
import org.ktorm.schema.SqlType
import org.ktorm.schema.VarcharSqlType
import fuookami.ospf.kotlin.framework.persistence.expression.*
import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.math.symbol.expression.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*

/**
 * Ktorm 常量绑定结果 / Ktorm constant binding
 *
 * 允许适配器为无目标列上下文的值对象注册 JDBC 值和 SQL 类型。
 * Allows adapters to register a JDBC value and SQL type for a value object without a target-column context.
 *
 * @property value JDBC 参数值 / JDBC parameter value
 * @property sqlType 目标 SQL 类型 / Target SQL type
 */
data class KtormScalarBinding(
    val value: Any,
    val sqlType: SqlType<Any>
) {
    companion object {
        /**
         * 使用目标列类型创建绑定结果 / Create a binding result for a target column type
         *
         * @param value 要交给 Ktorm 的领域值或 JDBC 值 / Domain or JDBC value passed to Ktorm
         * @param sqlType 目标列 SQL 类型 / Target column SQL type
         * @return 带目标类型的绑定结果 / Binding result with the target type
         */
        fun forTarget(value: Any, sqlType: SqlType<*>): KtormScalarBinding {
            @Suppress("UNCHECKED_CAST")
            return KtormScalarBinding(value, sqlType as SqlType<Any>)
        }
    }
}

/**
 * 目标 SQL 类型感知的常量绑定器 / Target-SQL-type-aware constant binder
 *
 * 适配器必须显式声明自定义或转换列如何接受常量；绑定器返回的 SQL 类型必须与传入目标类型相等。
 * Adapters must explicitly declare how custom or transformed columns accept constants; the returned SQL type
 * must equal the supplied target type.
 */
typealias KtormTargetConstantBinder = (Any, SqlType<*>) -> KtormScalarBinding?

/**
 * Ktorm 标量表达式翻译器 / Ktorm scalar expression translator
 *
 * @property resolveColumn 列解析函数 / Column resolver function
 * @property unsupportedPredicatePolicy 不支持谓词时的策略 / Policy for unsupported predicates
 * @property targetConstantBinder 目标 SQL 类型感知的常量绑定器 / Target-SQL-type-aware constant binder
*/
class KtormScalarTranslator(
    private val resolveColumn: KtormColumnResolver,
    private val unsupportedPredicatePolicy: UnsupportedPredicatePolicy = UnsupportedPredicatePolicy.AlwaysFalse,
    private val constantBinder: (Any) -> KtormScalarBinding? = ::defaultSqlConstantBinding,
    private val parameterTypeSink: ((SqlType<*>) -> Unit)? = null,
    private val resolveColumnDetailed: ((String) -> PersistenceFieldResolution<ColumnDeclaring<*>>)? = null,
    private val targetConstantBinder: KtormTargetConstantBinder? = null
) {

    /**
     * 翻译标量表达式为 Ktorm 标量 SQL 表达式 / Translate scalar expression to Ktorm scalar SQL expression
     *
     * @param expr 标量表达式 / Scalar expression
     * @return Ktorm 标量表达式，不支持时返回 null / Ktorm scalar expression, or null if unsupported
    */
    fun translate(expr: ScalarExpression<*>): Ret<KtormScalarExpression<*>?> {
        return when (expr) {
            is ScalarReference<*> -> {
                val column = resolveColumn(expr.path.value)
                    ?: return unsupported(resolveFailure(expr.path.value))
                Ok(column.asExpression())
            }
            is ScalarConstant<*> -> translateConstant(expr.value)
            is ScalarUnary<*> -> translateUnary(expr)
            is ScalarBinary<*> -> translateBinary(expr)
            is ScalarFunction<*> -> translateFunction(expr)
            else -> unsupported("Unsupported scalar expression: ${expr.typeName}")
        }
    }

    /**
     * 翻译常量值为 Ktorm 参数表达式 / Translate constant value to Ktorm argument expression
     *
     * @param value 常量值 / Constant value
     * @return Ktorm 参数表达式 / Ktorm argument expression
    */
    internal fun translateConstant(
        value: Any?,
        sqlType: SqlType<*>? = null
    ): Ret<KtormScalarExpression<*>?> {
        if (value == null) {
            return unsupported("Null scalar constants are not supported in predicates")
        }
        @Suppress("UNCHECKED_CAST")
        if (sqlType != null) {
            val target = sqlType as SqlType<Any>
            val registeredBinding = try {
                targetConstantBinder?.invoke(value, target)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                return unsupported(
                    "No SQL binding registered for ${value::class.qualifiedName} with target " +
                        "${target.typeName}: ${error.message ?: error::class.qualifiedName}"
                )
            }
                ?.takeIf { it.sqlType == target }
                ?: defaultTargetSqlConstantBinding(value, target)
                ?: constantBinder(value)?.takeIf { it.sqlType == target }
            if (registeredBinding == null) {
                return unsupported(
                    "No SQL binding registered for ${value::class.qualifiedName} with target ${target.typeName}"
                )
            }
            val argument = ArgumentExpression(registeredBinding.value, target)
            parameterTypeSink?.invoke(target)
            return Ok(argument)
        }
        val binding = constantBinder(value)
            ?: return unsupported("No SQL binding registered for constant type ${value::class.qualifiedName}")
        parameterTypeSink?.invoke(binding.sqlType)
        return Ok(ArgumentExpression(binding.value, binding.sqlType))
    }

    private fun defaultTargetSqlConstantBinding(
        value: Any,
        target: SqlType<Any>
    ): KtormScalarBinding? {
        return when (target) {
            IntSqlType -> if (value is Int) KtormScalarBinding.forTarget(value, target) else null
            LongSqlType -> if (value is Long) KtormScalarBinding.forTarget(value, target) else null
            org.ktorm.schema.FloatSqlType -> {
                if (value is Float) KtormScalarBinding.forTarget(value, target) else null
            }
            DoubleSqlType -> {
                if (value is Double || value is Float) KtormScalarBinding.forTarget(value, target) else null
            }
            BooleanSqlType -> if (value is Boolean) KtormScalarBinding.forTarget(value, target) else null
            VarcharSqlType -> if (value is String) KtormScalarBinding.forTarget(value, target) else null
            DecimalSqlType -> {
                if (value is java.math.BigDecimal) KtormScalarBinding.forTarget(value, target) else null
            }
            else -> null
        }
    }

    /**
     * 翻译一元标量表达式为 Ktorm 一元表达式 / Translate unary scalar expression to Ktorm unary expression
     *
     * @param expr 一元标量表达式 / Unary scalar expression
     * @return Ktorm 一元表达式 / Ktorm unary expression
    */
    private fun translateUnary(expr: ScalarUnary<*>): Ret<KtormScalarExpression<*>?> {
        val translatedOperand = translate(expr.operand)
        if (translatedOperand.failed) return propagateFailure(translatedOperand)
        val operand = translatedOperand.value ?: return Ok(null)
        val type = when (expr.operator) {
            UnaryOperator.Negate -> UnaryExpressionType.UNARY_MINUS
            UnaryOperator.Positive -> UnaryExpressionType.UNARY_PLUS
            UnaryOperator.Abs -> return unsupported("ABS unary scalar expression is not supported")
        }
        @Suppress("UNCHECKED_CAST")
        val sqlType = operand.sqlType as SqlType<Any>
        return Ok(UnaryExpression(type, operand, sqlType))
    }

    /**
     * 翻译二元标量表达式为 Ktorm 二元表达式 / Translate binary scalar expression to Ktorm binary expression
     *
     * @param expr 二元标量表达式 / Binary scalar expression
     * @return Ktorm 二元表达式 / Ktorm binary expression
    */
    private fun translateBinary(expr: ScalarBinary<*>): Ret<KtormScalarExpression<*>?> {
        val translatedLeft = translate(expr.left)
        if (translatedLeft.failed) return propagateFailure(translatedLeft)
        val left = translatedLeft.value ?: return Ok(null)
        val translatedRight = translate(expr.right)
        if (translatedRight.failed) return propagateFailure(translatedRight)
        val right = translatedRight.value ?: return Ok(null)
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

    /**
     * 翻译标量函数调用为 Ktorm 函数表达式 / Translate scalar function call to Ktorm function expression
     *
     * @param expr 标量函数表达式 / Scalar function expression
     * @return Ktorm 函数表达式 / Ktorm function expression
    */
    private fun translateFunction(expr: ScalarFunction<*>): Ret<KtormScalarExpression<*>?> {
        val arguments = mutableListOf<KtormScalarExpression<*>>()
        expr.arguments.forEach { argument ->
            val translated = translate(argument)
            if (translated.failed) return propagateFailure(translated)
            arguments += translated.value ?: return Ok(null)
        }
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

    companion object {
        /** 默认 OSPF 数值类型绑定 / Default OSPF numeric bindings */
        fun defaultSqlConstantBinding(value: Any): KtormScalarBinding? {
            @Suppress("UNCHECKED_CAST")
            return when (value) {
                is Int -> KtormScalarBinding(value, IntSqlType as SqlType<Any>)
                is Long -> KtormScalarBinding(value, LongSqlType as SqlType<Any>)
                is Float -> KtormScalarBinding(value, org.ktorm.schema.FloatSqlType as SqlType<Any>)
                is Double -> KtormScalarBinding(value, DoubleSqlType as SqlType<Any>)
                is Boolean -> KtormScalarBinding(value, BooleanSqlType as SqlType<Any>)
                is String -> KtormScalarBinding(value, VarcharSqlType as SqlType<Any>)
                is UInt64 -> KtormScalarBinding(value.toLong(), LongSqlType as SqlType<Any>)
                is FltX -> KtormScalarBinding(value.toDecimal(), DecimalSqlType as SqlType<Any>)
                else -> null
            }
        }
    }

    /**
     * 处理不支持的标量表达式情况 / Handle unsupported scalar expression cases
     *
     * @param reason 不支持的原因 / Reason why the expression is unsupported
     * @return 根据策略返回失败或 null / Returns failure or null depending on the policy
    */
    private fun unsupported(reason: String): Ret<KtormScalarExpression<*>?> {
        return when (unsupportedPredicatePolicy) {
            UnsupportedPredicatePolicy.FailFast -> {
                val detail = UnsupportedPredicateDetail.failFast(
                    expressionType = "ScalarExpression",
                    reason = reason,
                    backendName = "Ktorm"
                )
                Failed(detail.toError())
            }
            UnsupportedPredicatePolicy.AlwaysFalse -> Ok(null)
            UnsupportedPredicatePolicy.ClientFilter -> {
                val detail = UnsupportedPredicateDetail.clientFilter(
                    expressionType = "ScalarExpression",
                    reason = reason,
                    backendName = "Ktorm"
                )
                Failed(detail.toError())
            }
        }
    }

    private fun resolveFailure(path: String): String {
        return when (val result = resolveColumnDetailed?.invoke(path)) {
            is PersistenceFieldResolution.Ambiguous -> {
                "Ambiguous path $path: ${result.candidates.joinToString(", ")}"
            }
            is PersistenceFieldResolution.Missing -> "Unresolved path: $path"
            is PersistenceFieldResolution.InvalidConfiguration -> result.reason
            is PersistenceFieldResolution.Resolved, null -> "Unresolved path: $path"
        }
    }

    private fun propagateFailure(
        result: Ret<KtormScalarExpression<*>?>
    ): Ret<KtormScalarExpression<*>?> {
        @Suppress("UNCHECKED_CAST")
        val failed = result as Failed<KtormScalarExpression<*>?, ErrorCode, Error<ErrorCode>>
        return Failed(failed.error)
    }
}
