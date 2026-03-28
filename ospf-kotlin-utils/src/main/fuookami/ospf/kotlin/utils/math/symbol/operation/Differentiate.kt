package fuookami.ospf.kotlin.utils.math.symbol.operation

import fuookami.ospf.kotlin.utils.math.algebra.number.*
import fuookami.ospf.kotlin.utils.math.algebra.value_range.*

import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Fatal
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.math.algebra.number.Flt64
import fuookami.ospf.kotlin.utils.math.symbol.Symbol
import fuookami.ospf.kotlin.utils.math.symbol.generic.derivative as derivativeGeneric
import fuookami.ospf.kotlin.utils.math.symbol.generic.gradient as gradientGeneric
import fuookami.ospf.kotlin.utils.math.symbol.generic.toCanonicalPolynomial as toCanonicalPolynomialFromGeneric
import fuookami.ospf.kotlin.utils.math.symbol.generic.toGenericCanonicalMonomial
import fuookami.ospf.kotlin.utils.math.symbol.generic.toGenericCanonicalPolynomial
import fuookami.ospf.kotlin.utils.math.symbol.generic.toGenericLinearMonomial
import fuookami.ospf.kotlin.utils.math.symbol.generic.toGenericLinearPolynomial
import fuookami.ospf.kotlin.utils.math.symbol.generic.toGenericQuadraticMonomial
import fuookami.ospf.kotlin.utils.math.symbol.generic.toGenericQuadraticPolynomial
import fuookami.ospf.kotlin.utils.math.symbol.generic.toLinearPolynomial as toLinearPolynomialFromGeneric
import fuookami.ospf.kotlin.utils.math.symbol.monomial.CanonicalMonomial
import fuookami.ospf.kotlin.utils.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.utils.math.symbol.monomial.QuadraticMonomial
import fuookami.ospf.kotlin.utils.math.symbol.polynomial.CanonicalPolynomial
import fuookami.ospf.kotlin.utils.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.utils.math.symbol.polynomial.QuadraticPolynomial

fun LinearMonomial<Flt64>.derivative(symbol: Symbol): Flt64 {
    return toGenericLinearMonomial().derivativeGeneric(symbol, zero = Flt64.zero)
}

fun LinearPolynomial<Flt64>.derivative(symbol: Symbol): Flt64 {
    return toGenericLinearPolynomial().derivativeGeneric(symbol, zero = Flt64.zero)
}

fun LinearPolynomial<Flt64>.gradient(order: List<Symbol>): List<Flt64> {
    return toGenericLinearPolynomial().gradientGeneric(order, zero = Flt64.zero)
}

fun QuadraticMonomial<Flt64>.derivative(
    symbol: Symbol,
    combineTerms: Boolean = true
): LinearPolynomial<Flt64> {
    return toGenericQuadraticMonomial()
        .derivativeGeneric(
            symbol = symbol,
            zero = Flt64.zero,
            combineTerms = combineTerms,
            isZero = { it == Flt64.zero }
        )
        .toLinearPolynomialFromGeneric()
}

fun QuadraticPolynomial<Flt64>.derivative(
    symbol: Symbol,
    combineTerms: Boolean = true
): LinearPolynomial<Flt64> {
    return toGenericQuadraticPolynomial()
        .derivativeGeneric(
            symbol = symbol,
            zero = Flt64.zero,
            combineTerms = combineTerms,
            isZero = { it == Flt64.zero }
        )
        .toLinearPolynomialFromGeneric()
}

fun QuadraticPolynomial<Flt64>.gradient(
    order: List<Symbol>,
    combineTerms: Boolean = true
): List<LinearPolynomial<Flt64>> {
    return toGenericQuadraticPolynomial()
        .gradientGeneric(
            order = order,
            zero = Flt64.zero,
            combineTerms = combineTerms,
            isZero = { it == Flt64.zero }
        )
        .map { it.toLinearPolynomialFromGeneric() }
}

fun QuadraticPolynomial<Flt64>.hessian(
    order: List<Symbol>,
    combineTerms: Boolean = true
): Array<DoubleArray> {
    val firstOrderDerivatives = gradient(order, combineTerms)
    return Array(order.size) { i ->
        DoubleArray(order.size) { j ->
            firstOrderDerivatives[i].derivative(order[j]).toDouble()
        }
    }
}

fun CanonicalMonomial<Flt64>.derivative(
    symbol: Symbol,
    combineTerms: Boolean = true
): CanonicalPolynomial<Flt64> {
    return toGenericCanonicalMonomial()
        .derivativeGeneric(
            symbol = symbol,
            zero = Flt64.zero,
            combineTerms = combineTerms,
            isZero = { it == Flt64.zero }
        )
        .toCanonicalPolynomialFromGeneric()
}

fun CanonicalPolynomial<Flt64>.derivative(
    symbol: Symbol,
    combineTerms: Boolean = true
): CanonicalPolynomial<Flt64> {
    return toGenericCanonicalPolynomial()
        .derivativeGeneric(
            symbol = symbol,
            zero = Flt64.zero,
            combineTerms = combineTerms,
            isZero = { it == Flt64.zero }
        )
        .toCanonicalPolynomialFromGeneric()
}

fun CanonicalPolynomial<Flt64>.gradient(
    order: List<Symbol>,
    combineTerms: Boolean = true
): List<CanonicalPolynomial<Flt64>> {
    return toGenericCanonicalPolynomial()
        .gradientGeneric(
            order = order,
            zero = Flt64.zero,
            combineTerms = combineTerms,
            isZero = { it == Flt64.zero }
        )
        .map { it.toCanonicalPolynomialFromGeneric() }
}

fun CanonicalPolynomial<Flt64>.hessian(
    order: List<Symbol>,
    combineTerms: Boolean = true,
    symbolComparator: java.util.Comparator<Symbol>? = null
): Array<DoubleArray> {
    val source = if (combineTerms) {
        this.combineTerms(symbolComparator)
    } else {
        this
    }
    return when (val quadratic = source.toQuadraticPolynomialRet(symbolComparator)) {
        is Ok -> {
            quadratic.value.hessian(order = order, combineTerms = false)
        }

        is Failed -> {
            throw IllegalArgumentException(
                "Cannot compute canonical hessian for non-quadratic polynomial: ${quadratic.error.message}"
            )
        }

        is Fatal -> {
            throw IllegalArgumentException(
                "Cannot compute canonical hessian for non-quadratic polynomial: ${quadratic.errors.joinToString { it.message }}"
            )
        }
    }
}



