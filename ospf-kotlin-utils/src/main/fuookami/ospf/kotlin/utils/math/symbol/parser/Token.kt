package fuookami.ospf.kotlin.utils.math.symbol.parser

import fuookami.ospf.kotlin.utils.math.algebra.number.*
import fuookami.ospf.kotlin.utils.math.algebra.concept.*
import fuookami.ospf.kotlin.utils.math.algebra.value_range.*

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

