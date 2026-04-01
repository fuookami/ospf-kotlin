package fuookami.ospf.kotlin.utils.math.algebra.value_range

import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Fatal
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.Ret
import fuookami.ospf.kotlin.utils.math.algebra.concept.NumberField
import fuookami.ospf.kotlin.utils.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.utils.math.algebra.concept.RealNumberConstants
import fuookami.ospf.kotlin.utils.math.algebra.concept.resolveRealNumberConstants

sealed interface IntervalKind {
    val interval: Interval
}

data object ClosedIntervalKind : IntervalKind {
    override val interval: Interval = Interval.Closed
}

data object OpenIntervalKind : IntervalKind {
    override val interval: Interval = Interval.Open
}

data class RuntimeIntervalKind(
    override val interval: Interval
) : IntervalKind

typealias DynamicTypedValueRange<T> = TypedValueRange<T, RuntimeIntervalKind, RuntimeIntervalKind>
typealias ClosedTypedValueRange<T> = TypedValueRange<T, ClosedIntervalKind, ClosedIntervalKind>
typealias OpenTypedValueRange<T> = TypedValueRange<T, OpenIntervalKind, OpenIntervalKind>
typealias ClosedOpenTypedValueRange<T> = TypedValueRange<T, ClosedIntervalKind, OpenIntervalKind>
typealias OpenClosedTypedValueRange<T> = TypedValueRange<T, OpenIntervalKind, ClosedIntervalKind>

