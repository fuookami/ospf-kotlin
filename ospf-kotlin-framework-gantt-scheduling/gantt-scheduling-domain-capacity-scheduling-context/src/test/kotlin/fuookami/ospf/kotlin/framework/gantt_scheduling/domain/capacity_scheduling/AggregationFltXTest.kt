@file:OptIn(kotlin.time.ExperimentalTime::class)
package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.capacity_scheduling

import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.hours
import kotlin.time.DurationUnit
import kotlin.time.Instant
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.quantities.unit.NoneUnit
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.capacity_scheduling.model.ActionAllocation
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.capacity_scheduling.model.ExecutorCapacityResult
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.capacity_scheduling.model.ProductionAction
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.Executor
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.TimeRange
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.TimeSlot
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.TimeWindow

private val fltXTestExecutor = Executor("e-fltx", "FltX-Machine")

private object FltXTestAction : ProductionAction {
    override val id = "fltX-action-1"
    override val name = "FltX-Drilling"
    override val executor = fltXTestExecutor
    override val discrete = false

    override fun unitCapacity(timeWindow: TimeWindow<Flt64>): Flt64 = Flt64(1.0)

    override fun unitCost(time: Instant): Flt64 = Flt64(10.5)

    override fun upperBound(slot: TimeSlot, timeWindow: TimeWindow<Flt64>): UInt64 = UInt64(3)
}

private object FltXTestAction2 : ProductionAction {
    override val id = "fltX-action-2"
    override val name = "FltX-Milling"
    override val executor = fltXTestExecutor
    override val discrete = false

    override fun unitCapacity(timeWindow: TimeWindow<Flt64>): Flt64 = Flt64(2.0)

    override fun unitCost(time: Instant): Flt64 = Flt64(5.0)

    override fun upperBound(slot: TimeSlot, timeWindow: TimeWindow<Flt64>): UInt64 = UInt64(4)
}

private fun fltXTimeWindow(): TimeWindow<FltX> {
    val start = Instant.parse("2026-06-07T00:00:00Z")
    val end = start + 8.hours
    return TimeWindow(
        window = TimeRange(start, end),
        continues = true,
        durationUnit = DurationUnit.HOURS,
        interval = 1.hours,
        fromDouble = { FltX(it) },
        toDouble = { it.toDouble() }
    )
}

private val slot1 = TimeRange(
    Instant.parse("2026-06-07T00:00:00Z"),
    Instant.parse("2026-06-07T01:00:00Z")
)

private val slot2 = TimeRange(
    Instant.parse("2026-06-07T01:00:00Z"),
    Instant.parse("2026-06-07T02:00:00Z")
)

class AggregationFltXTest {
    @Test
    fun capacitySchedulingResultShouldExposeDurationQuantities() {
        val timeWindow = fltXTimeWindow()
        val allocation = ActionAllocation(
            action = FltXTestAction,
            slot = slot1,
            slotIndex = 0,
            amount = UInt64(2),
            duration = 2.hours
        )
        val capacity = ExecutorCapacityResult(
            executor = fltXTestExecutor,
            slot = slot1,
            slotIndex = 0,
            totalDuration = 3.hours
        )

        assertTrue(allocation.durationQuantity(timeWindow).value eq FltX("2.0"))
        assertEquals(NoneUnit, allocation.durationQuantity(timeWindow).unit)
        assertTrue(capacity.totalDurationQuantity(timeWindow).value eq FltX("3.0"))
        assertEquals(NoneUnit, capacity.totalDurationQuantity(timeWindow).unit)
    }

    @Test
    fun aggregationShouldSupportFltXTimeWindow() {
        val tw = fltXTimeWindow()
        val aggregation = CapacitySchedulingAggregation<FltX, ProductionAction>(
            actions = listOf(FltXTestAction, FltXTestAction2),
            slots = listOf(slot1),
            timeWindow = tw
        )

        assertEquals(2, aggregation.actionCount)
        assertEquals(1, aggregation.slotCount)
        assertEquals(1, aggregation.executorCount)
    }

    @Test
    fun totalCapacityWithFltXShouldReturnVType() {
        val tw = fltXTimeWindow()
        val aggregation = CapacitySchedulingAggregation<FltX, ProductionAction>(
            actions = listOf(FltXTestAction),
            slots = listOf(slot1),
            timeWindow = tw
        )

        // 泛型容量和上界应保留 V 类型 / Generic capacity and upper bound should keep the V type.
        val total = aggregation.totalCapacityQuantity()
        assertEquals(NoneUnit, total.unit)
        assertTrue(total.value eq FltX(3.0))
    }

    @Test
    fun totalCapacityWithFltXMultipleActionsAndSlots() {
        val tw = fltXTimeWindow()
        val aggregation = CapacitySchedulingAggregation<FltX, ProductionAction>(
            actions = listOf(FltXTestAction, FltXTestAction2),
            slots = listOf(slot1, slot2),
            timeWindow = tw
        )

        // 多动作多时间槽总容量应按 V 类型累计 / Multi-action and multi-slot capacity should accumulate as V.
        val total = aggregation.totalCapacityQuantity()
        assertTrue(total.value eq FltX(22.0))
    }

    @Test
    fun totalCapacityQuantityShouldSupportFltX() {
        val tw = fltXTimeWindow()
        val aggregation = CapacitySchedulingAggregation<FltX, ProductionAction>(
            actions = listOf(FltXTestAction, FltXTestAction2),
            slots = listOf(slot1),
            timeWindow = tw
        )

        val total = aggregation.totalCapacityQuantity()
        assertEquals(NoneUnit, total.unit)
        assertTrue(total.value eq FltX(11.0))
    }
}

class ProductionActionUnitCostVTest {

    @Test
    fun unitCostVShouldConvertFromSolverValueToFltX() {
        val time = Instant.parse("2026-06-07T00:00:00Z")
        val result = FltXTestAction.unitCostQuantity<FltX>(
            time = time,
            fromDouble = { FltX(it) }
        )
        assertEquals(NoneUnit, result.unit)
        assertTrue(result.value eq FltX(10.5))
    }

    @Test
    fun unitCapacityVShouldWorkWithFltX() {
        val tw = fltXTimeWindow()
        val result = FltXTestAction.unitCapacityQuantity(tw)
        assertEquals(NoneUnit, result.unit)
        assertTrue(result.value eq FltX(1.0))
    }

    @Test
    fun upperBoundVShouldWorkWithFltX() {
        val tw = fltXTimeWindow()
        val result = FltXTestAction.upperBoundV<FltX>(slot1, tw)
        assertEquals(UInt64(3), result)
    }

    @Test
    fun unitCapacityQuantityShouldSupportFltX() {
        val tw = fltXTimeWindow()
        val result = FltXTestAction.unitCapacityQuantity(tw)
        assertEquals(NoneUnit, result.unit)
        assertTrue(result.value eq FltX(1.0))
    }

    @Test
    fun unitCostQuantityShouldSupportFltX() {
        val time = Instant.parse("2026-06-07T00:00:00Z")
        val result = FltXTestAction.unitCostQuantity<FltX>(time, fromDouble = { FltX(it) })
        assertEquals(NoneUnit, result.unit)
        assertTrue(result.value eq FltX(10.5))
    }
}
