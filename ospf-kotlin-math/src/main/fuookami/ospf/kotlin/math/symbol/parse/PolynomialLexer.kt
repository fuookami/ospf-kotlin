/**
 * 多项式词法分析器
 * Polynomial Lexer
 *
 * 提供多项式表达式的词法分析功能，将字符串分解为词法单元序列。
 * Provides lexical analysis for polynomial expressions, decomposing strings into token sequences.
 */
package fuookami.ospf.kotlin.math.symbol.parse

internal enum class PolynomialTokenType {
    Number,
    Identifier,
    Plus,
    Minus,
    Star,
    Caret,
    LeftParen,
    RightParen,
    Less,
    LessEqual,
    Equal,
    NotEqual,
    GreaterEqual,
    Greater,
    End
}

internal data class PolynomialToken(
    val type: PolynomialTokenType,
    val text: String,
    val position: Int
)

internal class PolynomialLexer(
    private val input: String
) {
    private var index: Int = 0

    fun lex(): List<PolynomialToken> {
        val tokens = ArrayList<PolynomialToken>()
        while (true) {
            skipWhitespace()
            if (isEnd()) {
                tokens.add(PolynomialToken(PolynomialTokenType.End, "", index))
                return tokens
            }
            val start = index
            val current = input[index]
            when {
                current.isDigit() || current == '.' -> {
                    tokens.add(readNumber())
                }

                current.isLetter() || current == '_' -> {
                    tokens.add(readIdentifier())
                }

                current == '+' -> {
                    index += 1
                    tokens.add(PolynomialToken(PolynomialTokenType.Plus, "+", start))
                }

                current == '-' -> {
                    index += 1
                    tokens.add(PolynomialToken(PolynomialTokenType.Minus, "-", start))
                }

                current == '*' -> {
                    index += 1
                    tokens.add(PolynomialToken(PolynomialTokenType.Star, "*", start))
                }

                current == '^' -> {
                    index += 1
                    tokens.add(PolynomialToken(PolynomialTokenType.Caret, "^", start))
                }

                current == '(' -> {
                    index += 1
                    tokens.add(PolynomialToken(PolynomialTokenType.LeftParen, "(", start))
                }

                current == ')' -> {
                    index += 1
                    tokens.add(PolynomialToken(PolynomialTokenType.RightParen, ")", start))
                }

                current == '<' -> {
                    if (peekNext() == '=') {
                        index += 2
                        tokens.add(PolynomialToken(PolynomialTokenType.LessEqual, "<=", start))
                    } else {
                        index += 1
                        tokens.add(PolynomialToken(PolynomialTokenType.Less, "<", start))
                    }
                }

                current == '>' -> {
                    if (peekNext() == '=') {
                        index += 2
                        tokens.add(PolynomialToken(PolynomialTokenType.GreaterEqual, ">=", start))
                    } else {
                        index += 1
                        tokens.add(PolynomialToken(PolynomialTokenType.Greater, ">", start))
                    }
                }

                current == '=' -> {
                    index += 1
                    tokens.add(PolynomialToken(PolynomialTokenType.Equal, "=", start))
                }

                current == '!' -> {
                    if (peekNext() == '=') {
                        index += 2
                        tokens.add(PolynomialToken(PolynomialTokenType.NotEqual, "!=", start))
                    } else {
                        throw DirectParseError("Unexpected character '!'", start)
                    }
                }

                else -> {
                    throw DirectParseError("Unexpected character '$current'", start)
                }
            }
        }
    }

    private fun readNumber(): PolynomialToken {
        val start = index
        var hasDot = false
        if (input[index] == '.') {
            hasDot = true
            index += 1
            if (isEnd() || !input[index].isDigit()) {
                throw DirectParseError("Invalid number", start)
            }
        }
        while (!isEnd()) {
            val c = input[index]
            if (c.isDigit()) {
                index += 1
            } else if (c == '.' && !hasDot) {
                hasDot = true
                index += 1
            } else {
                break
            }
        }
        return PolynomialToken(
            type = PolynomialTokenType.Number,
            text = input.substring(start, index),
            position = start
        )
    }

    private fun readIdentifier(): PolynomialToken {
        val start = index
        while (!isEnd()) {
            val c = input[index]
            if (c.isLetterOrDigit() || c == '_') {
                index += 1
            } else {
                break
            }
        }
        return PolynomialToken(
            type = PolynomialTokenType.Identifier,
            text = input.substring(start, index),
            position = start
        )
    }

    private fun skipWhitespace() {
        while (!isEnd() && input[index].isWhitespace()) {
            index += 1
        }
    }

    private fun isEnd(): Boolean {
        return index >= input.length
    }

    private fun peekNext(): Char? {
        return if (index + 1 < input.length) {
            input[index + 1]
        } else {
            null
        }
    }
}

internal class DirectParseError(
    message: String,
    val position: Int
) : IllegalArgumentException("$message at position $position")
