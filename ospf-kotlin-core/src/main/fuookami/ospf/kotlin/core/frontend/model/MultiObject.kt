package fuookami.ospf.kotlin.core.frontend.model

import fuookami.ospf.kotlin.utils.math.*

data class MultiObjectLocation(
    val priority: UInt64,
    val weight: Flt64
)

typealias MulObj = List<Pair<MultiObjectLocation, Flt64>>
