package fuookami.ospf.kotlin.utils.math.symbol.inequality

import fuookami.ospf.kotlin.utils.math.Flt64
import fuookami.ospf.kotlin.utils.math.symbol.polynomial.CanonicalPolynomial

data class CanonicalInequality(
    val lhs: CanonicalPolynomial<Flt64>,
    val rhs: CanonicalPolynomial<Flt64>,
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
