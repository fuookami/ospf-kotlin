/**
 * 解析结果与解析问题
 * Parse Result and Parse Issues
 *
 * 定义多项式解析的结果类型别名和解析问题类型。
 * Defines type aliases for polynomial parse results and parse issue types.
 */
package fuookami.ospf.kotlin.math.symbol.parse

import fuookami.ospf.kotlin.utils.functional.Ret

/** 解析结果类型别名 / Parse result type alias */
typealias ParseResult<T> = Ret<T>

/**
 * 解析问题类型
 * Parse issue type
 */
enum class ParseIssueType {
    /** 词法错误 / Lexical error */
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
