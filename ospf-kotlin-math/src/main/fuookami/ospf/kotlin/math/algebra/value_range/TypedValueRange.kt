/**
 * 类型化值范围
 * Typed Value Range
 *
 * 定义类型安全的值范围类，使用泛型参数静态编码区间开闭性，支持闭区间、开区间、半开半闭区间等类型，并提供算术运算和类型推导。
 * Defines type-safe value range class, using generic parameters to statically encode interval openness/closedness, supporting closed, open, and half-open interval types, with arithmetic operations and type inference.
 */
package fuookami.ospf.kotlin.math.algebra.value_range

import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Fatal
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.Ret
import fuookami.ospf.kotlin.math.algebra.concept.NumberField
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.concept.RealNumberConstants
import fuookami.ospf.kotlin.math.algebra.concept.resolveRealNumberConstants

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
    // Typed inference rule matrix / Typed 推导规则矩阵：
    // 1) plus/minus/times/div: compute ValueRange result first, then infer Closed/Open from result bounds.
    //    先计算 ValueRange 运算结果，再依据结果上下界区间推导 Closed/Open typed kind。
    // 2) Fallback-to-null only when ValueRange returns null (e.g. empty interval, divide by zero).
    //    仅当 ValueRange 返回 null 时回退为 null（如空区间、除零）。
    // 3) RuntimeIntervalKind is reserved for dynamic wrappers only, not for statically-inferable typed APIs.
    //    RuntimeIntervalKind 仅用于 dynamic 包装，不用于可静态推导的 typed API 返回值。
    private fun kindOf(interval: Interval): IntervalKind {
        return when (interval) {
            Interval.Closed -> ClosedIntervalKind
            Interval.Open -> OpenIntervalKind
        }
    }

    private fun toSameKindRange(range: ValueRange<T>): TypedValueRange<T, LB, UB> {
        return TypedValueRange.fromDynamic(
            range = range,
            lowerKind = lowerKind,
            upperKind = upperKind
        ).value!!
    }

    private fun toMostStaticKindRange(range: ValueRange<T>): TypedValueRange<T, *, *>? {
        val inferredLower = kindOf(range.lowerBound.interval)
        val inferredUpper = kindOf(range.upperBound.interval)
        return toKindRangeOrNull(
            range = range,
            lowerKind = inferredLower,
            upperKind = inferredUpper
        )
    }

    private fun <NLB : IntervalKind, NUB : IntervalKind> toKindRangeOrNull(
        range: ValueRange<T>,
        lowerKind: NLB,
        upperKind: NUB
    ): TypedValueRange<T, NLB, NUB>? {
        return when (val result = TypedValueRange.fromDynamic(
            range = range,
            lowerKind = lowerKind,
            upperKind = upperKind
        )) {
            is Ok -> {
                result.value
            }

            is Failed -> {
                null
            }

            is Fatal -> {
                null
            }
        }
    }

    private fun isPositive(value: T): Boolean {
        val zero = value - value
        return value > zero
    }

    private fun isNegative(value: T): Boolean {
        val zero = value - value
        return value < zero
    }

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

    infix fun unionTyped(rhs: TypedValueRange<T, LB, UB>): TypedValueRange<T, LB, UB>? {
        return (valueRange union rhs.valueRange)?.let { toSameKindRange(it) }
    }

    infix fun intersect(rhs: TypedValueRange<T, *, *>): DynamicTypedValueRange<T>? {
        return (valueRange intersect rhs.valueRange)?.let { toDynamicRange(it) }
    }

    infix fun intersectTyped(rhs: TypedValueRange<T, LB, UB>): TypedValueRange<T, LB, UB>? {
        return (valueRange intersect rhs.valueRange)?.let { toSameKindRange(it) }
    }

    fun plusTyped(rhs: T): TypedValueRange<T, LB, UB> {
        return toSameKindRange(valueRange + rhs)
    }

    operator fun plus(rhs: T): TypedValueRange<T, LB, UB> {
        return plusTyped(rhs)
    }

    operator fun plus(rhs: TypedValueRange<T, *, *>): DynamicTypedValueRange<T> {
        return toDynamicRange(valueRange + rhs.valueRange)
    }

    fun plusTyped(rhs: TypedValueRange<T, LB, UB>): TypedValueRange<T, LB, UB>? {
        return toKindRangeOrNull(valueRange + rhs.valueRange, lowerKind, upperKind)
    }

    fun plusTypedAcrossKinds(rhs: TypedValueRange<T, *, *>): TypedValueRange<T, *, *>? {
        return toMostStaticKindRange(valueRange + rhs.valueRange)
    }

    fun minusTyped(rhs: T): TypedValueRange<T, LB, UB> {
        return toSameKindRange(valueRange - rhs)
    }

    operator fun minus(rhs: T): TypedValueRange<T, LB, UB> {
        return minusTyped(rhs)
    }

    operator fun minus(rhs: TypedValueRange<T, *, *>): DynamicTypedValueRange<T> {
        return toDynamicRange(valueRange - rhs.valueRange)
    }

    fun minusTyped(rhs: TypedValueRange<T, LB, UB>): TypedValueRange<T, LB, UB>? {
        return toKindRangeOrNull(valueRange - rhs.valueRange, lowerKind, upperKind)
    }

    fun minusTypedAcrossKinds(rhs: TypedValueRange<T, *, *>): TypedValueRange<T, *, *>? {
        return toMostStaticKindRange(valueRange - rhs.valueRange)
    }

    operator fun times(rhs: T): DynamicTypedValueRange<T>? {
        return (valueRange * rhs)?.let { toDynamicRange(it) }
    }

    fun timesPositive(rhs: T): TypedValueRange<T, LB, UB>? {
        if (!isPositive(rhs)) {
            return null
        }
        return (valueRange * rhs)?.let { toKindRangeOrNull(it, lowerKind, upperKind) }
    }

    fun timesNegative(rhs: T): TypedValueRange<T, UB, LB>? {
        if (!isNegative(rhs)) {
            return null
        }
        return (valueRange * rhs)?.let { toKindRangeOrNull(it, upperKind, lowerKind) }
    }

    fun timesTyped(rhs: T): TypedValueRange<T, *, *>? {
        val scaled = valueRange * rhs ?: return null
        return toMostStaticKindRange(scaled)
    }

    operator fun times(rhs: TypedValueRange<T, *, *>): DynamicTypedValueRange<T>? {
        return (valueRange * rhs.valueRange)?.let { toDynamicRange(it) }
    }

    fun timesTypedAcrossKinds(rhs: TypedValueRange<T, *, *>): TypedValueRange<T, *, *>? {
        val scaled = valueRange * rhs.valueRange ?: return null
        return toMostStaticKindRange(scaled)
    }

    operator fun div(rhs: T): DynamicTypedValueRange<T>? {
        return (valueRange / rhs)?.let { toDynamicRange(it) }
    }

    fun divPositive(rhs: T): TypedValueRange<T, LB, UB>? {
        if (!isPositive(rhs)) {
            return null
        }
        return (valueRange / rhs)?.let { toKindRangeOrNull(it, lowerKind, upperKind) }
    }

    fun divNegative(rhs: T): TypedValueRange<T, UB, LB>? {
        if (!isNegative(rhs)) {
            return null
        }
        return (valueRange / rhs)?.let { toKindRangeOrNull(it, upperKind, lowerKind) }
    }

    fun divTyped(rhs: T): TypedValueRange<T, *, *>? {
        val scaled = valueRange / rhs ?: return null
        return toMostStaticKindRange(scaled)
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
