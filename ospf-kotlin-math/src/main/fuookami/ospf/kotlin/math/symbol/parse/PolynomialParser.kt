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
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.math.symbol.monomial.CanonicalMonomial
import fuookami.ospf.kotlin.math.symbol.operation.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.math.symbol.serde.symbolOfSerializedIdentifier
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Fatal
import fuookami.ospf.kotlin.utils.functional.Ok

// ============================================================================
// Generic parsing
// ============================================================================

/**
 * 捕获解析边界外异常并转换为解析失败
 * Catch boundary exceptions and convert them into parse failures
 *
 * @param input 原始输入字符串 / Original input string
 * @param block 解析代码块 / Parsing block
 * @return 解析结果 / Parse result
 */
private inline fun <T> parseSafely(
    input: String,
    crossinline block: () -> ParseResult<T>
): ParseResult<T> {
    return try {
        block()
    } catch (error: IllegalArgumentException) {
        parseConversionFailed(input, error.message ?: "Conversion failed.")
    } catch (error: Exception) {
        parseUnknownFailed(input, error)
    }
}

/**
 * 解析字符串为规范多项式（泛型类型版本）
 * Parses a string into a canonical polynomial (generic number type version)
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
fun <T> parseCanonical(
    input: String,
    numberParser: NumberParser<T>,
    zero: T,
    one: T,
    symbolOf: (String) -> Symbol = ::symbolOfSerializedIdentifier,
    isZero: (T) -> Boolean = { it == zero },
    symbolComparator: Comparator<Symbol>? = null
): ParseResult<CanonicalPolynomial<T>> where T : Ring<T> {
    return parseSafely(input) {
        PolynomialLexer(input).lex().andThen { tokens ->
            DirectPolynomialParser(input, tokens, numberParser, zero, one, symbolOf, isZero)
                .parsePolynomial()
                .map { it.toCanonicalPolynomial(zero, isZero, symbolComparator) }
        }
    }
}

/**
 * 解析字符串为线性多项式
 * Parses a string into a linear polynomial
 *
 * @param input 输入字符串 / Input string
 * @param numberParser 数值解析器 / Number parser
 * @param zero 类型零值 / Zero value of the type
 * @param one 类型单位值 / One value of the type
 * @param symbolOf 符号解析函数 / Symbol resolution function
 * @param isZero 零值判断函数 / Zero-check function
 * @return 线性多项式解析结果 / Linear polynomial parse result
 */
