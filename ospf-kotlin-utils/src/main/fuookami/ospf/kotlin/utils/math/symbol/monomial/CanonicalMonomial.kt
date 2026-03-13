package fuookami.ospf.kotlin.utils.math.symbol.monomial

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.math.symbol.*

data class CanonicalMonomial(
    val coefficient: Flt64 = Flt64.one,
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

