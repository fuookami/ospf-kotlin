/**
 * 语法分析器
 * Parser
 *
 * 将标记序列解析为表达式抽象语法树（AST）。
 * 支持表达式和不等式的解析，遵循标准运算符优先级。
 * 是符号表达式解析的第二阶段。
 * Parses token sequences into expression abstract syntax trees (AST).
 * Supports parsing of expressions and inequalities,
 * following standard operator precedence.
 * This is the second stage of symbolic expression parsing.
 */
package fuookami.ospf.kotlin.math.symbol.parser

import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.value_range.*
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Fatal
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.inequality.Comparison
import fuookami.ospf.kotlin.math.symbol.inequality.LinearInequality
import fuookami.ospf.kotlin.math.symbol.inequality.QuadraticInequality
import fuookami.ospf.kotlin.math.symbol.polynomial.CanonicalPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.QuadraticPolynomial
import fuookami.ospf.kotlin.math.symbol.parse.parseCanonicalRet
import fuookami.ospf.kotlin.math.symbol.parse.parseCanonicalTypedRet
import fuookami.ospf.kotlin.math.symbol.parse.parseLinearRet
import fuookami.ospf.kotlin.math.symbol.parse.parseLinearTypedRetOrNull
import fuookami.ospf.kotlin.math.symbol.parse.parseQuadraticRet
import fuookami.ospf.kotlin.math.symbol.parse.parseQuadraticTypedRetOrNull
import fuookami.ospf.kotlin.math.symbol.parse.parseCanonicalInequalityRet
import fuookami.ospf.kotlin.math.symbol.parse.parseLinearInequalityRet
import fuookami.ospf.kotlin.math.symbol.parse.parseQuadraticInequalityRet

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

fun parseLegacySymbolExpression(input: String): Expr {
    return Parser(lexSymbolExpression(input)).parseExpression()
}

fun parseLegacySymbolInequality(input: String): Expr.Comparison {
    return Parser(lexSymbolExpression(input)).parseInequality()
}

@Deprecated(
    message = "parseSymbolExpression parses the legacy Expr AST. Prefer parseLegacySymbolExpression for explicit legacy usage or symbol.expression.parser for the new expression stack.",
    replaceWith = ReplaceWith("parseLegacySymbolExpression(input)")
)
fun parseSymbolExpression(input: String): Expr {
    return parseLegacySymbolExpression(input)
}

@Deprecated(
    message = "parseSymbolInequality parses the legacy Expr AST. Prefer parseLegacySymbolInequality for explicit legacy usage or symbol.expression.parser for the new expression stack.",
    replaceWith = ReplaceWith("parseLegacySymbolInequality(input)")
)
fun parseSymbolInequality(input: String): Expr.Comparison {
    return parseLegacySymbolInequality(input)
}

private fun parseIssueTypeOf(error: ParseError): ParseIssueType {
    val message = error.message.orEmpty()
    return if (message.startsWith("Unexpected character") || message.startsWith("Invalid number")) {
        ParseIssueType.Lexical
    } else {
        ParseIssueType.Syntax
    }
}

private fun parseIssueOf(error: ParseError, input: String): ParseIssue {
    val normalizedMessage = error.message?.removeSuffix(" at position ${error.position}") ?: "Parse error."
    return ParseIssue(
        type = parseIssueTypeOf(error),
        message = normalizedMessage,
        input = input,
        position = error.position
    )
}

private fun <T> parseFailed(issue: ParseIssue): ParseResult<T> {
    return Failed(ErrorCode.IllegalArgument, issue)
}

private fun <T> parseConversionFailed(input: String, message: String): ParseResult<T> {
    return parseFailed(
        ParseIssue(
            type = ParseIssueType.Conversion,
            message = message,
            input = input
        )
    )
}

private fun <T> parseUnknownFailed(input: String, message: String): ParseResult<T> {
    return parseFailed(
        ParseIssue(
            type = ParseIssueType.Unknown,
            message = message,
            input = input
        )
    )
}

private inline fun <T> mapExpressionRet(
    input: String,
    crossinline transform: (Expr) -> T
): ParseResult<T> {
    return when (val parsed = parseLegacySymbolExpressionRet(input)) {
        is Ok -> {
            try {
                Ok(transform(parsed.value))
            } catch (error: IllegalArgumentException) {
                parseConversionFailed(input, error.message ?: "Expression conversion failed.")
            } catch (error: Exception) {
                parseUnknownFailed(input, error.message ?: "Unexpected conversion error.")
            }
        }

        is Failed -> Failed(parsed.error)
        is Fatal -> Fatal(parsed.errors)
    }
}

