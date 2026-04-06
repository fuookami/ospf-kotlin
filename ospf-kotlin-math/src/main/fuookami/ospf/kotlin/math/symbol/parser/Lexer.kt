/**
 * 词法分析器
 * Lexer
 *
 * 将输入字符串转换为标记（Token）序列。
 * 支持数字、标识符、运算符、括号和比较运算符的识别。
 * 是符号表达式解析的第一阶段。
 * Converts input strings into token sequences.
 * Supports recognition of numbers, identifiers, operators,
 * parentheses, and comparison operators.
 * This is the first stage of symbolic expression parsing.
 */
package fuookami.ospf.kotlin.math.symbol.parser

import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.value_range.*

class Lexer(
    private val input: String
) {
    private var index: Int = 0

    fun lex(): List<Token> {
        val tokens = ArrayList<Token>()
        while (true) {
            skipWhitespace()
            if (isEnd()) {
                tokens.add(Token(TokenType.End, "", index))
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
                    tokens.add(Token(TokenType.Plus, "+", start))
                }

                current == '-' -> {
                    index += 1
                    tokens.add(Token(TokenType.Minus, "-", start))
                }

                current == '*' -> {
                    index += 1
                    tokens.add(Token(TokenType.Star, "*", start))
                }

                current == '^' -> {
                    index += 1
                    tokens.add(Token(TokenType.Caret, "^", start))
                }

                current == '(' -> {
                    index += 1
                    tokens.add(Token(TokenType.LeftParen, "(", start))
                }

                current == ')' -> {
                    index += 1
                    tokens.add(Token(TokenType.RightParen, ")", start))
                }

                current == ',' -> {
                    index += 1
                    tokens.add(Token(TokenType.Comma, ",", start))
                }

                current == '<' -> {
                    if (peekNext() == '=') {
                        index += 2
                        tokens.add(Token(TokenType.LessEqual, "<=", start))
                    } else {
                        index += 1
                        tokens.add(Token(TokenType.Less, "<", start))
                    }
                }

                current == '>' -> {
                    if (peekNext() == '=') {
                        index += 2
                        tokens.add(Token(TokenType.GreaterEqual, ">=", start))
                    } else {
                        index += 1
                        tokens.add(Token(TokenType.Greater, ">", start))
                    }
                }

                current == '=' -> {
                    index += 1
                    tokens.add(Token(TokenType.Equal, "=", start))
                }

                current == '!' -> {
                    if (peekNext() == '=') {
                        index += 2
                        tokens.add(Token(TokenType.NotEqual, "!=", start))
                    } else {
                        throw ParseError("Unexpected character '!'", start)
                    }
                }

                else -> {
                    throw ParseError("Unexpected character '$current'", start)
                }
            }
        }
    }

    private fun readNumber(): Token {
        val start = index
        var hasDot = false
        if (input[index] == '.') {
            hasDot = true
            index += 1
            if (isEnd() || !input[index].isDigit()) {
                throw ParseError("Invalid number", start)
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
        return Token(
            type = TokenType.Number,
            text = input.substring(start, index),
            position = start
        )
    }

    private fun readIdentifier(): Token {
        val start = index
        while (!isEnd()) {
            val c = input[index]
            if (c.isLetterOrDigit() || c == '_') {
                index += 1
            } else {
                break
            }
        }
        return Token(
            type = TokenType.Identifier,
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

fun lexSymbolExpression(input: String): List<Token> {
    return Lexer(input).lex()
}

