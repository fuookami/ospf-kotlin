/**
 * 仓储标量函数 DSL
 * Repository scalar function DSL
 *
 * 提供面向仓储谓词的字符串与空值函数入口。
 * Provides string and null-handling function entry points for repository predicates.
 */
package fuookami.ospf.kotlin.framework.persistence.expression

import fuookami.ospf.kotlin.math.symbol.expression.*
import fuookami.ospf.kotlin.math.symbol.expression.dsl.PathBuilder

/** 将标量表达式无损转换为 Any? 类型 / Cast scalar expression to Any? type losslessly */
@Suppress("UNCHECKED_CAST")
private fun anyScalar(expr: ScalarExpression<*>): ScalarExpression<Any?> = expr as ScalarExpression<Any?>

/**
 * 构建标量函数
 * Build scalar function
 *
 * @param name 函数名 / Function name
 * @param arguments 参数列表 / Argument list
 * @return 标量函数表达式 / Scalar function expression
 */
private fun function(name: String, arguments: List<ScalarExpression<*>>): ScalarFunction<Any?> {
    return ScalarFunction(name, arguments.map { anyScalar(it) })
}

/**
 * 将表达式转为小写
 * Convert expression to lowercase
 *
 * @param expr 输入表达式 / Input expression
 * @return 小写转换函数表达式 / Lowercase conversion function expression
 */
fun lower(expr: ScalarExpression<*>): ScalarFunction<Any?> =
    function(ScalarFunctionNames.Lower, listOf(expr))

/**
 * 将路径转为小写
 * Convert path to lowercase
 *
 * @param path 字段路径 / Field path
 * @return 小写转换函数表达式 / Lowercase conversion function expression
 */
fun lower(path: PathBuilder): ScalarFunction<Any?> = lower(path.asScalar<Any?>())

/**
 * 将表达式转为大写
 * Convert expression to uppercase
 *
 * @param expr 输入表达式 / Input expression
 * @return 大写转换函数表达式 / Uppercase conversion function expression
 */
fun upper(expr: ScalarExpression<*>): ScalarFunction<Any?> =
    function(ScalarFunctionNames.Upper, listOf(expr))

/**
 * 将路径转为大写
 * Convert path to uppercase
 *
 * @param path 字段路径 / Field path
 * @return 大写转换函数表达式 / Uppercase conversion function expression
 */
fun upper(path: PathBuilder): ScalarFunction<Any?> = upper(path.asScalar<Any?>())

/**
 * 去除表达式两端空白
 * Trim whitespace from both ends of expression
 *
 * @param expr 输入表达式 / Input expression
 * @return 去空白函数表达式 / Trim function expression
 */
fun trim(expr: ScalarExpression<*>): ScalarFunction<Any?> =
    function(ScalarFunctionNames.Trim, listOf(expr))

/**
 * 去除路径两端空白
 * Trim whitespace from both ends of path
 *
 * @param path 字段路径 / Field path
 * @return 去空白函数表达式 / Trim function expression
 */
fun trim(path: PathBuilder): ScalarFunction<Any?> = trim(path.asScalar<Any?>())

/**
 * 获取表达式长度
 * Get length of expression
 *
 * @param expr 输入表达式 / Input expression
 * @return 长度函数表达式 / Length function expression
 */
fun length(expr: ScalarExpression<*>): ScalarFunction<Any?> =
    function(ScalarFunctionNames.Length, listOf(expr))

/**
 * 获取路径长度
 * Get length of path
 *
 * @param path 字段路径 / Field path
 * @return 长度函数表达式 / Length function expression
 */
fun length(path: PathBuilder): ScalarFunction<Any?> = length(path.asScalar<Any?>())

/**
 * 返回第一个非空表达式
 * Return first non-null expression
 *
 * @param expressions 候选表达式列表 / Candidate expression list
 * @return 合并函数表达式 / Coalesce function expression
 */
fun coalesce(vararg expressions: ScalarExpression<*>): ScalarFunction<Any?> =
    function(ScalarFunctionNames.Coalesce, expressions.toList())

/**
 * 返回第一个非空路径
 * Return first non-null path
 *
 * @param paths 候选路径列表 / Candidate path list
 * @return 合并函数表达式 / Coalesce function expression
 */
fun coalesce(vararg paths: PathBuilder): ScalarFunction<Any?> =
    coalesce(*paths.map { it.asScalar<Any?>() }.toTypedArray())