class TypedValueRange<T, LB : IntervalKind, UB : IntervalKind> private constructor(
    private val valueRange: ValueRange<T>,
    val lowerKind: LB,
    val upperKind: UB
) where T : RealNumber<T>, T : NumberField<T> {
    companion object {
        private fun <T> toDynamicRange(
            range: ValueRange<T>
        ): DynamicTypedValueRange<T> where T : RealNumber<T>, T : NumberField<T> {
            return TypedValueRange(
                valueRange = range.copy(),
                lowerKind = RuntimeIntervalKind(range.lowerBound.interval),
                upperKind = RuntimeIntervalKind(range.upperBound.interval)
            )
        }

        fun <T, LB : IntervalKind, UB : IntervalKind> fromDynamic(
            range: ValueRange<T>,
            lowerKind: LB,
            upperKind: UB
        ): Ret<TypedValueRange<T, LB, UB>> where T : RealNumber<T>, T : NumberField<T> {
            return if (range.lowerBound.interval == lowerKind.interval && range.upperBound.interval == upperKind.interval) {
                Ok(
                    TypedValueRange(
                        valueRange = range.copy(),
                        lowerKind = lowerKind,
                        upperKind = upperKind
                    )
                )
            } else {
                Failed(
                    ErrorCode.IllegalArgument,
                    "TypedValueRange interval mismatch: expected lower=${lowerKind.interval}, upper=${upperKind.interval}, actual lower=${range.lowerBound.interval}, upper=${range.upperBound.interval}."
                )
            }
        }

        fun <T, LB : IntervalKind, UB : IntervalKind> fromValues(
            lb: T,
            ub: T,
            lowerKind: LB,
            upperKind: UB,
            constants: RealNumberConstants<T>
        ): Ret<TypedValueRange<T, LB, UB>> where T : RealNumber<T>, T : NumberField<T> {
            return when (val result = ValueRange(lb, ub, lowerKind.interval, upperKind.interval, constants)) {
                is Ok -> {
                    fromDynamic(result.value, lowerKind, upperKind)
                }

                is Failed -> {
                    Failed(result.error)
                }

                is Fatal -> {
                    Fatal(result.errors)
                }
            }
        }

        fun <T, LB : IntervalKind, UB : IntervalKind> fromBounds(
            lb: ValueWrapper<T>,
            ub: ValueWrapper<T>,
            lowerKind: LB,
            upperKind: UB,
            constants: RealNumberConstants<T>
        ): Ret<TypedValueRange<T, LB, UB>> where T : RealNumber<T>, T : NumberField<T> {
            return when (val result = ValueRange(lb, ub, lowerKind.interval, upperKind.interval, constants)) {
                is Ok -> {
                    fromDynamic(result.value, lowerKind, upperKind)
                }

                is Failed -> {
                    Failed(result.error)
                }

                is Fatal -> {
                    Fatal(result.errors)
                }
            }
        }

        fun <T> closed(
            lb: T,
            ub: T,
            constants: RealNumberConstants<T>
        ): Ret<ClosedTypedValueRange<T>> where T : RealNumber<T>, T : NumberField<T> {
            return fromValues(lb, ub, ClosedIntervalKind, ClosedIntervalKind, constants)
        }

        fun <T> open(
            lb: T,
            ub: T,
            constants: RealNumberConstants<T>
        ): Ret<OpenTypedValueRange<T>> where T : RealNumber<T>, T : NumberField<T> {
            return fromValues(lb, ub, OpenIntervalKind, OpenIntervalKind, constants)
        }

        fun <T> closedOpen(
            lb: T,
            ub: T,
            constants: RealNumberConstants<T>
        ): Ret<ClosedOpenTypedValueRange<T>> where T : RealNumber<T>, T : NumberField<T> {
            return fromValues(lb, ub, ClosedIntervalKind, OpenIntervalKind, constants)
        }

        fun <T> openClosed(
            lb: T,
            ub: T,
            constants: RealNumberConstants<T>
        ): Ret<OpenClosedTypedValueRange<T>> where T : RealNumber<T>, T : NumberField<T> {
            return fromValues(lb, ub, OpenIntervalKind, ClosedIntervalKind, constants)
        }

        inline fun <reified T> closed(
            lb: T,
            ub: T
        ): Ret<ClosedTypedValueRange<T>> where T : RealNumber<T>, T : NumberField<T> {
            return closed(lb, ub, resolveRealNumberConstants<T>("TypedValueRange.closed"))
        }

        inline fun <reified T> open(
            lb: T,
            ub: T
        ): Ret<OpenTypedValueRange<T>> where T : RealNumber<T>, T : NumberField<T> {
            return open(lb, ub, resolveRealNumberConstants<T>("TypedValueRange.open"))
        }

        inline fun <reified T> closedOpen(
            lb: T,
            ub: T
        ): Ret<ClosedOpenTypedValueRange<T>> where T : RealNumber<T>, T : NumberField<T> {
            return closedOpen(lb, ub, resolveRealNumberConstants<T>("TypedValueRange.closedOpen"))
        }

        inline fun <reified T> openClosed(
            lb: T,
            ub: T
        ): Ret<OpenClosedTypedValueRange<T>> where T : RealNumber<T>, T : NumberField<T> {
            return openClosed(lb, ub, resolveRealNumberConstants<T>("TypedValueRange.openClosed"))
        }
    }

    val lowerBound: ValueWrapper<T> get() = valueRange.lowerBound.value
    val upperBound: ValueWrapper<T> get() = valueRange.upperBound.value
    val lowerInterval: Interval get() = valueRange.lowerBound.interval
    val upperInterval: Interval get() = valueRange.upperBound.interval

    val fixed: Boolean get() = valueRange.fixed
    val fixedValue: T? get() = valueRange.fixedValue

    fun toDynamic(): ValueRange<T> = valueRange.copy()

    infix operator fun contains(value: T): Boolean {
        return valueRange.contains(value)
    }

    infix operator fun contains(rhs: TypedValueRange<T, *, *>): Boolean {
        return valueRange.contains(rhs.valueRange)
    }

    infix fun union(rhs: TypedValueRange<T, *, *>): DynamicTypedValueRange<T>? {
        return (valueRange union rhs.valueRange)?.let { toDynamicRange(it) }
    }

    infix fun intersect(rhs: TypedValueRange<T, *, *>): DynamicTypedValueRange<T>? {
        return (valueRange intersect rhs.valueRange)?.let { toDynamicRange(it) }
    }

    operator fun plus(rhs: T): DynamicTypedValueRange<T> {
        return toDynamicRange(valueRange + rhs)
    }

    operator fun plus(rhs: TypedValueRange<T, *, *>): DynamicTypedValueRange<T> {
        return toDynamicRange(valueRange + rhs.valueRange)
    }

    operator fun minus(rhs: T): DynamicTypedValueRange<T> {
        return toDynamicRange(valueRange - rhs)
    }

    operator fun minus(rhs: TypedValueRange<T, *, *>): DynamicTypedValueRange<T> {
        return toDynamicRange(valueRange - rhs.valueRange)
    }

    operator fun times(rhs: T): DynamicTypedValueRange<T>? {
        return (valueRange * rhs)?.let { toDynamicRange(it) }
    }

    operator fun times(rhs: TypedValueRange<T, *, *>): DynamicTypedValueRange<T>? {
        return (valueRange * rhs.valueRange)?.let { toDynamicRange(it) }
    }

    operator fun div(rhs: T): DynamicTypedValueRange<T>? {
        return (valueRange / rhs)?.let { toDynamicRange(it) }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is TypedValueRange<*, *, *>) {
            return false
        }
        return valueRange == other.valueRange && lowerKind == other.lowerKind && upperKind == other.upperKind
    }

    override fun hashCode(): Int {
        var result = valueRange.hashCode()
        result = 31 * result + lowerKind.hashCode()
        result = 31 * result + upperKind.hashCode()
        return result
    }

    override fun toString(): String {
        return "TypedValueRange(lower=$lowerBound, upper=$upperBound, lowerInterval=$lowerInterval, upperInterval=$upperInterval)"
    }
}

fun <T> ValueRange<T>.toDynamicTypedValueRange(): DynamicTypedValueRange<T>
        where T : RealNumber<T>, T : NumberField<T> {
    return TypedValueRange.fromDynamic(
        range = this,
        lowerKind = RuntimeIntervalKind(lowerBound.interval),
        upperKind = RuntimeIntervalKind(upperBound.interval)
    ).value!!
}
