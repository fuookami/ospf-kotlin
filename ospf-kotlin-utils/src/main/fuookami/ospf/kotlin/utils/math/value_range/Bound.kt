package fuookami.ospf.kotlin.utils.math.value_range

import fuookami.ospf.kotlin.utils.concept.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.operator.*

class Bound<T>(
    val value: ValueWrapper<T>,
    interval: Interval
) : Cloneable, Copyable<Bound<T>>, Ord<Bound<T>>, Eq<Bound<T>>,
    Plus<Bound<T>, Bound<T>>, Minus<Bound<T>, Bound<T>>,
    Times<Bound<T>, Bound<T>>, Div<Bound<T>, Bound<T>>
        where T : RealNumber<T>, T : NumberField<T>
{
    val interval: Interval = if (value.isInfinityOrNegativeInfinity) {
        Interval.Open
    } else {
        interval
    }

    override fun copy(): Bound<T> {
        return Bound(value.copy(), interval)
    }

    fun eq(rhs: T): Boolean = value eq rhs && interval == Interval.Closed

    override fun partialEq(rhs: Bound<T>): Boolean? = (value partialEq rhs.value)?.let {
        it && interval == rhs.interval
    }

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

    operator fun plus(rhs: T): Bound<T> = Bound(value + rhs, interval)
    override fun plus(rhs: Bound<T>): Bound<T> = Bound(value + rhs.value, interval intersect rhs.interval)

    operator fun minus(rhs: T): Bound<T> = Bound(value - rhs, interval)
    override fun minus(rhs: Bound<T>): Bound<T> = Bound(value - rhs.value, interval intersect rhs.interval)

    operator fun times(rhs: T): Bound<T> = Bound(value * rhs, interval)
    override fun times(rhs: Bound<T>): Bound<T> = Bound(value * rhs.value, interval intersect rhs.interval)

    operator fun div(rhs: T): Bound<T> = Bound(value / rhs, interval)
    override fun div(rhs: Bound<T>): Bound<T> = Bound(value / rhs.value, interval intersect rhs.interval)

    fun toFlt64(): Bound<Flt64> = Bound(ValueWrapper(value.toFlt64()).value!!, interval)

    override fun toString(): String {
        return "Bound($value, $interval)"
    }
}

@JvmName("negBoundFlt32")
operator fun Bound<Flt32>.unaryMinus() = Bound(-value, interval)

@JvmName("negBoundFlt64")
operator fun Bound<Flt64>.unaryMinus() = Bound(-value, interval)

@JvmName("negBoundFltX")
operator fun Bound<FltX>.unaryMinus() = Bound(-value, interval)

@JvmName("negBoundInt8")
operator fun Bound<Int8>.unaryMinus() = Bound(-value, interval)

@JvmName("negBoundInt16")
operator fun Bound<Int16>.unaryMinus() = Bound(-value, interval)

@JvmName("negBoundInt32")
operator fun Bound<Int32>.unaryMinus() = Bound(-value, interval)

@JvmName("negBoundInt64")
operator fun Bound<Int64>.unaryMinus() = Bound(-value, interval)

@JvmName("negBoundIntX")
operator fun Bound<IntX>.unaryMinus() = Bound(-value, interval)
