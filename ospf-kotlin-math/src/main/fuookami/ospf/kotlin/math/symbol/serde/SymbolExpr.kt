/**
 * 符号表达式序列化（Legacy Expr 兼容层）
 * Symbol Expression Serialization (Legacy Expr Compatibility Layer)
 *
 * 提供多项式和不等式与表达式 AST 之间的双向转换，以及 JSON 序列化支持。
 * 符号身份序列化已拆分到 SymbolIdentitySerde.kt。
 * Provides bidirectional conversion between polynomials/inequalities and expression AST,
 * as well as JSON serialization support.
 * Symbol identity serialization has been split to SymbolIdentitySerde.kt.
 */
package fuookami.ospf.kotlin.math.symbol.serde

import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.value_range.*

import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.inequality.CanonicalInequality
import fuookami.ospf.kotlin.math.symbol.inequality.Comparison
import fuookami.ospf.kotlin.math.symbol.inequality.LinearInequality
import fuookami.ospf.kotlin.math.symbol.inequality.QuadraticInequality
import fuookami.ospf.kotlin.math.symbol.inequality.QuadraticInequalityOf
import fuookami.ospf.kotlin.math.symbol.monomial.CanonicalMonomial
import fuookami.ospf.kotlin.math.symbol.parser.BinaryOperator
import fuookami.ospf.kotlin.math.symbol.parser.ComparisonOperator
import fuookami.ospf.kotlin.math.symbol.parser.Expr
import fuookami.ospf.kotlin.math.symbol.parse.Flt64NumberParser
import fuookami.ospf.kotlin.math.symbol.parse.NumberParser
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
import fuookami.ospf.kotlin.utils.serialization.readFromJson
import fuookami.ospf.kotlin.utils.serialization.writeJson
import java.io.ByteArrayInputStream
import java.math.BigDecimal

typealias LegacySymbolExpr = Expr

@Deprecated(
    message = "SymbolExpr is the legacy Expr alias. Prefer LegacySymbolExpr for explicit legacy usage or symbol.expression.* for the new expression stack.",
    replaceWith = ReplaceWith("LegacySymbolExpr")
)
typealias SymbolExpr = LegacySymbolExpr

private fun formatNumber(value: Flt64): String {
    return BigDecimal.valueOf(value.toDouble()).stripTrailingZeros().toPlainString()
}

private fun numberExpr(value: Flt64): Expr.NumberLiteral {
    return Expr.NumberLiteral(formatNumber(value))
}

private fun symbolExpr(symbol: Symbol): Expr.Identifier {
    return Expr.Identifier(symbol.toSymbolIdentityExpr().toSerializedIdentifier())
}

private fun powerExpr(
    base: Expr,
    exponent: Int
): Expr {
    return if (exponent == 1) {
        base
    } else {
        Expr.Binary(
            left = base,
            operator = BinaryOperator.Power,
            right = Expr.NumberLiteral(exponent.toString())
        )
    }
}

private fun multiplyExpr(
    left: Expr,
    right: Expr
): Expr {
    return Expr.Binary(
        left = left,
        operator = BinaryOperator.Multiply,
        right = right
    )
}

private fun scaleExpr(
    coefficient: Flt64,
    body: Expr
): Expr {
    return if (coefficient == Flt64.one) {
        body
    } else {
        multiplyExpr(numberExpr(coefficient), body)
    }
}

private fun combineSignedTerms(terms: List<Pair<Boolean, Expr>>): Expr {
    if (terms.isEmpty()) {
        return Expr.NumberLiteral("0")
    }
    var expression = if (terms.first().first) {
        Expr.UnaryMinus(terms.first().second)
    } else {
        terms.first().second
    }
    for (i in 1 until terms.size) {
        val (negative, term) = terms[i]
        expression = Expr.Binary(
            left = expression,
            operator = if (negative) BinaryOperator.Subtract else BinaryOperator.Add,
            right = term
        )
    }
    return expression
}

