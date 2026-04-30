/**
 * 多项式解析器
 * Polynomial Parser
 *
 * 提供多项式和不等式的递归下降解析功能，支持 Flt64 和泛型 Ring<T> 类型。
 * Provides recursive descent parsing for polynomials and inequalities, supporting Flt64 and generic Ring<T> types.
 */
package fuookami.ospf.kotlin.math.symbol.parse

import fuookami.ospf.kotlin.math.algebra.concept.Ring
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.Int32
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.inequality.CanonicalInequality
import fuookami.ospf.kotlin.math.symbol.inequality.Comparison
import fuookami.ospf.kotlin.math.symbol.inequality.LinearInequality
import fuookami.ospf.kotlin.math.symbol.inequality.QuadraticInequality
import fuookami.ospf.kotlin.math.symbol.monomial.CanonicalMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.CanonicalPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.QuadraticPolynomial
import fuookami.ospf.kotlin.math.symbol.operation.combineCanonicalPolynomialTerms
import fuookami.ospf.kotlin.math.symbol.operation.combineTerms
import fuookami.ospf.kotlin.math.symbol.operation.toCanonicalInequality
import fuookami.ospf.kotlin.math.symbol.operation.toCanonicalPolynomial
import fuookami.ospf.kotlin.math.symbol.operation.toLinearInequalityOrNull
import fuookami.ospf.kotlin.math.symbol.operation.toLinearPolynomialOrNull
import fuookami.ospf.kotlin.math.symbol.operation.toQuadraticInequalityOrNull
import fuookami.ospf.kotlin.math.symbol.operation.toQuadraticPolynomialOrNull
import fuookami.ospf.kotlin.math.symbol.serde.symbolOfSerializedIdentifier
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Fatal
import fuookami.ospf.kotlin.utils.functional.Ok

// ============================================================================
// Internal polynomial term representation for direct parsing
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

    private fun current(): PolynomialToken {
        return tokens[position]
    }

    private fun advance() {
        if (position < tokens.size - 1) {
            position += 1
        }
    }

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

private fun addParsedPolynomials(lhs: ParsedPolynomial, rhs: ParsedPolynomial): ParsedPolynomial {
    return ParsedPolynomial(
        terms = lhs.terms + rhs.terms,
        constant = lhs.constant + rhs.constant
    )
}

private fun negateParsedPolynomial(poly: ParsedPolynomial): ParsedPolynomial {
    return ParsedPolynomial(
        terms = poly.terms.map { it.copy(coefficient = -it.coefficient) },
        constant = -poly.constant
    )
}

private fun subtractParsedPolynomials(lhs: ParsedPolynomial, rhs: ParsedPolynomial): ParsedPolynomial {
    return addParsedPolynomials(lhs, negateParsedPolynomial(rhs))
}

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

private fun ParsedPolynomial.toCanonicalPolynomial(
    symbolComparator: Comparator<Symbol>? = null
): CanonicalPolynomial<F64> {
    val monomials = terms.map { term ->
        CanonicalMonomial<F64>(
            coefficient = term.coefficient,
            powers = term.powers
        )
    }
    return CanonicalPolynomial<F64>(
        monomials = monomials,
        constant = constant
    ).combineTerms(symbolComparator)
}

private fun ParsedInequality.toCanonicalInequality(
    symbolComparator: Comparator<Symbol>? = null
): CanonicalInequality {
    return CanonicalInequality(
        lhs = lhs.toCanonicalPolynomial(symbolComparator),
        rhs = rhs.toCanonicalPolynomial(symbolComparator),
        comparison = comparison
    )
}

// ============================================================================
// Public API: Direct polynomial and inequality parsing
// ============================================================================

fun parseCanonical(
    input: String,
    symbolOf: (String) -> Symbol = ::symbolOfSerializedIdentifier,
    symbolComparator: Comparator<Symbol>? = null
): CanonicalPolynomial<F64> {
    val tokens = PolynomialLexer(input).lex()
    val parser = DirectPolynomialParser(tokens, symbolOf)
    return parser.parsePolynomial().toCanonicalPolynomial(symbolComparator)
}

