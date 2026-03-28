package fuookami.ospf.kotlin.utils.math.symbol.monomial

import fuookami.ospf.kotlin.utils.math.algebra.number.*
import fuookami.ospf.kotlin.utils.math.algebra.value_range.*

import fuookami.ospf.kotlin.utils.math.symbol.*

data class CanonicalMonomial<T>(
    val coefficient: T,
    val factors: List<Symbol> = emptyList()
) {
    val degree: Int
        get() = factors.size

    val category: Category
        get() = when (degree) {
            0, 1 -> Linear
            2 -> Quadratic
            else -> Nonlinear
        }
}