private fun canonicalMonomialToExpr(monomial: CanonicalMonomial<Flt64>): Expr {
    if (monomial.powers.isEmpty()) {
        return numberExpr(monomial.coefficient)
    }
    val factorExpressions = monomial.powers.map { (symbol, exp) ->
        powerExpr(symbolExpr(symbol), exp.toInt())
    }
    val product = factorExpressions.reduce { left, right -> multiplyExpr(left, right) }
    val absCoefficient = if (monomial.coefficient < Flt64.zero) {
        -monomial.coefficient
    } else {
        monomial.coefficient
    }
    return scaleExpr(absCoefficient, product)
}

fun LinearPolynomial<Flt64>.toLegacyExpr(): LegacySymbolExpr {
    return this.toCanonicalPolynomial().toLegacyExpr()
}

fun QuadraticPolynomial<Flt64>.toLegacyExpr(symbolComparator: Comparator<Symbol>? = null): LegacySymbolExpr {
    return this.toCanonicalPolynomial(symbolComparator).toLegacyExpr()
}

fun CanonicalPolynomial<Flt64>.toLegacyExpr(symbolComparator: Comparator<Symbol>? = null): LegacySymbolExpr {
    val source = this.combineTerms(symbolComparator)
    val terms = ArrayList<Pair<Boolean, Expr>>(source.monomials.size + 1)
    for (monomial in source.monomials) {
        if (monomial.coefficient == Flt64.zero) {
            continue
        }
        val negative = monomial.coefficient < Flt64.zero
        val normalized = if (negative) {
            monomial.copy(coefficient = -monomial.coefficient)
        } else {
            monomial
        }
        terms.add(negative to canonicalMonomialToExpr(normalized))
    }
    if (source.constant != Flt64.zero) {
        val negative = source.constant < Flt64.zero
        val normalized = if (negative) -source.constant else source.constant
        terms.add(negative to numberExpr(normalized))
    }
    return combineSignedTerms(terms)
}

@Deprecated(
    message = "toExpr() builds the legacy Expr AST. Prefer toLegacyExpr for explicit legacy usage.",
    replaceWith = ReplaceWith("toLegacyExpr()")
)
fun LinearPolynomial<Flt64>.toExpr(): LegacySymbolExpr {
    return toLegacyExpr()
}

@Deprecated(
    message = "toExpr() builds the legacy Expr AST. Prefer toLegacyExpr(symbolComparator) for explicit legacy usage.",
    replaceWith = ReplaceWith("toLegacyExpr(symbolComparator)")
)
fun QuadraticPolynomial<Flt64>.toExpr(symbolComparator: Comparator<Symbol>? = null): LegacySymbolExpr {
    return toLegacyExpr(symbolComparator)
}

@Deprecated(
    message = "toExpr() builds the legacy Expr AST. Prefer toLegacyExpr(symbolComparator) for explicit legacy usage.",
    replaceWith = ReplaceWith("toLegacyExpr(symbolComparator)")
)
fun CanonicalPolynomial<Flt64>.toExpr(symbolComparator: Comparator<Symbol>? = null): LegacySymbolExpr {
    return toLegacyExpr(symbolComparator)
}

private fun comparisonToExprOperator(comparison: Comparison): ComparisonOperator {
    return when (comparison) {
        Comparison.LT -> ComparisonOperator.Less
        Comparison.LE -> ComparisonOperator.LessEqual
        Comparison.EQ -> ComparisonOperator.Equal
        Comparison.NE -> ComparisonOperator.NotEqual
        Comparison.GE -> ComparisonOperator.GreaterEqual
        Comparison.GT -> ComparisonOperator.Greater
    }
}

private fun exprOperatorToComparison(operator: ComparisonOperator): Comparison {
    return when (operator) {
        ComparisonOperator.Less -> Comparison.LT
        ComparisonOperator.LessEqual -> Comparison.LE
        ComparisonOperator.Equal -> Comparison.EQ
        ComparisonOperator.NotEqual -> Comparison.NE
        ComparisonOperator.GreaterEqual -> Comparison.GE
        ComparisonOperator.Greater -> Comparison.GT
    }
}

fun LinearInequality<Flt64>.toLegacyExpr(): Expr.Comparison {
    return this.toCanonicalInequality().toLegacyExpr()
}

fun QuadraticInequalityOf<Flt64>.toLegacyExpr(symbolComparator: Comparator<Symbol>? = null): Expr.Comparison {
    return this.toCanonicalInequality(symbolComparator).toLegacyExpr()
}

