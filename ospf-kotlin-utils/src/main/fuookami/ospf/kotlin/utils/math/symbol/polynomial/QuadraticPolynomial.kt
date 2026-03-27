package fuookami.ospf.kotlin.utils.math.symbol.polynomial

import fuookami.ospf.kotlin.utils.math.Flt64
import fuookami.ospf.kotlin.utils.math.symbol.Category
import fuookami.ospf.kotlin.utils.math.symbol.Linear
import fuookami.ospf.kotlin.utils.math.symbol.Quadratic
import fuookami.ospf.kotlin.utils.math.symbol.monomial.QuadraticMonomial

@Suppress("UNCHECKED_CAST")
private fun <T> defaultQuadraticConstant(): T {
    return Flt64.zero as T
}

data class QuadraticPolynomial<T>(
    val monomials: List<QuadraticMonomial<T>> = emptyList(),
    val constant: T = defaultQuadraticConstant()
) {
    val category: Category
        get() = if (monomials.any { it.isQuadratic }) Quadratic else Linear
}
