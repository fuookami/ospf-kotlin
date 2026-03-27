package fuookami.ospf.kotlin.utils.math.symbol.parser

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
