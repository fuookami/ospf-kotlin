package fuookami.ospf.kotlin.core.frontend.expression

import fuookami.ospf.kotlin.utils.math.*

interface Expression {
    var name: String
    var displayName: String?
    val possibleRange: ValueRange<Flt64>
    var range: ValueRange<Flt64>

    fun intersectRange(range: ValueRange<Flt64>): Boolean

    fun rangeLess(value: Flt64): Boolean
    fun rangeLessEqual(value: Flt64): Boolean
    fun rangeGreater(value: Flt64): Boolean
    fun rangeGreaterEqual(value: Flt64): Boolean
}

internal class ExpressionImpl(
    private val possibleValueRangeGenerator: () -> ValueRange<Flt64>
) {
    val possibleRange: ValueRange<Flt64>
        get() {
            if (!this::_possibleRange.isInitialized) {
                _possibleRange = possibleValueRangeGenerator()
            }
            return _possibleRange
        }
    var range: ValueRange<Flt64>
        get() {
            if (!this::_range.isInitialized) {
                _range = possibleRange.clone()
            }
            return _range
        }
        set(value) {
            _range = value
        }
    private lateinit var _possibleRange: ValueRange<Flt64>
    private lateinit var _range: ValueRange<Flt64>

    fun intersectRange(range: ValueRange<Flt64>): Boolean {
        _range = _range.intersect(range)
        return !_range.empty()
    }

    fun rangeLess(value: Flt64) = if (range.empty()) {
        false
    } else {
        intersectRange(
            ValueRange(
                range.lowerBound,
                ValueWrapper(value, Flt64),
                range.lowerInterval,
                IntervalType.Open,
                Flt64
            )
        )
    }

    fun rangeLessEqual(value: Flt64) = if (range.empty()) {
        false
    } else {
        intersectRange(
            ValueRange(
                range.lowerBound,
                ValueWrapper(value, Flt64),
                range.lowerInterval,
                IntervalType.Closed,
                Flt64
            )
        )
    }

    fun rangeGreater(value: Flt64) = if (range.empty()) {
        false
    } else {
        intersectRange(
            ValueRange(
                ValueWrapper(value, Flt64),
                range.upperBound,
                IntervalType.Open,
                range.upperInterval,
                Flt64
            )
        )
    }

    fun rangeGreaterEqual(value: Flt64) = if (range.empty()) {
        false
    } else {
        intersectRange(
            ValueRange(
                ValueWrapper(value, Flt64),
                range.upperBound,
                IntervalType.Closed,
                range.upperInterval,
                Flt64
            )
        )
    }
}
