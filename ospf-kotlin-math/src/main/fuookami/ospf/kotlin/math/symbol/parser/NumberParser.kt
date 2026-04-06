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

import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.Int64

fun interface NumberParser<T> {
    fun parse(text: String): T?
}

data object Flt64NumberParser : NumberParser<Flt64> {
    override fun parse(text: String): Flt64? {
        return text.toDoubleOrNull()?.let(::Flt64)
    }
}

data object Int64NumberParser : NumberParser<Int64> {
    override fun parse(text: String): Int64? {
        return text.toLongOrNull()?.let(::Int64)
    }
}

