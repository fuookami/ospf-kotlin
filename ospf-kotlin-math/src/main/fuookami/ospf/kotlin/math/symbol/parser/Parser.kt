package fuookami.ospf.kotlin.math.symbol.parser

import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.value_range.*

class Parser(
    private val tokens: List<Token>
) {
    private var index: Int = 0

    fun parseExpression(): Expr {
        val expression = parseAdditive()
        expect(TokenType.End)
        return expression
    }

    fun parseInequality(): Expr.Comparison {
        val lhs = parseAdditive()
        val operator = when (peek().type) {
            TokenType.Less -> ComparisonOperator.Less
            TokenType.LessEqual -> ComparisonOperator.LessEqual
            TokenType.Equal -> ComparisonOperator.Equal
            TokenType.NotEqual -> ComparisonOperator.NotEqual
            TokenType.GreaterEqual -> ComparisonOperator.GreaterEqual
            TokenType.Greater -> ComparisonOperator.Greater
            else -> throw ParseError("Expected comparison operator", peek().position)
        }
        advance()
        val rhs = parseAdditive()
        expect(TokenType.End)
        return Expr.Comparison(
            left = lhs,
            operator = operator,
            right = rhs
        )
    }

    private fun parseAdditive(): Expr {
        var expression = parseMultiplicative()
        while (true) {
            expression = when (peek().type) {
                TokenType.Plus -> {
                    advance()
                    Expr.Binary(
                        left = expression,
                        operator = BinaryOperator.Add,
                        right = parseMultiplicative()
                    )
                }

                TokenType.Minus -> {
                    advance()
                    Expr.Binary(
                        left = expression,
                        operator = BinaryOperator.Subtract,
                        right = parseMultiplicative()
                    )
                }

                else -> {
                    return expression
                }
            }
        }
    }

    private fun parseMultiplicative(): Expr {
        var expression = parsePower()
        while (peek().type == TokenType.Star) {
            advance()
            expression = Expr.Binary(
                left = expression,
                operator = BinaryOperator.Multiply,
                right = parsePower()
            )
        }
        return expression
    }

    private fun parsePower(): Expr {
        val base = parseUnary()
        return if (peek().type == TokenType.Caret) {
            advance()
            Expr.Binary(
                left = base,
                operator = BinaryOperator.Power,
                right = parsePower()
            )
        } else {
            base
        }
    }

    private fun parseUnary(): Expr {
        return if (peek().type == TokenType.Minus) {
            advance()
            Expr.UnaryMinus(parseUnary())
        } else {
            parsePrimary()
        }
    }

    private fun parsePrimary(): Expr {
        return when (val token = peek()) {
            is Token -> {
                when (token.type) {
                    TokenType.Number -> {
                        advance()
                        Expr.NumberLiteral(token.text)
                    }

                    TokenType.Identifier -> {
                        advance()
                        if (peek().type == TokenType.LeftParen) {
                            parseFunctionCall(token)
                        } else {
                            Expr.Identifier(token.text)
                        }
                    }

                    TokenType.LeftParen -> {
                        advance()
                        val expression = parseAdditive()
                        expect(TokenType.RightParen)
                        expression
                    }

                    else -> {
                        throw ParseError("Unexpected token '${token.text}'", token.position)
                    }
                }
            }
        }
    }

    private fun parseFunctionCall(identifier: Token): Expr.FunctionCall {
        expect(TokenType.LeftParen)
        if (peek().type == TokenType.RightParen) {
            advance()
            return Expr.FunctionCall(name = identifier.text, arguments = emptyList())
        }
        val arguments = ArrayList<Expr>()
        while (true) {
            arguments.add(parseAdditive())
            if (peek().type == TokenType.Comma) {
                advance()
                continue
            }
            break
        }
        expect(TokenType.RightParen)
        return Expr.FunctionCall(name = identifier.text, arguments = arguments)
    }

    private fun peek(): Token {
        return tokens.getOrElse(index) {
            Token(TokenType.End, "", if (tokens.isEmpty()) 0 else tokens.last().position)
        }
    }

    private fun advance(): Token {
        val token = peek()
        index += 1
        return token
    }

    private fun expect(type: TokenType): Token {
        val token = peek()
        if (token.type != type) {
            throw ParseError("Expected token $type but got ${token.type}", token.position)
        }
        index += 1
        return token
    }
}

fun parseSymbolExpression(input: String): Expr {
    return Parser(lexSymbolExpression(input)).parseExpression()
}

fun parseSymbolInequality(input: String): Expr.Comparison {
    return Parser(lexSymbolExpression(input)).parseInequality()
}

