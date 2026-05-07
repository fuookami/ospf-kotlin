package fuookami.ospf.kotlin.core.model.basic

import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64

data class MultiObjectLocation(
    val priority: UInt64,
    val weight: Flt64
)

typealias MultiObject<Flt64> = List<Pair<MultiObjectLocation, Flt64>>



