package fuookami.ospf.kotlin.utils.math.symbol.polynomial

import fuookami.ospf.kotlin.utils.math.algebra.number.*
import fuookami.ospf.kotlin.utils.math.algebra.value_range.*

import fuookami.ospf.kotlin.utils.math.symbol.Category
import fuookami.ospf.kotlin.utils.math.symbol.Linear
import fuookami.ospf.kotlin.utils.math.symbol.Quadratic
import fuookami.ospf.kotlin.utils.math.symbol.monomial.QuadraticMonomial

data class QuadraticPolynomial<T>(
    val monomials: List<QuadraticMonomial<T>> = emptyList(),
    val constant: T
) {
    val category: Category
        get() = if (monomials.any { it.isQuadratic }) Quadratic else Linear
}
