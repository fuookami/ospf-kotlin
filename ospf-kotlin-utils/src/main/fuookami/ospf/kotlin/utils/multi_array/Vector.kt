package fuookami.ospf.kotlin.utils.multi_array

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.math.value_range.*

open class DummyIndex(
    val range: ValueRange<UInt64>?
) {
    constructor(value: UInt64) : this(ValueRange(value).value!!)
}

data object AllDummyIndex : DummyIndex(null)

val _a = AllDummyIndex
typealias DummyVector = List<DummyIndex>
