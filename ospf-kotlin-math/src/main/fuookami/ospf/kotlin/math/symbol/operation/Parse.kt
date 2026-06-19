/**
 * Flt64 多项式与不等式解析
 * Flt64 Polynomial and Inequality Parsing
 *
 * 提供从字符串直接解析 Flt64 多项式和不等式的功能。
 * 支持规范、线性和二次多项式以及不等式的解析，包含 Ret 包装的安全版本。
 * Provides direct parsing of Flt64 polynomials and inequalities from strings.
 * Supports canonical, linear, and quadratic polynomials and inequalities,
 * including Ret-wrapped safe versions.
 */
package fuookami.ospf.kotlin.math.symbol.operation

import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Fatal
import fuookami.ospf.kotlin.math.symbol.*
import fuookami.ospf.kotlin.math.symbol.parse.*
import fuookami.ospf.kotlin.math.symbol.serde.*
import fuookami.ospf.kotlin.math.symbol.monomial.*
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.concept.Ring

// ============================================================================
// Internal Flt64 polynomial term representation for direct parsing
// ============================================================================

private data class ParsedTerm(
    val coefficient: Flt64,
    val powers: Map<Symbol, Int32>
)

private data class ParsedPolynomial(
    val terms: List<ParsedTerm>,
    val constant: Flt64
)

private data class ParsedInequality(
    val lhs: ParsedPolynomial,
    val rhs: ParsedPolynomial,
    val comparison: Comparison
)

// ============================================================================
// Internal recursive descent parser
// ============================================================================

