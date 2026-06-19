/**
 * 多项式词法分析器
 * Polynomial Lexer
 *
 * 提供多项式表达式的词法分析功能，将字符串分解为词法单元序列。
 * Provides lexical analysis for polynomial expressions, decomposing strings into token sequences.
 */
package fuookami.ospf.kotlin.math.symbol.parse

import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Fatal

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

    fun lex(): ParseResult<List<PolynomialToken>> {
        val tokens = ArrayList<PolynomialToken>()
        while (true) {
            skipWhitespace()
            if (isEnd()) {
                tokens.add(PolynomialToken(PolynomialTokenType.End, "", index))
                return Ok(tokens)
            }
            val start = index
            val current = input[index]
            when {
                current.isDigit() || current == '.' -> {
                    when (val token = readNumber()) {
                        is Ok -> tokens.add(token.value)
                        is Failed -> return Failed(token.error)
                        is Fatal -> return Fatal(token.errors)
                    }
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
                        return parseLexicalFailed(input, "Unexpected character '!'", start)
                    }
                }

                else -> {
                    return parseLexicalFailed(input, "Unexpected character '$current'", start)
                }
            }
        }
    }

    /** 读取数字字面量（整数或小数） / Read a numeric literal (integer or decimal) */
    private fun readNumber(): ParseResult<PolynomialToken> {
        val start = index
        var hasDot = false
        if (input[index] == '.') {
            hasDot = true
            index += 1
            if (isEnd() || !input[index].isDigit()) {
                return parseLexicalFailed(input, "Invalid number", start)
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
        return Ok(
            PolynomialToken(
                type = PolynomialTokenType.Number,
                text = input.substring(start, index),
                position = start
            )
        )
    }

    /** 读取标识符（字母、数字或下划线组成的词） / Read an identifier (word composed of letters, digits, or underscores) */
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

    /** 跳过空白字符 / Skip whitespace characters */
    private fun skipWhitespace() {
        while (!isEnd() && input[index].isWhitespace()) {
            index += 1
        }
    }

    /** 判断是否已到达输入末尾 / Check whether the end of input has been reached */
    private fun isEnd(): Boolean {
        return index >= input.length
    }

    /** 查看下一个字符，不存在则返回 null / Peek at the next character, or null if absent */
    private fun peekNext(): Char? {
        return if (index + 1 < input.length) {
            input[index + 1]
        } else {
            null
        }
    }
}
