/**
 * 变量范围（Range）及其对二值/三值/平衡三值变量的约束扩展函数。
 * Variable range (Range) and its constraint extension functions for binary/ternary/balanced-ternary variables.
 */
package fuookami.ospf.kotlin.core.variable

import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.value_range.*
import fuookami.ospf.kotlin.core.model.basic.ExpressionRange

/**
 * 变量范围数据类，根据变量类型自动初始化值域边界。
 * Variable range data class that initializes value range bounds based on the variable type.
 *
 * @property type 变量类型 / Variable type
 * @property constants 数值类型常量 / Numeric type constants
 */
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

/**
 * 判断二值范围是否小于布尔值 / Check if binary range is less than boolean value
 *
 * @param value 待比较的值 / Value to compare
 * @return 是否满足条件 / Whether condition is met
 */
@JvmName("rangeBinaryLsBoolean")
infix fun Range<Binary, UInt8>.ls(value: Boolean): Boolean {
    return this.ls(UInt8(value))
}

/**
 * 判断二值范围是否小于等于布尔值 / Check if binary range is less than or equal to boolean value
 *
 * @param value 待比较的值 / Value to compare
 * @return 是否满足条件 / Whether condition is met
 */
@JvmName("rangeBinaryLeqBoolean")
infix fun Range<Binary, UInt8>.leq(value: Boolean): Boolean {
    return this.leq(UInt8(value))
}

/**
 * 判断二值范围是否大于布尔值 / Check if binary range is greater than boolean value
 *
 * @param value 待比较的值 / Value to compare
 * @return 是否满足条件 / Whether condition is met
 */
@JvmName("rangeBinaryGrBoolean")
infix fun Range<Binary, UInt8>.gr(value: Boolean): Boolean {
    return this.gr(UInt8(value))
}

/**
 * 判断二值范围是否大于等于布尔值 / Check if binary range is greater than or equal to boolean value
 *
 * @param value 待比较的值 / Value to compare
 * @return 是否满足条件 / Whether condition is met
 */
@JvmName("rangeBinaryGeqBoolean")
infix fun Range<Binary, UInt8>.geq(value: Boolean): Boolean {
    return this.geq(UInt8(value))
}

/**
 * 判断二值范围是否等于布尔值 / Check if binary range is equal to boolean value
 *
 * @param value 待比较的值 / Value to compare
 * @return 是否满足条件 / Whether condition is met
 */
@JvmName("rangeBinaryEqBoolean")
infix fun Range<Binary, UInt8>.eq(value: Boolean): Boolean {
    return this.eq(UInt8(value))
}

/**
 * 设置二值范围为真 / Set binary range to true
 *
 * @return 是否满足条件 / Whether condition is met
 */
@JvmName("rangeBinarySetTrue")
fun Range<Binary, UInt8>.setTrue(): Boolean {
    return this.geq(UInt8.one)
}

/**
 * 设置二值范围为假 / Set binary range to false
 *
 * @return 是否满足条件 / Whether condition is met
 */
@JvmName("rangeBinarySetFalse")
fun Range<Binary, UInt8>.setFalse(): Boolean {
    return this.leq(UInt8.zero)
}

/**
 * 判断三值范围是否小于三值 / Check if ternary range is less than trivalent value
 *
 * @param value 待比较的值 / Value to compare
 * @return 是否满足条件 / Whether condition is met
 */
infix fun Range<Ternary, UInt8>.ls(value: Trivalent): Boolean {
    return this.ls((value.value * URtn8.two).toUInt8())
}

/**
 * 判断三值范围是否小于等于三值 / Check if ternary range is less than or equal to trivalent value
 *
 * @param value 待比较的值 / Value to compare
 * @return 是否满足条件 / Whether condition is met
 */
infix fun Range<Ternary, UInt8>.leq(value: Trivalent): Boolean {
    return this.leq((value.value * URtn8.two).toUInt8())
}

/**
 * 判断三值范围是否大于三值 / Check if ternary range is greater than trivalent value
 *
 * @param value 待比较的值 / Value to compare
 * @return 是否满足条件 / Whether condition is met
 */
infix fun Range<Ternary, UInt8>.gr(value: Trivalent): Boolean {
    return this.gr((value.value * URtn8.two).toUInt8())
}

