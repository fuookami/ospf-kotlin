/**
 * 多项式解析器
 * Polynomial Parser
 *
 * 提供多项式和不等式的递归下降解析功能，支持泛型 Ring<T> 类型。
 * Provides recursive descent parsing for polynomials and inequalities, supporting generic Ring<T> types.
 */
package fuookami.ospf.kotlin.math.symbol.parse

import fuookami.ospf.kotlin.math.algebra.concept.Ring
import fuookami.ospf.kotlin.math.algebra.number.Int32
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.inequality.Comparison
import fuookami.ospf.kotlin.math.symbol.inequality.LinearInequality
import fuookami.ospf.kotlin.math.symbol.monomial.CanonicalMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.math.symbol.operation.*
import fuookami.ospf.kotlin.math.symbol.serde.symbolOfSerializedIdentifier
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Ok

// ============================================================================
// Generic typed parsing
// ============================================================================

/**
 * 解析字符串为规范多项式（泛型类型版本）
 * Parses a string into a canonical polynomial (generic typed version)
 *
 * @param input 输入字符串 / Input string
 * @param numberParser 数值解析器 / Number parser
 * @param zero 类型零值 / Zero value of the type
 * @param one 类型单位值 / One value of the type
 * @param symbolOf 符号解析函数 / Symbol resolution function
 * @param isZero 零值判断函数 / Zero-check function
 * @param symbolComparator 符号排序比较器 / Symbol ordering comparator
 * @return 解析后的规范多项式 / Parsed canonical polynomial
 */
fun <T> parseCanonicalTyped(
    input: String,
    numberParser: NumberParser<T>,
    zero: T,
    one: T,
    symbolOf: (String) -> Symbol = ::symbolOfSerializedIdentifier,
    isZero: (T) -> Boolean = { it == zero },
    symbolComparator: Comparator<Symbol>? = null
): CanonicalPolynomial<T> where T : Ring<T> {
    val tokens = PolynomialLexer(input).lex()
    val parser = DirectTypedPolynomialParser(tokens, numberParser, zero, one, symbolOf, isZero)
    return parser.parsePolynomial().toCanonicalPolynomial(zero, isZero, symbolComparator)
}

/**
 * 解析字符串为线性多项式，若非线性则返回 null
 * Parses a string into a linear polynomial, returns null if not linear
 *
 * @param input 输入字符串 / Input string
 * @param numberParser 数值解析器 / Number parser
 * @param zero 类型零值 / Zero value of the type
 * @param one 类型单位值 / One value of the type
 * @param symbolOf 符号解析函数 / Symbol resolution function
 * @param isZero 零值判断函数 / Zero-check function
 * @return 线性多项式或 null / Linear polynomial or null
 */
fun <T> parseLinearTypedOrNull(
    input: String,
    numberParser: NumberParser<T>,
    zero: T,
    one: T,
    symbolOf: (String) -> Symbol = ::symbolOfSerializedIdentifier,
    isZero: (T) -> Boolean = { it == zero }
): LinearPolynomial<T>? where T : Ring<T> {
    return parseCanonicalTyped(input, numberParser, zero, one, symbolOf, isZero).toLinearPolynomialOrNull(zero, isZero)
}

/**
 * 解析字符串为二次多项式，若非二次则返回 null
 * Parses a string into a quadratic polynomial, returns null if not quadratic
 *
 * @param input 输入字符串 / Input string
 * @param numberParser 数值解析器 / Number parser
 * @param zero 类型零值 / Zero value of the type
 * @param one 类型单位值 / One value of the type
 * @param symbolOf 符号解析函数 / Symbol resolution function
 * @param isZero 零值判断函数 / Zero-check function
 * @param symbolComparator 符号排序比较器 / Symbol ordering comparator
 * @return 二次多项式或 null / Quadratic polynomial or null
 */
fun <T> parseQuadraticTypedOrNull(
    input: String,
    numberParser: NumberParser<T>,
    zero: T,
    one: T,
    symbolOf: (String) -> Symbol = ::symbolOfSerializedIdentifier,
    isZero: (T) -> Boolean = { it == zero },
    symbolComparator: Comparator<Symbol>? = null
): QuadraticPolynomial<T>? where T : Ring<T> {
    return parseCanonicalTyped(input, numberParser, zero, one, symbolOf, isZero, symbolComparator)
        .toQuadraticPolynomialOrNull(zero, isZero, symbolComparator)
}

/**
 * 解析字符串为线性不等式，若非线性则返回 null
 * Parses a string into a linear inequality, returns null if not linear
 *
 * @param input 输入字符串 / Input string
 * @param numberParser 数值解析器 / Number parser
 * @param zero 类型零值 / Zero value of the type
 * @param one 类型单位值 / One value of the type
 * @param symbolOf 符号解析函数 / Symbol resolution function
 * @param isZero 零值判断函数 / Zero-check function
 * @return 线性不等式或 null / Linear inequality or null
 */
