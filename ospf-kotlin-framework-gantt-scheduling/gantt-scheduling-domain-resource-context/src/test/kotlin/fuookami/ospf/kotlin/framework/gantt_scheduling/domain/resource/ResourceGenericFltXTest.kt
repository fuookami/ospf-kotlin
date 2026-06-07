@file:OptIn(kotlin.time.ExperimentalTime::class)
package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.resource

import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.DurationUnit
import kotlinx.datetime.Instant
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.math.algebra.value_range.Interval
import fuookami.ospf.kotlin.math.algebra.value_range.ValueRange
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.resource.model.ResourceCapacity
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.resource.model.StorageResource
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.resource.model.StorageResourceTimeSlot
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.resource.model.ExecutionResource
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.resource.model.ExecutionResourceTimeSlot
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.resource.model.ConnectionResource
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.resource.model.ConnectionResourceTimeSlot
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.resource.model.resourceQuantityZero
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.AbstractTask
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.AssignmentPolicy
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.Executor
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.TimeRange
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.TimeWindow

/**
 * Tests verifying that the resource module's V-generic domain paths
 * work correctly with FltX (arbitrary-precision decimal).
 */
class ResourceGenericFltXTest {

    private val timeRange = TimeRange(
        start = Instant.parse("2024-01-01T08:00:00Z"),
        end = Instant.parse("2024-01-01T18:00:00Z")
    )

    // ---- resourceQuantityZero ----

    @Test
    fun resourceQuantityZeroShouldResolveFltXZeroFromUpperBound() {
        val range = ValueRange(FltX("0"), FltX("100"), Interval.Closed, Interval.Closed, FltX).value!!
        val cap = ResourceCapacity<FltX>(time = timeRange, quantity = range)
        val zero = resourceQuantityZero(listOf(cap))
        assertTrue(zero eq FltX.zero)
    }

    @Test
    fun resourceQuantityZeroShouldResolveFltXZeroFromLowerBound() {
        // Upper bound is INFINITE (no finite value), so zero comes from lower bound
        val range = ValueRange(FltX("5"), FltX("5"), Interval.Closed, Interval.Closed, FltX).value!!
        val cap = ResourceCapacity<FltX>(time = timeRange, quantity = range)
        val zero = resourceQuantityZero(listOf(cap))
        assertTrue(zero.constants.zero eq FltX("0"))
    }

    @Test
    fun resourceQuantityZeroShouldWorkWithMultipleCapacitiesFltX() {
        val range1 = ValueRange(FltX("10"), FltX("50"), Interval.Closed, Interval.Closed, FltX).value!!
        val range2 = ValueRange(FltX("0"), FltX("200"), Interval.Closed, Interval.Closed, FltX).value!!
        val cap1 = ResourceCapacity<FltX>(time = timeRange, quantity = range1)
        val cap2 = ResourceCapacity<FltX>(time = timeRange, quantity = range2)
        val zero = resourceQuantityZero(listOf(cap1, cap2))
        assertTrue(zero eq FltX("0"))
    }

    @Test
    fun resourceQuantityZeroShouldWorkWithFlt64() {
        val range = ValueRange(Flt64(0.0), Flt64(100.0), Interval.Closed, Interval.Closed, Flt64).value!!
        val cap = ResourceCapacity<Flt64>(time = timeRange, quantity = range)
        val zero = resourceQuantityZero(listOf(cap))
        assertTrue(zero eq Flt64.zero)
    }

    // ---- StorageResourceTimeSlot FltX ----

    @Test
    fun storageResourceTimeSlotRelationToWithFltX() {
        val range = ValueRange(FltX("0"), FltX("100"), Interval.Closed, Interval.Closed, FltX).value!!
        val cap = ResourceCapacity<FltX>(time = timeRange, quantity = range, interval = 2.hours)
        val resource = object : StorageResource<ResourceCapacity<FltX>, FltX>(
            id = "test-storage",
            name = "Test Storage",
            capacities = listOf(cap)
        ) {
            override fun <T : AbstractTask<E, A>, E : Executor, A : AssignmentPolicy<E>> costBy(
                task: T, time: Duration
            ): FltX = FltX("0")

            override fun <T : AbstractTask<E, A>, E : Executor, A : AssignmentPolicy<E>> supplyBy(
                task: T, time: Duration
            ): FltX = FltX("0")
        }

        val tw = TimeWindow<FltX>(
            window = timeRange, continues = true,
            durationUnit = DurationUnit.HOURS, dateOffset = Duration.ZERO,
            interval = 2.hours,
            fromDouble = { FltX(it.toString()) }, toDouble = { it.toDouble() }
        )
        val slot = StorageResourceTimeSlot(
            timeWindow = tw,
            origin = timeRange,
            resource = resource,
            resourceCapacity = cap,
            indexInRule = UInt64.zero
        )

        // relationTo with null task returns initialQuantity.constants.zero
        val relation = slot.relationTo<Executor, AssignmentPolicy<Executor>>(null, null)
        assertTrue(relation eq FltX("0"))
    }

