/**
 * MyBatis 标量表达式翻译器
 * MyBatis Scalar Expression Translator
 *
 * 将通用 ScalarExpression 翻译为参数化 SQL 片段。
 * Translates generic ScalarExpression to parameterized SQL fragments.
 */
package fuookami.ospf.kotlin.framework.persistence.expression.translator

import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.framework.persistence.expression.*
import fuookami.ospf.kotlin.math.symbol.expression.*

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

    private fun translateUnary(expr: ScalarUnary<*>): Ret<MybatisScalarSql?> {
        val operand = translate(expr.operand).value ?: return Ok(null)
        return when (expr.operator) {
            UnaryOperator.Negate -> Ok(operand.copy(sql = "(-${operand.sql})", isColumnOnly = false))
            UnaryOperator.Positive -> Ok(operand.copy(sql = "(+${operand.sql})", isColumnOnly = false))
            UnaryOperator.Abs -> unsupported("ABS unary scalar expression is not supported")
        }
    }

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
