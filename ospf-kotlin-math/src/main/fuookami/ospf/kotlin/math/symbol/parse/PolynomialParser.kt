/**
 * 多项式解析器
 * Polynomial Parser
 *
 * 提供多项式和不等式的递归下降解析功能，支持泛型 Ring<T> 类型。
 * Provides recursive descent parsing for polynomials and inequalities, supporting generic Ring<T> types.
 */
package fuookami.ospf.kotlin.math.symbol.parse

import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.math.symbol.serde.symbolOfSerializedIdentifier
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.monomial.CanonicalMonomial
import fuookami.ospf.kotlin.math.symbol.operation.*
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.math.algebra.number.Int32
import fuookami.ospf.kotlin.math.algebra.concept.Ring

// ============================================================================
// Generic parsing
// ============================================================================

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
): CanonicalPolynomial<T> where T : Ring<T> {
    val tokens = PolynomialLexer(input).lex()
    val parser = DirectPolynomialParser(tokens, numberParser, zero, one, symbolOf, isZero)
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
fun <T> parseLinearOrNull(
    input: String,
    numberParser: NumberParser<T>,
    zero: T,
    one: T,
    symbolOf: (String) -> Symbol = ::symbolOfSerializedIdentifier,
    isZero: (T) -> Boolean = { it == zero }
): LinearPolynomial<T>? where T : Ring<T> {
    return parseCanonical(input, numberParser, zero, one, symbolOf, isZero).toLinearPolynomialOrNull(zero, isZero)
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
): QuadraticPolynomial<T>? where T : Ring<T> {
    return parseCanonical(input, numberParser, zero, one, symbolOf, isZero, symbolComparator)
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
fun <T> parseLinearInequalityOrNull(
    input: String,
    numberParser: NumberParser<T>,
    zero: T,
    one: T,
    symbolOf: (String) -> Symbol = ::symbolOfSerializedIdentifier,
    isZero: (T) -> Boolean = { it == zero }
): LinearInequality<T>? where T : Ring<T> {
    val tokens = PolynomialLexer(input).lex()
    val parser = DirectPolynomialParser(tokens, numberParser, zero, one, symbolOf, isZero)
    val parsed = parser.parseInequality()
    val lhs = parsed.lhs.toCanonicalPolynomial(zero, isZero).toLinearPolynomialOrNull(zero, isZero) ?: return null
    val rhs = parsed.rhs.toCanonicalPolynomial(zero, isZero).toLinearPolynomialOrNull(zero, isZero) ?: return null
    return LinearInequality(lhs = lhs, rhs = rhs, comparison = parsed.comparison)
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
    private val tokens: List<PolynomialToken>,
    private val numberParser: NumberParser<T>,
    private val zero: T,
    private val one: T,
    private val symbolOf: (String) -> Symbol,
    private val isZero: (T) -> Boolean
) where T : Ring<T> {
    private var position: Int = 0

    fun parsePolynomial(): ParsedPolynomial<T> {
        val result = parseExpression()
        expect(PolynomialTokenType.End)
        return result
    }

    fun parseInequality(): ParsedInequality<T> {
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
        return ParsedInequality(lhs = lhs, rhs = rhs, comparison = comparison)
    }

    /** 解析加减法表达式 / Parse an addition/subtraction expression */
    private fun parseExpression(): ParsedPolynomial<T> {
        var result = parseTerm()
        while (true) {
            val token = current()
            when (token.type) {
                PolynomialTokenType.Plus -> {
                    advance()
                    val right = parseTerm()
                    result = addParsed(result, right)
                }

                PolynomialTokenType.Minus -> {
                    advance()
                    val right = parseTerm()
                    result = subtractParsed(result, right)
                }

                else -> break
            }
        }
        return result
    }

    /** 解析乘法项 / Parse a multiplication term */
    private fun parseTerm(): ParsedPolynomial<T> {
        var result = parsePower()
        while (true) {
            val token = current()
            when (token.type) {
                PolynomialTokenType.Star -> {
                    advance()
                    val right = parsePower()
                    result = multiplyParsed(result, right, isZero)
                }

                else -> break
            }
        }
        return result
    }

    /** 解析幂运算 / Parse a power expression */
    private fun parsePower(): ParsedPolynomial<T> {
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
            var powered = ParsedPolynomial<T>(emptyList(), one)
            repeat(exponent) {
                powered = multiplyParsed(powered, result, isZero)
            }
            result = powered
        }
        return result
    }

    /** 解析因子（数字、标识符、括号表达式或取反） / Parse a factor (number, identifier, parenthesized expression, or negation) */
    private fun parseFactor(): ParsedPolynomial<T> {
        val token = current()
        return when (token.type) {
            PolynomialTokenType.Minus -> {
                advance()
                val operand = parseFactor()
                negateParsed(operand)
            }

            PolynomialTokenType.Number -> {
                advance()
                val value = numberParser.parse(token.text)
                    ?: throw DirectParseError("Invalid number '${token.text}' for target type", token.position)
                ParsedPolynomial(emptyList(), value)
            }

            PolynomialTokenType.Identifier -> {
                advance()
                val symbol = symbolOf(token.text)
                ParsedPolynomial(
                    terms = listOf(ParsedTerm(one, mapOf(symbol to Int32.one))),
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

    /** 获取当前词法单元 / Get the current token */
    private fun current(): PolynomialToken = tokens[position]

    /** 前进到下一个词法单元 / Advance to the next token */
    private fun advance() {
        if (position < tokens.size - 1) position += 1
    }

    /**
     * 期望当前词法单元为指定类型，匹配则前进，否则抛出异常
     * Expect the current token to be of the given type; advance on match, throw on mismatch
     *
     * @param type 期望的词法单元类型 / Expected token type
     */
    private fun expect(type: PolynomialTokenType) {
        val token = current()
        if (token.type != type) {
            throw DirectParseError("Expected ${type.name}, got '${token.text}'", token.position)
        }
        advance()
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

// ============================================================================
// Ret-wrapped parsing API (generic)
// ============================================================================

/**
 * 根据解析错误信息判断问题类型（词法或语法）
 * Determine the issue type (lexical or syntax) from a parse error
 *
 * @param error 解析错误 / Parse error
 * @return 问题类型 / Issue type
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
 * 将解析错误转换为解析问题对象
 * Convert a parse error into a parse issue object
 *
 * @param error 解析错误 / Parse error
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

/**
 * 构造解析失败结果 / Construct a failed parse result
 *
 * @param issue 解析问题 / Parse issue
 * @return 失败的解析结果 / Failed parse result
 */
private fun <T> parseFailed(issue: ParseIssue): ParseResult<T> {
    return Failed(ErrorCode.IllegalArgument, issue)
}

/**
 * 构造转换失败的解析结果 / Construct a conversion-failed parse result
 *
 * @param input 原始输入字符串 / Original input string
 * @param message 错误信息 / Error message
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
 * 将解析操作包装为 Ret 安全结果，捕获各类异常
 * Wrap a parsing block into a Ret-safe result, catching various exceptions
 *
 * @param input 原始输入字符串 / Original input string
 * @param block 解析操作块 / Parsing block
 * @return 包装后的解析结果 / Wrapped parse result
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
fun <T> parseCanonicalRet(
    input: String,
    numberParser: NumberParser<T>,
    zero: T,
    one: T,
    symbolOf: (String) -> Symbol = ::symbolOfSerializedIdentifier,
    isZero: (T) -> Boolean = { it == zero },
    symbolComparator: Comparator<Symbol>? = null
): ParseResult<CanonicalPolynomial<T>> where T : Ring<T> {
    return wrapRet(input) { parseCanonical(input, numberParser, zero, one, symbolOf, isZero, symbolComparator) }
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
fun <T> parseLinearRetOrNull(
    input: String,
    numberParser: NumberParser<T>,
    zero: T,
    one: T,
    symbolOf: (String) -> Symbol = ::symbolOfSerializedIdentifier,
    isZero: (T) -> Boolean = { it == zero }
): ParseResult<LinearPolynomial<T>> where T : Ring<T> {
    return wrapRet(input) {
        parseLinearOrNull(input, numberParser, zero, one, symbolOf, isZero)
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
fun <T> parseLinearInequalityRetOrNull(
    input: String,
    numberParser: NumberParser<T>,
    zero: T,
    one: T,
    symbolOf: (String) -> Symbol = ::symbolOfSerializedIdentifier,
    isZero: (T) -> Boolean = { it == zero }
): ParseResult<LinearInequality<T>> where T : Ring<T> {
    return wrapRet(input) {
        parseLinearInequalityOrNull(input, numberParser, zero, one, symbolOf, isZero)
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
fun <T> parseQuadraticRetOrNull(
    input: String,
    numberParser: NumberParser<T>,
    zero: T,
    one: T,
    symbolOf: (String) -> Symbol = ::symbolOfSerializedIdentifier,
    isZero: (T) -> Boolean = { it == zero },
    symbolComparator: Comparator<Symbol>? = null
): ParseResult<QuadraticPolynomial<T>> where T : Ring<T> {
    return wrapRet(input) {
        parseQuadraticOrNull(input, numberParser, zero, one, symbolOf, isZero, symbolComparator)
            ?: throw IllegalArgumentException("Expression is not quadratic polynomial.")
    }
}
