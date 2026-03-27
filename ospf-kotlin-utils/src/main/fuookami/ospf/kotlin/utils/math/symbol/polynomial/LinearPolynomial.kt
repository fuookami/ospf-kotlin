package fuookami.ospf.kotlin.utils.math.symbol.polynomial

import fuookami.ospf.kotlin.utils.math.Flt64
import fuookami.ospf.kotlin.utils.math.symbol.Category
import fuookami.ospf.kotlin.utils.math.symbol.Linear
import fuookami.ospf.kotlin.utils.math.symbol.monomial.LinearMonomial

@Suppress("UNCHECKED_CAST")
private fun <T> defaultLinearConstant(): T {
    return Flt64.zero as T
}

data class LinearPolynomial<T>(
    val monomials: List<LinearMonomial<T>> = emptyList(),
    val constant: T = defaultLinearConstant()
) {
    val category: Category
        get() = Linear
}
