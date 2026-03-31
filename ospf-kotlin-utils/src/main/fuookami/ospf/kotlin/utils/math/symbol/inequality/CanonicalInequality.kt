package fuookami.ospf.kotlin.utils.math.symbol.inequality

import fuookami.ospf.kotlin.utils.math.algebra.number.*
import fuookami.ospf.kotlin.utils.math.algebra.value_range.*

import fuookami.ospf.kotlin.utils.math.algebra.number.Flt64
import fuookami.ospf.kotlin.utils.math.symbol.polynomial.CanonicalPolynomial

data class CanonicalInequality(
    val lhs: CanonicalPolynomial<Flt64, Int32>,
    val rhs: CanonicalPolynomial<Flt64, Int32>,
    val comparison: Comparison
) {
    fun reverse(): CanonicalInequality {
        return CanonicalInequality(
            lhs = rhs,
            rhs = lhs,
            comparison = comparison.reverse()
        )
    }
}