fun CanonicalInequality.toLegacyExpr(symbolComparator: Comparator<Symbol>? = null): Expr.Comparison {
    return Expr.Comparison(
        left = lhs.toLegacyExpr(symbolComparator),
        operator = comparisonToExprOperator(comparison),
        right = rhs.toLegacyExpr(symbolComparator)
    )
}

@Deprecated(
    message = "toExpr() builds the legacy Expr AST. Prefer toLegacyExpr for explicit legacy usage.",
    replaceWith = ReplaceWith("toLegacyExpr()")
)
fun LinearInequality<Flt64>.toExpr(): Expr.Comparison {
    return toLegacyExpr()
}

@Deprecated(
    message = "toExpr() builds the legacy Expr AST. Prefer toLegacyExpr(symbolComparator) for explicit legacy usage.",
    replaceWith = ReplaceWith("toLegacyExpr(symbolComparator)")
)
fun QuadraticInequalityOf<Flt64>.toExpr(symbolComparator: Comparator<Symbol>? = null): Expr.Comparison {
    return toLegacyExpr(symbolComparator)
}

@Deprecated(
    message = "toExpr() builds the legacy Expr AST. Prefer toLegacyExpr(symbolComparator) for explicit legacy usage.",
    replaceWith = ReplaceWith("toLegacyExpr(symbolComparator)")
)
fun CanonicalInequality.toExpr(symbolComparator: Comparator<Symbol>? = null): Expr.Comparison {
    return toLegacyExpr(symbolComparator)
}

private fun mergePowers(left: Map<Symbol, Int32>, right: Map<Symbol, Int32>): Map<Symbol, Int32> {
    val result = LinkedHashMap<Symbol, Int32>(left)
    for ((symbol, exp) in right) {
        result[symbol] = (result[symbol] ?: Int32.zero) + exp
    }
    return result
}

private fun addCanonical(
    lhs: CanonicalPolynomial<Flt64>,
    rhs: CanonicalPolynomial<Flt64>
): CanonicalPolynomial<Flt64> {
    return CanonicalPolynomial<Flt64>(
        monomials = lhs.monomials + rhs.monomials,
        constant = lhs.constant + rhs.constant
    ).combineTerms()
}

private fun negateCanonical(polynomial: CanonicalPolynomial<Flt64>): CanonicalPolynomial<Flt64> {
    return CanonicalPolynomial<Flt64>(
        monomials = polynomial.monomials.map { it.copy(coefficient = -it.coefficient) },
        constant = -polynomial.constant
    )
}

private fun subtractCanonical(
    lhs: CanonicalPolynomial<Flt64>,
    rhs: CanonicalPolynomial<Flt64>
): CanonicalPolynomial<Flt64> {
    return addCanonical(lhs, negateCanonical(rhs))
}

private fun multiplyCanonical(
    lhs: CanonicalPolynomial<Flt64>,
    rhs: CanonicalPolynomial<Flt64>
): CanonicalPolynomial<Flt64> {
    val monomials = ArrayList<CanonicalMonomial<Flt64>>(lhs.monomials.size * rhs.monomials.size + lhs.monomials.size + rhs.monomials.size)
    for (left in lhs.monomials) {
        for (right in rhs.monomials) {
            monomials.add(
                CanonicalMonomial<Flt64>(
                    coefficient = left.coefficient * right.coefficient,
                    powers = mergePowers(left.powers, right.powers)
                )
            )
        }
        if (rhs.constant != Flt64.zero) {
            monomials.add(
                left.copy(coefficient = left.coefficient * rhs.constant)
            )
        }
    }
    for (right in rhs.monomials) {
        if (lhs.constant != Flt64.zero) {
            monomials.add(
                right.copy(coefficient = right.coefficient * lhs.constant)
            )
        }
    }
    return CanonicalPolynomial<Flt64>(
        monomials = monomials,
        constant = lhs.constant * rhs.constant
    ).combineTerms()
}

private fun parseExponent(text: String): Int {
    val doubleValue = text.toDoubleOrNull()
        ?: throw IllegalArgumentException("Invalid exponent '$text'.")
    val rounded = doubleValue.toInt()
    require(rounded.toDouble() == doubleValue) {
        "Exponent must be an integer."
    }
    return rounded
}

