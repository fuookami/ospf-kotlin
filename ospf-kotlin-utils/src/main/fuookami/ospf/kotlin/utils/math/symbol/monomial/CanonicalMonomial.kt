package fuookami.ospf.kotlin.utils.math.symbol.monomial

import fuookami.ospf.kotlin.utils.math.Flt64
import fuookami.ospf.kotlin.utils.math.symbol.*

@Suppress("UNCHECKED_CAST")
private fun <T> defaultCanonicalCoefficient(): T {
    return Flt64.one as T
}

data class CanonicalMonomial<T>(
    val coefficient: T = defaultCanonicalCoefficient(),
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
