/**
 * 数字解析器
 * Number Parser
 *
 * 提供字符串到数值类型的解析接口。
 * 内置 Flt64 和 Int64 的解析器实现，支持自定义数值类型的扩展。
 * Provides parsing interface from string to numeric types.
 * Includes built-in parser implementations for Flt64 and Int64,
 * supporting extension for custom numeric types.
 */
package fuookami.ospf.kotlin.math.symbol.parser

@Deprecated(
    message = "Use fuookami.ospf.kotlin.math.symbol.parse.NumberParser instead.",
    replaceWith = ReplaceWith("fuookami.ospf.kotlin.math.symbol.parse.NumberParser", "fuookami.ospf.kotlin.math.symbol.parse")
)
typealias NumberParser<T> = fuookami.ospf.kotlin.math.symbol.parse.NumberParser<T>

@Deprecated(
    message = "Use fuookami.ospf.kotlin.math.symbol.parse.Flt64NumberParser instead.",
    replaceWith = ReplaceWith("fuookami.ospf.kotlin.math.symbol.parse.Flt64NumberParser", "fuookami.ospf.kotlin.math.symbol.parse")
)
typealias Flt64NumberParser = fuookami.ospf.kotlin.math.symbol.parse.Flt64NumberParser

@Deprecated(
    message = "Use fuookami.ospf.kotlin.math.symbol.parse.Int64NumberParser instead.",
    replaceWith = ReplaceWith("fuookami.ospf.kotlin.math.symbol.parse.Int64NumberParser", "fuookami.ospf.kotlin.math.symbol.parse")
)
typealias Int64NumberParser = fuookami.ospf.kotlin.math.symbol.parse.Int64NumberParser
