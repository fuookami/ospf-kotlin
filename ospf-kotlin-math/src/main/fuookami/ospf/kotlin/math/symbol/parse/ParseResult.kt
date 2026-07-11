/**
 * Parse result and parse issues
 * 解析结果与解析问题
 *
 * Defines type aliases for polynomial parse results and parse issue types.
 * 定义多项式解析的结果类型别名和解析问题类型。
*/
package fuookami.ospf.kotlin.math.symbol.parse

import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Fatal
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.Ret

/** Parse result type alias / 中文：解析结果类型别名 */
typealias ParseResult<T> = Ret<T>

/**
 * Parse issue type
 * 解析问题类型
*/
enum class ParseIssueType {
    /** Lexical error / 中文：词法错误 */
    Lexical,
    /** 语法错误 / Syntax error */
    Syntax,
    /** 转换错误 / Conversion error */
    Conversion,
    /** 语义错误 / Semantic error */
    Semantic,
    /** 未知错误 / Unknown error */
    Unknown
}

/**
 * 解析问题
 * Parse issue
 *
 * @param type 问题类型 / Issue type
 * @param message 问题描述 / Issue description
 * @param input 输入字符串 / Input string
 * @param position 问题位置 / Issue position
*/
data class ParseIssue(
    val type: ParseIssueType,
    val message: String,
    val input: String? = null,
    val position: Int? = null
)

/** 构造解析失败结果 / Build a failed parse result */
internal fun <T> parseFailed(issue: ParseIssue): ParseResult<T> {
    return Failed(ErrorCode.IllegalArgument, issue)
}

/** 构造词法失败结果 / Build a lexical parse failure */
internal fun <T> parseLexicalFailed(
    input: String,
    message: String,
    position: Int? = null
): ParseResult<T> {
    return parseFailed(
        ParseIssue(
            type = ParseIssueType.Lexical,
            message = message,
            input = input,
            position = position
        )
    )
}

/** 构造语法失败结果 / Build a syntax parse failure */
internal fun <T> parseSyntaxFailed(
    input: String,
    message: String,
    position: Int? = null
): ParseResult<T> {
    return parseFailed(
        ParseIssue(
            type = ParseIssueType.Syntax,
            message = message,
            input = input,
            position = position
        )
    )
}

/** 构造语义失败结果 / Build a semantic parse failure */
internal fun <T> parseSemanticFailed(
    input: String,
    message: String,
    position: Int? = null
): ParseResult<T> {
    return parseFailed(
        ParseIssue(
            type = ParseIssueType.Semantic,
            message = message,
            input = input,
            position = position
        )
    )
}

/** 构造转换失败结果 / Build a conversion parse failure */
internal fun <T> parseConversionFailed(input: String, message: String): ParseResult<T> {
    return parseFailed(
        ParseIssue(
            type = ParseIssueType.Conversion,
            message = message,
            input = input
        )
    )
}

/** 构造未知失败结果 / Build an unknown parse failure */
internal fun <T> parseUnknownFailed(input: String, error: Throwable): ParseResult<T> {
    return parseFailed(
        ParseIssue(
            type = ParseIssueType.Unknown,
            message = error.message ?: "Unexpected error.",
            input = input
        )
    )
}

/** 串联解析结果 / Chain parse results */
internal inline fun <T, U> ParseResult<T>.andThen(block: (T) -> ParseResult<U>): ParseResult<U> {
    return when (this) {
        is Ok -> block(value)
        is Failed -> Failed(error)
        is Fatal -> Fatal(errors)
    }
}
