package fuookami.ospf.kotlin.utils.math.symbol.polynomial

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.math.symbol.*
import fuookami.ospf.kotlin.utils.math.symbol.monomial.*

data class LinearPolynomial(
    val monomials: List<LinearMonomial> = emptyList(),
    val constant: Flt64 = Flt64.zero
) {
    val category: Category
        get() = Linear
}