    // ---- ExecutionResourceTimeSlot FltX ----

    @Test
    fun executionResourceTimeSlotUsedByWithFltX() {
        val range = ValueRange(FltX("0"), FltX("50"), Interval.Closed, Interval.Closed, FltX).value!!
        val cap = ResourceCapacity<FltX>(time = timeRange, quantity = range, interval = 1.hours)
        val resource = object : ExecutionResource<ResourceCapacity<FltX>, FltX>(
            id = "test-exec",
            name = "Test Exec",
            capacities = listOf(cap)
        ) {
            override fun <E : Executor, A : AssignmentPolicy<E>> usedBy(
                task: AbstractTask<E, A>, time: TimeRange
            ): FltX = FltX("3.5")
        }

        val slot = ExecutionResourceTimeSlot(
            origin = timeRange,
            resource = resource,
            resourceCapacity = cap,
            indexInRule = UInt64.zero
        )

        // relationTo with null task returns initialQuantity.constants.zero
        val relation = slot.relationTo<Executor, AssignmentPolicy<Executor>>(null, null)
        assertTrue(relation eq FltX("0"))
    }

    // ---- ConnectionResourceTimeSlot FltX ----

    @Test
    fun connectionResourceTimeSlotUsedByWithFltX() {
        val range = ValueRange(FltX("0"), FltX("200"), Interval.Closed, Interval.Closed, FltX).value!!
        val cap = ResourceCapacity<FltX>(time = timeRange, quantity = range, interval = 1.hours)
        val resource = object : ConnectionResource<ResourceCapacity<FltX>, FltX>(
            id = "test-conn",
            name = "Test Conn",
            capacities = listOf(cap)
        ) {
            override fun <T : AbstractTask<E, A>, E : Executor, A : AssignmentPolicy<E>> usedBy(
                prevTask: T?, task: T?, time: TimeRange
            ): FltX = FltX("1.5")
        }

        val slot = ConnectionResourceTimeSlot(
            origin = timeRange,
            resource = resource,
            resourceCapacity = cap,
            indexInRule = UInt64.zero
        )

        // relationTo with null tasks returns usedBy(null, null) = 1.5
        val relation = slot.relationTo<Executor, AssignmentPolicy<Executor>>(null, null)
        assertTrue(relation eq FltX("1.5"))
    }

    // ---- V to Flt64 conversion boundary ----

    @Test
    fun fltXToFlt64ConversionAtSolverBoundary() {
        val range = ValueRange(FltX("0"), FltX("100"), Interval.Closed, Interval.Closed, FltX).value!!
        val cap = ResourceCapacity<FltX>(
            time = timeRange, quantity = range,
            lessQuantity = FltX("5.0"), overQuantity = FltX("10.0")
        )

        // Verify V→Flt64 conversion at boundary
        val ubFlt64 = cap.quantity.upperBound.value.unwrap().toFlt64()
        assertTrue(ubFlt64 eq Flt64(100.0))
        val lbFlt64 = cap.quantity.lowerBound.value.unwrap().toFlt64()
        assertTrue(lbFlt64 eq Flt64(0.0))
        val lessFlt64 = cap.lessQuantity!!.toFlt64()
        assertTrue(lessFlt64 eq Flt64(5.0))
        val overFlt64 = cap.overQuantity!!.toFlt64()
        assertTrue(overFlt64 eq Flt64(10.0))
    }

    // ---- Flt64 compat with existing ResourceCapacity ----

    @Test
    fun resourceCapacityFlt64Compat() {
        val range = ValueRange(Flt64(0.0), Flt64(100.0), Interval.Closed, Interval.Closed, Flt64).value!!
        val cap = ResourceCapacity<Flt64>(
            time = timeRange, quantity = range,
            lessQuantity = Flt64(5.0), overQuantity = Flt64(10.0),
            interval = 2.hours, name = "flt64-compat"
        )

        assertTrue(cap.quantity.lowerBound.value eq Flt64(0.0))
        assertTrue(cap.quantity.upperBound.value eq Flt64(100.0))
        assertTrue(cap.lessQuantity!! eq Flt64(5.0))
        assertTrue(cap.overQuantity!! eq Flt64(10.0))
    }
}