// ============================================================================
// Generic Typed Canonical Polynomial Operations (Ring-based)
// ============================================================================

private fun <T> addTypedCanonical(
    lhs: CanonicalPolynomial<T>,
    rhs: CanonicalPolynomial<T>,
    zero: T,
    isZero: (T) -> Boolean = { it == zero },
    symbolComparator: Comparator<Symbol>? = null
): CanonicalPolynomial<T> where T : Ring<T> {
    return CanonicalPolynomial(
        monomials = lhs.monomials + rhs.monomials,
        constant = lhs.constant + rhs.constant
    ).combineCanonicalPolynomialTerms(zero, isZero, symbolComparator)
}

private fun <T> negateTypedCanonical(
    polynomial: CanonicalPolynomial<T>
): CanonicalPolynomial<T> where T : Ring<T> {
    return CanonicalPolynomial(
        monomials = polynomial.monomials.map { it.copy(coefficient = -it.coefficient) },
        constant = -polynomial.constant
    )
}

private fun <T> subtractTypedCanonical(
    lhs: CanonicalPolynomial<T>,
    rhs: CanonicalPolynomial<T>,
    zero: T,
    isZero: (T) -> Boolean = { it == zero },
    symbolComparator: Comparator<Symbol>? = null
): CanonicalPolynomial<T> where T : Ring<T> {
    return addTypedCanonical(lhs, negateTypedCanonical(rhs), zero, isZero, symbolComparator)
}

private fun <T> multiplyTypedCanonical(
    lhs: CanonicalPolynomial<T>,
    rhs: CanonicalPolynomial<T>,
    zero: T,
    isZero: (T) -> Boolean = { it == zero },
    symbolComparator: Comparator<Symbol>? = null
): CanonicalPolynomial<T> where T : Ring<T> {
    val monomials = ArrayList<CanonicalMonomial<T>>(lhs.monomials.size * rhs.monomials.size + lhs.monomials.size + rhs.monomials.size)
    for (left in lhs.monomials) {
        for (right in rhs.monomials) {
            monomials.add(
                CanonicalMonomial(
                    coefficient = left.coefficient * right.coefficient,
                    powers = mergePowers(left.powers, right.powers)
                )
            )
        }
        if (!isZero(rhs.constant)) {
            monomials.add(
                left.copy(coefficient = left.coefficient * rhs.constant)
            )
        }
    }
    for (right in rhs.monomials) {
        if (!isZero(lhs.constant)) {
            monomials.add(
                right.copy(coefficient = right.coefficient * lhs.constant)
            )
        }
    }
    return CanonicalPolynomial(
        monomials = monomials,
        constant = lhs.constant * rhs.constant
    ).combineCanonicalPolynomialTerms(zero, isZero, symbolComparator)
}

private fun <T> powTypedCanonical(
    base: CanonicalPolynomial<T>,
    exponent: Int,
    zero: T,
    one: T,
    isZero: (T) -> Boolean = { it == zero },
    symbolComparator: Comparator<Symbol>? = null
): CanonicalPolynomial<T> where T : Ring<T> {
    require(exponent >= 0) {
        "Negative exponent is not supported for polynomial conversion."
    }
    var result = CanonicalPolynomial<T>(constant = one)
    repeat(exponent) {
        result = multiplyTypedCanonical(result, base, zero, isZero, symbolComparator)
    }
    return result
}

