package fuookami.ospf.kotlin.utils.multi_array

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.math.value_range.*

data class DummyIndex(
    val range: ValueRange<UInt64>?
) {
    constructor(value: UInt64) : this(ValueRange(value).value!!)
}

val _a = DummyIndex(null)
typealias DummyVector = List<DummyIndex>