/**
 * 判断三值范围是否大于等于三值 / Check if ternary range is greater than or equal to trivalent value
 *
 * @param value 待比较的值 / Value to compare
 * @return 是否满足条件 / Whether condition is met
 */
infix fun Range<Ternary, UInt8>.geq(value: Trivalent): Boolean {
    return this.geq((value.value * URtn8.two).toUInt8())
}

/**
 * 判断三值范围是否等于三值 / Check if ternary range is equal to trivalent value
 *
 * @param value 待比较的值 / Value to compare
 * @return 是否满足条件 / Whether condition is met
 */
infix fun Range<Ternary, UInt8>.eq(value: Trivalent): Boolean {
    return this.eq((value.value * URtn8.two).toUInt8())
}

/**
 * 判断三值范围是否小于布尔值 / Check if ternary range is less than boolean value
 *
 * @param value 待比较的值 / Value to compare
 * @return 是否满足条件 / Whether condition is met
 */
@JvmName("rangeTernaryLsBoolean")
infix fun Range<Ternary, UInt8>.ls(value: Boolean): Boolean {
    return this.ls(Trivalent(value))
}

/**
 * 判断三值范围是否小于等于布尔值 / Check if ternary range is less than or equal to boolean value
 *
 * @param value 待比较的值 / Value to compare
 * @return 是否满足条件 / Whether condition is met
 */
@JvmName("rangeTernaryLeqBoolean")
infix fun Range<Ternary, UInt8>.leq(value: Boolean): Boolean {
    return this.leq(Trivalent(value))
}

/**
 * 判断三值范围是否大于布尔值 / Check if ternary range is greater than boolean value
 *
 * @param value 待比较的值 / Value to compare
 * @return 是否满足条件 / Whether condition is met
 */
@JvmName("rangeTernaryGrBoolean")
infix fun Range<Ternary, UInt8>.gr(value: Boolean): Boolean {
    return this.gr(Trivalent(value))
}

/**
 * 判断三值范围是否大于等于布尔值 / Check if ternary range is greater than or equal to boolean value
 *
 * @param value 待比较的值 / Value to compare
 * @return 是否满足条件 / Whether condition is met
 */
@JvmName("rangeTernaryGeqBoolean")
infix fun Range<Ternary, UInt8>.geq(value: Boolean): Boolean {
    return this.geq(Trivalent(value))
}

/**
 * 判断三值范围是否等于布尔值 / Check if ternary range is equal to boolean value
 *
 * @param value 待比较的值 / Value to compare
 * @return 是否满足条件 / Whether condition is met
 */
@JvmName("rangeTernaryEqBoolean")
infix fun Range<Ternary, UInt8>.eq(value: Boolean): Boolean {
    return this.eq(Trivalent(value))
}

/**
 * 设置三值范围为真 / Set ternary range to true
 *
 * @return 是否满足条件 / Whether condition is met
 */
@JvmName("rangeTernarySetTrue")
fun Range<Ternary, UInt8>.setTrue(): Boolean {
    return this.geq(UInt8.two)
}

/**
 * 设置三值范围为假 / Set ternary range to false
 *
 * @return 是否满足条件 / Whether condition is met
 */
@JvmName("rangeTernarySetFalse")
fun Range<Ternary, UInt8>.setFalse(): Boolean {
    return this.leq(UInt8.zero)
}

/**
 * 设置三值范围为未知 / Set ternary range to unknown
 *
 * @return 是否满足条件 / Whether condition is met
 */
@JvmName("rangeTernarySetUnknown")
fun Range<Ternary, UInt8>.setUnknown(): Boolean {
    return this.eq(UInt8.one)
}

/**
 * 判断平衡三值范围是否小于平衡三值 / Check if balanced ternary range is less than balanced trivalent value
 *
 * @param value 待比较的值 / Value to compare
 * @return 是否满足条件 / Whether condition is met
 */
@JvmName("rangeBalancedTernaryLsBoolean")
infix fun Range<BalancedTernary, Int8>.ls(value: BalancedTrivalent): Boolean {
    return this.ls(value.value)
}

/**
 * 判断平衡三值范围是否小于等于平衡三值 / Check if balanced ternary range is less than or equal to balanced trivalent value
 *
 * @param value 待比较的值 / Value to compare
 * @return 是否满足条件 / Whether condition is met
 */
