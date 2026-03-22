package fuookami.ospf.kotlin.utils.math.symbol.monomial

import fuookami.ospf.kotlin.utils.math.Flt64
import fuookami.ospf.kotlin.utils.math.symbol.Symbol

data class LinearMonomial(
    val coefficient: Flt64 = Flt64.one,
    val symbol: Symbol
)
