package fuookami.ospf.kotlin.utils.math.symbol.generic

import fuookami.ospf.kotlin.utils.math.algebra.number.*
import fuookami.ospf.kotlin.utils.math.algebra.concept.*
import fuookami.ospf.kotlin.utils.math.algebra.value_range.*

import fuookami.ospf.kotlin.utils.math.algebra.concept.Ring
import fuookami.ospf.kotlin.utils.math.symbol.Symbol
import kotlin.math.abs

private fun <T> scaleByInt(
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

fun <T> GenericLinearMonomial<T>.derivative(
    symbol: Symbol,
    zero: T
): T where T : Ring<T> {
    return if (this.symbol == symbol) {
        coefficient
    } else {
        zero
    }
}

fun <T> GenericLinearPolynomial<T>.derivative(
    symbol: Symbol,
    zero: T
): T where T : Ring<T> {
    var derivative = zero
    for (monomial in monomials) {
        derivative += monomial.derivative(symbol, zero)
    }
    return derivative
}

fun <T> GenericLinearPolynomial<T>.gradient(
    order: List<Symbol>,
    zero: T
): List<T> where T : Ring<T> {
    return order.map { derivative(it, zero) }
}

fun <T> GenericQuadraticMonomial<T>.derivative(
    symbol: Symbol,
    zero: T,
    combineTerms: Boolean = true,
    isZero: (T) -> Boolean = { it == zero }
): GenericLinearPolynomial<T> where T : Ring<T> {
    if (symbol2 == null) {
        return if (symbol1 == symbol) {
            GenericLinearPolynomial(constant = coefficient)
        } else {
            GenericLinearPolynomial(constant = zero)
        }
    }

    val derivativeMonomials = ArrayList<GenericLinearMonomial<T>>()
    if (symbol1 == symbol) {
        derivativeMonomials.add(
            GenericLinearMonomial(
                coefficient = coefficient,
                symbol = symbol2
            )
        )
    }
    if (symbol2 == symbol) {
        derivativeMonomials.add(
            GenericLinearMonomial(
                coefficient = coefficient,
                symbol = symbol1
            )
        )
    }
    val derivative = GenericLinearPolynomial(
        monomials = derivativeMonomials,
        constant = zero
    )
    return if (combineTerms) {
        derivative.combineTerms(zero, isZero)
    } else {
        derivative
    }
}

fun <T> GenericQuadraticPolynomial<T>.derivative(
    symbol: Symbol,
    zero: T,
    combineTerms: Boolean = true,
    isZero: (T) -> Boolean = { it == zero }
): GenericLinearPolynomial<T> where T : Ring<T> {
    val derivativeMonomials = ArrayList<GenericLinearMonomial<T>>()
    var derivativeConstant = zero
    for (monomial in monomials) {
        val monomialDerivative = monomial.derivative(
            symbol = symbol,
            zero = zero,
            combineTerms = false,
            isZero = isZero
        )
        derivativeMonomials.addAll(monomialDerivative.monomials)
        derivativeConstant += monomialDerivative.constant
    }
    val derivative = GenericLinearPolynomial(
        monomials = derivativeMonomials,
        constant = derivativeConstant
    )
    return if (combineTerms) {
        derivative.combineTerms(zero, isZero)
    } else {
        derivative
    }
}

fun <T> GenericQuadraticPolynomial<T>.gradient(
    order: List<Symbol>,
    zero: T,
    combineTerms: Boolean = true,
    isZero: (T) -> Boolean = { it == zero }
): List<GenericLinearPolynomial<T>> where T : Ring<T> {
    return order.map { derivative(it, zero, combineTerms, isZero) }
}

fun <T> GenericCanonicalMonomial<T>.derivative(
    symbol: Symbol,
    zero: T,
    combineTerms: Boolean = true,
    isZero: (T) -> Boolean = { it == zero },
    symbolComparator: Comparator<Symbol>? = null
): GenericCanonicalPolynomial<T> where T : Ring<T> {
    val exponent = powers[symbol] ?: 0
    if (exponent == 0) {
        return GenericCanonicalPolynomial(constant = zero)
    }

    val remainedPowers = LinkedHashMap(powers)
    if (exponent == 1) {
        remainedPowers.remove(symbol)
    } else {
        remainedPowers[symbol] = exponent - 1
    }

    val scaledCoefficient = scaleByInt(coefficient, exponent, zero)
    val derivative = GenericCanonicalPolynomial(
        monomials = listOf(
            GenericCanonicalMonomial(
                coefficient = scaledCoefficient,
                powers = remainedPowers
            )
        ),
        constant = zero
    )
    return if (combineTerms) {
        derivative.combineTerms(zero, isZero, symbolComparator)
    } else {
        derivative
    }
}

fun <T> GenericCanonicalPolynomial<T>.derivative(
    symbol: Symbol,
    zero: T,
    combineTerms: Boolean = true,
    isZero: (T) -> Boolean = { it == zero },
    symbolComparator: Comparator<Symbol>? = null
): GenericCanonicalPolynomial<T> where T : Ring<T> {
    val derivativeMonomials = ArrayList<GenericCanonicalMonomial<T>>()
    for (monomial in monomials) {
        derivativeMonomials.addAll(
            monomial.derivative(
                symbol = symbol,
                zero = zero,
                combineTerms = false,
                isZero = isZero,
                symbolComparator = symbolComparator
            ).monomials
        )
    }
    val derivative = GenericCanonicalPolynomial(
        monomials = derivativeMonomials,
        constant = zero
    )
    return if (combineTerms) {
        derivative.combineTerms(zero, isZero, symbolComparator)
    } else {
        derivative
    }
}

fun <T> GenericCanonicalPolynomial<T>.gradient(
    order: List<Symbol>,
    zero: T,
    combineTerms: Boolean = true,
    isZero: (T) -> Boolean = { it == zero },
    symbolComparator: Comparator<Symbol>? = null
): List<GenericCanonicalPolynomial<T>> where T : Ring<T> {
    return order.map {
        derivative(
            symbol = it,
            zero = zero,
            combineTerms = combineTerms,
            isZero = isZero,
            symbolComparator = symbolComparator
        )
    }
}

