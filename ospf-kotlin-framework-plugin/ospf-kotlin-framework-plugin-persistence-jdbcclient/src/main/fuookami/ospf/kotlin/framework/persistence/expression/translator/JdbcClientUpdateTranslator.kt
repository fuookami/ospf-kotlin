/**
 * JdbcClient 更新翻译器 / JdbcClient update translator
 *
 * 在执行更新前验证全部字段和值表达式，避免构造部分 SET 子句。
 * Validates every field and value expression before execution, so no partial SET clause can be applied.
 */
package fuookami.ospf.kotlin.framework.persistence.expression.translator

import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.symbol.expression.ScalarExpression
import fuookami.ospf.kotlin.framework.persistence.expression.*
import fuookami.ospf.kotlin.framework.persistence.jdbcclient.*

/**
 * 更新赋值翻译器 / Update assignment translator
 *
 * @property resolveColumnName 属性路径解析器 / Property path resolver
 * @property dialect SQL 方言 / SQL dialect
 */
class JdbcClientUpdateTranslator(
    private val resolveColumnName: JdbcClientColumnNameResolver,
    private val dialect: JdbcClientDialect
) {
    /**
     * 翻译全部更新赋值 / Translate all update assignments
     *
     * @param assignments 更新赋值集合 / Update assignments
     * @return SET 子句及有序参数 / SET clause and ordered parameters
     */
    fun translate(assignments: UpdateAssignments): Ret<JdbcClientSqlFragment> {
        if (assignments.isEmpty()) return Ok(JdbcClientSqlFragment(""))
        val clauses = mutableListOf<String>()
        val parameters = mutableListOf<Any?>()
        val scalarTranslator = JdbcClientScalarTranslator(resolveColumnName, dialect)
        for (assignment in assignments.items) {
            val column = resolveColumnName(assignment.path)
                ?: return invalidAssignment(assignment.path, "字段无法解析 / Field cannot be resolved")
            val quoted = when (val result = quoteIdentifierPath(column, dialect)) {
                is Ok -> result.value
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
            when (assignment) {
                is SetValue -> {
                    clauses.add("$quoted = ?")
                    parameters.add(JdbcClientValueConverter.convert(assignment.value))
                }
                is SetNull -> {
                    clauses.add("$quoted = ?")
                    parameters.add(null)
                }
                is SetFromExpression -> {
                    val expression = when (val result = scalarTranslator.translate(assignment.expression)) {
                        is Ok -> result.value ?: return invalidAssignment(
                            assignment.path,
                            "赋值表达式不支持：${assignment.expression.typeName} / Unsupported value expression: ${assignment.expression.typeName}"
                        )
                        is Failed -> return Failed(result.error)
                        is Fatal -> return Fatal(result.errors)
                    }
                    clauses.add("$quoted = ${expression.sql}")
                    parameters.addAll(expression.parameters)
                }
            }
        }
        return Ok(JdbcClientSqlFragment(clauses.joinToString(", "), parameters))
    }

    private fun invalidAssignment(path: String, reason: String): Ret<JdbcClientSqlFragment> {
        return Failed(
            ErrorCode.IllegalArgument,
            "更新字段 [$path] 无效：$reason / Invalid update field [$path]: $reason"
        )
    }
}
