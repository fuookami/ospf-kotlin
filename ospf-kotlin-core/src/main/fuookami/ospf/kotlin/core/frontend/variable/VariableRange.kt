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

@JvmName("rangeBinaryLsBoolean")
infix fun Range<Binary, UInt8>.ls(value: Boolean): Boolean {
    return this.ls(UInt8(value))
}

@JvmName("rangeBinaryLeqBoolean")
infix fun Range<Binary, UInt8>.leq(value: Boolean): Boolean {
    return this.leq(UInt8(value))
}

@JvmName("rangeBinaryGrBoolean")
infix fun Range<Binary, UInt8>.gr(value: Boolean): Boolean {
    return this.gr(UInt8(value))
}

@JvmName("rangeBinaryGeqBoolean")
infix fun Range<Binary, UInt8>.geq(value: Boolean): Boolean {
    return this.geq(UInt8(value))
}

@JvmName("rangeBinaryEqBoolean")
infix fun Range<Binary, UInt8>.eq(value: Boolean): Boolean {
    return this.eq(UInt8(value))
}

@JvmName("rangeBinarySetTrue")
fun Range<Binary, UInt8>.setTrue(): Boolean {
    return this.geq(UInt8.one)
}

@JvmName("rangeBinarySetFalse")
fun Range<Binary, UInt8>.setFalse(): Boolean {
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

@JvmName("rangeTernaryLsBoolean")
infix fun Range<Ternary, UInt8>.ls(value: Boolean): Boolean {
    return this.ls(Trivalent(value))
}

@JvmName("rangeTernaryLeqBoolean")
infix fun Range<Ternary, UInt8>.leq(value: Boolean): Boolean {
    return this.leq(Trivalent(value))
}

@JvmName("rangeTernaryGrBoolean")
infix fun Range<Ternary, UInt8>.gr(value: Boolean): Boolean {
    return this.gr(Trivalent(value))
}

@JvmName("rangeTernaryGeqBoolean")
infix fun Range<Ternary, UInt8>.geq(value: Boolean): Boolean {
    return this.geq(Trivalent(value))
}

@JvmName("rangeTernaryEqBoolean")
infix fun Range<Ternary, UInt8>.eq(value: Boolean): Boolean {
    return this.eq(Trivalent(value))
}

@JvmName("rangeTernarySetTrue")
fun Range<Ternary, UInt8>.setTrue(): Boolean {
    return this.geq(UInt8.two)
}

@JvmName("rangeTernarySetFalse")
fun Range<Ternary, UInt8>.setFalse(): Boolean {
    return this.leq(UInt8.zero)
}

@JvmName("rangeTernarySetUnknown")
fun Range<Ternary, UInt8>.setUnknown(): Boolean {
    return this.eq(UInt8.one)
}

@JvmName("rangeBalancedTernaryLsBoolean")
infix fun Range<BalancedTernary, Int8>.ls(value: BalancedTrivalent): Boolean {
    return this.ls(value.value)
}

@JvmName("rangeBalancedTernaryLeqBoolean")
infix fun Range<BalancedTernary, Int8>.leq(value: BalancedTrivalent): Boolean {
    return this.leq(value.value)
}

@JvmName("rangeBalancedTernaryGrBoolean")
infix fun Range<BalancedTernary, Int8>.gr(value: BalancedTrivalent): Boolean {
    return this.gr(value.value)
}

@JvmName("rangeBalancedTernaryGeqBoolean")
infix fun Range<BalancedTernary, Int8>.geq(value: BalancedTrivalent): Boolean {
    return this.geq(value.value)
}

@JvmName("rangeBalancedTernaryEqBoolean")
infix fun Range<BalancedTernary, Int8>.eq(value: BalancedTrivalent): Boolean {
    return this.eq(value.value)
}

infix fun Range<BalancedTernary, Int8>.ls(value: Boolean): Boolean {
    return this.ls(BalancedTrivalent(value))
}

infix fun Range<BalancedTernary, Int8>.leq(value: Boolean): Boolean {
    return this.leq(BalancedTrivalent(value))
}

infix fun Range<BalancedTernary, Int8>.gr(value: Boolean): Boolean {
    return this.gr(BalancedTrivalent(value))
}

infix fun Range<BalancedTernary, Int8>.geq(value: Boolean): Boolean {
    return this.geq(BalancedTrivalent(value))
}

infix fun Range<BalancedTernary, Int8>.eq(value: Boolean): Boolean {
    return this.eq(BalancedTrivalent(value))
}

@JvmName("rangeBalancedTernarySetTrue")
fun Range<BalancedTernary, Int8>.setTrue(): Boolean {
    return this.geq(Int8.one)
}

@JvmName("rangeBalancedTernarySetFalse")
fun Range<BalancedTernary, Int8>.setFalse(): Boolean {
    return this.leq(-Int8.one)
}

@JvmName("rangeBalancedTernarySetUnknown")
fun Range<BalancedTernary, Int8>.setUnknown(): Boolean {
    return this.eq(Int8.zero)
}