fun <T> LegacySymbolExpr.legacyToCanonicalPolynomialTyped(
    numberParser: NumberParser<T>,
    zero: T,
    one: T,
    symbolOf: (String) -> Symbol = ::symbolOfSerializedIdentifier,
    isZero: (T) -> Boolean = { it == zero },
    symbolComparator: Comparator<Symbol>? = null
): CanonicalPolynomial<T> where T : Ring<T> {
    return when (this) {
        is Expr.NumberLiteral -> {
            val number = numberParser.parse(text)
                ?: throw IllegalArgumentException("Invalid number literal '$text' for target type.")
            CanonicalPolynomial<T>(constant = number)
        }

        is Expr.Identifier -> {
            CanonicalPolynomial(
                monomials = listOf(
                    CanonicalMonomial(
                        coefficient = one,
                        powers = mapOf(symbolOf(name) to Int32.one)
                    )
                ),
                constant = zero
            )
        }

        is Expr.UnaryMinus -> {
            negateTypedCanonical(
                operand.legacyToCanonicalPolynomialTyped(
                    numberParser = numberParser,
                    zero = zero,
                    one = one,
                    symbolOf = symbolOf,
                    isZero = isZero,
                    symbolComparator = symbolComparator
                )
            )
        }

        is Expr.Binary -> {
            val leftPolynomial = left.legacyToCanonicalPolynomialTyped(
                numberParser = numberParser,
                zero = zero,
                one = one,
                symbolOf = symbolOf,
                isZero = isZero,
                symbolComparator = symbolComparator
            )
            val rightPolynomial = right.legacyToCanonicalPolynomialTyped(
                numberParser = numberParser,
                zero = zero,
                one = one,
                symbolOf = symbolOf,
                isZero = isZero,
                symbolComparator = symbolComparator
            )
            when (operator) {
                BinaryOperator.Add -> addTypedCanonical(leftPolynomial, rightPolynomial, zero, isZero, symbolComparator)
                BinaryOperator.Subtract -> subtractTypedCanonical(leftPolynomial, rightPolynomial, zero, isZero, symbolComparator)
                BinaryOperator.Multiply -> multiplyTypedCanonical(leftPolynomial, rightPolynomial, zero, isZero, symbolComparator)
                BinaryOperator.Power -> {
                    val exponent = when (right) {
                        is Expr.NumberLiteral -> parseExponent(right.text)
                        else -> throw IllegalArgumentException("Exponent must be number literal.")
                    }
                    powTypedCanonical(leftPolynomial, exponent, zero, one, isZero, symbolComparator)
                }
            }
        }

        is Expr.FunctionCall -> {
            throw IllegalArgumentException("Cannot convert function call '${name}' to canonical polynomial.")
        }

        is Expr.Comparison -> {
            throw IllegalArgumentException("Cannot convert comparison expression to canonical polynomial.")
        }
    }.combineCanonicalPolynomialTerms(zero, isZero, symbolComparator)
}

fun <T> LegacySymbolExpr.legacyToLinearPolynomialTypedOrNull(
    numberParser: NumberParser<T>,
    zero: T,
    one: T,
    symbolOf: (String) -> Symbol = ::symbolOfSerializedIdentifier,
    isZero: (T) -> Boolean = { it == zero }
): LinearPolynomial<T>? where T : Ring<T> = legacyToCanonicalPolynomialTyped(
    numberParser = numberParser,
    zero = zero,
    one = one,
    symbolOf = symbolOf,
    isZero = isZero
).toLinearPolynomialOrNull(
    zero = zero,
    isZero = isZero
)

fun <T> LegacySymbolExpr.legacyToQuadraticPolynomialTypedOrNull(
    numberParser: NumberParser<T>,
    zero: T,
    one: T,
    symbolOf: (String) -> Symbol = ::symbolOfSerializedIdentifier,
    isZero: (T) -> Boolean = { it == zero },
    symbolComparator: Comparator<Symbol>? = null
): QuadraticPolynomial<T>? where T : Ring<T> = legacyToCanonicalPolynomialTyped(
    numberParser = numberParser,
    zero = zero,
    one = one,
    symbolOf = symbolOf,
    isZero = isZero,
    symbolComparator = symbolComparator
).toQuadraticPolynomialOrNull(
    zero = zero,
    isZero = isZero,
    symbolComparator = symbolComparator
)

private fun powCanonical(
    base: CanonicalPolynomial<Flt64>,
    exponent: Int
): CanonicalPolynomial<Flt64> {
    require(exponent >= 0) {
        "Negative exponent is not supported for polynomial conversion."
    }
    var result = CanonicalPolynomial<Flt64>(constant = Flt64.one)
    repeat(exponent) {
        result = multiplyCanonical(result, base)
    }
    return result
}