@JvmName("rangeBalancedTernaryLeqBoolean")
infix fun Range<BalancedTernary, Int8>.leq(value: BalancedTrivalent): Boolean {
    return this.leq(value.value)
}

/**
 * 判断平衡三值范围是否大于平衡三值 / Check if balanced ternary range is greater than balanced trivalent value
 *
 * @param value 待比较的值 / Value to compare
 * @return 是否满足条件 / Whether condition is met
 */
@JvmName("rangeBalancedTernaryGrBoolean")
infix fun Range<BalancedTernary, Int8>.gr(value: BalancedTrivalent): Boolean {
    return this.gr(value.value)
}

/**
 * 判断平衡三值范围是否大于等于平衡三值 / Check if balanced ternary range is greater than or equal to balanced trivalent value
 *
 * @param value 待比较的值 / Value to compare
 * @return 是否满足条件 / Whether condition is met
 */
@JvmName("rangeBalancedTernaryGeqBoolean")
infix fun Range<BalancedTernary, Int8>.geq(value: BalancedTrivalent): Boolean {
    return this.geq(value.value)
}

/**
 * 判断平衡三值范围是否等于平衡三值 / Check if balanced ternary range is equal to balanced trivalent value
 *
 * @param value 待比较的值 / Value to compare
 * @return 是否满足条件 / Whether condition is met
 */
@JvmName("rangeBalancedTernaryEqBoolean")
infix fun Range<BalancedTernary, Int8>.eq(value: BalancedTrivalent): Boolean {
    return this.eq(value.value)
}

/**
 * 判断平衡三值范围是否小于布尔值 / Check if balanced ternary range is less than boolean value
 *
 * @param value 待比较的值 / Value to compare
 * @return 是否满足条件 / Whether condition is met
 */
infix fun Range<BalancedTernary, Int8>.ls(value: Boolean): Boolean {
    return this.ls(BalancedTrivalent(value))
}

/**
 * 判断平衡三值范围是否小于等于布尔值 / Check if balanced ternary range is less than or equal to boolean value
 *
 * @param value 待比较的值 / Value to compare
 * @return 是否满足条件 / Whether condition is met
 */
infix fun Range<BalancedTernary, Int8>.leq(value: Boolean): Boolean {
    return this.leq(BalancedTrivalent(value))
}

/**
 * 判断平衡三值范围是否大于布尔值 / Check if balanced ternary range is greater than boolean value
 *
 * @param value 待比较的值 / Value to compare
 * @return 是否满足条件 / Whether condition is met
 */
infix fun Range<BalancedTernary, Int8>.gr(value: Boolean): Boolean {
    return this.gr(BalancedTrivalent(value))
}

/**
 * 判断平衡三值范围是否大于等于布尔值 / Check if balanced ternary range is greater than or equal to boolean value
 *
 * @param value 待比较的值 / Value to compare
 * @return 是否满足条件 / Whether condition is met
 */
infix fun Range<BalancedTernary, Int8>.geq(value: Boolean): Boolean {
    return this.geq(BalancedTrivalent(value))
}

/**
 * 判断平衡三值范围是否等于布尔值 / Check if balanced ternary range is equal to boolean value
 *
 * @param value 待比较的值 / Value to compare
 * @return 是否满足条件 / Whether condition is met
 */
infix fun Range<BalancedTernary, Int8>.eq(value: Boolean): Boolean {
    return this.eq(BalancedTrivalent(value))
}

/**
 * 设置平衡三值范围为真 / Set balanced ternary range to true
 *
 * @return 是否满足条件 / Whether condition is met
 */
@JvmName("rangeBalancedTernarySetTrue")
fun Range<BalancedTernary, Int8>.setTrue(): Boolean {
    return this.geq(Int8.one)
}

/**
 * 设置平衡三值范围为假 / Set balanced ternary range to false
 *
 * @return 是否满足条件 / Whether condition is met
 */
@JvmName("rangeBalancedTernarySetFalse")
fun Range<BalancedTernary, Int8>.setFalse(): Boolean {
    return this.leq(-Int8.one)
}

/**
 * 设置平衡三值范围为未知 / Set balanced ternary range to unknown
 *
 * @return 是否满足条件 / Whether condition is met
 */
@JvmName("rangeBalancedTernarySetUnknown")
fun Range<BalancedTernary, Int8>.setUnknown(): Boolean {
    return this.eq(Int8.zero)
}
