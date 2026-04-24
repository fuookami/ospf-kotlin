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

import fuookami.ospf.kotlin.utils.functional.Ret

@Deprecated(
    message = "Use fuookami.ospf.kotlin.math.symbol.parse.ParseResult instead.",
    replaceWith = ReplaceWith("fuookami.ospf.kotlin.math.symbol.parse.ParseResult", "fuookami.ospf.kotlin.math.symbol.parse")
)
typealias ParseResult<T> = fuookami.ospf.kotlin.math.symbol.parse.ParseResult<T>

@Deprecated(
    message = "Use fuookami.ospf.kotlin.math.symbol.parse.ParseIssueType instead.",
    replaceWith = ReplaceWith("fuookami.ospf.kotlin.math.symbol.parse.ParseIssueType", "fuookami.ospf.kotlin.math.symbol.parse")
)
typealias ParseIssueType = fuookami.ospf.kotlin.math.symbol.parse.ParseIssueType

@Deprecated(
    message = "Use fuookami.ospf.kotlin.math.symbol.parse.ParseIssue instead.",
    replaceWith = ReplaceWith("fuookami.ospf.kotlin.math.symbol.parse.ParseIssue", "fuookami.ospf.kotlin.math.symbol.parse")
)
typealias ParseIssue = fuookami.ospf.kotlin.math.symbol.parse.ParseIssue

class ParseError(
    message: String,
    val position: Int
) : IllegalArgumentException("$message at position $position")