fun LegacySymbolExpr.legacyToCanonicalPolynomial(
    symbolOf: (String) -> Symbol = ::symbolOfSerializedIdentifier
): CanonicalPolynomial<Flt64> {
    return legacyToCanonicalPolynomialTyped(
        numberParser = Flt64NumberParser,
        zero = Flt64.zero,
        one = Flt64.one,
        symbolOf = symbolOf,
        isZero = { it == Flt64.zero }
    ).combineTerms()
}

fun LegacySymbolExpr.legacyToLinearPolynomialOrNull(
    symbolOf: (String) -> Symbol = ::symbolOfSerializedIdentifier
): LinearPolynomial<Flt64>? {
    return legacyToCanonicalPolynomial(symbolOf).toLinearPolynomialOrNull()
}

fun LegacySymbolExpr.legacyToQuadraticPolynomialOrNull(
    symbolOf: (String) -> Symbol = ::symbolOfSerializedIdentifier,
    symbolComparator: Comparator<Symbol>? = null
): QuadraticPolynomial<Flt64>? {
    return legacyToCanonicalPolynomial(symbolOf).toQuadraticPolynomialOrNull(symbolComparator)
}

fun Expr.Comparison.legacyToCanonicalInequality(
    symbolOf: (String) -> Symbol = ::symbolOfSerializedIdentifier
): CanonicalInequality {
    return CanonicalInequality(
        lhs = left.legacyToCanonicalPolynomial(symbolOf),
        rhs = right.legacyToCanonicalPolynomial(symbolOf),
        comparison = exprOperatorToComparison(operator)
    )
}

fun Expr.Comparison.legacyToLinearInequalityOrNull(
    symbolOf: (String) -> Symbol = ::symbolOfSerializedIdentifier
): LinearInequality<Flt64>? {
    return legacyToCanonicalInequality(symbolOf).toLinearInequalityOrNull()
}

fun Expr.Comparison.legacyToQuadraticInequalityOrNull(
    symbolOf: (String) -> Symbol = ::symbolOfSerializedIdentifier,
    symbolComparator: Comparator<Symbol>? = null
): QuadraticInequality? {
    return legacyToCanonicalInequality(symbolOf).toQuadraticInequalityOrNull(symbolComparator)
}

@Deprecated(
    message = "toCanonicalPolynomialTyped() converts from the legacy Expr AST. Prefer legacyToCanonicalPolynomialTyped for explicit legacy usage.",
    replaceWith = ReplaceWith("legacyToCanonicalPolynomialTyped(numberParser, zero, one, symbolOf, isZero, symbolComparator)")
)
fun <T> Expr.toCanonicalPolynomialTyped(
    numberParser: NumberParser<T>,
    zero: T,
    one: T,
    symbolOf: (String) -> Symbol = ::symbolOfSerializedIdentifier,
    isZero: (T) -> Boolean = { it == zero },
    symbolComparator: Comparator<Symbol>? = null
): CanonicalPolynomial<T> where T : Ring<T> = legacyToCanonicalPolynomialTyped(
    numberParser = numberParser,
    zero = zero,
    one = one,
    symbolOf = symbolOf,
    isZero = isZero,
    symbolComparator = symbolComparator
)

@Deprecated(
    message = "toLinearPolynomialTypedOrNull() converts from the legacy Expr AST. Prefer legacyToLinearPolynomialTypedOrNull for explicit legacy usage.",
    replaceWith = ReplaceWith("legacyToLinearPolynomialTypedOrNull(numberParser, zero, one, symbolOf, isZero)")
)
fun <T> Expr.toLinearPolynomialTypedOrNull(
    numberParser: NumberParser<T>,
    zero: T,
    one: T,
    symbolOf: (String) -> Symbol = ::symbolOfSerializedIdentifier,
    isZero: (T) -> Boolean = { it == zero }
): LinearPolynomial<T>? where T : Ring<T> = legacyToLinearPolynomialTypedOrNull(
    numberParser = numberParser,
    zero = zero,
    one = one,
    symbolOf = symbolOf,
    isZero = isZero
)

