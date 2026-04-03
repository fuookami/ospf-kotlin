package fuookami.ospf.kotlin.math.symbol.operation

import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.value_range.*
import fuookami.ospf.kotlin.math.algebra.concept.Ring
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.defaultSymbolComparator
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.monomial.QuadraticMonomial
import fuookami.ospf.kotlin.math.symbol.monomial.CanonicalMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.QuadraticPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.CanonicalPolynomial
import kotlin.math.abs

// ============================================================================
// Differentiation Operations (Ring-based, no Generic conversion)
// ============================================================================

private fun <T> scaleByIntWithSign(
    value: T,
    amount: Int,
    zero: T
): T where T : Ring<T> {
    if (amount == 0) {
        return zero
    }
    var result = zero
    repeat(abs(amount)) {
        result += value
    }
    return if (amount < 0) {
        -result
    } else {
        result
    }
}

/**
 * Derivative of a linear monomial.
 */
fun <T> LinearMonomial<T>.derivativeLinear(
    symbol: Symbol,
    zero: T
): T where T : Ring<T> {
    return if (this.symbol == symbol) {
        coefficient
    } else {
        zero
    }
}

/**
 * Derivative of a linear polynomial.
 */
fun <T> LinearPolynomial<T>.derivativeLinear(
    symbol: Symbol,
    zero: T
): T where T : Ring<T> {
    var derivative = zero
    for (monomial in monomials) {
        derivative += monomial.derivativeLinear(symbol, zero)
    }
    return derivative
}

/**
 * Gradient of a linear polynomial.
 */
fun <T> LinearPolynomial<T>.gradientLinear(
    order: List<Symbol>,
    zero: T
): List<T> where T : Ring<T> {
    return order.map { derivativeLinear(it, zero) }
}

/**
 * Derivative of a quadratic monomial.
 */
fun <T> QuadraticMonomial<T>.derivativeQuadratic(
    symbol: Symbol,
    zero: T,
    combineTerms: Boolean = true,
    isZero: (T) -> Boolean = { it == zero }
): LinearPolynomial<T> where T : Ring<T> {
    if (symbol2 == null) {
        return if (symbol1 == symbol) {
            LinearPolynomial(constant = coefficient)
        } else {
            LinearPolynomial(constant = zero)
        }
    }

    val derivativeMonomials = ArrayList<LinearMonomial<T>>()
    if (symbol1 == symbol) {
        derivativeMonomials.add(
            LinearMonomial(
                coefficient = coefficient,
                symbol = symbol2
            )
        )
    }
    if (symbol2 == symbol) {
        derivativeMonomials.add(
            LinearMonomial(
                coefficient = coefficient,
                symbol = symbol1
            )
        )
    }
    val derivative = LinearPolynomial(
        monomials = derivativeMonomials,
        constant = zero
    )
    return if (combineTerms) {
        derivative.combineLinearTerms(zero, isZero)
    } else {
        derivative
    }
}

/**
 * Derivative of a quadratic polynomial.
 */
fun <T> QuadraticPolynomial<T>.derivativeQuadratic(
    symbol: Symbol,
    zero: T,
    combineTerms: Boolean = true,
    isZero: (T) -> Boolean = { it == zero }
): LinearPolynomial<T> where T : Ring<T> {
    val derivativeMonomials = ArrayList<LinearMonomial<T>>()
    var derivativeConstant = zero
    for (monomial in monomials) {
        val monomialDerivative = monomial.derivativeQuadratic(
            symbol = symbol,
            zero = zero,
            combineTerms = false,
            isZero = isZero
        )
        derivativeMonomials.addAll(monomialDerivative.monomials)
        derivativeConstant += monomialDerivative.constant
    }
    val derivative = LinearPolynomial(
        monomials = derivativeMonomials,
        constant = derivativeConstant
    )
    return if (combineTerms) {
        derivative.combineLinearTerms(zero, isZero)
    } else {
        derivative
    }
}

/**
 * Gradient of a quadratic polynomial.
 */
fun <T> QuadraticPolynomial<T>.gradientQuadratic(
    order: List<Symbol>,
    zero: T,
    combineTerms: Boolean = true,
    isZero: (T) -> Boolean = { it == zero }
): List<LinearPolynomial<T>> where T : Ring<T> {
    return order.map { derivativeQuadratic(it, zero, combineTerms, isZero) }
}

/**
 * Derivative of a canonical monomial.
 */
fun <T> CanonicalMonomial<T>.derivativeCanonical(
    symbol: Symbol,
    zero: T,
    combineTerms: Boolean = true,
    isZero: (T) -> Boolean = { it == zero },
    symbolComparator: Comparator<Symbol>? = null
): CanonicalPolynomial<T> where T : Ring<T> {
    val exponent = powers[symbol]?.toInt() ?: 0
    if (exponent == 0) {
        return CanonicalPolynomial(constant = zero)
    }

    val remainedPowers = LinkedHashMap(powers)
    if (exponent == 1) {
        remainedPowers.remove(symbol)
    } else {
        remainedPowers[symbol] = Int32(exponent - 1)
    }

    val scaledCoefficient = scaleByIntWithSign(coefficient, exponent, zero)
    val derivative = CanonicalPolynomial(
        monomials = listOf(
            CanonicalMonomial(
                coefficient = scaledCoefficient,
                powers = remainedPowers
            )
        ),
        constant = zero
    )
    return if (combineTerms) {
        derivative.combineCanonicalPolynomialTerms(zero, isZero, symbolComparator)
    } else {
        derivative
    }
}

/**
 * Derivative of a canonical polynomial.
 */
fun <T> CanonicalPolynomial<T>.derivativeCanonical(
    symbol: Symbol,
    zero: T,
    combineTerms: Boolean = true,
    isZero: (T) -> Boolean = { it == zero },
    symbolComparator: Comparator<Symbol>? = null
): CanonicalPolynomial<T> where T : Ring<T> {
    val derivativeMonomials = ArrayList<CanonicalMonomial<T>>()
    for (monomial in monomials) {
        derivativeMonomials.addAll(
            monomial.derivativeCanonical(
                symbol = symbol,
                zero = zero,
                combineTerms = false,
                isZero = isZero,
                symbolComparator = symbolComparator
            ).monomials
        )
    }
    val derivative = CanonicalPolynomial(
        monomials = derivativeMonomials,
        constant = zero
    )
    return if (combineTerms) {
        derivative.combineCanonicalPolynomialTerms(zero, isZero, symbolComparator)
    } else {
        derivative
    }
}

/**
 * Gradient of a canonical polynomial.
 */
fun <T> CanonicalPolynomial<T>.gradientCanonical(
    order: List<Symbol>,
    zero: T,
    combineTerms: Boolean = true,
    isZero: (T) -> Boolean = { it == zero },
    symbolComparator: Comparator<Symbol>? = null
): List<CanonicalPolynomial<T>> where T : Ring<T> {
    return order.map {
        derivativeCanonical(
            symbol = it,
            zero = zero,
            combineTerms = combineTerms,
            isZero = isZero,
            symbolComparator = symbolComparator
        )
    }
}