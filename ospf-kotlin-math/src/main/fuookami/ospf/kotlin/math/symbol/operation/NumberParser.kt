/**
 * Flt64 数字解析器
 * Flt64 Number Parser
 *
 * 提供将文本字符串解析为 Flt64 数值的功能。
 * Provides parsing of text strings into Flt64 numeric values.
 */
package fuookami.ospf.kotlin.math.symbol.operation

import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.parse.NumberParser

/**
 * Flt64 数字解析器单例
 * Flt64 number parser singleton
 *
 * 实现 NumberParser 接口，使用 Kotlin 标准库的 toDoubleOrNull 进行解析。
 * Implements NumberParser interface, using Kotlin stdlib's toDoubleOrNull for parsing.
 */
data object Flt64NumberParser : NumberParser<fuookami.ospf.kotlin.math.algebra.number.Flt64> {
    /**
     * 将文本解析为 Flt64 数值
     * Parse text into a Flt64 value
     *
     * @param text 待解析的文本 / Text to parse
     * @return 解析后的 Flt64 值，若解析失败则返回 null / Parsed Flt64 value, or null if parsing fails
     */
    override fun parse(text: String): Flt64? {
        return text.toDoubleOrNull()?.let(::Flt64)
    }
}
