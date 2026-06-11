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

import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.Failed
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
    private val tokens: List<PolynomialToken>,
    private val symbolOf: (String) -> Symbol
) {
    private var position: Int = 0

    fun parsePolynomial(): ParsedPolynomial {
        val result = parseExpression()
        expect(PolynomialTokenType.End)
        return result
    }

    fun parseInequality(): ParsedInequality {
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
        return ParsedInequality(lhs, rhs, comparison)
    }

    /** 解析表达式（加减法层级） / Parse expression (addition/subtraction level) */
    private fun parseExpression(): ParsedPolynomial {
        var result = parseTerm()
        while (true) {
            val token = current()
            when (token.type) {
                PolynomialTokenType.Plus -> {
                    advance()
                    val right = parseTerm()
                    result = addParsedPolynomials(result, right)
                }

                PolynomialTokenType.Minus -> {
                    advance()
                    val right = parseTerm()
                    result = subtractParsedPolynomials(result, right)
                }

                else -> break
            }
        }
        return result
    }

    /** 解析项（乘法层级） / Parse term (multiplication level) */
    private fun parseTerm(): ParsedPolynomial {
        var result = parsePower()
        while (true) {
            val token = current()
            when (token.type) {
                PolynomialTokenType.Star -> {
                    advance()
                    val right = parsePower()
                    result = multiplyParsedPolynomials(result, right)
                }

                else -> break
            }
        }
        return result
    }

    /** 解析幂运算层级 / Parse power operation level */
    private fun parsePower(): ParsedPolynomial {
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
            var powered = ParsedPolynomial(emptyList(), Flt64.one)
            repeat(exponent) {
                powered = multiplyParsedPolynomials(powered, result)
            }
            result = powered
        }
        return result
    }

    /** 解析因子（数字、变量、括号、负号） / Parse factor (number, variable, parentheses, negation) */
    private fun parseFactor(): ParsedPolynomial {
        val token = current()
        return when (token.type) {
            PolynomialTokenType.Minus -> {
                advance()
                val operand = parseFactor()
                negateParsedPolynomial(operand)
            }

            PolynomialTokenType.Number -> {
                advance()
                val value = token.text.toDoubleOrNull()
                    ?: throw DirectParseError("Invalid number '${token.text}'", token.position)
                ParsedPolynomial(emptyList(), Flt64(value))
            }

            PolynomialTokenType.Identifier -> {
                advance()
                val symbol = symbolOf(token.text)
                ParsedPolynomial(
                    terms = listOf(ParsedTerm(Flt64.one, mapOf(symbol to Int32.one))),
                    constant = Flt64.zero
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
     * @throws DirectParseError 若当前 token 类型不匹配 / If current token type does not match
     */
    private fun expect(type: PolynomialTokenType) {
        val token = current()
        if (token.type != type) {
            throw DirectParseError("Expected ${type.name}, got '${token.text}'", token.position)
        }
        advance()
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
// Public API: Direct polynomial and inequality parsing (Flt64)
// ============================================================================

/**
 * 解析字符串为 Flt64 规范多项式
 * Parse a string into a Flt64 canonical polynomial
 *
 * @param input 输入表达式字符串 / Input expression string
 * @param symbolOf 符号解析函数 / Symbol resolution function
 * @param symbolComparator 符号比较器 / Symbol comparator
 * @return 规范多项式 / Canonical polynomial
 * @throws DirectParseError 若解析失败 / If parsing fails
 */
fun parseCanonicalFlt64(
    input: String,
    symbolOf: (String) -> Symbol = ::symbolOfSerializedIdentifier,
    symbolComparator: Comparator<Symbol>? = null
): CanonicalPolynomial<Flt64> {
    val tokens = PolynomialLexer(input).lex()
    val parser = DirectPolynomialParser(tokens, symbolOf)
    return parser.parsePolynomial().toCanonicalPolynomial(symbolComparator)
}

/**
 * 解析字符串为 Flt64 线性多项式
 * Parse a string into a Flt64 linear polynomial
 *
 * @param input 输入表达式字符串 / Input expression string
 * @param symbolOf 符号解析函数 / Symbol resolution function
 * @return 线性多项式，若表达式非线性则返回 null / Linear polynomial, or null if expression is not linear
 */
fun parseLinearFlt64(
    input: String,
    symbolOf: (String) -> Symbol = ::symbolOfSerializedIdentifier
): LinearPolynomial<Flt64>? {
    return parseCanonicalFlt64(input, symbolOf).toLinearPolynomialOrNull()
}

/**
 * 解析字符串为 Flt64 二次多项式
 * Parse a string into a Flt64 quadratic polynomial
 *
 * @param input 输入表达式字符串 / Input expression string
 * @param symbolOf 符号解析函数 / Symbol resolution function
 * @param symbolComparator 符号比较器 / Symbol comparator
 * @return 二次多项式，若表达式非二次则返回 null / Quadratic polynomial, or null if expression is not quadratic
 */
fun parseQuadraticFlt64(
    input: String,
    symbolOf: (String) -> Symbol = ::symbolOfSerializedIdentifier,
    symbolComparator: Comparator<Symbol>? = null
): QuadraticPolynomial<Flt64>? {
    return parseCanonicalFlt64(input, symbolOf, symbolComparator).toQuadraticPolynomialOrNull(symbolComparator)
}

/**
 * 解析字符串为 Flt64 线性不等式
 * Parse a string into a Flt64 linear inequality
 *
 * @param input 输入表达式字符串 / Input expression string
 * @param symbolOf 符号解析函数 / Symbol resolution function
 * @return 线性不等式，若不可转换则返回 null / Linear inequality, or null if not convertible
 */
fun parseLinearInequalityFlt64(
    input: String,
    symbolOf: (String) -> Symbol = ::symbolOfSerializedIdentifier
): LinearInequality<Flt64>? {
    val tokens = PolynomialLexer(input).lex()
    val parser = DirectPolynomialParser(tokens, symbolOf)
    return parser.parseInequality().toCanonicalInequality().toLinearInequalityOrNull()
}

/**
 * 解析字符串为 Flt64 二次不等式
 * Parse a string into a Flt64 quadratic inequality
 *
 * @param input 输入表达式字符串 / Input expression string
 * @param symbolOf 符号解析函数 / Symbol resolution function
 * @param symbolComparator 符号比较器 / Symbol comparator
 * @return 二次不等式，若不可转换则返回 null / Quadratic inequality, or null if not convertible
 */
fun parseQuadraticInequalityFlt64(
    input: String,
    symbolOf: (String) -> Symbol = ::symbolOfSerializedIdentifier,
    symbolComparator: Comparator<Symbol>? = null
): QuadraticInequalityOf<Flt64>? {
    val tokens = PolynomialLexer(input).lex()
    val parser = DirectPolynomialParser(tokens, symbolOf)
    return parser.parseInequality().toCanonicalInequality().toQuadraticInequalityOrNull(symbolComparator)
}

/**
 * 解析字符串为 Flt64 规范不等式
 * Parse a string into a Flt64 canonical inequality
 *
 * @param input 输入表达式字符串 / Input expression string
 * @param symbolOf 符号解析函数 / Symbol resolution function
 * @param symbolComparator 符号比较器 / Symbol comparator
 * @return 规范不等式 / Canonical inequality
 */
fun parseCanonicalInequalityFlt64(
    input: String,
    symbolOf: (String) -> Symbol = ::symbolOfSerializedIdentifier,
    symbolComparator: Comparator<Symbol>? = null
): CanonicalInequality<Flt64> {
    val tokens = PolynomialLexer(input).lex()
    val parser = DirectPolynomialParser(tokens, symbolOf)
    return parser.parseInequality().toCanonicalInequality(symbolComparator)
}

// ============================================================================
// Ret-wrapped parsing API (Flt64)
// ============================================================================

/**
 * 根据解析错误判断问题类型
 * Determine issue type from parse error
 *
 * @param error 直接解析错误 / Direct parse error
 * @return 解析问题类型 / Parse issue type
 */
private fun parseIssueTypeOf(error: DirectParseError): ParseIssueType {
    val message = error.message.orEmpty()
    return if (message.startsWith("Unexpected") || message.startsWith("Invalid number")) {
        ParseIssueType.Lexical
    } else {
        ParseIssueType.Syntax
    }
}

/**
 * 从解析错误构建解析问题对象
 * Build a parse issue object from a parse error
 *
 * @param error 直接解析错误 / Direct parse error
 * @param input 原始输入字符串 / Original input string
 * @return 解析问题 / Parse issue
 */
private fun parseIssueOf(error: DirectParseError, input: String): ParseIssue {
    val normalizedMessage = error.message?.removeSuffix(" at position ${error.position}") ?: "Parse error."
    return ParseIssue(
        type = parseIssueTypeOf(error),
        message = normalizedMessage,
        input = input,
        position = error.position
    )
}

/** 构建失败的解析结果 / Build a failed parse result */
private fun <T> parseFailed(issue: ParseIssue): ParseResult<T> {
    return Failed(ErrorCode.IllegalArgument, issue)
}

/**
 * 构建转换失败的解析结果
 * Build a parse result for conversion failure
 *
 * @param input 原始输入字符串 / Original input string
 * @param message 错误消息 / Error message
 * @return 失败的解析结果 / Failed parse result
 */
private fun <T> parseConversionFailed(input: String, message: String): ParseResult<T> {
    return parseFailed(
        ParseIssue(
            type = ParseIssueType.Conversion,
            message = message,
            input = input
        )
    )
}

/**
 * 以 Ret 方式包装解析块，捕获异常并转换为失败结果
 * Wrap a parsing block in Ret style, catching exceptions and converting to failure results
 *
 * @param input 原始输入字符串 / Original input string
 * @param block 解析代码块 / Parsing code block
 * @return 解析结果 / Parse result
 */
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
 * 解析字符串为 Flt64 规范多项式（Ret 安全版本）
 * Parse a string into a Flt64 canonical polynomial (Ret-safe version)
 *
 * @param input 输入表达式字符串 / Input expression string
 * @param symbolOf 符号解析函数 / Symbol resolution function
 * @param symbolComparator 符号比较器 / Symbol comparator
 * @return 解析结果 / Parse result
 */
fun parseCanonicalRetFlt64(
    input: String,
    symbolOf: (String) -> Symbol = ::symbolOfSerializedIdentifier,
    symbolComparator: Comparator<Symbol>? = null
): ParseResult<CanonicalPolynomial<Flt64>> {
    return wrapRet(input) { parseCanonicalFlt64(input, symbolOf, symbolComparator) }
}

/**
 * 解析字符串为 Flt64 线性多项式（Ret 安全版本）
 * Parse a string into a Flt64 linear polynomial (Ret-safe version)
 *
 * @param input 输入表达式字符串 / Input expression string
 * @param symbolOf 符号解析函数 / Symbol resolution function
 * @return 解析结果 / Parse result
 */
fun parseLinearRetFlt64(
    input: String,
    symbolOf: (String) -> Symbol = ::symbolOfSerializedIdentifier
): ParseResult<LinearPolynomial<Flt64>> {
    return wrapRet(input) {
        parseLinearFlt64(input, symbolOf)
            ?: throw IllegalArgumentException("Expression is not linear polynomial.")
    }
}

/**
 * 解析字符串为 Flt64 二次多项式（Ret 安全版本）
 * Parse a string into a Flt64 quadratic polynomial (Ret-safe version)
 *
 * @param input 输入表达式字符串 / Input expression string
 * @param symbolOf 符号解析函数 / Symbol resolution function
 * @param symbolComparator 符号比较器 / Symbol comparator
 * @return 解析结果 / Parse result
 */
fun parseQuadraticRetFlt64(
    input: String,
    symbolOf: (String) -> Symbol = ::symbolOfSerializedIdentifier,
    symbolComparator: Comparator<Symbol>? = null
): ParseResult<QuadraticPolynomial<Flt64>> {
    return wrapRet(input) {
        parseQuadraticFlt64(input, symbolOf, symbolComparator)
            ?: throw IllegalArgumentException("Expression is not quadratic polynomial.")
    }
}

/**
 * 解析字符串为 Flt64 规范不等式（Ret 安全版本）
 * Parse a string into a Flt64 canonical inequality (Ret-safe version)
 *
 * @param input 输入表达式字符串 / Input expression string
 * @param symbolOf 符号解析函数 / Symbol resolution function
 * @param symbolComparator 符号比较器 / Symbol comparator
 * @return 解析结果 / Parse result
 */
fun parseCanonicalInequalityRetFlt64(
    input: String,
    symbolOf: (String) -> Symbol = ::symbolOfSerializedIdentifier,
    symbolComparator: Comparator<Symbol>? = null
): ParseResult<CanonicalInequality<Flt64>> {
    return wrapRet(input) { parseCanonicalInequalityFlt64(input, symbolOf, symbolComparator) }
}

/**
 * 解析字符串为 Flt64 线性不等式（Ret 安全版本）
 * Parse a string into a Flt64 linear inequality (Ret-safe version)
 *
 * @param input 输入表达式字符串 / Input expression string
 * @param symbolOf 符号解析函数 / Symbol resolution function
 * @return 解析结果 / Parse result
 */
fun parseLinearInequalityRetFlt64(
    input: String,
    symbolOf: (String) -> Symbol = ::symbolOfSerializedIdentifier
): ParseResult<LinearInequality<Flt64>> {
    return wrapRet(input) {
        parseLinearInequalityFlt64(input, symbolOf)
            ?: throw IllegalArgumentException("Inequality is not linear.")
    }
}

/**
 * 解析字符串为 Flt64 二次不等式（Ret 安全版本）
 * Parse a string into a Flt64 quadratic inequality (Ret-safe version)
 *
 * @param input 输入表达式字符串 / Input expression string
 * @param symbolOf 符号解析函数 / Symbol resolution function
 * @param symbolComparator 符号比较器 / Symbol comparator
 * @return 解析结果 / Parse result
 */
fun parseQuadraticInequalityRetFlt64(
    input: String,
    symbolOf: (String) -> Symbol = ::symbolOfSerializedIdentifier,
    symbolComparator: Comparator<Symbol>? = null
): ParseResult<QuadraticInequalityOf<Flt64>> {
    return wrapRet(input) {
        parseQuadraticInequalityFlt64(input, symbolOf, symbolComparator)
            ?: throw IllegalArgumentException("Inequality is not quadratic.")
    }
}
