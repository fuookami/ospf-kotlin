package fuookami.ospf.kotlin.utils.math.benchmark

import fuookami.ospf.kotlin.utils.math.algebra.number.Flt64
import fuookami.ospf.kotlin.utils.math.algebra.value_range.ClosedIntervalKind
import fuookami.ospf.kotlin.utils.math.algebra.value_range.OpenIntervalKind
import fuookami.ospf.kotlin.utils.math.algebra.value_range.RuntimeIntervalKind
import fuookami.ospf.kotlin.utils.math.algebra.value_range.TypedValueRange
import fuookami.ospf.kotlin.utils.math.algebra.value_range.toDynamicTypedValueRange

object TypedValueRangeBenchmarkOps {
    @JvmStatic
    fun typedClosedOpen(
        lower: Double,
        upper: Double
    ): TypedValueRange<Flt64, ClosedIntervalKind, OpenIntervalKind> {
        return TypedValueRange.closedOpen(Flt64(lower), Flt64(upper), Flt64).value!!
    }

    @JvmStatic
    fun dynamicClosedOpen(
        lower: Double,
        upper: Double
    ): TypedValueRange<Flt64, RuntimeIntervalKind, RuntimeIntervalKind> {
        return typedClosedOpen(lower, upper).toDynamic().toDynamicTypedValueRange()
    }

    @JvmStatic
    fun contains(
        range: TypedValueRange<Flt64, *, *>,
        value: Double
    ): Boolean {
        return Flt64(value) in range
    }

    @JvmStatic
    fun shiftTyped(
        range: TypedValueRange<Flt64, ClosedIntervalKind, OpenIntervalKind>,
        delta: Double
    ): TypedValueRange<Flt64, ClosedIntervalKind, OpenIntervalKind> {
        return range + Flt64(delta)
    }

    @JvmStatic
    fun shiftDynamic(
        range: TypedValueRange<Flt64, RuntimeIntervalKind, RuntimeIntervalKind>,
        delta: Double
    ): TypedValueRange<Flt64, RuntimeIntervalKind, RuntimeIntervalKind> {
        return range + Flt64(delta)
    }

    @JvmStatic
    fun intersectTyped(
        lhs: TypedValueRange<Flt64, ClosedIntervalKind, OpenIntervalKind>,
        rhs: TypedValueRange<Flt64, ClosedIntervalKind, OpenIntervalKind>
    ): TypedValueRange<Flt64, ClosedIntervalKind, OpenIntervalKind>? {
        return lhs intersectTyped rhs
    }

    @JvmStatic
    fun intersectDynamic(
        lhs: TypedValueRange<Flt64, RuntimeIntervalKind, RuntimeIntervalKind>,
        rhs: TypedValueRange<Flt64, RuntimeIntervalKind, RuntimeIntervalKind>
    ): TypedValueRange<Flt64, RuntimeIntervalKind, RuntimeIntervalKind>? {
        return lhs intersect rhs
    }

    @JvmStatic
    fun signedScaleTypedPositive(
        range: TypedValueRange<Flt64, ClosedIntervalKind, OpenIntervalKind>,
        factor: Double
    ): TypedValueRange<Flt64, ClosedIntervalKind, OpenIntervalKind>? {
        return range.timesPositive(Flt64(factor))
    }

    @JvmStatic
    fun signedScaleTypedNegative(
        range: TypedValueRange<Flt64, ClosedIntervalKind, OpenIntervalKind>,
        factor: Double
    ): TypedValueRange<Flt64, *, *>? {
        return range.timesNegative(Flt64(factor))
    }

    @JvmStatic
    fun signedScaleDynamicPositive(
        range: TypedValueRange<Flt64, RuntimeIntervalKind, RuntimeIntervalKind>,
        factor: Double
    ): TypedValueRange<Flt64, RuntimeIntervalKind, RuntimeIntervalKind>? {
        return range.timesPositive(Flt64(factor))
    }
}