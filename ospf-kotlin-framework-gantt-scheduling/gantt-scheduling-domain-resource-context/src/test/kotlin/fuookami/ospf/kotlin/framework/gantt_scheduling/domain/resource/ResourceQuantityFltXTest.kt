@file:OptIn(kotlin.time.ExperimentalTime::class)
package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.resource

import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.DurationUnit
import kotlin.time.Instant
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.math.algebra.value_range.Interval
import fuookami.ospf.kotlin.math.algebra.value_range.ValueRange
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.quantities.unit.NoneUnit
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.resource.model.ResourceCapacity
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.resource.model.StorageResource
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.resource.model.StorageResourceTimeSlot
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.resource.model.ExecutionResource
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.resource.model.ExecutionResourceTimeSlot
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.resource.model.ConnectionResource
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.resource.model.ConnectionResourceTimeSlot
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.resource.model.resourceQuantityZero
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.toSolverValue
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.AbstractTask
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.AssignmentPolicy
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.Executor
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.TimeRange
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.TimeWindow

/**
 * 验证资源模块的数值领域路径支持 FltX。
 * Tests verifying that the resource module's numeric domain paths support FltX.
 */
class ResourceQuantityFltXTest {

    private val timeRange = TimeRange(
        start = Instant.parse("2024-01-01T08:00:00Z"),
        end = Instant.parse("2024-01-01T18:00:00Z")
    )

    // ---- resourceQuantityZero / 资源数量零值 ----

    @Test
    fun resourceQuantityZeroShouldResolveFltXZeroFromUpperBound() {
        val range = ValueRange(FltX("0"), FltX("100"), Interval.Closed, Interval.Closed, FltX).value!!
        val cap = ResourceCapacity<FltX>(
            time = timeRange,
            quantityRangeValue = Quantity(range, NoneUnit)
        )
        val zero = resourceQuantityZero(listOf(cap))!!
        assertTrue(zero eq FltX.zero)
    }

    @Test
    fun resourceQuantityZeroShouldResolveFltXZeroFromLowerBound() {
        // 上界不可用时从下界取得数值类型零值 / Use the lower bound when the upper bound is unavailable.
        val range = ValueRange(FltX("5"), FltX("5"), Interval.Closed, Interval.Closed, FltX).value!!
        val cap = ResourceCapacity<FltX>(
            time = timeRange,
            quantityRangeValue = Quantity(range, NoneUnit)
        )
        val zero = resourceQuantityZero(listOf(cap))!!
        assertTrue(zero.constants.zero eq FltX("0"))
    }

    @Test
    fun resourceQuantityZeroShouldWorkWithMultipleCapacitiesFltX() {
        val range1 = ValueRange(FltX("10"), FltX("50"), Interval.Closed, Interval.Closed, FltX).value!!
        val range2 = ValueRange(FltX("0"), FltX("200"), Interval.Closed, Interval.Closed, FltX).value!!
        val cap1 = ResourceCapacity<FltX>(
            time = timeRange,
            quantityRangeValue = Quantity(range1, NoneUnit)
        )
        val cap2 = ResourceCapacity<FltX>(
            time = timeRange,
            quantityRangeValue = Quantity(range2, NoneUnit)
        )
        val zero = resourceQuantityZero(listOf(cap1, cap2))!!
        assertTrue(zero eq FltX("0"))
    }

    @Test
    fun resourceQuantityZeroShouldWorkWithSolverValue() {
        val range = ValueRange(Flt64(0.0), Flt64(100.0), Interval.Closed, Interval.Closed, Flt64).value!!
        val cap = ResourceCapacity<Flt64>(
            time = timeRange,
            quantityRangeValue = Quantity(range, NoneUnit)
        )
        val zero = resourceQuantityZero(listOf(cap))!!
        assertTrue(zero eq Flt64.zero)
    }

    @Test
    fun resourceCapacityQuantityShouldSupportFltX() {
        val range = ValueRange(
            lb = FltX("0"),
            ub = FltX("100"),
            lbInterval = Interval.Closed,
            ubInterval = Interval.Closed,
            constants = FltX
        ).value!!
        val cap = ResourceCapacity<FltX>(
            time = timeRange,
            quantityRangeValue = Quantity(range, NoneUnit),
            lessQuantityValue = Quantity(FltX("2.5"), NoneUnit),
            overQuantityValue = Quantity(FltX("7.5"), NoneUnit)
        )

        val quantityRange = cap.quantityRange()
        val lessQuantity = cap.lessQuantity()
        val overQuantity = cap.overQuantity()

        assertEquals(NoneUnit, quantityRange.unit)
        assertTrue(quantityRange.value.lowerBound.value eq FltX("0"))
        assertTrue(quantityRange.value.upperBound.value eq FltX("100"))
        assertEquals(NoneUnit, lessQuantity!!.unit)
        assertTrue(lessQuantity.value eq FltX("2.5"))
        assertEquals(NoneUnit, overQuantity!!.unit)
        assertTrue(overQuantity.value eq FltX("7.5"))
    }

