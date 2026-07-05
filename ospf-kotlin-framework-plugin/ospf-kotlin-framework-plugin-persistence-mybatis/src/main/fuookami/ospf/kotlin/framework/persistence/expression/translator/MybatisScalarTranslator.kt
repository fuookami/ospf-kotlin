/**
 * MyBatis 标量表达式翻译器
 * MyBatis Scalar Expression Translator
 *
 * 将通用 ScalarExpression 翻译为参数化 SQL 片段。
 * Translates generic ScalarExpression to parameterized SQL fragments.
 */
package fuookami.ospf.kotlin.framework.persistence.expression.translator

import fuookami.ospf.kotlin.framework.persistence.expression.*
import fuookami.ospf.kotlin.math.symbol.expression.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*

/**
 * MyBatis 标量 SQL 片段
 * MyBatis scalar SQL fragment
 *
 * @property sql SQL 片段字符串 / SQL fragment string
 * @property params 参数列表 / Parameter list
 * @property isColumnOnly 是否仅为列引用 / Whether it's a column reference only
 */
data class MybatisScalarSql(
    val sql: String,
    val params: List<Any?> = emptyList(),
    val isColumnOnly: Boolean = false
) {
    /**
     * 将参数占位符整体右移
     * Shift parameter placeholders by offset
     *
     * @param offset 偏移量 / Offset value
     * @return 偏移后的新 SQL 片段 / New SQL fragment with shifted placeholders
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
 *
 * @property resolveColumnName 列名解析函数 / Column name resolver function
 * @property unsupportedPredicatePolicy 不支持谓词时的策略 / Policy for unsupported predicates
 */
class MybatisScalarTranslator(
    private val resolveColumnName: MybatisColumnNameResolver,
    private val unsupportedPredicatePolicy: UnsupportedPredicatePolicy = UnsupportedPredicatePolicy.AlwaysFalse
) {
    /**
     * 翻译标量表达式为参数化 SQL 片段
     * Translate scalar expression to parameterized SQL fragment
     *
     * @param expr 标量表达式 / Scalar expression
     * @return 参数化 SQL 片段，不支持时返回 null / Parameterized SQL fragment, or null if unsupported
     */
    fun translate(expr: ScalarExpression<*>): Ret<MybatisScalarSql?> {
        return when (expr) {
            is ScalarReference<*> -> {
                val column = resolveColumnName(expr.path.value)
                    ?: return unsupported("Unresolved path: ${expr.path.value}")
                Ok(MybatisScalarSql(column, isColumnOnly = true))
            }
            is ScalarConstant<*> -> Ok(MybatisScalarSql("{0}", listOf(MybatisValueConverter.convert(expr.value)), isColumnOnly = false))
            is ScalarUnary<*> -> translateUnary(expr)
            is ScalarBinary<*> -> translateBinary(expr)
            is ScalarFunction<*> -> translateFunction(expr)
            else -> unsupported("Unsupported scalar expression: ${expr.typeName}")
        }
    }

    /**
     * 翻译一元标量表达式为参数化 SQL 片段
     * Translate unary scalar expression to parameterized SQL fragment
     *
     * @param expr 一元标量表达式 / Unary scalar expression
     * @return 参数化 SQL 片段 / Parameterized SQL fragment
     */
    private fun translateUnary(expr: ScalarUnary<*>): Ret<MybatisScalarSql?> {
        val operand = translate(expr.operand).value ?: return Ok(null)
        return when (expr.operator) {
            UnaryOperator.Negate -> Ok(operand.copy(sql = "(-${operand.sql})", isColumnOnly = false))
            UnaryOperator.Positive -> Ok(operand.copy(sql = "(+${operand.sql})", isColumnOnly = false))
            UnaryOperator.Abs -> unsupported("ABS unary scalar expression is not supported")
        }
    }

    /**
     * 翻译二元标量表达式为参数化 SQL 片段
     * Translate binary scalar expression to parameterized SQL fragment
     *
     * @param expr 二元标量表达式 / Binary scalar expression
     * @return 参数化 SQL 片段 / Parameterized SQL fragment
     */
    private fun translateBinary(expr: ScalarBinary<*>): Ret<MybatisScalarSql?> {
        val left = translate(expr.left).value ?: return Ok(null)
        val right = translate(expr.right).value?.shifted(left.params.size) ?: return Ok(null)
        val operator = when (expr.operator) {
            BinaryOperator.Add -> "+"
            BinaryOperator.Subtract -> "-"
            BinaryOperator.Multiply -> "*"
            BinaryOperator.Divide -> "/"
            BinaryOperator.Modulo -> "%"
            BinaryOperator.Power -> return unsupported("POWER scalar expression is not supported")
        }
        return Ok(MybatisScalarSql(
            sql = "(${left.sql} $operator ${right.sql})",
            params = left.params + right.params,
            isColumnOnly = false
        ))
    }

    /**
     * 翻译标量函数调用为参数化 SQL 函数片段
     * Translate scalar function call to parameterized SQL function fragment
     *
     * @param expr 标量函数表达式 / Scalar function expression
     * @return 参数化 SQL 函数片段 / Parameterized SQL function fragment
     */
    private fun translateFunction(expr: ScalarFunction<*>): Ret<MybatisScalarSql?> {
        val arguments = mutableListOf<MybatisScalarSql>()
        var paramOffset = 0
        for (argument in expr.arguments) {
            val translated = translate(argument).value?.shifted(paramOffset) ?: return Ok(null)
            paramOffset += translated.params.size
            arguments.add(translated)
        }

        return when (expr.name.lowercase()) {
            ScalarFunctionNames.Abs -> translateSqlFunction(expr.name, "ABS", arguments, expected = 1)
            ScalarFunctionNames.Lower -> translateSqlFunction(expr.name, "LOWER", arguments, expected = 1)
            ScalarFunctionNames.Upper -> translateSqlFunction(expr.name, "UPPER", arguments, expected = 1)
            ScalarFunctionNames.Trim -> translateSqlFunction(expr.name, "TRIM", arguments, expected = 1)
            ScalarFunctionNames.Length -> translateSqlFunction(expr.name, "LENGTH", arguments, expected = 1)
            ScalarFunctionNames.Coalesce -> {
                if (arguments.isEmpty()) return unsupported("Function coalesce expects at least one argument")
                Ok(MybatisScalarSql(
                    sql = "COALESCE(${arguments.joinToString(", ") { it.sql }})",
                    params = arguments.flatMap { it.params },
                    isColumnOnly = false
                ))
            }
            else -> unsupported("Unsupported scalar function: ${expr.name}")
        }
    }

    /**
     * 生成标准 SQL 函数的参数化片段
     * Generate parameterized fragment for standard SQL function
     *
     * @param logicalName 逻辑函数名（用于错误信息） / Logical function name (for error messages)
     * @param sqlName SQL 函数名 / SQL function name
     * @param arguments 已翻译的参数列表 / Translated argument list
     * @param expected 期望参数数量 / Expected argument count
     * @return 参数化 SQL 函数片段 / Parameterized SQL function fragment
     */
    private fun translateSqlFunction(
        logicalName: String,
        sqlName: String,
        arguments: List<MybatisScalarSql>,
        expected: Int
    ): Ret<MybatisScalarSql?> {
        if (arguments.size != expected) {
            return unsupported("Function $logicalName expects exactly $expected argument(s)")
        }
        return Ok(MybatisScalarSql(
            sql = "$sqlName(${arguments.joinToString(", ") { it.sql }})",
            params = arguments.flatMap { it.params },
            isColumnOnly = false
        ))
    }

    /**
     * 根据不支持谓词策略处理不支持的表达式
     * Handle unsupported expression based on unsupported predicate policy
     *
     * @param reason 不支持的原因 / Reason for being unsupported
     * @return 根据策略返回失败或 null / Returns failure or null based on policy
     */
    private fun unsupported(reason: String): Ret<MybatisScalarSql?> {
        return when (unsupportedPredicatePolicy) {
            UnsupportedPredicatePolicy.FailFast -> {
                val detail = UnsupportedPredicateDetail.failFast(
                    expressionType = "ScalarExpression",
                    reason = reason,
                    backendName = "MyBatis"
                )
                Failed(detail.toError())
            }
            UnsupportedPredicatePolicy.AlwaysFalse -> Ok(null)
            UnsupportedPredicatePolicy.ClientFilter -> {
                val detail = UnsupportedPredicateDetail.clientFilter(
                    expressionType = "ScalarExpression",
                    reason = reason,
                    backendName = "MyBatis"
                )
                Failed(detail.toError())
            }
        }
    }
}
