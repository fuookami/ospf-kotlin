package fuookami.ospf.kotlin.utils.math.symbol.polynomial

import fuookami.ospf.kotlin.utils.math.Flt64
import fuookami.ospf.kotlin.utils.math.symbol.Category
import fuookami.ospf.kotlin.utils.math.symbol.Linear
import fuookami.ospf.kotlin.utils.math.symbol.Quadratic
import fuookami.ospf.kotlin.utils.math.symbol.monomial.QuadraticMonomial

data class QuadraticPolynomial(
    val monomials: List<QuadraticMonomial> = emptyList(),
    val constant: Flt64 = Flt64.zero
) {
    val category: Category
        get() = if (monomials.any { it.isQuadratic }) Quadratic else Linear
}
