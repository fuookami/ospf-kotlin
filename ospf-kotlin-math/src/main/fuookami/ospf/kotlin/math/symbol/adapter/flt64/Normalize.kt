package fuookami.ospf.kotlin.math.symbol.adapter.flt64

import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.inequality.LinearInequality
import fuookami.ospf.kotlin.math.symbol.monomial.QuadraticMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.QuadraticPolynomial
import fuookami.ospf.kotlin.math.symbol.operation.subtractLinear

fun LinearInequality<Flt64>.normalize(): LinearInequality<Flt64> {
    val normalizedLhs = lhs.subtractLinear(rhs, Flt64.zero).combineTerms()
    return copy(
        lhs = normalizedLhs,
        rhs = LinearPolynomial(emptyList(), Flt64.zero)
    )
}

fun QuadraticInequality.normalize(): QuadraticInequality {
    val negatedRhsMonomials = rhs.monomials.map { QuadraticMonomial(-it.coefficient, it.symbol1, it.symbol2) }
    val diff = QuadraticPolynomial(
        monomials = lhs.monomials + negatedRhsMonomials,
        constant = lhs.constant - rhs.constant
    )
    val normalizedLhs = diff.combineTerms()
    return copy(
        lhs = normalizedLhs,
        rhs = QuadraticPolynomial(emptyList(), Flt64.zero)
    )
}
