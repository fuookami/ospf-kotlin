/**
 * 标记定义
 * Token Definition
 *
 * 定义词法分析器输出的标记类型和数据结构。
 * 每个标记包含类型、文本内容和在源字符串中的位置。
 * Defines token types and data structures output by the lexer.
 * Each token contains type, text content, and position in the source string.
 */
package fuookami.ospf.kotlin.math.symbol.parser

import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.value_range.*

enum class TokenType {
    Number,
    Identifier,
    Plus,
    Minus,
    Star,
    Caret,
    LeftParen,
    RightParen,
    Comma,
    Less,
    LessEqual,
    Equal,
    NotEqual,
    GreaterEqual,
    Greater,
    End
}

data class Token(
    val type: TokenType,
    val text: String,
    val position: Int
)

