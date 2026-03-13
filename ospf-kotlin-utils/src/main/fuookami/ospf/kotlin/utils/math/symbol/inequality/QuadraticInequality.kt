package fuookami.ospf.kotlin.utils.math.symbol.inequality

import fuookami.ospf.kotlin.utils.math.symbol.polynomial.*

data class QuadraticInequality(
    val lhs: QuadraticPolynomial,
    val rhs: QuadraticPolynomial,
    val comparison: Comparison
)
