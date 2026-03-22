package fuookami.ospf.kotlin.utils.math.symbol.polynomial

import fuookami.ospf.kotlin.utils.math.Flt64
import fuookami.ospf.kotlin.utils.math.symbol.Category
import fuookami.ospf.kotlin.utils.math.symbol.Linear
import fuookami.ospf.kotlin.utils.math.symbol.monomial.LinearMonomial

data class LinearPolynomial(
    val monomials: List<LinearMonomial> = emptyList(),
    val constant: Flt64 = Flt64.zero
) {
    val category: Category
        get() = Linear
}
