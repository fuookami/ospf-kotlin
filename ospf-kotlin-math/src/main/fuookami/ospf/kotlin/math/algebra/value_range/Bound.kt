/**
 * 边界籌
 * Bound Class
 *
 * 定义值范围的边界，包含一个值和对应的区间类型，支持算术运算、比较操作和复制。
 * Defines the boundary of a value range, containing a value and its corresponding interval type, with support for arithmetic operations, comparison operations, and copying.
 */
package fuookami.ospf.kotlin.math.algebra.value_range

import fuookami.ospf.kotlin.utils.concept.Copyable
import fuookami.ospf.kotlin.utils.functional.Eq
import fuookami.ospf.kotlin.utils.functional.Ord
import fuookami.ospf.kotlin.utils.functional.Order
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.number.Flt32
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.math.algebra.number.Int16
import fuookami.ospf.kotlin.math.algebra.number.Int32
import fuookami.ospf.kotlin.math.algebra.number.Int64
import fuookami.ospf.kotlin.math.algebra.number.Int8
import fuookami.ospf.kotlin.math.algebra.number.IntX
import fuookami.ospf.kotlin.math.operator.Div
import fuookami.ospf.kotlin.math.operator.Minus
import fuookami.ospf.kotlin.math.operator.Plus
import fuookami.ospf.kotlin.math.operator.Times

/**
 * 边界
 * Bound
 *
 * 表示值范围的一个边界点，包含值包装器和区间类型。
 * 当值为无穷大时，区间类型自动设置为开区间。
 *
 * Represents a boundary point of a value range, containing a value wrapper and interval type.
 * When the value is infinity, the interval type is automatically set to Open.
 *
 * @param T 数值类型，必须是实数和数域
 * @property value 边界值的包装噌
 * @property interval 边界的区间类型（开区间或闭区间，
 */
class Bound<T>(
    val value: ValueWrapper<T>,
    interval: Interval
) : Cloneable, Copyable<Bound<T>>, Ord<Bound<T>>, Eq<Bound<T>>,
    Plus<Bound<T>, Bound<T>>, Minus<Bound<T>, Bound<T>>,
    Times<Bound<T>, Bound<T>>, Div<Bound<T>, Bound<T>>
        where T : RealNumber<T>, T : NumberField<T> {
    /**
     * 边界的区间类垌
     * Interval type of the bound
     *
     * 如果值为无穷大（正无穷或负无穷），则区间类型自动设置为开区间。
     * If the value is infinity (positive or negative), the interval type is automatically set to Open.
     */
    val interval: Interval = if (value.isInfinityOrNegativeInfinity) {
        Interval.Open
    } else {
        interval
    }

    /**
     * 复制边界
     * Copies the bound
     *
     * @return 新的边界副本
     */
    override fun copy(): Bound<T> {
        return Bound(value.copy(), interval)
    }

    /**
     * 判断边界值是否等于指定数值（仅在闭区间时有效，
     * Determines if bound value equals specified number (only valid for closed interval)
     *
     * @param rhs 要比较的数倌
     * @return 是否相等且为闭区闌
     */
    fun eq(rhs: T): Boolean = value eq rhs && interval == Interval.Closed

    /**
     * 部分相等比较
     * Partial equality comparison
     *
     * 比较两个边界的值和区间类型是否都相等。
     * Compares whether both the values and interval types of two bounds are equal.
     *
     * @param rhs 另一个边界
     * @return 是否相等，或无法确定时返囌null
     */
    override fun partialEq(rhs: Bound<T>): Boolean? = (value partialEq rhs.value)?.let {
        it && interval == rhs.interval
    }

    /**
     * 部分序比辌
     * Partial order comparison
     *
     * 当值相等时，根据区间类型判断顺序（闭区间更宽松，排在前面）。
     * When values are equal, determines order based on interval type (closed interval is more relaxed, comes first).
     *
     * @param rhs 另一个边界
     * @return 比较结果（Less、Equal 戌Greater），或无法确定时返回 null
     */
    override fun partialOrd(rhs: Bound<T>): Order? = when (val result = value partialOrd rhs.value) {
        Order.Equal -> {
            if (interval outer rhs.interval) {
                Order.Less()
            } else {
                Order.Greater()
            }
        }

        else -> {
            result
        }
    }

    /**
     * 边界与数值相劌
     * Adds a number to the bound
     *
     * @param rhs 要添加的数倌
     * @return 新的边界（区间类型保持不变）
     */
    operator fun plus(rhs: T): Bound<T> = Bound(value + rhs, interval)

    /**
     * 两个边界相加
     * Adds two bounds
     *
     * 结果的区间类型为两个边界区间类型的交集。
     * The interval type of the result is the intersection of both bounds' interval types.
     *
     * @param rhs 另一个边界
     * @return 新的边界
     */
    override fun plus(rhs: Bound<T>): Bound<T> = Bound(value + rhs.value, interval intersect rhs.interval)

    /**
     * 边界与数值相凌
     * Subtracts a number from the bound
     *
     * @param rhs 要减去的数倌
     * @return 新的边界（区间类型保持不变）
     */
    operator fun minus(rhs: T): Bound<T> = Bound(value - rhs, interval)

    /**
     * 两个边界相减
     * Subtracts two bounds
     *
     * 结果的区间类型为两个边界区间类型的交集。
     * The interval type of the result is the intersection of both bounds' interval types.
     *
     * @param rhs 另一个边界
     * @return 新的边界
     */
    override fun minus(rhs: Bound<T>): Bound<T> = Bound(value - rhs.value, interval intersect rhs.interval)

    /**
     * 边界与数值相乌
     * Multiplies bound by a number
     *
     * @param rhs 要乘的数倌
     * @return 新的边界（区间类型保持不变）
     */
    operator fun times(rhs: T): Bound<T> = Bound(value * rhs, interval)

    /**
     * 两个边界相乘
     * Multiplies two bounds
     *
     * 结果的区间类型为两个边界区间类型的交集。
     * The interval type of the result is the intersection of both bounds' interval types.
     *
     * @param rhs 另一个边界
     * @return 新的边界
     */
    override fun times(rhs: Bound<T>): Bound<T> = Bound(value * rhs.value, interval intersect rhs.interval)

    /**
     * 边界与数值相陌
     * Divides bound by a number
     *
     * @param rhs 要除的数倌
     * @return 新的边界（区间类型保持不变）
     */
    operator fun div(rhs: T): Bound<T> = Bound(value / rhs, interval)

    /**
     * 两个边界相除
     * Divides two bounds
     *
     * 结果的区间类型为两个边界区间类型的交集。
     * The interval type of the result is the intersection of both bounds' interval types.
     *
     * @param rhs 另一个边界
     * @return 新的边界
     */
    override fun div(rhs: Bound<T>): Bound<T> = Bound(value / rhs.value, interval intersect rhs.interval)

    /**
     * 转换丌Flt64 类型的边界
     * Converts to Flt64 typed bound
     *
     * @return Flt64 类型的新边界
     */
    fun toFlt64(): Bound<fuookami.ospf.kotlin.math.algebra.number.Flt64> = Bound(ValueWrapper(value.toFlt64()).value!!, interval)

    /**
     * 获取字符串表礌
     * Gets string representation
     *
     * @return 边界的字符串形式
     */
    override fun toString(): String {
        return "Bound($value, $interval)"
    }
}

