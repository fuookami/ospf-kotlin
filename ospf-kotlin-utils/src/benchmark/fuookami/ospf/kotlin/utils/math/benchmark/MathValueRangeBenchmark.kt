package fuookami.ospf.kotlin.utils.math.benchmark

import fuookami.ospf.kotlin.utils.math.algebra.number.Flt64
import fuookami.ospf.kotlin.utils.math.algebra.number.Int64
import fuookami.ospf.kotlin.utils.math.algebra.value_range.Interval
import fuookami.ospf.kotlin.utils.math.algebra.value_range.ValueRange
import fuookami.ospf.kotlin.utils.math.algebra.value_range.Bound
import fuookami.ospf.kotlin.utils.math.algebra.value_range.ValueWrapper
import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.annotations.Scope
import org.openjdk.jmh.annotations.State
import org.openjdk.jmh.annotations.Param
import org.openjdk.jmh.annotations.Setup
import org.openjdk.jmh.annotations.Level

/**
 * ValueRange benchmark for performance baseline.
 * Covers: construction, contains (single value), contains (range), half-infinite ranges, batch operations.
 */
@State(Scope.Thread)
open class MathValueRangeBenchmark {

    // ==================== Construction Benchmarks ====================

    @Benchmark
    fun constructFiniteClosedRangeFlt64(): ValueRange<Flt64> {
        return ValueRange(
            lowerBound = Bound(ValueWrapper(Flt64(0.0), Flt64).value!!, Interval.Closed),
            upperBound = Bound(ValueWrapper(Flt64(100.0), Flt64).value!!, Interval.Closed),
            constants = Flt64
        )
    }

    @Benchmark
    fun constructFiniteOpenRangeFlt64(): ValueRange<Flt64> {
        return ValueRange(
            lowerBound = Bound(ValueWrapper(Flt64(0.0), Flt64).value!!, Interval.Open),
            upperBound = Bound(ValueWrapper(Flt64(100.0), Flt64).value!!, Interval.Open),
            constants = Flt64
        )
    }

    @Benchmark
    fun constructHalfInfiniteLowerFlt64(): ValueRange<Flt64> {
        return ValueRange(
            lowerBound = Bound(ValueWrapper(Flt64(0.0), Flt64).value!!, Interval.Closed),
            upperBound = Bound(ValueWrapper.Infinity(Flt64), Interval.Open),
            constants = Flt64
        )
    }

    @Benchmark
    fun constructHalfInfiniteUpperFlt64(): ValueRange<Flt64> {
        return ValueRange(
            lowerBound = Bound(ValueWrapper.NegativeInfinity(Flt64), Interval.Open),
            upperBound = Bound(ValueWrapper(Flt64(100.0), Flt64).value!!, Interval.Closed),
            constants = Flt64
        )
    }

    @Benchmark
    fun constructInfiniteRangeFlt64(): ValueRange<Flt64> {
        return ValueRange(Flt64)
    }

    @Benchmark
    fun constructFiniteClosedRangeInt64(): ValueRange<Int64> {
        return ValueRange(
            lowerBound = Bound(ValueWrapper(Int64(0L), Int64).value!!, Interval.Closed),
            upperBound = Bound(ValueWrapper(Int64(1000L), Int64).value!!, Interval.Closed),
            constants = Int64
        )
    }

    // ==================== Contains Single Value Benchmarks ====================

    private lateinit var finiteClosedRangeFlt64: ValueRange<Flt64>
    private lateinit var finiteOpenRangeFlt64: ValueRange<Flt64>
    private lateinit var halfInfiniteLowerFlt64: ValueRange<Flt64>
    private lateinit var halfInfiniteUpperFlt64: ValueRange<Flt64>
    private lateinit var infiniteRangeFlt64: ValueRange<Flt64>

    private lateinit var finiteClosedRangeInt64: ValueRange<Int64>

    private val testValueInside = Flt64(50.0)
    private val testValueOutside = Flt64(150.0)
    private val testValueBoundary = Flt64(100.0)

    private val testValueInsideInt64 = Int64(500L)
    private val testValueOutsideInt64 = Int64(1500L)

    @Setup(Level.Trial)
    fun setup() {
        finiteClosedRangeFlt64 = ValueRange(
            lowerBound = Bound(ValueWrapper(Flt64(0.0), Flt64).value!!, Interval.Closed),
            upperBound = Bound(ValueWrapper(Flt64(100.0), Flt64).value!!, Interval.Closed),
            constants = Flt64
        )

        finiteOpenRangeFlt64 = ValueRange(
            lowerBound = Bound(ValueWrapper(Flt64(0.0), Flt64).value!!, Interval.Open),
            upperBound = Bound(ValueWrapper(Flt64(100.0), Flt64).value!!, Interval.Open),
            constants = Flt64
        )

        halfInfiniteLowerFlt64 = ValueRange(
            lowerBound = Bound(ValueWrapper(Flt64(0.0), Flt64).value!!, Interval.Closed),
            upperBound = Bound(ValueWrapper.Infinity(Flt64), Interval.Open),
            constants = Flt64
        )

        halfInfiniteUpperFlt64 = ValueRange(
            lowerBound = Bound(ValueWrapper.NegativeInfinity(Flt64), Interval.Open),
            upperBound = Bound(ValueWrapper(Flt64(100.0), Flt64).value!!, Interval.Closed),
            constants = Flt64
        )

        infiniteRangeFlt64 = ValueRange(Flt64)

        finiteClosedRangeInt64 = ValueRange(
            lowerBound = Bound(ValueWrapper(Int64(0L), Int64).value!!, Interval.Closed),
            upperBound = Bound(ValueWrapper(Int64(1000L), Int64).value!!, Interval.Closed),
            constants = Int64
        )
    }

