package fuookami.ospf.kotlin.utils.math.symbol.polynomial

import fuookami.ospf.kotlin.utils.math.algebra.number.*
import fuookami.ospf.kotlin.utils.math.algebra.value_range.*

import fuookami.ospf.kotlin.utils.math.symbol.Category
import fuookami.ospf.kotlin.utils.math.symbol.Linear
import fuookami.ospf.kotlin.utils.math.symbol.monomial.LinearMonomial

data class LinearPolynomial<T>(
    val monomials: List<LinearMonomial<T>> = emptyList(),
    val constant: T
) {
    val category: Category
        get() = Linear
}
