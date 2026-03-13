package fuookami.ospf.kotlin.utils.math.symbol.inequality

import fuookami.ospf.kotlin.utils.math.symbol.polynomial.*

data class LinearInequality(
    val lhs: LinearPolynomial,
    val rhs: LinearPolynomial,
    val comparison: Comparison
)