@Deprecated(
    message = "toQuadraticPolynomialTypedOrNull() converts from the legacy Expr AST. Prefer legacyToQuadraticPolynomialTypedOrNull for explicit legacy usage.",
    replaceWith = ReplaceWith("legacyToQuadraticPolynomialTypedOrNull(numberParser, zero, one, symbolOf, isZero, symbolComparator)")
)
fun <T> Expr.toQuadraticPolynomialTypedOrNull(
    numberParser: NumberParser<T>,
    zero: T,
    one: T,
    symbolOf: (String) -> Symbol = ::symbolOfSerializedIdentifier,
    isZero: (T) -> Boolean = { it == zero },
    symbolComparator: Comparator<Symbol>? = null
): QuadraticPolynomial<T>? where T : Ring<T> = legacyToQuadraticPolynomialTypedOrNull(
    numberParser = numberParser,
    zero = zero,
    one = one,
    symbolOf = symbolOf,
    isZero = isZero,
    symbolComparator = symbolComparator
)

@Deprecated(
    message = "toCanonicalPolynomial() converts from the legacy Expr AST. Prefer legacyToCanonicalPolynomial for explicit legacy usage.",
    replaceWith = ReplaceWith("legacyToCanonicalPolynomial(symbolOf)")
)
fun Expr.toCanonicalPolynomial(
    symbolOf: (String) -> Symbol = ::symbolOfSerializedIdentifier
): CanonicalPolynomial<Flt64> = legacyToCanonicalPolynomial(symbolOf)

@Deprecated(
    message = "toLinearPolynomialOrNull() converts from the legacy Expr AST. Prefer legacyToLinearPolynomialOrNull for explicit legacy usage.",
    replaceWith = ReplaceWith("legacyToLinearPolynomialOrNull(symbolOf)")
)
fun Expr.toLinearPolynomialOrNull(
    symbolOf: (String) -> Symbol = ::symbolOfSerializedIdentifier
): LinearPolynomial<Flt64>? = legacyToLinearPolynomialOrNull(symbolOf)

@Deprecated(
    message = "toQuadraticPolynomialOrNull() converts from the legacy Expr AST. Prefer legacyToQuadraticPolynomialOrNull for explicit legacy usage.",
    replaceWith = ReplaceWith("legacyToQuadraticPolynomialOrNull(symbolOf, symbolComparator)")
)
fun Expr.toQuadraticPolynomialOrNull(
    symbolOf: (String) -> Symbol = ::symbolOfSerializedIdentifier,
    symbolComparator: Comparator<Symbol>? = null
): QuadraticPolynomial<Flt64>? = legacyToQuadraticPolynomialOrNull(symbolOf, symbolComparator)

@Deprecated(
    message = "toCanonicalInequality() converts from the legacy Expr AST. Prefer legacyToCanonicalInequality for explicit legacy usage.",
    replaceWith = ReplaceWith("legacyToCanonicalInequality(symbolOf)")
)
fun Expr.Comparison.toCanonicalInequality(
    symbolOf: (String) -> Symbol = ::symbolOfSerializedIdentifier
): CanonicalInequality = legacyToCanonicalInequality(symbolOf)

@Deprecated(
    message = "toLinearInequalityOrNull() converts from the legacy Expr AST. Prefer legacyToLinearInequalityOrNull for explicit legacy usage.",
    replaceWith = ReplaceWith("legacyToLinearInequalityOrNull(symbolOf)")
)
fun Expr.Comparison.toLinearInequalityOrNull(
    symbolOf: (String) -> Symbol = ::symbolOfSerializedIdentifier
): LinearInequality<Flt64>? = legacyToLinearInequalityOrNull(symbolOf)

@Deprecated(
    message = "toQuadraticInequalityOrNull() converts from the legacy Expr AST. Prefer legacyToQuadraticInequalityOrNull for explicit legacy usage.",
    replaceWith = ReplaceWith("legacyToQuadraticInequalityOrNull(symbolOf, symbolComparator)")
)
fun Expr.Comparison.toQuadraticInequalityOrNull(
    symbolOf: (String) -> Symbol = ::symbolOfSerializedIdentifier,
    symbolComparator: Comparator<Symbol>? = null
): QuadraticInequality? = legacyToQuadraticInequalityOrNull(symbolOf, symbolComparator)

fun LegacySymbolExpr.toLegacyJsonString(): String {
    return writeJson(this)
}

