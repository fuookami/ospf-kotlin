package fuookami.ospf.kotlin.core.frontend.variable

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.math.value_range.*
import fuookami.ospf.kotlin.core.frontend.expression.*

data class Range<T, V>(
    val type: T,
    override val constants: RealNumberConstants<V>
) : ExpressionRange<V>(
    _range = ValueRange(
        type.minimum,
        type.maximum,
        Interval.Closed,
        Interval.Closed,
        constants
    ).value!!,
    constants = constants
) where T : VariableType<V>, V : RealNumber<V>, V : NumberField<V>

infix fun Range<Binary, UInt8>.ls(value: Boolean): Boolean {
    return this.ls(UInt8(value))
}

infix fun Range<Binary, UInt8>.leq(value: Boolean): Boolean {
    return this.leq(UInt8(value))
}

infix fun Range<Binary, UInt8>.gr(value: Boolean): Boolean {
    return this.gr(UInt8(value))
}

infix fun Range<Binary, UInt8>.geq(value: Boolean): Boolean {
    return this.geq(UInt8(value))
}

infix fun Range<Binary, UInt8>.eq(value: Boolean): Boolean {
    return this.eq(UInt8(value))
}