    @Benchmark
    fun containsValueInsideFiniteClosed(): Boolean {
        return testValueInside in finiteClosedRangeFlt64
    }

    @Benchmark
    fun containsValueOutsideFiniteClosed(): Boolean {
        return testValueOutside in finiteClosedRangeFlt64
    }

    @Benchmark
    fun containsValueBoundaryFiniteClosed(): Boolean {
        return testValueBoundary in finiteClosedRangeFlt64
    }

    @Benchmark
    fun containsValueBoundaryFiniteOpen(): Boolean {
        return testValueBoundary in finiteOpenRangeFlt64
    }

    @Benchmark
    fun containsValueHalfInfiniteLower(): Boolean {
        return testValueInside in halfInfiniteLowerFlt64
    }

    @Benchmark
    fun containsValueHalfInfiniteUpper(): Boolean {
        return testValueInside in halfInfiniteUpperFlt64
    }

    @Benchmark
    fun containsValueInfinite(): Boolean {
        return testValueOutside in infiniteRangeFlt64
    }

    @Benchmark
    fun containsValueInt64Inside(): Boolean {
        return testValueInsideInt64 in finiteClosedRangeInt64
    }

    @Benchmark
    fun containsValueInt64Outside(): Boolean {
        return testValueOutsideInt64 in finiteClosedRangeInt64
    }

    // ==================== Contains Range Benchmarks ====================

    private lateinit var subrangeInside: ValueRange<Flt64>
    private lateinit var subrangeOutside: ValueRange<Flt64>
    private lateinit var subrangeOverlapping: ValueRange<Flt64>

    @Setup(Level.Trial)
    fun setupRanges() {
        subrangeInside = ValueRange(
            lowerBound = Bound(ValueWrapper(Flt64(25.0), Flt64).value!!, Interval.Closed),
            upperBound = Bound(ValueWrapper(Flt64(75.0), Flt64).value!!, Interval.Closed),
            constants = Flt64
        )

        subrangeOutside = ValueRange(
            lowerBound = Bound(ValueWrapper(Flt64(150.0), Flt64).value!!, Interval.Closed),
            upperBound = Bound(ValueWrapper(Flt64(200.0), Flt64).value!!, Interval.Closed),
            constants = Flt64
        )

        subrangeOverlapping = ValueRange(
            lowerBound = Bound(ValueWrapper(Flt64(50.0), Flt64).value!!, Interval.Closed),
            upperBound = Bound(ValueWrapper(Flt64(150.0), Flt64).value!!, Interval.Closed),
            constants = Flt64
        )
    }

    @Benchmark
    fun containsRangeInside(): Boolean {
        return finiteClosedRangeFlt64.contains(subrangeInside)
    }

    @Benchmark
    fun containsRangeOutside(): Boolean {
        return finiteClosedRangeFlt64.contains(subrangeOutside)
    }

    @Benchmark
    fun containsRangeOverlapping(): Boolean {
        return finiteClosedRangeFlt64.contains(subrangeOverlapping)
    }

    // ==================== Batch Contains Benchmarks ====================

    private val batchValues: List<Flt64> = (0..999).map { Flt64(it.toDouble()) }

    @Benchmark
    fun batchContainsFiniteClosed(): Int {
        var count = 0
        for (value in batchValues) {
            if (value in finiteClosedRangeFlt64) {
                count++
            }
        }
        return count
    }

    @Benchmark
    fun batchContainsHalfInfiniteLower(): Int {
        var count = 0
        for (value in batchValues) {
            if (value in halfInfiniteLowerFlt64) {
                count++
            }
        }
        return count
    }

    @Benchmark
    fun batchContainsInfinite(): Int {
        var count = 0
        for (value in batchValues) {
            if (value in infiniteRangeFlt64) {
                count++
            }
        }
        return count
    }

    // ==================== Copy Benchmarks ====================

    @Benchmark
    fun copyFiniteClosedRange(): ValueRange<Flt64> {
        return finiteClosedRangeFlt64.copy()
    }

    @Benchmark
    fun copyHalfInfiniteLower(): ValueRange<Flt64> {
        return halfInfiniteLowerFlt64.copy()
    }

    @Benchmark
    fun copyInfiniteRange(): ValueRange<Flt64> {
        return infiniteRangeFlt64.copy()
    }
}