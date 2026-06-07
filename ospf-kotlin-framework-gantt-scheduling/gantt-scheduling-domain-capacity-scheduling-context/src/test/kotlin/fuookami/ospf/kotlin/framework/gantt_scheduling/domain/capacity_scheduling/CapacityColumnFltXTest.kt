@file:OptIn(kotlin.time.ExperimentalTime::class)
package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.capacity_scheduling

import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.datetime.Instant
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.quantities.unit.NoneUnit
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.capacity_scheduling.model.CapacityColumn
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.capacity_scheduling.model.ProductionAction
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.Executor
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.TimeSlot
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.TimeWindow

private val testExecutor = Executor("e1", "Machine-A")

private object TestAction : ProductionAction {
    override val id = "action-1"
    override val name = "Drilling"
    override val executor = testExecutor
    override val discrete = true
    override fun unitCapacity(timeWindow: TimeWindow<Flt64>) =
        throw UnsupportedOperationException("stub")
    override fun unitCost(time: Instant) =
        throw UnsupportedOperationException("stub")
    override fun upperBound(slot: TimeSlot, timeWindow: TimeWindow<Flt64>) =
        throw UnsupportedOperationException("stub")
}

private object TestAction2 : ProductionAction {
    override val id = "action-2"
    override val name = "Milling"
    override val executor = testExecutor
    override val discrete = false
    override fun unitCapacity(timeWindow: TimeWindow<Flt64>) =
        throw UnsupportedOperationException("stub")
    override fun unitCost(time: Instant) =
        throw UnsupportedOperationException("stub")
    override fun upperBound(slot: TimeSlot, timeWindow: TimeWindow<Flt64>) =
        throw UnsupportedOperationException("stub")
}

class CapacityColumnFltXTest {
    @Test
    fun capacityColumnShouldSupportFltXCost() {
        val column = CapacityColumn<Executor, ProductionAction, FltX>(
            executor = testExecutor,
            slotIndex = 0,
            order = 0,
            allocations = mapOf(TestAction to UInt64(5)),
            cost = FltX("125.75")
        )

        assertTrue(column.cost eq FltX("125.75"))
        assertEquals(testExecutor, column.executor)
        assertEquals(0, column.slotIndex)
        assertEquals(0, column.order)
    }

    @Test
    fun capacityColumnAmountForShouldWorkWithFltX() {
        val column = CapacityColumn<Executor, ProductionAction, FltX>(
            executor = testExecutor,
            slotIndex = 1,
            order = 0,
            allocations = mapOf(TestAction to UInt64(3)),
            cost = FltX("50.0")
        )

        assertEquals(UInt64(3), column.amountFor(TestAction))
        assertEquals(UInt64.zero, column.amountFor(TestAction2))
    }

    @Test
    fun capacityColumnTotalAmountShouldWorkWithFltX() {
        val column = CapacityColumn<Executor, ProductionAction, FltX>(
            executor = testExecutor,
            slotIndex = 0,
            order = 0,
            allocations = mapOf(
                TestAction to UInt64(2),
                TestAction2 to UInt64(7)
            ),
            cost = FltX("200.50")
        )

        assertEquals(UInt64(9), column.totalAmount)
    }

    @Test
    fun capacityColumnIsEmptyShouldWorkWithFltX() {
        val emptyColumn = CapacityColumn<Executor, ProductionAction, FltX>(
            executor = testExecutor,
            slotIndex = 0,
            order = 0,
            allocations = emptyMap(),
            cost = FltX("0")
        )
        assertTrue(emptyColumn.isEmpty)

        val nonEmptyColumn = CapacityColumn<Executor, ProductionAction, FltX>(
            executor = testExecutor,
            slotIndex = 0,
            order = 0,
            allocations = mapOf(TestAction to UInt64(1)),
            cost = FltX("10.0")
        )
        assertTrue(!nonEmptyColumn.isEmpty)
    }

    @Test
    fun capacityColumnWithZeroAllocationsIsEmptyWithFltX() {
        val column = CapacityColumn<Executor, ProductionAction, FltX>(
            executor = testExecutor,
            slotIndex = 0,
            order = 0,
            allocations = mapOf(TestAction to UInt64.zero),
            cost = FltX("0")
        )
        assertTrue(column.isEmpty)
    }

    @Test
    fun capacityColumnCostQuantityShouldSupportFltX() {
        val column = CapacityColumn<Executor, ProductionAction, FltX>(
            executor = testExecutor,
            slotIndex = 0,
            order = 0,
            allocations = mapOf(TestAction to UInt64(2)),
            cost = FltX("88.25")
        )

        val cost = column.costQuantity()
        assertEquals(NoneUnit, cost.unit)
        assertTrue(cost.value eq FltX("88.25"))
    }

    @Test
    fun capacityColumnShouldSupportFltXQuantityField() {
        val column = CapacityColumn<Executor, ProductionAction, FltX>(
            executor = testExecutor,
            slotIndex = 0,
            order = 0,
            allocations = mapOf(TestAction to UInt64(2)),
            columnCost = Quantity(FltX("91.25"), NoneUnit)
        )

        assertTrue(column.cost eq FltX("91.25"))
        assertEquals(NoneUnit, column.columnCost.unit)
        assertTrue(column.costQuantity().value eq FltX("91.25"))
    }
}