@Deprecated(
    message = "toJsonString() on Expr serializes the legacy Expr AST. Prefer toLegacyJsonString for explicit legacy usage.",
    replaceWith = ReplaceWith("toLegacyJsonString()")
)
fun LegacySymbolExpr.toJsonString(): String {
    return toLegacyJsonString()
}

fun legacySymbolExprFromJson(json: String): LegacySymbolExpr {
    return readFromJson(ByteArrayInputStream(json.toByteArray(Charsets.UTF_8)))
}

@Deprecated(
    message = "symbolExprFromJson parses the legacy Expr AST. Prefer legacySymbolExprFromJson for explicit legacy usage.",
    replaceWith = ReplaceWith("legacySymbolExprFromJson(json)")
)
fun symbolExprFromJson(json: String): LegacySymbolExpr {
    return legacySymbolExprFromJson(json)
}

// ============================================================================
// Polynomial convenience serialization methods
// ============================================================================

fun LinearPolynomial<Flt64>.toJsonString(): String {
    return this.toLegacyExpr().toLegacyJsonString()
}

fun QuadraticPolynomial<Flt64>.toJsonString(symbolComparator: Comparator<Symbol>? = null): String {
    return this.toLegacyExpr(symbolComparator).toLegacyJsonString()
}

fun CanonicalPolynomial<Flt64>.toJsonString(symbolComparator: Comparator<Symbol>? = null): String {
    return this.toLegacyExpr(symbolComparator).toLegacyJsonString()
}

// ============================================================================
// Inequality convenience serialization methods
// ============================================================================

fun LinearInequality<Flt64>.toJsonString(): String {
    return this.toLegacyExpr().toLegacyJsonString()
}

fun QuadraticInequalityOf<Flt64>.toJsonString(symbolComparator: Comparator<Symbol>? = null): String {
    return this.toLegacyExpr(symbolComparator).toLegacyJsonString()
}

fun CanonicalInequality.toJsonString(symbolComparator: Comparator<Symbol>? = null): String {
    return this.toLegacyExpr(symbolComparator).toLegacyJsonString()
}

// ============================================================================
// Deserialize from JSON
// ============================================================================

fun linearPolynomialFromJson(json: String, symbolOf: (String) -> Symbol = ::symbolOfSerializedIdentifier): LinearPolynomial<Flt64>? {
    return legacySymbolExprFromJson(json).legacyToLinearPolynomialOrNull(symbolOf)
}

fun quadraticPolynomialFromJson(
    json: String,
    symbolOf: (String) -> Symbol = ::symbolOfSerializedIdentifier,
    symbolComparator: Comparator<Symbol>? = null
): QuadraticPolynomial<Flt64>? {
    return legacySymbolExprFromJson(json).legacyToQuadraticPolynomialOrNull(symbolOf, symbolComparator)
}

fun canonicalPolynomialFromJson(json: String, symbolOf: (String) -> Symbol = ::symbolOfSerializedIdentifier): CanonicalPolynomial<Flt64> {
    return legacySymbolExprFromJson(json).legacyToCanonicalPolynomial(symbolOf)
}

fun linearInequalityFromJson(json: String, symbolOf: (String) -> Symbol = ::symbolOfSerializedIdentifier): LinearInequality<Flt64>? {
    val expr = legacySymbolExprFromJson(json)
    return (expr as? Expr.Comparison)?.legacyToLinearInequalityOrNull(symbolOf)
}

fun quadraticInequalityFromJson(
    json: String,
    symbolOf: (String) -> Symbol = ::symbolOfSerializedIdentifier,
    symbolComparator: Comparator<Symbol>? = null
): QuadraticInequality? {
    val expr = legacySymbolExprFromJson(json)
    return (expr as? Expr.Comparison)?.legacyToQuadraticInequalityOrNull(symbolOf, symbolComparator)
}

fun canonicalInequalityFromJson(json: String, symbolOf: (String) -> Symbol = ::symbolOfSerializedIdentifier): CanonicalInequality? {
    val expr = legacySymbolExprFromJson(json)
    return (expr as? Expr.Comparison)?.legacyToCanonicalInequality(symbolOf)
}