/**
 * Flt32 类型边界的取负操佌
 * Negation operation for Flt32 typed bound
 *
 * @return 取负后的新边界
 */
@JvmName("negBoundFlt32")
operator fun Bound<Flt32>.unaryMinus() = Bound(-value, interval)

/**
 * Flt64 类型边界的取负操佌
 * Negation operation for Flt64 typed bound
 *
 * @return 取负后的新边界
 */
@JvmName("negBoundFlt64")
operator fun Bound<fuookami.ospf.kotlin.math.algebra.number.Flt64>.unaryMinus() = Bound(-value, interval)

/**
 * FltX 类型边界的取负操佌
 * Negation operation for FltX typed bound
 *
 * @return 取负后的新边界
 */
@JvmName("negBoundFltX")
operator fun Bound<FltX>.unaryMinus() = Bound(-value, interval)

/**
 * Int8 类型边界的取负操佌
 * Negation operation for Int8 typed bound
 *
 * @return 取负后的新边界
 */
@JvmName("negBoundInt8")
operator fun Bound<Int8>.unaryMinus() = Bound(-value, interval)

/**
 * Int16 类型边界的取负操佌
 * Negation operation for Int16 typed bound
 *
 * @return 取负后的新边界
 */
@JvmName("negBoundInt16")
operator fun Bound<Int16>.unaryMinus() = Bound(-value, interval)

/**
 * Int32 类型边界的取负操佌
 * Negation operation for Int32 typed bound
 *
 * @return 取负后的新边界
 */
@JvmName("negBoundInt32")
operator fun Bound<Int32>.unaryMinus() = Bound(-value, interval)

/**
 * Int64 类型边界的取负操佌
 * Negation operation for Int64 typed bound
 *
 * @return 取负后的新边界
 */
@JvmName("negBoundInt64")
operator fun Bound<Int64>.unaryMinus() = Bound(-value, interval)

/**
 * IntX 类型边界的取负操佌
 * Negation operation for IntX typed bound
 *
 * @return 取负后的新边界
 */
@JvmName("negBoundIntX")
operator fun Bound<IntX>.unaryMinus() = Bound(-value, interval)
