package fuookami.ospf.kotlin.core.frontend.model

import fuookami.ospf.kotlin.utils.math.algebra.number.Flt64
import fuookami.ospf.kotlin.utils.math.algebra.number.UInt64

data class MultiObjectLocation(
    val priority: UInt64,
    val weight: Flt64
)

typealias MulObj = List<Pair<MultiObjectLocation, Flt64>>



