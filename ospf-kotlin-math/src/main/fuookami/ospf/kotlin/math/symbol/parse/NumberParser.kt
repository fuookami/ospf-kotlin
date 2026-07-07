/**
 * Number parser
 * 数值解析器
 *
 * Defines the number parser interface and Int64 parsing implementation.
 * 定义数值解析器接口及 Int64 的解析实现。
 */
package fuookami.ospf.kotlin.math.symbol.parse

import fuookami.ospf.kotlin.math.algebra.number.Int64

/**
 * Number parser functional interface
 * 数值解析器函数式接口
 *
 * @param T The target type to parse into / 中文：解析目标类型
 */
fun interface NumberParser<T> {
    /**
     * Parse a string into a numeric value
     * 将字符串解析为数值
     *
     * @param text The string to parse / 中文：待解析的字符串
     * @return The parsed value, or null if parsing fails / 中文：解析结果，失败返回 null
     */
    fun parse(text: String): T?
}

/**
 * Int64 number parser
 * Int64 数值解析器
 */
data object Int64NumberParser : NumberParser<Int64> {
    override fun parse(text: String): Int64? {
        return text.toLongOrNull()?.let(::Int64)
    }
}
