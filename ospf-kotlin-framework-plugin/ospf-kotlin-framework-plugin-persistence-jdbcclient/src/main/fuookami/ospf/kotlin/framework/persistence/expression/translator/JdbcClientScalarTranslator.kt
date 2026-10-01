/**
 * JdbcClient 标量表达式翻译器 / JdbcClient scalar expression translator
 *
 * 翻译结果只包含 `?` 占位符和有序参数，不接受表达式提供任意 SQL 文本。
 * Translation uses only `?` placeholders and ordered parameters; expressions cannot inject SQL text.
 */
package fuookami.ospf.kotlin.framework.persistence.expression.translator

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.symbol.expression.*
import fuookami.ospf.kotlin.framework.persistence.expression.*
import fuookami.ospf.kotlin.framework.persistence.jdbcclient.*

/**
 * 标量表达式翻译器 / Scalar expression translator
 *
 * @property resolveColumnName 属性路径解析器 / Property path resolver
 * @property dialect SQL 方言 / SQL dialect
 */
class JdbcClientScalarTranslator(
    private val resolveColumnName: JdbcClientColumnNameResolver,
    private val dialect: JdbcClientDialect
) {
    /**
     * 翻译标量表达式 / Translate a scalar expression
     *
     * @param expression 标量表达式 / Scalar expression
     * @return SQL 片段；不支持的表达式返回成功的 null / SQL fragment or successful null for unsupported expressions
     */
    fun translate(expression: ScalarExpression<*>): Ret<JdbcClientSqlFragment?> {
        return when (expression) {
            is ScalarReference<*> -> translateReference(expression)
            is ScalarConstant<*> -> Ok(JdbcClientSqlFragment("?", listOf(JdbcClientValueConverter.convert(expression.value))))
            is ScalarUnary<*> -> translateUnary(expression)
            is ScalarBinary<*> -> translateBinary(expression)
            is ScalarFunction<*> -> translateFunction(expression)
            else -> Ok(null)
        }
    }

    private fun translateReference(expression: ScalarReference<*>): Ret<JdbcClientSqlFragment?> {
        val column = resolveColumnName(expression.path.value) ?: return Ok(null)
        return quoteIdentifierPath(column, dialect).map { JdbcClientSqlFragment(it) }
    }

    private fun translateUnary(expression: ScalarUnary<*>): Ret<JdbcClientSqlFragment?> {
        val operand = when (val result = translate(expression.operand)) {
            is Ok -> result.value ?: return Ok(null)
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        val sql = when (expression.operator) {
            UnaryOperator.Negate -> "(-${operand.sql})"
            UnaryOperator.Positive -> "(+${operand.sql})"
            UnaryOperator.Abs -> "ABS(${operand.sql})"
        }
        return Ok(JdbcClientSqlFragment(sql, operand.parameters))
    }

    private fun translateBinary(expression: ScalarBinary<*>): Ret<JdbcClientSqlFragment?> {
        val left = when (val result = translate(expression.left)) {
            is Ok -> result.value ?: return Ok(null)
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        val right = when (val result = translate(expression.right)) {
            is Ok -> result.value ?: return Ok(null)
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        val operator = when (expression.operator) {
            BinaryOperator.Add -> "+"
            BinaryOperator.Subtract -> "-"
            BinaryOperator.Multiply -> "*"
            BinaryOperator.Divide -> "/"
            BinaryOperator.Modulo -> "%"
            BinaryOperator.Power -> return Ok(null)
        }
        return Ok(JdbcClientSqlFragment("(${left.sql} $operator ${right.sql})", left.parameters + right.parameters))
    }

    private fun translateFunction(expression: ScalarFunction<*>): Ret<JdbcClientSqlFragment?> {
        val arguments = mutableListOf<JdbcClientSqlFragment>()
        for (argument in expression.arguments) {
            when (val result = translate(argument)) {
                is Ok -> arguments.add(result.value ?: return Ok(null))
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
        }

        val name = when (expression.name.lowercase()) {
            ScalarFunctionNames.Abs -> "ABS"
            ScalarFunctionNames.Lower -> "LOWER"
            ScalarFunctionNames.Upper -> "UPPER"
            ScalarFunctionNames.Trim -> "TRIM"
            ScalarFunctionNames.Length -> "LENGTH"
            ScalarFunctionNames.Coalesce -> "COALESCE"
            else -> return Ok(null)
        }
        if (name != "COALESCE" && arguments.size != 1 || name == "COALESCE" && arguments.isEmpty()) {
            return Ok(null)
        }
        return Ok(
            JdbcClientSqlFragment(
                sql = "$name(${arguments.joinToString(", ") { it.sql }})",
                parameters = arguments.flatMap { it.parameters }
            )
        )
    }
}
