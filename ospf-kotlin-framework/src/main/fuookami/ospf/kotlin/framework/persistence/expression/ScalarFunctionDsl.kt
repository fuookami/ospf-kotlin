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

@Suppress("UNCHECKED_CAST")
private fun anyScalar(expr: ScalarExpression<*>): ScalarExpression<Any?> = expr as ScalarExpression<Any?>

private fun function(name: String, arguments: List<ScalarExpression<*>>): ScalarFunction<Any?> {
    return ScalarFunction(name, arguments.map { anyScalar(it) })
}

fun lower(expr: ScalarExpression<*>): ScalarFunction<Any?> =
    function(ScalarFunctionNames.Lower, listOf(expr))

fun lower(path: PathBuilder): ScalarFunction<Any?> = lower(path.asScalar<Any?>())

fun upper(expr: ScalarExpression<*>): ScalarFunction<Any?> =
    function(ScalarFunctionNames.Upper, listOf(expr))

fun upper(path: PathBuilder): ScalarFunction<Any?> = upper(path.asScalar<Any?>())

fun trim(expr: ScalarExpression<*>): ScalarFunction<Any?> =
    function(ScalarFunctionNames.Trim, listOf(expr))

fun trim(path: PathBuilder): ScalarFunction<Any?> = trim(path.asScalar<Any?>())

fun length(expr: ScalarExpression<*>): ScalarFunction<Any?> =
    function(ScalarFunctionNames.Length, listOf(expr))

fun length(path: PathBuilder): ScalarFunction<Any?> = length(path.asScalar<Any?>())

fun coalesce(vararg expressions: ScalarExpression<*>): ScalarFunction<Any?> =
    function(ScalarFunctionNames.Coalesce, expressions.toList())

fun coalesce(vararg paths: PathBuilder): ScalarFunction<Any?> =
    coalesce(*paths.map { it.asScalar<Any?>() }.toTypedArray())
