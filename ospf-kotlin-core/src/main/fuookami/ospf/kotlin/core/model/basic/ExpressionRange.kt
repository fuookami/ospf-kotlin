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
        /** 创建默认值域（全范围）的 ExpressionRange / Create an ExpressionRange with default (full) value range */
        @Suppress("UNCHECKED_CAST")
        inline operator fun <reified T> invoke(): ExpressionRange<T> where T : RealNumber<T>, T : NumberField<T> {
            val constants = (T::class.companionObjectInstance!! as RealNumberConstants<T>)
            return ExpressionRange(constants)
        }

        /** 使用指定值域创建 ExpressionRange / Create an ExpressionRange with the specified value range */
        @Suppress("UNCHECKED_CAST")
        inline operator fun <reified T> invoke(
            range: ValueRange<T>
        ): ExpressionRange<T> where T : RealNumber<T>, T : NumberField<T> {
            val constants = (T::class.companionObjectInstance!! as RealNumberConstants<T>)
            return ExpressionRange(range, constants)
        }

        /** 使用指定常量提供器创建默认值域的 ExpressionRange / Create an ExpressionRange with default range using the specified constants provider */
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

    /** 值范围 / Value range */
    val range by ::_range

    /** 值范围对象 / Value range object */
    val valueRange get() = range?.toFlt64()

    /** 下界 / Lower bound */
    val lowerBound get() = range?.lowerBound

    /** 上界 / Upper bound */
    val upperBound get() = range?.upperBound

    /** 是否为空 / Whether empty */
    val empty get() = range == null

    /** 是否为固定值 / Whether fixed */
    val fixed get() = range?.fixed == true

    /** 固定值 / Fixed value */
    val fixedValue get() = range?.fixedValue

    private var _set = false
    internal val set get() = _set

    /**
     * 设置值范围
     * Set value range
     *
     * @param range   要设置的值范围 / The value range to set
    */
    fun set(range: ValueRange<V>) {
        _set = true
        _range = range
    }

    /**
     * 与指定范围求交
     * Intersect with specified range
     *
     * @param range   要求交的值范围 / The value range to intersect with
     * @return        交集是否非空 / Whether the intersection is non-empty
    */
    fun intersectWith(range: ValueRange<V>): Boolean {
        _set = true
        _range = _range?.intersect(range)
        return _range != null
    }

    /**
     * 判断是否小于
     * Check if less than
     *
     * @param value   比较值 / The value to compare against
     * @return        约束是否可行 / Whether the constraint is feasible
    */
    infix fun ls(value: Invariant<V>): Boolean {
        return intersectWith(
            ValueRange.leq(
                value.value(),
                Interval.Closed,
                constants
            ).value!!
        )
    }

    /**
     * 判断是否小于等于
     * Check if less than or equal
     *
     * @param value   比较值 / The value to compare against
     * @return        约束是否可行 / Whether the constraint is feasible
    */
    infix fun leq(value: Invariant<V>): Boolean {
        return ls(value)
    }

    /**
     * 判断是否大于
     * Check if greater than
     *
     * @param value   比较值 / The value to compare against
     * @return        约束是否可行 / Whether the constraint is feasible
    */
    infix fun gr(value: Invariant<V>): Boolean {
        return intersectWith(
            ValueRange.geq(
                value.value(),
                Interval.Closed,
                constants
            ).value!!
        )
    }

    /**
     * 判断是否大于等于
     * Check if greater than or equal
     *
     * @param value   比较值 / The value to compare against
     * @return        约束是否可行 / Whether the constraint is feasible
    */
    infix fun geq(value: Invariant<V>): Boolean {
        return gr(value)
    }

    /**
     * 判断是否等于
     * Check if equal
     *
     * @param value   比较值 / The value to compare against
     * @return        约束是否可行 / Whether the constraint is feasible
    */
    infix fun eq(value: Invariant<V>): Boolean {
        return intersectWith(
            ValueRange(
                value.value(),
                constants
            ).value!!
        )
    }

    /**
     * 与指定上下界求交
     * Intersect with specified bounds
     *
     * @param lb      下界 / The lower bound
     * @param ub      上界 / The upper bound
     * @return        交集是否非空 / Whether the intersection is non-empty
    */
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
     *
     * @param value   上限值 / The upper bound value
     * @return        约束是否可行 / Whether the constraint is feasible
    */
    fun setUb(value: Invariant<V>): Boolean {
        return leq(value)
    }

    /**
     * 设置下限
     * Set lower bound
     *
     * @param value   下限值 / The lower bound value
     * @return        约束是否可行 / Whether the constraint is feasible
    */
    fun setLb(value: Invariant<V>): Boolean {
        return geq(value)
    }

    override fun toString(): String {
        return range?.toString() ?: "empty"
    }
}
