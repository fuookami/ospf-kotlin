package fuookami.ospf.kotlin.utils.math.symbol.monomial

import fuookami.ospf.kotlin.utils.math.Flt64
import fuookami.ospf.kotlin.utils.math.symbol.Category
import fuookami.ospf.kotlin.utils.math.symbol.Linear
import fuookami.ospf.kotlin.utils.math.symbol.Quadratic
import fuookami.ospf.kotlin.utils.math.symbol.Symbol

data class QuadraticMonomial(
    val coefficient: Flt64 = Flt64.one,
    val symbol1: Symbol,
    val symbol2: Symbol? = null
) {
    val isQuadratic: Boolean
        get() = symbol2 != null

    val category: Category
        get() = if (isQuadratic) Quadratic else Linear
}