private class DirectPolynomialParser(
    private val input: String,
    private val tokens: List<PolynomialToken>,
    private val symbolOf: (String) -> Symbol
) {
    private var position: Int = 0

    fun parsePolynomial(): ParseResult<ParsedPolynomial> {
        return parseExpression().andThen { result ->
            expect(PolynomialTokenType.End).map { result }
        }
    }

    fun parseInequality(): ParseResult<ParsedInequality> {
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
                    ParsedInequality(lhs, rhs, comparison)
                }
            }
        }
    }

    /** 解析表达式（加减法层级） / Parse expression (addition/subtraction level) */
    private fun parseExpression(): ParseResult<ParsedPolynomial> {
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
                    result = addParsedPolynomials(result, right)
                }

                PolynomialTokenType.Minus -> {
                    advance()
                    val right = when (val parsed = parseTerm()) {
                        is Ok -> parsed.value
                        is Failed -> return Failed(parsed.error)
                        is Fatal -> return Fatal(parsed.errors)
                    }
                    result = subtractParsedPolynomials(result, right)
                }

                else -> break
            }
        }
        return Ok(result)
    }

    /** 解析项（乘法层级） / Parse term (multiplication level) */
    private fun parseTerm(): ParseResult<ParsedPolynomial> {
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
                    result = multiplyParsedPolynomials(result, right)
                }

                else -> break
            }
        }
        return Ok(result)
    }

    /** 解析幂运算层级 / Parse power operation level */
    private fun parsePower(): ParseResult<ParsedPolynomial> {
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
            var powered = ParsedPolynomial(emptyList(), Flt64.one)
            repeat(exponent) {
                powered = multiplyParsedPolynomials(powered, result)
            }
            result = powered
        }
        return Ok(result)
    }

    /** 解析因子（数字、变量、括号、负号） / Parse factor (number, variable, parentheses, negation) */
    private fun parseFactor(): ParseResult<ParsedPolynomial> {
        val token = current()
        return when (token.type) {
            PolynomialTokenType.Minus -> {
                advance()
                parseFactor().map { negateParsedPolynomial(it) }
            }

            PolynomialTokenType.Number -> {
                advance()
                val value = token.text.toDoubleOrNull()
                    ?: return parseLexicalFailed(
                        input = input,
                        message = "Invalid number '${token.text}'",
                        position = token.position
                    )
                Ok(ParsedPolynomial(emptyList(), Flt64(value)))
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
                        terms = listOf(ParsedTerm(Flt64.one, mapOf(symbol to Int32.one))),
                        constant = Flt64.zero
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

    /** 获取当前 token / Get current token */
    private fun current(): PolynomialToken {
        return tokens[position]
    }

    /** 前进到下一个 token / Advance to next token */
    private fun advance() {
        if (position < tokens.size - 1) {
            position += 1
        }
    }

    /**
     * 期望当前 token 为指定类型并前进
     * Expect current token to be of given type and advance
     *
     * @param type 期望的 token 类型 / Expected token type
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

// ============================================================================
// Internal polynomial arithmetic on ParsedPolynomial
// ============================================================================

/**
 * 将两个已解析多项式相加
 * Add two parsed polynomials
 *
 * @param lhs 左操作数 / Left operand
 * @param rhs 右操作数 / Right operand
 * @return 相加结果 / Addition result
 */
private fun addParsedPolynomials(lhs: ParsedPolynomial, rhs: ParsedPolynomial): ParsedPolynomial {
    return ParsedPolynomial(
        terms = lhs.terms + rhs.terms,
        constant = lhs.constant + rhs.constant
    )
}

/**
 * 对已解析多项式取反
 * Negate a parsed polynomial
 *
 * @param poly 输入多项式 / Input polynomial
 * @return 取反结果 / Negated result
 */
private fun negateParsedPolynomial(poly: ParsedPolynomial): ParsedPolynomial {
    return ParsedPolynomial(
        terms = poly.terms.map { it.copy(coefficient = -it.coefficient) },
        constant = -poly.constant
    )
}

/**
 * 将两个已解析多项式相减
 * Subtract two parsed polynomials
 *
 * @param lhs 左操作数 / Left operand
 * @param rhs 右操作数 / Right operand
 * @return 相减结果 / Subtraction result
 */
private fun subtractParsedPolynomials(lhs: ParsedPolynomial, rhs: ParsedPolynomial): ParsedPolynomial {
    return addParsedPolynomials(lhs, negateParsedPolynomial(rhs))
}

/**
 * 将两个已解析多项式相乘
 * Multiply two parsed polynomials
 *
 * @param lhs 左操作数 / Left operand
 * @param rhs 右操作数 / Right operand
 * @return 相乘结果 / Multiplication result
 */
private fun multiplyParsedPolynomials(lhs: ParsedPolynomial, rhs: ParsedPolynomial): ParsedPolynomial {
    val terms = ArrayList<ParsedTerm>(lhs.terms.size * rhs.terms.size + lhs.terms.size + rhs.terms.size)
    for (left in lhs.terms) {
        for (right in rhs.terms) {
            val mergedPowers = LinkedHashMap<Symbol, Int32>(left.powers.size + right.powers.size)
            for ((symbol, exp) in left.powers) {
                mergedPowers[symbol] = exp
            }
            for ((symbol, exp) in right.powers) {
                mergedPowers[symbol] = (mergedPowers[symbol] ?: Int32.zero) + exp
            }
            terms.add(ParsedTerm(left.coefficient * right.coefficient, mergedPowers))
        }
        if (rhs.constant != Flt64.zero) {
            terms.add(ParsedTerm(left.coefficient * rhs.constant, left.powers))
        }
    }
    for (right in rhs.terms) {
        if (lhs.constant != Flt64.zero) {
            terms.add(ParsedTerm(right.coefficient * lhs.constant, right.powers))
        }
    }
    return ParsedPolynomial(
        terms = terms,
        constant = lhs.constant * rhs.constant
    )
}

// ============================================================================
// Conversion from ParsedPolynomial to CanonicalPolynomial
// ============================================================================

/**
 * 将已解析多项式转换为规范多项式
 * Convert a parsed polynomial to canonical polynomial
 *
 * @param symbolComparator 符号比较器 / Symbol comparator
 * @return 规范多项式 / Canonical polynomial
 */
private fun ParsedPolynomial.toCanonicalPolynomial(
    symbolComparator: Comparator<Symbol>? = null
): CanonicalPolynomial<Flt64> {
    val monomials = terms.map { term ->
        CanonicalMonomial<Flt64>(
            coefficient = term.coefficient,
            powers = term.powers
        )
    }
    return CanonicalPolynomial<Flt64>(
        monomials = monomials,
        constant = constant
    ).combineTerms(symbolComparator)
}

/**
 * 将已解析不等式转换为规范不等式
 * Convert a parsed inequality to canonical inequality
 *
 * @param symbolComparator 符号比较器 / Symbol comparator
 * @return 规范不等式 / Canonical inequality
 */
private fun ParsedInequality.toCanonicalInequality(
    symbolComparator: Comparator<Symbol>? = null
): CanonicalInequality<Flt64> {
    return CanonicalInequality<Flt64>(
        lhs = lhs.toCanonicalPolynomial(symbolComparator),
        rhs = rhs.toCanonicalPolynomial(symbolComparator),
        comparison = comparison
    )
}

// ============================================================================
// Public API: Flt64 polynomial and inequality parsing
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
 * 解析字符串为 Flt64 规范多项式
 * Parse a string into a Flt64 canonical polynomial
 *
 * @param input 输入表达式字符串 / Input expression string
 * @param symbolOf 符号解析函数 / Symbol resolution function
 * @param symbolComparator 符号比较器 / Symbol comparator
 * @return 解析结果 / Parse result
 */
fun parseCanonicalFlt64(
    input: String,
    symbolOf: (String) -> Symbol = ::symbolOfSerializedIdentifier,
    symbolComparator: Comparator<Symbol>? = null
): ParseResult<CanonicalPolynomial<Flt64>> {
    return parseSafely(input) {
        PolynomialLexer(input).lex().andThen { tokens ->
            DirectPolynomialParser(input, tokens, symbolOf).parsePolynomial()
                .map { it.toCanonicalPolynomial(symbolComparator) }
        }
    }
}

/**
 * 解析字符串为 Flt64 线性多项式
 * Parse a string into a Flt64 linear polynomial
 *
 * @param input 输入表达式字符串 / Input expression string
 * @param symbolOf 符号解析函数 / Symbol resolution function
 * @return 解析结果 / Parse result
 */
fun parseLinearFlt64(
    input: String,
    symbolOf: (String) -> Symbol = ::symbolOfSerializedIdentifier
): ParseResult<LinearPolynomial<Flt64>> {
    return parseLinearOrNullFlt64(input, symbolOf).andThen { polynomial ->
        polynomial
            ?.let { Ok(it) }
            ?: parseConversionFailed(input, "Expression is not linear polynomial.")
    }
}

/**
 * 解析字符串为 Flt64 线性多项式，非线性时返回 null
 * Parse a string into a Flt64 linear polynomial, returning null if nonlinear
 *
 * @param input 输入表达式字符串 / Input expression string
 * @param symbolOf 符号解析函数 / Symbol resolution function
 * @return 解析结果 / Parse result
 */
fun parseLinearOrNullFlt64(
    input: String,
    symbolOf: (String) -> Symbol = ::symbolOfSerializedIdentifier
): ParseResult<LinearPolynomial<Flt64>?> {
    return parseCanonicalFlt64(input, symbolOf).map { it.toLinearPolynomialOrNull() }
}

/**
 * 解析字符串为 Flt64 二次多项式
 * Parse a string into a Flt64 quadratic polynomial
 *
 * @param input 输入表达式字符串 / Input expression string
 * @param symbolOf 符号解析函数 / Symbol resolution function
 * @param symbolComparator 符号比较器 / Symbol comparator
 * @return 解析结果 / Parse result
 */
fun parseQuadraticFlt64(
    input: String,
    symbolOf: (String) -> Symbol = ::symbolOfSerializedIdentifier,
    symbolComparator: Comparator<Symbol>? = null
): ParseResult<QuadraticPolynomial<Flt64>> {
    return parseQuadraticOrNullFlt64(input, symbolOf, symbolComparator).andThen { polynomial ->
        polynomial
            ?.let { Ok(it) }
            ?: parseConversionFailed(input, "Expression is not quadratic polynomial.")
    }
}

/**
 * 解析字符串为 Flt64 二次多项式，非二次时返回 null
 * Parse a string into a Flt64 quadratic polynomial, returning null if not quadratic
 *
 * @param input 输入表达式字符串 / Input expression string
 * @param symbolOf 符号解析函数 / Symbol resolution function
 * @param symbolComparator 符号比较器 / Symbol comparator
 * @return 解析结果 / Parse result
 */
fun parseQuadraticOrNullFlt64(
    input: String,
    symbolOf: (String) -> Symbol = ::symbolOfSerializedIdentifier,
    symbolComparator: Comparator<Symbol>? = null
): ParseResult<QuadraticPolynomial<Flt64>?> {
    return parseCanonicalFlt64(input, symbolOf, symbolComparator)
        .map { it.toQuadraticPolynomialOrNull(symbolComparator) }
}

/**
 * 解析字符串为 Flt64 规范不等式
 * Parse a string into a Flt64 canonical inequality
 *
 * @param input 输入表达式字符串 / Input expression string
 * @param symbolOf 符号解析函数 / Symbol resolution function
 * @param symbolComparator 符号比较器 / Symbol comparator
 * @return 解析结果 / Parse result
 */
fun parseCanonicalInequalityFlt64(
    input: String,
    symbolOf: (String) -> Symbol = ::symbolOfSerializedIdentifier,
    symbolComparator: Comparator<Symbol>? = null
): ParseResult<CanonicalInequality<Flt64>> {
    return parseSafely(input) {
        PolynomialLexer(input).lex().andThen { tokens ->
            DirectPolynomialParser(input, tokens, symbolOf).parseInequality()
                .map { it.toCanonicalInequality(symbolComparator) }
        }
    }
}

/**
 * 解析字符串为 Flt64 线性不等式
 * Parse a string into a Flt64 linear inequality
 *
 * @param input 输入表达式字符串 / Input expression string
 * @param symbolOf 符号解析函数 / Symbol resolution function
 * @return 解析结果 / Parse result
 */
fun parseLinearInequalityFlt64(
    input: String,
    symbolOf: (String) -> Symbol = ::symbolOfSerializedIdentifier
): ParseResult<LinearInequality<Flt64>> {
    return parseLinearInequalityOrNullFlt64(input, symbolOf).andThen { inequality ->
        inequality
            ?.let { Ok(it) }
            ?: parseConversionFailed(input, "Inequality is not linear.")
    }
}

/**
 * 解析字符串为 Flt64 线性不等式，非线性时返回 null
 * Parse a string into a Flt64 linear inequality, returning null if nonlinear
 *
 * @param input 输入表达式字符串 / Input expression string
 * @param symbolOf 符号解析函数 / Symbol resolution function
 * @return 解析结果 / Parse result
 */
fun parseLinearInequalityOrNullFlt64(
    input: String,
    symbolOf: (String) -> Symbol = ::symbolOfSerializedIdentifier
): ParseResult<LinearInequality<Flt64>?> {
    return parseCanonicalInequalityFlt64(input, symbolOf)
        .map { it.toLinearInequalityOrNull() }
}

/**
 * 解析字符串为 Flt64 二次不等式
 * Parse a string into a Flt64 quadratic inequality
 *
 * @param input 输入表达式字符串 / Input expression string
 * @param symbolOf 符号解析函数 / Symbol resolution function
 * @param symbolComparator 符号比较器 / Symbol comparator
 * @return 解析结果 / Parse result
 */
fun parseQuadraticInequalityFlt64(
    input: String,
    symbolOf: (String) -> Symbol = ::symbolOfSerializedIdentifier,
    symbolComparator: Comparator<Symbol>? = null
): ParseResult<QuadraticInequalityOf<Flt64>> {
    return parseQuadraticInequalityOrNullFlt64(input, symbolOf, symbolComparator).andThen { inequality ->
        inequality
            ?.let { Ok(it) }
            ?: parseConversionFailed(input, "Inequality is not quadratic.")
    }
}

/**
 * 解析字符串为 Flt64 二次不等式，非二次时返回 null
 * Parse a string into a Flt64 quadratic inequality, returning null if not quadratic
 *
 * @param input 输入表达式字符串 / Input expression string
 * @param symbolOf 符号解析函数 / Symbol resolution function
 * @param symbolComparator 符号比较器 / Symbol comparator
 * @return 解析结果 / Parse result
 */
fun parseQuadraticInequalityOrNullFlt64(
    input: String,
    symbolOf: (String) -> Symbol = ::symbolOfSerializedIdentifier,
    symbolComparator: Comparator<Symbol>? = null
): ParseResult<QuadraticInequalityOf<Flt64>?> {
    return parseCanonicalInequalityFlt64(input, symbolOf, symbolComparator)
        .map { it.toQuadraticInequalityOrNull(symbolComparator) }
}
