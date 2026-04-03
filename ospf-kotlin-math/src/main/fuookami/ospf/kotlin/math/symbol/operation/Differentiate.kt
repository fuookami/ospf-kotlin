package fuookami.ospf.kotlin.math.symbol.operation

import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Fatal
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.defaultSymbolComparator
import fuookami.ospf.kotlin.math.symbol.monomial.CanonicalMonomial
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.monomial.QuadraticMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.CanonicalPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.QuadraticPolynomial

fun LinearMonomial<Flt64>.derivative(symbol: Symbol): Flt64 {
    return derivativeLinear(symbol, zero = Flt64.zero)
}

fun LinearPolynomial<Flt64>.derivative(symbol: Symbol): Flt64 {
    return derivativeLinear(symbol, zero = Flt64.zero)
}

fun LinearPolynomial<Flt64>.gradient(order: List<Symbol>): List<Flt64> {
    return gradientLinear(order, zero = Flt64.zero)
}

fun QuadraticMonomial<Flt64>.derivative(
    symbol: Symbol,
    combineTerms: Boolean = true
): LinearPolynomial<Flt64> {
    return derivativeQuadratic(
        symbol = symbol,
        zero = Flt64.zero,
        combineTerms = combineTerms,
        isZero = { it == Flt64.zero }
    )
}

fun QuadraticPolynomial<Flt64>.derivative(
    symbol: Symbol,
    combineTerms: Boolean = true
): LinearPolynomial<Flt64> {
    return derivativeQuadratic(
        symbol = symbol,
        zero = Flt64.zero,
        combineTerms = combineTerms,
        isZero = { it == Flt64.zero }
    )
}

fun QuadraticPolynomial<Flt64>.gradient(
    order: List<Symbol>,
    combineTerms: Boolean = true
): List<LinearPolynomial<Flt64>> {
    return gradientQuadratic(
        order = order,
        zero = Flt64.zero,
        combineTerms = combineTerms,
        isZero = { it == Flt64.zero }
    )
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
    return derivativeCanonical(
        symbol = symbol,
        zero = Flt64.zero,
        combineTerms = combineTerms,
        isZero = { it == Flt64.zero }
    )
}

fun CanonicalPolynomial<Flt64>.derivative(
    symbol: Symbol,
    combineTerms: Boolean = true
): CanonicalPolynomial<Flt64> {
    return derivativeCanonical(
        symbol = symbol,
        zero = Flt64.zero,
        combineTerms = combineTerms,
        isZero = { it == Flt64.zero }
    )
}

fun CanonicalPolynomial<Flt64>.gradient(
    order: List<Symbol>,
    combineTerms: Boolean = true
): List<CanonicalPolynomial<Flt64>> {
    return gradientCanonical(
        order = order,
        zero = Flt64.zero,
        combineTerms = combineTerms,
        isZero = { it == Flt64.zero }
    )
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