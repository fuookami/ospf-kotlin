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

infix fun Range<Binary, UInt8>.isTrue(value: Boolean): Boolean {
    return this.geq(UInt8.one)
}

infix fun Range<Binary, UInt8>.isFalse(value: Boolean): Boolean {
    return this.leq(UInt8.zero)
}

infix fun Range<Ternary, UInt8>.ls(value: Trivalent): Boolean {
    return this.ls((value.value * URtn8.two).toUInt8())
}

infix fun Range<Ternary, UInt8>.leq(value: Trivalent): Boolean {
    return this.leq((value.value * URtn8.two).toUInt8())
}

infix fun Range<Ternary, UInt8>.gr(value: Trivalent): Boolean {
    return this.gr((value.value * URtn8.two).toUInt8())
}

infix fun Range<Ternary, UInt8>.geq(value: Trivalent): Boolean {
    return this.geq((value.value * URtn8.two).toUInt8())
}

infix fun Range<Ternary, UInt8>.eq(value: Trivalent): Boolean {
    return this.eq((value.value * URtn8.two).toUInt8())
}

infix fun Range<Ternary, UInt8>.isFalse(value: Trivalent): Boolean {
    return this.leq(UInt8.zero)
}

infix fun Range<Ternary, UInt8>.isTrue(value: Trivalent): Boolean {
    return this.geq(UInt8.two)
}

infix fun Range<Ternary, UInt8>.isUnknown(value: Trivalent): Boolean {
    return this.eq(UInt8.one)
}

infix fun Range<BalancedTernary, Int8>.ls(value: BalancedTrivalent): Boolean {
    return this.ls(value.value)
}

infix fun Range<BalancedTernary, Int8>.leq(value: BalancedTrivalent): Boolean {
    return this.leq(value.value)
}

infix fun Range<BalancedTernary, Int8>.gr(value: BalancedTrivalent): Boolean {
    return this.gr(value.value)
}

infix fun Range<BalancedTernary, Int8>.geq(value: BalancedTrivalent): Boolean {
    return this.geq(value.value)
}

infix fun Range<BalancedTernary, Int8>.eq(value: BalancedTrivalent): Boolean {
    return this.eq(value.value)
}

infix fun Range<BalancedTernary, Int8>.isFalse(value: BalancedTrivalent): Boolean {
    return this.leq(-Int8.one)
}

infix fun Range<BalancedTernary, Int8>.isTrue(value: BalancedTrivalent): Boolean {
    return this.geq(Int8.one)
}

infix fun Range<BalancedTernary, Int8>.isUnknown(value: BalancedTrivalent): Boolean {
    return this.eq(Int8.zero)
}
