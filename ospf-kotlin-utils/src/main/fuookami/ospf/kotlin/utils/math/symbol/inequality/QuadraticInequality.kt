package fuookami.ospf.kotlin.utils.math.symbol.inequality

import fuookami.ospf.kotlin.utils.math.Flt64
import fuookami.ospf.kotlin.utils.math.symbol.polynomial.QuadraticPolynomial

data class QuadraticInequality(
    val lhs: QuadraticPolynomial<Flt64>,
    val rhs: QuadraticPolynomial<Flt64>,
    val comparison: Comparison
)
