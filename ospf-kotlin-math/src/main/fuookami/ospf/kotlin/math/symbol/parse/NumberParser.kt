/**
 * 数值解析器
 * Number Parser
 *
 * 定义数值解析器接口及 Int64 的解析实现。
 * Defines the number parser interface and Int64 parsing implementation.
 */
package fuookami.ospf.kotlin.math.symbol.parse

import fuookami.ospf.kotlin.math.algebra.number.Int64

/**
 * 数值解析器函数式接口
 * Number parser functional interface
 *
 * @param T 解析目标类型 / The target type to parse into
 */
fun interface NumberParser<T> {
    /**
     * 将字符串解析为数值
     * Parse a string into a numeric value
     *
     * @param text 待解析的字符串 / The string to parse
     * @return 解析结果，失败返回 null / The parsed value, or null if parsing fails
     */
    fun parse(text: String): T?
}

/**
 * Int64 数值解析器
 * Int64 number parser
 */
data object Int64NumberParser : NumberParser<Int64> {
    override fun parse(text: String): Int64? {
        return text.toLongOrNull()?.let(::Int64)
    }
}
