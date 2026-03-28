package fuookami.ospf.kotlin.utils.math.symbol.inequality

import fuookami.ospf.kotlin.utils.math.algebra.number.*
import fuookami.ospf.kotlin.utils.math.algebra.value_range.*

import fuookami.ospf.kotlin.utils.math.algebra.number.Flt64
import fuookami.ospf.kotlin.utils.math.symbol.polynomial.LinearPolynomial

data class LinearInequality(
    val lhs: LinearPolynomial<Flt64>,
    val rhs: LinearPolynomial<Flt64>,
    val comparison: Comparison
)