private inline fun <T> mapInequalityRet(
    input: String,
    crossinline transform: (Expr.Comparison) -> T
): ParseResult<T> {
    return when (val parsed = parseLegacySymbolInequalityRet(input)) {
        is Ok -> {
            try {
                Ok(transform(parsed.value))
            } catch (error: IllegalArgumentException) {
                parseConversionFailed(input, error.message ?: "Inequality conversion failed.")
            } catch (error: Exception) {
                parseUnknownFailed(input, error.message ?: "Unexpected conversion error.")
            }
        }

        is Failed -> Failed(parsed.error)
        is Fatal -> Fatal(parsed.errors)
    }
}

private fun ComparisonOperator.toInequalityComparison(): Comparison {
    return when (this) {
        ComparisonOperator.Less -> Comparison.LT
        ComparisonOperator.LessEqual -> Comparison.LE
        ComparisonOperator.Equal -> Comparison.EQ
        ComparisonOperator.NotEqual -> Comparison.NE
        ComparisonOperator.GreaterEqual -> Comparison.GE
        ComparisonOperator.Greater -> Comparison.GT
    }
}

/**
 * 解析符号表达式并返回 Ret 封装的结果
 * Parse a symbol expression and return a Ret-wrapped result
 *
 * 与 [parseLegacySymbolExpression] 不同，此函数不会抛出异常，而是通过 `Ret<Expr>` 返回解析结果。
 * 词法错误和语法错误会被包装为 [ParseIssue]，语义错误会被单独分类。
 * Unlike [parseLegacySymbolExpression], this function does not throw exceptions but returns
 * the result wrapped in `Ret<Expr>`. Lexical and syntax errors are wrapped as [ParseIssue],
 * and semantic errors are categorized separately.
 *
 * @param input 要解析的表达式字符串 / Expression string to parse
 * @return 解析结果，成功时返回表达式，失败时包含错误详情
 *         Parse result: the expression on success, error details on failure
 */
fun parseLegacySymbolExpressionRet(input: String): ParseResult<Expr> {
    return try {
        Ok(parseLegacySymbolExpression(input))
    } catch (error: ParseError) {
        parseFailed(parseIssueOf(error, input))
    } catch (error: IllegalArgumentException) {
        parseFailed(
            ParseIssue(
                type = ParseIssueType.Semantic,
                message = error.message ?: "Invalid expression.",
                input = input
            )
        )
    } catch (error: Exception) {
        parseUnknownFailed(input, error.message ?: "Unexpected parse error.")
    }
}

/**
 * 解析符号不等式并返回 Ret 封装的结果
 * Parse a symbol inequality and return a Ret-wrapped result
 *
 * 与 [parseLegacySymbolInequality] 不同，此函数不会抛出异常，而是通过 `Ret<Expr.Comparison>` 返回解析结果。
 * Unlike [parseLegacySymbolInequality], this function does not throw exceptions but returns
 * the result wrapped in `Ret<Expr.Comparison>`.
 *
 * @param input 要解析的不等式字符串 / Inequality string to parse
 * @return 解析结果，成功时返回比较表达式，失败时包含错误详情
 *         Parse result: the comparison expression on success, error details on failure
 */
fun parseLegacySymbolInequalityRet(input: String): ParseResult<Expr.Comparison> {
    return try {
        Ok(parseLegacySymbolInequality(input))
    } catch (error: ParseError) {
        parseFailed(parseIssueOf(error, input))
    } catch (error: IllegalArgumentException) {
        parseFailed(
            ParseIssue(
                type = ParseIssueType.Semantic,
                message = error.message ?: "Invalid inequality.",
                input = input
            )
        )
    } catch (error: Exception) {
        parseUnknownFailed(input, error.message ?: "Unexpected parse error.")
    }
}

@Deprecated(
    message = "parseSymbolExpressionRet parses the legacy Expr AST. Prefer parseLegacySymbolExpressionRet for explicit legacy usage or symbol.expression.parser for the new expression stack.",
    replaceWith = ReplaceWith("parseLegacySymbolExpressionRet(input)")
)
fun parseSymbolExpressionRet(input: String): ParseResult<Expr> {
    return parseLegacySymbolExpressionRet(input)
}

