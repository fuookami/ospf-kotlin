package fuookami.ospf.kotlin.utils.math.symbol.monomial

import fuookami.ospf.kotlin.utils.math.algebra.number.*
import fuookami.ospf.kotlin.utils.math.algebra.value_range.*

import fuookami.ospf.kotlin.utils.math.symbol.Category
import fuookami.ospf.kotlin.utils.math.symbol.Linear
import fuookami.ospf.kotlin.utils.math.symbol.Quadratic
import fuookami.ospf.kotlin.utils.math.symbol.Symbol

data class QuadraticMonomial<T>(
    val coefficient: T,
    val symbol1: Symbol,
    val symbol2: Symbol? = null
) {
    val isQuadratic: Boolean
        get() = symbol2 != null

    val category: Category
        get() = if (isQuadratic) Quadratic else Linear
}