fun <T> parseLinear(
    input: String,
    numberParser: NumberParser<T>,
    zero: T,
    one: T,
    symbolOf: (String) -> Symbol = ::symbolOfSerializedIdentifier,
    isZero: (T) -> Boolean = { it == zero }
): ParseResult<LinearPolynomial<T>> where T : Ring<T> {
    return parseLinearOrNull(input, numberParser, zero, one, symbolOf, isZero).andThen { polynomial ->
        polynomial
            ?.let { Ok(it) }
            ?: parseConversionFailed(input, "Expression is not linear polynomial.")
    }
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
fun <T> parseLinearOrNull(
    input: String,
    numberParser: NumberParser<T>,
    zero: T,
    one: T,
    symbolOf: (String) -> Symbol = ::symbolOfSerializedIdentifier,
    isZero: (T) -> Boolean = { it == zero }
): ParseResult<LinearPolynomial<T>?> where T : Ring<T> {
    return parseCanonical(input, numberParser, zero, one, symbolOf, isZero)
        .map { it.toLinearPolynomialOrNull(zero, isZero) }
}

/**
 * 解析字符串为二次多项式
 * Parses a string into a quadratic polynomial
 *
 * @param input 输入字符串 / Input string
 * @param numberParser 数值解析器 / Number parser
 * @param zero 类型零值 / Zero value of the type
 * @param one 类型单位值 / One value of the type
 * @param symbolOf 符号解析函数 / Symbol resolution function
 * @param isZero 零值判断函数 / Zero-check function
 * @param symbolComparator 符号排序比较器 / Symbol ordering comparator
 * @return 二次多项式解析结果 / Quadratic polynomial parse result
 */
fun <T> parseQuadratic(
    input: String,
    numberParser: NumberParser<T>,
    zero: T,
    one: T,
    symbolOf: (String) -> Symbol = ::symbolOfSerializedIdentifier,
    isZero: (T) -> Boolean = { it == zero },
    symbolComparator: Comparator<Symbol>? = null
): ParseResult<QuadraticPolynomial<T>> where T : Ring<T> {
    return parseQuadraticOrNull(input, numberParser, zero, one, symbolOf, isZero, symbolComparator)
        .andThen { polynomial ->
            polynomial
                ?.let { Ok(it) }
                ?: parseConversionFailed(input, "Expression is not quadratic polynomial.")
        }
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
fun <T> parseQuadraticOrNull(
    input: String,
    numberParser: NumberParser<T>,
    zero: T,
    one: T,
    symbolOf: (String) -> Symbol = ::symbolOfSerializedIdentifier,
    isZero: (T) -> Boolean = { it == zero },
    symbolComparator: Comparator<Symbol>? = null
): ParseResult<QuadraticPolynomial<T>?> where T : Ring<T> {
    return parseCanonical(input, numberParser, zero, one, symbolOf, isZero, symbolComparator)
        .map { it.toQuadraticPolynomialOrNull(zero, isZero, symbolComparator) }
}

/**
 * 解析字符串为线性不等式
 * Parses a string into a linear inequality
 *
 * @param input 输入字符串 / Input string
 * @param numberParser 数值解析器 / Number parser
 * @param zero 类型零值 / Zero value of the type
 * @param one 类型单位值 / One value of the type
 * @param symbolOf 符号解析函数 / Symbol resolution function
 * @param isZero 零值判断函数 / Zero-check function
 * @return 线性不等式解析结果 / Linear inequality parse result
 */
fun <T> parseLinearInequality(
    input: String,
    numberParser: NumberParser<T>,
    zero: T,
    one: T,
    symbolOf: (String) -> Symbol = ::symbolOfSerializedIdentifier,
    isZero: (T) -> Boolean = { it == zero }
): ParseResult<LinearInequality<T>> where T : Ring<T> {
    return parseLinearInequalityOrNull(input, numberParser, zero, one, symbolOf, isZero).andThen { inequality ->
        inequality
            ?.let { Ok(it) }
            ?: parseConversionFailed(input, "Inequality is not linear.")
    }
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
fun <T> parseLinearInequalityOrNull(
    input: String,
    numberParser: NumberParser<T>,
    zero: T,
    one: T,
    symbolOf: (String) -> Symbol = ::symbolOfSerializedIdentifier,
    isZero: (T) -> Boolean = { it == zero }
): ParseResult<LinearInequality<T>?> where T : Ring<T> {
    return parseSafely(input) {
        PolynomialLexer(input).lex().andThen { tokens ->
            DirectPolynomialParser(input, tokens, numberParser, zero, one, symbolOf, isZero)
                .parseInequality()
                .map { parsed ->
                    val lhs = parsed.lhs.toCanonicalPolynomial(zero, isZero)
                        .toLinearPolynomialOrNull(zero, isZero) ?: return@map null
                    val rhs = parsed.rhs.toCanonicalPolynomial(zero, isZero)
                        .toLinearPolynomialOrNull(zero, isZero) ?: return@map null
                    LinearInequality(lhs = lhs, rhs = rhs, comparison = parsed.comparison)
                }
        }
    }
}

// ============================================================================
// Internal polynomial term and parser
// ============================================================================

private data class ParsedTerm<T>(
    val coefficient: T,
    val powers: Map<Symbol, Int32>
)

private data class ParsedPolynomial<T>(
    val terms: List<ParsedTerm<T>>,
    val constant: T
)

private data class ParsedInequality<T>(
    val lhs: ParsedPolynomial<T>,
    val rhs: ParsedPolynomial<T>,
    val comparison: Comparison
)

private class DirectPolynomialParser<T>(
    private val input: String,
    private val tokens: List<PolynomialToken>,
    private val numberParser: NumberParser<T>,
    private val zero: T,
    private val one: T,
    private val symbolOf: (String) -> Symbol,
    private val isZero: (T) -> Boolean
) where T : Ring<T> {
    private var position: Int = 0

    /** 解析多项式 / Parse polynomial */
    fun parsePolynomial(): ParseResult<ParsedPolynomial<T>> {
        return parseExpression().andThen { result ->
            expect(PolynomialTokenType.End).map { result }
        }
    }

    /** 解析不等式 / Parse inequality */
    fun parseInequality(): ParseResult<ParsedInequality<T>> {
        return parseExpression().andThen { lhs ->
            val comparisonToken = current()
            val comparison = when (comparisonToken.type) {
                PolynomialTokenType.Less -> Comparison.LT
                PolynomialTokenType.LessEqual -> Comparison.LE
                PolynomialTokenType.Equal -> Comparison.EQ
                PolynomialTokenType.NotEqual -> Comparison.NE
                PolynomialTokenType.GreaterEqual -> Comparison.GE
                PolynomialTokenType.Greater -> Comparison.GT
                else -> return@andThen parseSyntaxFailed(
                    input = input,
                    message = "Expected comparison operator",
                    position = comparisonToken.position
                )
            }
            advance()
            parseExpression().andThen { rhs ->
                expect(PolynomialTokenType.End).map {
                    ParsedInequality(lhs = lhs, rhs = rhs, comparison = comparison)
                }
            }
        }
    }

    /** 解析加减法表达式 / Parse an addition/subtraction expression */
    private fun parseExpression(): ParseResult<ParsedPolynomial<T>> {
        var result = when (val parsed = parseTerm()) {
            is Ok -> parsed.value
            is Failed -> return Failed(parsed.error)
            is Fatal -> return Fatal(parsed.errors)
        }
        while (true) {
            val token = current()
            when (token.type) {
                PolynomialTokenType.Plus -> {
                    advance()
                    val right = when (val parsed = parseTerm()) {
                        is Ok -> parsed.value
                        is Failed -> return Failed(parsed.error)
                        is Fatal -> return Fatal(parsed.errors)
                    }
                    result = addParsed(result, right)
                }

                PolynomialTokenType.Minus -> {
                    advance()
                    val right = when (val parsed = parseTerm()) {
                        is Ok -> parsed.value
                        is Failed -> return Failed(parsed.error)
                        is Fatal -> return Fatal(parsed.errors)
                    }
                    result = subtractParsed(result, right)
                }

                else -> break
            }
        }
        return Ok(result)
    }

    /** 解析乘法项 / Parse a multiplication term */
    private fun parseTerm(): ParseResult<ParsedPolynomial<T>> {
        var result = when (val parsed = parsePower()) {
            is Ok -> parsed.value
            is Failed -> return Failed(parsed.error)
            is Fatal -> return Fatal(parsed.errors)
        }
        while (true) {
            val token = current()
            when (token.type) {
                PolynomialTokenType.Star -> {
                    advance()
                    val right = when (val parsed = parsePower()) {
                        is Ok -> parsed.value
                        is Failed -> return Failed(parsed.error)
                        is Fatal -> return Fatal(parsed.errors)
                    }
                    result = multiplyParsed(result, right, isZero)
                }

                else -> break
            }
        }
        return Ok(result)
    }

    /** 解析幂运算 / Parse a power expression */
    private fun parsePower(): ParseResult<ParsedPolynomial<T>> {
        var result = when (val parsed = parseFactor()) {
            is Ok -> parsed.value
            is Failed -> return Failed(parsed.error)
            is Fatal -> return Fatal(parsed.errors)
        }
        val token = current()
        if (token.type == PolynomialTokenType.Caret) {
            advance()
            val exponentToken = current()
            if (exponentToken.type != PolynomialTokenType.Number) {
                return parseSyntaxFailed(
                    input = input,
                    message = "Expected integer exponent after '^'",
                    position = exponentToken.position
                )
            }
            advance()
            val exponent = exponentToken.text.toIntOrNull()
                ?: return parseSyntaxFailed(
                    input = input,
                    message = "Exponent must be an integer",
                    position = exponentToken.position
                )
            if (exponent < 0) {
                return parseSemanticFailed(
                    input = input,
                    message = "Negative exponent is not supported",
                    position = exponentToken.position
                )
            }
            var powered = ParsedPolynomial<T>(emptyList(), one)
            repeat(exponent) {
                powered = multiplyParsed(powered, result, isZero)
            }
            result = powered
        }
        return Ok(result)
    }

    /** 解析因子（数字、标识符、括号表达式或取反） / Parse a factor (number, identifier, parenthesized expression, or negation) */
    private fun parseFactor(): ParseResult<ParsedPolynomial<T>> {
        val token = current()
        return when (token.type) {
            PolynomialTokenType.Minus -> {
                advance()
                parseFactor().map { negateParsed(it) }
            }

            PolynomialTokenType.Number -> {
                advance()
                val value = numberParser.parse(token.text)
                    ?: return parseLexicalFailed(
                        input = input,
                        message = "Invalid number '${token.text}' for target type",
                        position = token.position
                    )
                Ok(ParsedPolynomial(emptyList(), value))
            }

            PolynomialTokenType.Identifier -> {
                advance()
                val symbol = try {
                    symbolOf(token.text)
                } catch (error: Exception) {
                    return parseUnknownFailed(input, error)
                }
                Ok(
                    ParsedPolynomial(
                        terms = listOf(ParsedTerm(one, mapOf(symbol to Int32.one))),
                        constant = zero
                    )
                )
            }

            PolynomialTokenType.LeftParen -> {
                advance()
                parseExpression().andThen { inner ->
                    expect(PolynomialTokenType.RightParen).map { inner }
                }
            }

            else -> parseSyntaxFailed(
                input = input,
                message = "Unexpected token '${token.text}'",
                position = token.position
            )
        }
    }

    /** 获取当前词法单元 / Get the current token */
    private fun current(): PolynomialToken = tokens[position]

    /** 前进到下一个词法单元 / Advance to the next token */
    private fun advance() {
        if (position < tokens.size - 1) position += 1
    }

    /**
     * 期望当前词法单元为指定类型，匹配则前进
     * Expect the current token to be of the given type; advance on match
     *
     * @param type 期望的词法单元类型 / Expected token type
     * @return 匹配结果 / Match result
     */
    private fun expect(type: PolynomialTokenType): ParseResult<Unit> {
        val token = current()
        if (token.type != type) {
            return parseSyntaxFailed(
                input = input,
                message = "Expected ${type.name}, got '${token.text}'",
                position = token.position
            )
        }
        advance()
        return Ok(Unit)
    }
}

/**
 * 两个解析多项式相加
 * Add two parsed polynomials
 *
 * @param lhs 左操作数 / Left-hand operand
 * @param rhs 右操作数 / Right-hand operand
 * @return 相加后的多项式 / Sum polynomial
 */
private fun <T> addParsed(lhs: ParsedPolynomial<T>, rhs: ParsedPolynomial<T>): ParsedPolynomial<T> where T : Ring<T> {
    return ParsedPolynomial(lhs.terms + rhs.terms, lhs.constant + rhs.constant)
}

/**
 * 对解析多项式取反
 * Negate a parsed polynomial
 *
 * @param poly 要取反的多项式 / Polynomial to negate
 * @return 取反后的多项式 / Negated polynomial
 */
private fun <T> negateParsed(poly: ParsedPolynomial<T>): ParsedPolynomial<T> where T : Ring<T> {
    return ParsedPolynomial(poly.terms.map { it.copy(coefficient = -it.coefficient) }, -poly.constant)
}

/**
 * 两个解析多项式相减
 * Subtract one parsed polynomial from another
 *
 * @param lhs 左操作数 / Left-hand operand
 * @param rhs 右操作数 / Right-hand operand
 * @return 相减后的多项式 / Difference polynomial
 */
private fun <T> subtractParsed(lhs: ParsedPolynomial<T>, rhs: ParsedPolynomial<T>): ParsedPolynomial<T> where T : Ring<T> {
    return addParsed(lhs, negateParsed(rhs))
}

/**
 * 两个解析多项式相乘
 * Multiply two parsed polynomials
 *
 * @param lhs 左操作数 / Left-hand operand
 * @param rhs 右操作数 / Right-hand operand
 * @param isZero 零值判断函数 / Zero-check function
 * @return 相乘后的多项式 / Product polynomial
 */
private fun <T> multiplyParsed(
    lhs: ParsedPolynomial<T>,
    rhs: ParsedPolynomial<T>,
    isZero: (T) -> Boolean
): ParsedPolynomial<T> where T : Ring<T> {
    val terms = ArrayList<ParsedTerm<T>>(lhs.terms.size * rhs.terms.size + lhs.terms.size + rhs.terms.size)
    for (left in lhs.terms) {
        for (right in rhs.terms) {
            val mergedPowers = LinkedHashMap<Symbol, Int32>(left.powers.size + right.powers.size)
            for ((symbol, exp) in left.powers) mergedPowers[symbol] = exp
            for ((symbol, exp) in right.powers) mergedPowers[symbol] = (mergedPowers[symbol] ?: Int32.zero) + exp
            terms.add(ParsedTerm(left.coefficient * right.coefficient, mergedPowers))
        }
        if (!isZero(rhs.constant)) {
            terms.add(ParsedTerm(left.coefficient * rhs.constant, left.powers))
        }
    }
    for (right in rhs.terms) {
        if (!isZero(lhs.constant)) {
            terms.add(ParsedTerm(right.coefficient * lhs.constant, right.powers))
        }
    }
    return ParsedPolynomial(terms, lhs.constant * rhs.constant)
}

/**
 * 将解析多项式转换为规范多项式
 * Convert a parsed polynomial to a canonical polynomial
 *
 * @param zero 类型零值 / Zero value of the type
 * @param isZero 零值判断函数 / Zero-check function
 * @param symbolComparator 符号排序比较器（可选） / Comparator for symbol ordering (optional)
 * @return 规范多项式 / Canonical polynomial
 */
private fun <T> ParsedPolynomial<T>.toCanonicalPolynomial(
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

