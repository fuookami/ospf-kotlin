package fuookami.ospf.kotlin.utils.math.symbol.inequality

import fuookami.ospf.kotlin.utils.math.symbol.polynomial.CanonicalPolynomial

data class CanonicalInequality(
    val lhs: CanonicalPolynomial,
    val rhs: CanonicalPolynomial,
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
