package fuookami.ospf.kotlin.utils.math.symbol.polynomial

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.math.symbol.*
import fuookami.ospf.kotlin.utils.math.symbol.monomial.*

data class CanonicalPolynomial(
    val monomials: List<CanonicalMonomial> = emptyList(),
    val constant: Flt64 = Flt64.zero
) {
    val category: Category
        get() = when (monomials.maxOfOrNull { it.degree } ?: 0) {
            0, 1 -> Linear
            2 -> Quadratic
            else -> Nonlinear
        }
}

