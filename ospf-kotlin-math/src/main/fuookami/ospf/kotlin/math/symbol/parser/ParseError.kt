/**
 * 解析错误
 * Parse Error
 *
 * 定义词法分析和语法分析过程中的错误类型。
 * 包含错误消息和在源字符串中的位置信息。
 * Defines error types during lexical analysis and parsing.
 * Contains error message and position information in the source string.
 */
package fuookami.ospf.kotlin.math.symbol.parser

import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.value_range.*

class ParseError(
    message: String,
    val position: Int
) : IllegalArgumentException("$message at position $position")

