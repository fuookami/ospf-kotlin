package fuookami.ospf.kotlin.utils.math.symbol.operation

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.math.symbol.*
import fuookami.ospf.kotlin.utils.math.symbol.monomial.*
import fuookami.ospf.kotlin.utils.math.symbol.polynomial.*

fun LinearMonomial.derivative(symbol: Symbol): Flt64 {
    return if (this.symbol == symbol) {
        coefficient
    } else {
        Flt64.zero
    }
}

fun LinearPolynomial.derivative(symbol: Symbol): Flt64 {
    var derivative = Flt64.zero
    for (monomial in monomials) {
        derivative += monomial.derivative(symbol)
    }
    return derivative
}

fun LinearPolynomial.gradient(order: List<Symbol>): List<Flt64> {
    return order.map { derivative(it) }
}

fun QuadraticMonomial.derivative(
    symbol: Symbol,
    combineTerms: Boolean = true
): LinearPolynomial {
    if (symbol2 == null) {
        return if (symbol1 == symbol) {
            LinearPolynomial(constant = coefficient)
        } else {
            LinearPolynomial()
        }
    }

    val derivativeMonomials = ArrayList<LinearMonomial>()
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
    val derivative = LinearPolynomial(monomials = derivativeMonomials)
    return if (combineTerms) {
        derivative.combineTerms()
    } else {
        derivative
    }
}

fun QuadraticPolynomial.derivative(
    symbol: Symbol,
    combineTerms: Boolean = true
): LinearPolynomial {
    val derivativeMonomials = ArrayList<LinearMonomial>()
    var derivativeConstant = Flt64.zero
    for (monomial in monomials) {
        val monomialDerivative = monomial.derivative(symbol, combineTerms = false)
        derivativeMonomials.addAll(monomialDerivative.monomials)
        derivativeConstant += monomialDerivative.constant
    }
    val derivative = LinearPolynomial(
        monomials = derivativeMonomials,
        constant = derivativeConstant
    )
    return if (combineTerms) {
        derivative.combineTerms()
    } else {
        derivative
    }
}

fun QuadraticPolynomial.gradient(
    order: List<Symbol>,
    combineTerms: Boolean = true
): List<LinearPolynomial> {
    return order.map { derivative(it, combineTerms) }
}