    @Test
    fun resourceCapacityShouldSupportFltXQuantityFields() {
        val range = ValueRange(
            lb = FltX("0"),
            ub = FltX("100"),
            lbInterval = Interval.Closed,
            ubInterval = Interval.Closed,
            constants = FltX
        ).value!!
        val cap = ResourceCapacity<FltX>(
            time = timeRange,
            quantityRangeValue = Quantity(range, NoneUnit),
            lessQuantityValue = Quantity(FltX("2.5"), NoneUnit),
            overQuantityValue = Quantity(FltX("7.5"), NoneUnit)
        )

        assertTrue(cap.quantityRangeValue.value.lowerBound.value eq FltX("0"))
        assertTrue(cap.lessQuantityValue!!.value eq FltX("2.5"))
        assertTrue(cap.overQuantityValue!!.value eq FltX("7.5"))
        assertEquals(NoneUnit, cap.quantityRangeValue.unit)
        assertEquals(NoneUnit, cap.lessQuantityValue!!.unit)
        assertEquals(NoneUnit, cap.overQuantityValue!!.unit)
    }

    @Test
    fun resourceInitialQuantityShouldSupportFltXQuantity() {
        val range = ValueRange(
            lb = FltX("0"),
            ub = FltX("50"),
            lbInterval = Interval.Closed,
            ubInterval = Interval.Closed,
            constants = FltX
        ).value!!
        val cap = ResourceCapacity<FltX>(
            time = timeRange,
            quantityRangeValue = Quantity(range, NoneUnit)
        )
        val resource = object : ExecutionResource<ResourceCapacity<FltX>, FltX>(
            id = "quantity-exec",
            name = "Quantity Exec",
            capacities = listOf(cap),
            initialQuantityValue = FltX("12.5")
        ) {
            override fun <E : Executor, A : AssignmentPolicy<E>> usedBy(
                task: AbstractTask<E, A>,
                time: TimeRange
            ): FltX {
                return FltX("3.5")
            }
        }

        val initialQuantity = resource.initialQuantity()
        assertEquals(NoneUnit, initialQuantity.unit)
        assertTrue(initialQuantity.value eq FltX("12.5"))
    }

    // ---- StorageResourceTimeSlot FltX / FltX 存储资源时间槽 ----

    @Test
    fun storageResourceTimeSlotRelationToWithFltX() {
        val range = ValueRange(FltX("0"), FltX("100"), Interval.Closed, Interval.Closed, FltX).value!!
        val cap = ResourceCapacity<FltX>(
            time = timeRange,
            quantityRangeValue = Quantity(range, NoneUnit),
            interval = 2.hours
        )
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
            window = timeRange,
            continues = true,
            durationUnit = DurationUnit.HOURS,
            dateOffset = Duration.ZERO,
            interval = 2.hours,
            fromDouble = { FltX(it.toString()) },
            toDouble = { it.toDouble() }
        )
        val slot = StorageResourceTimeSlot(
            timeWindow = tw,
            origin = timeRange,
            resource = resource,
            resourceCapacity = cap,
            indexInRule = UInt64.zero
        )

        // 空任务返回初始数量的零值 / Null task returns the initial-quantity zero value.
        val relation = slot.relationTo<Executor, AssignmentPolicy<Executor>>(null, null)
        assertTrue(relation eq FltX("0"))
    }

    // ---- ExecutionResourceTimeSlot FltX / FltX 执行资源时间槽 ----

    @Test
    fun executionResourceTimeSlotUsedByWithFltX() {
        val range = ValueRange(FltX("0"), FltX("50"), Interval.Closed, Interval.Closed, FltX).value!!
        val cap = ResourceCapacity<FltX>(
            time = timeRange,
            quantityRangeValue = Quantity(range, NoneUnit),
            interval = 1.hours
        )
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

        // 空任务返回初始数量的零值 / Null task returns the initial-quantity zero value.
        val relation = slot.relationTo<Executor, AssignmentPolicy<Executor>>(null, null)
        assertTrue(relation eq FltX("0"))
    }

    // ---- ConnectionResourceTimeSlot FltX / FltX 连接资源时间槽 ----

    @Test
    fun connectionResourceTimeSlotUsedByWithFltX() {
        val range = ValueRange(FltX("0"), FltX("200"), Interval.Closed, Interval.Closed, FltX).value!!
        val cap = ResourceCapacity<FltX>(
            time = timeRange,
            quantityRangeValue = Quantity(range, NoneUnit),
            interval = 1.hours
        )
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

        // 空任务返回 usedBy(null, null) 的结果 / Null tasks return the usedBy(null, null) result.
        val relation = slot.relationTo<Executor, AssignmentPolicy<Executor>>(null, null)
        assertTrue(relation eq FltX("1.5"))
    }

    // ---- V to solver conversion boundary / V 到 solver 转换边界 ----

    @Test
    fun fltXShouldConvertAtSolverBoundary() {
        val range = ValueRange(FltX("0"), FltX("100"), Interval.Closed, Interval.Closed, FltX).value!!
        val cap = ResourceCapacity<FltX>(
            time = timeRange,
            quantityRangeValue = Quantity(range, NoneUnit),
            lessQuantityValue = Quantity(FltX("5.0"), NoneUnit),
            overQuantityValue = Quantity(FltX("10.0"), NoneUnit)
        )

        val solverUpperBound = cap.quantityRangeValue.value.upperBound.value.unwrap().toSolverValue()
        assertTrue(solverUpperBound eq Flt64(100.0))
        val solverLowerBound = cap.quantityRangeValue.value.lowerBound.value.unwrap().toSolverValue()
        assertTrue(solverLowerBound eq Flt64(0.0))
        val solverLessQuantity = cap.lessQuantityValue!!.value.toSolverValue()
        assertTrue(solverLessQuantity eq Flt64(5.0))
        val solverOverQuantity = cap.overQuantityValue!!.value.toSolverValue()
        assertTrue(solverOverQuantity eq Flt64(10.0))
    }

}
