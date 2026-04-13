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
import fuookami.ospf.kotlin.utils.functional.Ret

/**
 * 解析结果类型别名
 * Parse result type alias
 *
 * 使用 `Ret<T>` 作为解析结果的统一返回类型，支持成功、失败和致命错误三种状态。
 * Alias for `Ret<T>` as the unified return type for parsing operations,
 * supporting success, failure, and fatal error states.
 */
typealias ParseResult<T> = Ret<T>

/**
 * 解析问题类型枚举
 * Parse Issue Type Enumeration
 *
 * 标识解析过程中遇到问题的分类：
 * - [Lexical]: 词法错误（非法字符、无效数字等）
 * - [Syntax]: 语法错误（括号不匹配、运算符位置错误等）
 * - [Conversion]: 转换错误（表达式无法转换为目标类型）
 * - [Semantic]: 语义错误（符号未定义、类型不匹配等）
 * - [Unknown]: 未知的错误类型
 * Classifies issues encountered during parsing:
 * - [Lexical]: Lexical errors (invalid characters, invalid numbers, etc.)
 * - [Syntax]: Syntax errors (mismatched parentheses, misplaced operators, etc.)
 * - [Conversion]: Conversion errors (expression cannot be converted to target type)
 * - [Semantic]: Semantic errors (undefined symbols, type mismatches, etc.)
 * - [Unknown]: Unknown error type
 */
enum class ParseIssueType {
    Lexical,
    Syntax,
    Conversion,
    Semantic,
    Unknown
}

/**
 * 解析问题详情
 * Parse Issue Details
 *
 * 封装解析过程中遇到的非致命问题信息，包括问题类型、错误消息、
 * 原始输入和错误位置。用于构造可恢复的解析错误。
 * Encapsulates information about non-fatal issues encountered during parsing,
 * including issue type, error message, original input, and error position.
 * Used to construct recoverable parse errors.
 *
 * @property type 问题类型 / Issue type
 * @property message 错误描述 / Error description
 * @property input 导致错误的原始输入字符串（可选）/ Original input string that caused the error (optional)
 * @property position 错误在输入中的位置（可选）/ Position of the error in the input (optional)
 */
data class ParseIssue(
    val type: ParseIssueType,
    val message: String,
    val input: String? = null,
    val position: Int? = null
)

class ParseError(
    message: String,
    val position: Int
) : IllegalArgumentException("$message at position $position")