fun parseLinear(
    input: String,
    symbolOf: (String) -> Symbol = ::symbolOfSerializedIdentifier
): LinearPolynomial<F64>? {
    return parseCanonical(input, symbolOf).toLinearPolynomialOrNull()
}

fun parseQuadratic(
    input: String,
    symbolOf: (String) -> Symbol = ::symbolOfSerializedIdentifier,
    symbolComparator: Comparator<Symbol>? = null
): QuadraticPolynomial<F64>? {
    return parseCanonical(input, symbolOf, symbolComparator).toQuadraticPolynomialOrNull(symbolComparator)
}

fun parseLinearInequality(
    input: String,
    symbolOf: (String) -> Symbol = ::symbolOfSerializedIdentifier
): LinearInequality<F64>? {
    val tokens = PolynomialLexer(input).lex()
    val parser = DirectPolynomialParser(tokens, symbolOf)
    return parser.parseInequality().toCanonicalInequality().toLinearInequalityOrNull()
}

fun parseQuadraticInequality(
    input: String,
    symbolOf: (String) -> Symbol = ::symbolOfSerializedIdentifier,
    symbolComparator: Comparator<Symbol>? = null
): QuadraticInequality? {
    val tokens = PolynomialLexer(input).lex()
    val parser = DirectPolynomialParser(tokens, symbolOf)
    return parser.parseInequality().toCanonicalInequality().toQuadraticInequalityOrNull(symbolComparator)
}

fun parseCanonicalInequality(
    input: String,
    symbolOf: (String) -> Symbol = ::symbolOfSerializedIdentifier,
    symbolComparator: Comparator<Symbol>? = null
): CanonicalInequality {
    val tokens = PolynomialLexer(input).lex()
    val parser = DirectPolynomialParser(tokens, symbolOf)
    return parser.parseInequality().toCanonicalInequality(symbolComparator)
}

// ============================================================================
// Generic typed parsing
// ============================================================================

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
// Ret-wrapped parsing API
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

fun parseCanonicalRet(
    input: String,
    symbolOf: (String) -> Symbol = ::symbolOfSerializedIdentifier,
    symbolComparator: Comparator<Symbol>? = null
): ParseResult<CanonicalPolynomial<F64>> {
    return wrapRet(input) { parseCanonical(input, symbolOf, symbolComparator) }
}

fun parseLinearRet(
    input: String,
    symbolOf: (String) -> Symbol = ::symbolOfSerializedIdentifier
): ParseResult<LinearPolynomial<F64>> {
    return wrapRet(input) {
        parseLinear(input, symbolOf)
            ?: throw IllegalArgumentException("Expression is not linear polynomial.")
    }
}

fun parseQuadraticRet(
    input: String,
    symbolOf: (String) -> Symbol = ::symbolOfSerializedIdentifier,
    symbolComparator: Comparator<Symbol>? = null
): ParseResult<QuadraticPolynomial<F64>> {
    return wrapRet(input) {
        parseQuadratic(input, symbolOf, symbolComparator)
            ?: throw IllegalArgumentException("Expression is not quadratic polynomial.")
    }
}

fun parseCanonicalInequalityRet(
    input: String,
    symbolOf: (String) -> Symbol = ::symbolOfSerializedIdentifier,
    symbolComparator: Comparator<Symbol>? = null
): ParseResult<CanonicalInequality> {
    return wrapRet(input) { parseCanonicalInequality(input, symbolOf, symbolComparator) }
}

fun parseLinearInequalityRet(
    input: String,
    symbolOf: (String) -> Symbol = ::symbolOfSerializedIdentifier
): ParseResult<LinearInequality<F64>> {
    return wrapRet(input) {
        parseLinearInequality(input, symbolOf)
            ?: throw IllegalArgumentException("Inequality is not linear.")
    }
}

fun parseQuadraticInequalityRet(
    input: String,
    symbolOf: (String) -> Symbol = ::symbolOfSerializedIdentifier,
    symbolComparator: Comparator<Symbol>? = null
): ParseResult<QuadraticInequality> {
    return wrapRet(input) {
        parseQuadraticInequality(input, symbolOf, symbolComparator)
            ?: throw IllegalArgumentException("Inequality is not quadratic.")
    }
}

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