@Deprecated(
    message = "parseSymbolInequalityRet parses the legacy Expr AST. Prefer parseLegacySymbolInequalityRet for explicit legacy usage or symbol.expression.parser for the new expression stack.",
    replaceWith = ReplaceWith("parseLegacySymbolInequalityRet(input)")
)
fun parseSymbolInequalityRet(input: String): ParseResult<Expr.Comparison> {
    return parseLegacySymbolInequalityRet(input)
}

/**
 * 解析规范多项式表达式
 * Parse a canonical polynomial expression
 *
 * 将字符串解析为 CanonicalPolynomial。提供泛型和 Flt64 两个重载版本。
 * Parses a string into a CanonicalPolynomial. Provides both generic and Flt64 overloaded versions.
 *
 * @param input 要解析的表达式字符串 / Expression string to parse
 * @param numberParser 数字解析器 / Number parser for the target ring type
 * @param zero 零元 / Zero element of the ring
 * @param one 单位元 / One element of the ring
 * @param symbolOf 符号工厂函数 / Symbol factory function
 * @param isZero 零值判断函数 / Zero check function
 * @param symbolComparator 符号比较器（用于项合并，可选）/ Symbol comparator for term combining (optional)
 * @return 解析结果 / Parse result
 */
fun <T> parseCanonical(
    input: String,
    numberParser: NumberParser<T>,
    zero: T,
    one: T,
    symbolOf: (String) -> Symbol = ::symbolOfSerializedIdentifier,
    isZero: (T) -> Boolean = { it == zero },
    symbolComparator: Comparator<Symbol>? = null
): ParseResult<CanonicalPolynomial<T>> where T : Ring<T> {
    return parseCanonicalTypedRet(input, numberParser, zero, one, symbolOf, isZero, symbolComparator)
}

fun parseCanonical(
    input: String,
    symbolOf: (String) -> Symbol = ::symbolOfSerializedIdentifier,
    symbolComparator: Comparator<Symbol>? = null
): ParseResult<CanonicalPolynomial<Flt64>> {
    return parseCanonicalRet(input, symbolOf, symbolComparator)
}

fun <T> parseLinear(
    input: String,
    numberParser: NumberParser<T>,
    zero: T,
    one: T,
    symbolOf: (String) -> Symbol = ::symbolOfSerializedIdentifier,
    isZero: (T) -> Boolean = { it == zero }
): ParseResult<LinearPolynomial<T>> where T : Ring<T> {
    return parseLinearTypedRetOrNull(input, numberParser, zero, one, symbolOf, isZero)
}

fun parseLinear(
    input: String,
    symbolOf: (String) -> Symbol = ::symbolOfSerializedIdentifier
): ParseResult<LinearPolynomial<Flt64>> {
    return parseLinearRet(input, symbolOf)
}

fun <T> parseQuadratic(
    input: String,
    numberParser: NumberParser<T>,
    zero: T,
    one: T,
    symbolOf: (String) -> Symbol = ::symbolOfSerializedIdentifier,
    isZero: (T) -> Boolean = { it == zero },
    symbolComparator: Comparator<Symbol>? = null
): ParseResult<QuadraticPolynomial<T>> where T : Ring<T> {
    return parseQuadraticTypedRetOrNull(input, numberParser, zero, one, symbolOf, isZero, symbolComparator)
}

fun parseQuadratic(
    input: String,
    symbolOf: (String) -> Symbol = ::symbolOfSerializedIdentifier,
    symbolComparator: Comparator<Symbol>? = null
): ParseResult<QuadraticPolynomial<Flt64>> {
    return parseQuadraticRet(input, symbolOf, symbolComparator)
}

fun <T> parseLinearInequality(
    input: String,
    numberParser: NumberParser<T>,
    zero: T,
    one: T,
    symbolOf: (String) -> Symbol = ::symbolOfSerializedIdentifier,
    isZero: (T) -> Boolean = { it == zero }
): ParseResult<LinearInequality<T>> where T : Ring<T> {
    return parseLinearInequalityRet(input, symbolOf)
}

fun parseLinearInequality(
    input: String,
    symbolOf: (String) -> Symbol = ::symbolOfSerializedIdentifier
): ParseResult<LinearInequality<Flt64>> {
    return parseLinearInequalityRet(input, symbolOf)
}

fun parseQuadraticInequality(
    input: String,
    symbolOf: (String) -> Symbol = ::symbolOfSerializedIdentifier,
    symbolComparator: Comparator<Symbol>? = null
): ParseResult<QuadraticInequality> {
    return parseQuadraticInequalityRet(input, symbolOf, symbolComparator)
}

