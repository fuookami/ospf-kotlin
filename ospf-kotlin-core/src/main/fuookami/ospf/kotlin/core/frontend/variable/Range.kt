package fuookami.ospf.kotlin.core.frontend.variable

import fuookami.ospf.kotlin.utils.math.*

data class Range<T, V>(
    val type: T,
    private val constants: RealNumberConstants<V>
) where T : VariableType<V>, V : RealNumber<V>, V : NumberField<V> {

    private var _range: ValueRange<V> =
        ValueRange(
            type.minimum,
            type.maximum,
            IntervalType.Closed,
            IntervalType.Closed,
            constants
        )

    val range: ValueRange<V> get() = _range;
    val valueRange: ValueRange<Flt64>
        get() = ValueRange(
            range.lowerBound.toFlt64(),
            range.upperBound.toFlt64(),
            range.lowerInterval,
            range.upperInterval,
            Flt64
        )
    val lowerBound: ValueWrapper<V>?
        get() = if (empty()) {
            null
        } else {
            range.lowerBound
        }
    val upperBound: ValueWrapper<V>?
        get() = if (empty()) {
            null
        } else {
            range.upperBound
        }

    fun empty() = range.empty()
    fun fixed() = range.fixed()
    fun fixedValue() = range.fixedValue()

    fun set(range: ValueRange<V>) {
        _range = range
    }

    fun intersectWith(range: ValueRange<V>): Boolean {
        _range = _range.intersect(range)
        return !_range.empty()
    }

    infix fun ls(value: Invariant<V>): Boolean {
        return if (range.empty()) {
            false
        } else {
            intersectWith(
                ValueRange(
                    lowerBound!!,
                    ValueWrapper(value.value(), constants),
                    IntervalType.Closed,
                    IntervalType.Closed,
                    constants
                )
            )
        }
    }

    infix fun leq(value: Invariant<V>): Boolean {
        return ls(value)
    }

    infix fun gr(value: Invariant<V>): Boolean {
        return if (range.empty()) {
            false
        } else {
            intersectWith(
                ValueRange(
                    ValueWrapper(value.value(), constants),
                    upperBound!!,
                    IntervalType.Closed,
                    IntervalType.Closed,
                    constants
                )
            )
        }
    }

    infix fun geq(value: Invariant<V>): Boolean {
        return gr(value)
    }

    infix fun eq(value: Invariant<V>): Boolean {
        return if (range.empty()) {
            false
        } else {
            intersectWith(
                ValueRange(
                    ValueWrapper(value.value(), constants),
                    ValueWrapper(value.value(), constants),
                    IntervalType.Closed,
                    IntervalType.Closed,
                    constants
                )
            )
        }
    }

    fun intersectWith(lb: Invariant<V>, ub: Invariant<V>): Boolean {
        return if (range.empty()) {
            false
        } else {
            intersectWith(
                ValueRange(
                    ValueWrapper(lb.value(), constants),
                    ValueWrapper(ub.value(), constants),
                    IntervalType.Closed,
                    IntervalType.Closed,
                    constants
                )
            )
        }
    }
}
