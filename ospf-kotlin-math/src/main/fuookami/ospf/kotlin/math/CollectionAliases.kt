package fuookami.ospf.kotlin.math

import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.value_range.*

val Collection<*>.usize: UInt64
    get() = UInt64(size)

val Collection<*>.uIndices: IntegerRange<UInt64>
    get() = UInt64.zero until usize