fun <T> parseLinearInequalityTypedOrNull(
    input: String,
    numberParser: NumberParser<T>,
    zero: T,
    one: T,
    symbolOf: (String) -> Symbol = ::symbolOfSerializedIdentifier,
    isZero: (T) -> Boolean = { it == zero }
): LinearInequality<T>? where T : Ring<T> {
    val tokens = PolynomialLexer(input).lex()
    val parser = DirectTypedPolynomialParser(tokens, numberParser, zero, one, symbolOf, isZero)
    val parsed = parser.parseInequality()
    val lhs = parsed.lhs.toCanonicalPolynomial(zero, isZero).toLinearPolynomialOrNull(zero, isZero) ?: return null
    val rhs = parsed.rhs.toCanonicalPolynomial(zero, isZero).toLinearPolynomialOrNull(zero, isZero) ?: return null
    return LinearInequality(lhs = lhs, rhs = rhs, comparison = parsed.comparison)
}

// ============================================================================
// Internal typed polynomial term and parser
// ============================================================================

private data class TypedParsedTerm<T>(
    val coefficient: T,
    val powers: Map<Symbol, Int32>
)

private data class TypedParsedPolynomial<T>(
    val terms: List<TypedParsedTerm<T>>,
    val constant: T
)

private data class TypedParsedInequality<T>(
    val lhs: TypedParsedPolynomial<T>,
    val rhs: TypedParsedPolynomial<T>,
    val comparison: Comparison
)

private class DirectTypedPolynomialParser<T>(
    private val tokens: List<PolynomialToken>,
    private val numberParser: NumberParser<T>,
    private val zero: T,
    private val one: T,
    private val symbolOf: (String) -> Symbol,
    private val isZero: (T) -> Boolean
) where T : Ring<T> {
    private var position: Int = 0

    fun parsePolynomial(): TypedParsedPolynomial<T> {
        val result = parseExpression()
        expect(PolynomialTokenType.End)
        return result
    }

    fun parseInequality(): TypedParsedInequality<T> {
        val lhs = parseExpression()
        val comparisonToken = current()
        val comparison = when (comparisonToken.type) {
            PolynomialTokenType.Less -> Comparison.LT
            PolynomialTokenType.LessEqual -> Comparison.LE
            PolynomialTokenType.Equal -> Comparison.EQ
            PolynomialTokenType.NotEqual -> Comparison.NE
            PolynomialTokenType.GreaterEqual -> Comparison.GE
            PolynomialTokenType.Greater -> Comparison.GT
            else -> throw DirectParseError("Expected comparison operator", comparisonToken.position)
        }
        advance()
        val rhs = parseExpression()
        expect(PolynomialTokenType.End)
        return TypedParsedInequality(lhs = lhs, rhs = rhs, comparison = comparison)
    }

    private fun parseExpression(): TypedParsedPolynomial<T> {
        var result = parseTerm()
        while (true) {
            val token = current()
            when (token.type) {
                PolynomialTokenType.Plus -> {
                    advance()
                    val right = parseTerm()
                    result = addTyped(result, right)
                }

                PolynomialTokenType.Minus -> {
                    advance()
                    val right = parseTerm()
                    result = subtractTyped(result, right)
                }

                else -> break
            }
        }
        return result
    }

    private fun parseTerm(): TypedParsedPolynomial<T> {
        var result = parsePower()
        while (true) {
            val token = current()
            when (token.type) {
                PolynomialTokenType.Star -> {
                    advance()
                    val right = parsePower()
                    result = multiplyTyped(result, right, isZero)
                }

                else -> break
            }
        }
        return result
    }

    private fun parsePower(): TypedParsedPolynomial<T> {
        var result = parseFactor()
        val token = current()
        if (token.type == PolynomialTokenType.Caret) {
            advance()
            val exponentToken = current()
            if (exponentToken.type != PolynomialTokenType.Number) {
                throw DirectParseError("Expected integer exponent after '^'", exponentToken.position)
            }
            advance()
            val exponent = exponentToken.text.toIntOrNull()
                ?: throw DirectParseError("Exponent must be an integer", exponentToken.position)
            if (exponent < 0) {
                throw DirectParseError("Negative exponent is not supported", exponentToken.position)
            }
            var powered = TypedParsedPolynomial<T>(emptyList(), one)
            repeat(exponent) {
                powered = multiplyTyped(powered, result, isZero)
            }
            result = powered
        }
        return result
    }

    private fun parseFactor(): TypedParsedPolynomial<T> {
        val token = current()
        return when (token.type) {
            PolynomialTokenType.Minus -> {
                advance()
                val operand = parseFactor()
                negateTyped(operand)
            }

            PolynomialTokenType.Number -> {
                advance()
                val value = numberParser.parse(token.text)
                    ?: throw DirectParseError("Invalid number '${token.text}' for target type", token.position)
                TypedParsedPolynomial(emptyList(), value)
            }

            PolynomialTokenType.Identifier -> {
                advance()
                val symbol = symbolOf(token.text)
                TypedParsedPolynomial(
                    terms = listOf(TypedParsedTerm(one, mapOf(symbol to Int32.one))),
                    constant = zero
                )
            }

            PolynomialTokenType.LeftParen -> {
                advance()
                val inner = parseExpression()
                expect(PolynomialTokenType.RightParen)
                inner
            }

            else -> throw DirectParseError("Unexpected token '${token.text}'", token.position)
        }
    }

    private fun current(): PolynomialToken = tokens[position]

    private fun advance() {
        if (position < tokens.size - 1) position += 1
    }

    private fun expect(type: PolynomialTokenType) {
        val token = current()
        if (token.type != type) {
            throw DirectParseError("Expected ${type.name}, got '${token.text}'", token.position)
        }
        advance()
    }
}

