/**
 * 多项式词法分析器
 * Polynomial Lexer
 *
 * 提供多项式表达式的词法分析功能，将字符串分解为词法单元序列。
 * Provides lexical analysis for polynomial expressions, decomposing strings into token sequences.
 */
package fuookami.ospf.kotlin.math.symbol.parse

import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Fatal
import fuookami.ospf.kotlin.utils.functional.Ok

/**
 * Token types for polynomial lexical analysis.
 * 多项式词法分析的词法单元类型。
 */
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

/**
 * A token produced by the polynomial lexer.
 * 多项式词法分析器产生的词法单元。
 *
 * @property type the token type / 词法单元类型
 * @property text the raw text of the token / 词法单元的原始文本
 * @property position the starting position in the input string / 在输入字符串中的起始位置
 */
internal data class PolynomialToken(
    val type: PolynomialTokenType,
    val text: String,
    val position: Int
)

/**
 * Lexer for polynomial expressions, tokenizing an input string into a sequence of tokens.
 * 多项式表达式的词法分析器，将输入字符串分解为词法单元序列。
 *
 * @property input the input string to tokenize / 待词法分析的输入字符串
 */
internal class PolynomialLexer(
    private val input: String
) {
    private var index: Int = 0

    /**
     * Executes lexical analysis on the input string.
     * 对输入字符串执行词法分析。
     *
     * @return the list of tokens or a parse error / 词法单元列表或解析错误
     */
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

    /**
     * Reads a numeric literal (integer or decimal) from the input.
     * 从输入中读取数字字面量（整数或小数）。
     *
     * @return the numeric token or a parse error / 数字词法单元或解析错误
     */
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

    /**
     * Reads an identifier (word composed of letters, digits, or underscores) from the input.
     * 从输入中读取标识符（由字母、数字或下划线组成的词）。
     *
     * @return the identifier token / 标识符词法单元
     */
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

    /**
     * Skips whitespace characters in the input.
     * 跳过输入中的空白字符。
     */
    private fun skipWhitespace() {
        while (!isEnd() && input[index].isWhitespace()) {
            index += 1
        }
    }

    /**
     * Checks whether the end of input has been reached.
     * 判断是否已到达输入末尾。
     *
     * @return whether the index is past the end of input / 索引是否已超过输入末尾
     */
    private fun isEnd(): Boolean {
        return index >= input.length
    }

    /**
     * Peeks at the next character without advancing the index.
     * 查看下一个字符但不推进索引。
     *
     * @return the next character, or null if absent / 下一个字符，不存在则返回 null
     */
    private fun peekNext(): Char? {
        return if (index + 1 < input.length) {
            input[index + 1]
        } else {
            null
        }
    }
}
