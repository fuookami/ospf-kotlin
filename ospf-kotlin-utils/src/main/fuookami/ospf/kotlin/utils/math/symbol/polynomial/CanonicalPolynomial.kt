package fuookami.ospf.kotlin.utils.math.symbol.polynomial

import fuookami.ospf.kotlin.utils.math.Flt64
import fuookami.ospf.kotlin.utils.math.symbol.Category
import fuookami.ospf.kotlin.utils.math.symbol.Linear
import fuookami.ospf.kotlin.utils.math.symbol.Nonlinear
import fuookami.ospf.kotlin.utils.math.symbol.Quadratic
import fuookami.ospf.kotlin.utils.math.symbol.monomial.CanonicalMonomial

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