private fun <T> addTyped(lhs: TypedParsedPolynomial<T>, rhs: TypedParsedPolynomial<T>): TypedParsedPolynomial<T> where T : Ring<T> {
    return TypedParsedPolynomial(lhs.terms + rhs.terms, lhs.constant + rhs.constant)
}

private fun <T> negateTyped(poly: TypedParsedPolynomial<T>): TypedParsedPolynomial<T> where T : Ring<T> {
    return TypedParsedPolynomial(poly.terms.map { it.copy(coefficient = -it.coefficient) }, -poly.constant)
}

private fun <T> subtractTyped(lhs: TypedParsedPolynomial<T>, rhs: TypedParsedPolynomial<T>): TypedParsedPolynomial<T> where T : Ring<T> {
    return addTyped(lhs, negateTyped(rhs))
}

private fun <T> multiplyTyped(
    lhs: TypedParsedPolynomial<T>,
    rhs: TypedParsedPolynomial<T>,
    isZero: (T) -> Boolean
): TypedParsedPolynomial<T> where T : Ring<T> {
    val terms = ArrayList<TypedParsedTerm<T>>(lhs.terms.size * rhs.terms.size + lhs.terms.size + rhs.terms.size)
    for (left in lhs.terms) {
        for (right in rhs.terms) {
            val mergedPowers = LinkedHashMap<Symbol, Int32>(left.powers.size + right.powers.size)
            for ((symbol, exp) in left.powers) mergedPowers[symbol] = exp
            for ((symbol, exp) in right.powers) mergedPowers[symbol] = (mergedPowers[symbol] ?: Int32.zero) + exp
            terms.add(TypedParsedTerm(left.coefficient * right.coefficient, mergedPowers))
        }
        if (!isZero(rhs.constant)) {
            terms.add(TypedParsedTerm(left.coefficient * rhs.constant, left.powers))
        }
    }
    for (right in rhs.terms) {
        if (!isZero(lhs.constant)) {
            terms.add(TypedParsedTerm(right.coefficient * lhs.constant, right.powers))
        }
    }
    return TypedParsedPolynomial(terms, lhs.constant * rhs.constant)
}

private fun <T> TypedParsedPolynomial<T>.toCanonicalPolynomial(
    zero: T,
    isZero: (T) -> Boolean,
    symbolComparator: Comparator<Symbol>? = null
): CanonicalPolynomial<T> where T : Ring<T> {
    val monomials = terms.map { term ->
        CanonicalMonomial(coefficient = term.coefficient, powers = term.powers)
    }
    return CanonicalPolynomial(monomials = monomials, constant = constant)
        .combineCanonicalPolynomialTerms(zero, isZero, symbolComparator)
}

// ============================================================================
// Ret-wrapped parsing API (generic typed)
// ============================================================================

private fun parseIssueTypeOf(error: DirectParseError): ParseIssueType {
    val message = error.message.orEmpty()
    return if (message.startsWith("Unexpected") || message.startsWith("Invalid number")) {
        ParseIssueType.Lexical
    } else {
        ParseIssueType.Syntax
    }
}

