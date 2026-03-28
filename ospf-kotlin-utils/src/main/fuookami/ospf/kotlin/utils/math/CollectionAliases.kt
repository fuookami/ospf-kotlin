package fuookami.ospf.kotlin.utils.math

import fuookami.ospf.kotlin.utils.math.algebra.number.*
import fuookami.ospf.kotlin.utils.math.algebra.value_range.*

val Collection<*>.usize: UInt64
    get() = UInt64(size)

val Collection<*>.uIndices: IntegerRange<UInt64>
    get() = UInt64.zero until usize



