/**
 * 表达式值域
 * Expression value range
 */
package fuookami.ospf.kotlin.core.model.basic

import kotlin.reflect.full.companionObjectInstance
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.value_range.*

/**
 * 表达式的值域，支持通过交集操作逐步收紧上下界。
 * Value range of an expression, supporting progressive tightening of bounds via intersection.
 *
 * @param V   数值类型 / The numeric type
 * @property _range      当前值域（null 表示空集） / Current value range (null means empty set)
 * @property constants   数值类型常量提供器 / Numeric type constants provider
 */
open class ExpressionRange<V>(
    private var _range: ValueRange<V>?,
    protected open val constants: RealNumberConstants<V>
) where V : RealNumber<V>, V : NumberField<V> {
    companion object {
        @Suppress("UNCHECKED_CAST")
        inline operator fun <reified T> invoke(): ExpressionRange<T> where T : RealNumber<T>, T : NumberField<T> {
            val constants = (T::class.companionObjectInstance!! as RealNumberConstants<T>)
            return ExpressionRange(constants)
        }
        @Suppress("UNCHECKED_CAST")
        inline operator fun <reified T> invoke(
            range: ValueRange<T>
        ): ExpressionRange<T> where T : RealNumber<T>, T : NumberField<T> {
            val constants = (T::class.companionObjectInstance!! as RealNumberConstants<T>)
            return ExpressionRange(range, constants)
        }

        operator fun <T> invoke(
            constants: RealNumberConstants<T>
        ): ExpressionRange<T> where T : RealNumber<T>, T : NumberField<T> {
            return ExpressionRange(
                _range = ValueRange(
                    constants.minimum,
                    constants.maximum,
                    Interval.Closed,
                    Interval.Closed,
                    constants
                ).value!!,
                constants = constants
            )
        }
    }

    val range by ::_range
    val valueRange get() = range?.toFlt64()

    val lowerBound get() = range?.lowerBound
    val upperBound get() = range?.upperBound

    val empty get() = range == null
    val fixed get() = range?.fixed == true
    val fixedValue get() = range?.fixedValue

    private var _set = false
    internal val set get() = _set

    fun set(range: ValueRange<V>) {
        _set = true
        _range = range
    }

    fun intersectWith(range: ValueRange<V>): Boolean {
        _set = true
        _range = _range?.intersect(range)
        return _range != null
    }

    infix fun ls(value: Invariant<V>): Boolean {
        return intersectWith(
            ValueRange.leq(
                value.value(),
                Interval.Closed,
                constants
            ).value!!
        )
    }

    infix fun leq(value: Invariant<V>): Boolean {
        return ls(value)
    }

    infix fun gr(value: Invariant<V>): Boolean {
        return intersectWith(
            ValueRange.geq(
                value.value(),
                Interval.Closed,
                constants
            ).value!!
        )
    }

    infix fun geq(value: Invariant<V>): Boolean {
        return gr(value)
    }

    infix fun eq(value: Invariant<V>): Boolean {
        return intersectWith(
            ValueRange(
                value.value(),
                constants
            ).value!!
        )
    }

    fun intersectWith(lb: Invariant<V>, ub: Invariant<V>): Boolean {
        return intersectWith(
            ValueRange(
                lb.value(),
                ub.value(),
                Interval.Closed,
                Interval.Closed,
                constants
            ).value!!
        )
    }

    /**
     * 设置上限
     * Set upper bound
     */
    fun setUb(value: Invariant<V>): Boolean {
        return leq(value)
    }

    /**
     * 设置下限
     * Set lower bound
     */
    fun setLb(value: Invariant<V>): Boolean {
        return geq(value)
    }

    override fun toString(): String {
        return range?.toString() ?: "empty"
    }
}