private fun parseIssueOf(error: DirectParseError, input: String): ParseIssue {
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

private inline fun <T> wrapRet(
    input: String,
    crossinline block: () -> T
): ParseResult<T> {
    return try {
        Ok(block())
    } catch (error: DirectParseError) {
        parseFailed(parseIssueOf(error, input))
    } catch (error: IllegalArgumentException) {
        parseConversionFailed(input, error.message ?: "Conversion failed.")
    } catch (error: Exception) {
        parseFailed(
            ParseIssue(
                type = ParseIssueType.Unknown,
                message = error.message ?: "Unexpected error.",
                input = input
            )
        )
    }
}

/**
 * 解析字符串为规范多项式，返回 Ret 包装结果
 * Parses a string into a canonical polynomial, returning a Ret-wrapped result
 *
 * @param input 输入字符串 / Input string
 * @param numberParser 数值解析器 / Number parser
 * @param zero 类型零值 / Zero value of the type
 * @param one 类型单位值 / One value of the type
 * @param symbolOf 符号解析函数 / Symbol resolution function
 * @param isZero 零值判断函数 / Zero-check function
 * @param symbolComparator 符号排序比较器 / Symbol ordering comparator
 * @return 包装了规范多项式的解析结果 / Parse result wrapping a canonical polynomial
 */
fun <T> parseCanonicalTypedRet(
    input: String,
    numberParser: NumberParser<T>,
    zero: T,
    one: T,
    symbolOf: (String) -> Symbol = ::symbolOfSerializedIdentifier,
    isZero: (T) -> Boolean = { it == zero },
    symbolComparator: Comparator<Symbol>? = null
): ParseResult<CanonicalPolynomial<T>> where T : Ring<T> {
    return wrapRet(input) { parseCanonicalTyped(input, numberParser, zero, one, symbolOf, isZero, symbolComparator) }
}

/**
 * 解析字符串为线性多项式，返回 Ret 包装结果
 * Parses a string into a linear polynomial, returning a Ret-wrapped result
 *
 * @param input 输入字符串 / Input string
 * @param numberParser 数值解析器 / Number parser
 * @param zero 类型零值 / Zero value of the type
 * @param one 类型单位值 / One value of the type
 * @param symbolOf 符号解析函数 / Symbol resolution function
 * @param isZero 零值判断函数 / Zero-check function
 * @return 包装了线性多项式的解析结果 / Parse result wrapping a linear polynomial
 */
fun <T> parseLinearTypedRetOrNull(
    input: String,
    numberParser: NumberParser<T>,
    zero: T,
    one: T,
    symbolOf: (String) -> Symbol = ::symbolOfSerializedIdentifier,
    isZero: (T) -> Boolean = { it == zero }
): ParseResult<LinearPolynomial<T>> where T : Ring<T> {
    return wrapRet(input) {
        parseLinearTypedOrNull(input, numberParser, zero, one, symbolOf, isZero)
            ?: throw IllegalArgumentException("Expression is not linear polynomial.")
    }
}

/**
 * 解析字符串为线性不等式，返回 Ret 包装结果
 * Parses a string into a linear inequality, returning a Ret-wrapped result
 *
 * @param input 输入字符串 / Input string
 * @param numberParser 数值解析器 / Number parser
 * @param zero 类型零值 / Zero value of the type
 * @param one 类型单位值 / One value of the type
 * @param symbolOf 符号解析函数 / Symbol resolution function
 * @param isZero 零值判断函数 / Zero-check function
 * @return 包装了线性不等式的解析结果 / Parse result wrapping a linear inequality
 */
fun <T> parseLinearInequalityTypedRetOrNull(
    input: String,
    numberParser: NumberParser<T>,
    zero: T,
    one: T,
    symbolOf: (String) -> Symbol = ::symbolOfSerializedIdentifier,
    isZero: (T) -> Boolean = { it == zero }
): ParseResult<LinearInequality<T>> where T : Ring<T> {
    return wrapRet(input) {
        parseLinearInequalityTypedOrNull(input, numberParser, zero, one, symbolOf, isZero)
            ?: throw IllegalArgumentException("Inequality is not linear.")
    }
}

/**
 * 解析字符串为二次多项式，返回 Ret 包装结果
 * Parses a string into a quadratic polynomial, returning a Ret-wrapped result
 *
 * @param input 输入字符串 / Input string
 * @param numberParser 数值解析器 / Number parser
 * @param zero 类型零值 / Zero value of the type
 * @param one 类型单位值 / One value of the type
 * @param symbolOf 符号解析函数 / Symbol resolution function
 * @param isZero 零值判断函数 / Zero-check function
 * @param symbolComparator 符号排序比较器 / Symbol ordering comparator
 * @return 包装了二次多项式的解析结果 / Parse result wrapping a quadratic polynomial
 */
fun <T> parseQuadraticTypedRetOrNull(
    input: String,
    numberParser: NumberParser<T>,
    zero: T,
    one: T,
    symbolOf: (String) -> Symbol = ::symbolOfSerializedIdentifier,
    isZero: (T) -> Boolean = { it == zero },
    symbolComparator: Comparator<Symbol>? = null
): ParseResult<QuadraticPolynomial<T>> where T : Ring<T> {
    return wrapRet(input) {
        parseQuadraticTypedOrNull(input, numberParser, zero, one, symbolOf, isZero, symbolComparator)
            ?: throw IllegalArgumentException("Expression is not quadratic polynomial.")
    }
}
