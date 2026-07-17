@file:OptIn(kotlin.time.ExperimentalTime::class)
package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.capacity_scheduling

import kotlin.test.*
import kotlin.time.Duration.Companion.hours
import kotlin.time.DurationUnit
import kotlin.time.Instant
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.quantities.unit.NoneUnit
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.capacity_scheduling.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.capacity_scheduling.service.limits.*

class CapacityColumnSelectionContractTest {
    private val executor = Executor(SelectionExecutorId("executor"), "executor")
    private val action = SelectionAction(executor)
    private val slots = listOf(
        TimeRange(
            start = Instant.parse("2026-07-17T00:00:00Z"),
            end = Instant.parse("2026-07-17T01:00:00Z")
        ),
        TimeRange(
            start = Instant.parse("2026-07-17T01:00:00Z"),
            end = Instant.parse("2026-07-17T02:00:00Z")
        )
    )

    @Test
    fun capacityColumnsShouldExposeAuthoritativeVariablesAndExactlyOneRows() = runBlocking {
        val compilation = IterativeCapacityCompilation(
            executors = listOf(executor),
            actions = listOf(action),
            slots = slots,
            timeWindow = timeWindow()
        )
        val selection = CapacityColumnSelectionConstraint(
            executors = listOf(executor),
            slots = slots,
            compilation = compilation
        )
        val idle = CapacityColumn<Executor, SelectionAction, Flt64>(
            executor = executor,
            slotIndex = 0,
            order = 0,
            allocations = emptyMap(),
            columnCost = Quantity(Flt64.zero, NoneUnit)
        )
        val active = CapacityColumn<Executor, SelectionAction, Flt64>(
            executor = executor,
            slotIndex = 0,
            order = 1,
            allocations = mapOf(action to UInt64.one),
            columnCost = Quantity(Flt64.one, NoneUnit),
            key = "active-state-a"
        )
        val sameAllocationOtherState = CapacityColumn<Executor, SelectionAction, Flt64>(
            executor = executor,
            slotIndex = 0,
            order = 1,
            allocations = mapOf(action to UInt64.one),
            columnCost = Quantity(Flt64.one, NoneUnit),
            key = "active-state-b"
        )
        val nextIdle = CapacityColumn<Executor, SelectionAction, Flt64>(
            executor = executor,
            slotIndex = 1,
            order = 0,
            allocations = emptyMap(),
            columnCost = Quantity(Flt64.zero, NoneUnit)
        )

        LinearMetaModel<Flt64>("capacity-column-selection", converter = schedulingSolverValueAdapter).use { model ->
            compilation.register(model).assertOk()
            selection.register(model)
            selection(model).assertOk()
            compilation.addColumns(
                iteration = UInt64.zero,
                newColumns = listOf(idle, active, sameAllocationOtherState),
                model = model
            ).valueOrFail()
            compilation.addColumns(
                iteration = UInt64.one,
                newColumns = listOf(nextIdle),
                model = model
            ).valueOrFail()

            assertEquals(4, compilation.variableByColumn.size)
            assertNotNull(compilation.variableByColumn[idle])
            assertNotNull(compilation.variableByColumn[active])
            assertNotNull(compilation.variableByColumn[sameAllocationOtherState])
            assertNotNull(compilation.variableByColumn[nextIdle])
            val expectedVariables: List<Any?> = listOf(
                compilation.variableByColumn[idle],
                compilation.variableByColumn[active],
                compilation.variableByColumn[sameAllocationOtherState]
            )
            val actualVariables: List<Any?> = compilation.executorSlotCompilation
                .getValue(executor to slots[0])
                .asMutable().monomials.map { it.symbol }
            assertEquals(expectedVariables, actualVariables)
            val nextExpectedVariables: List<Any?> = listOf(compilation.variableByColumn[nextIdle])
            val nextActualVariables: List<Any?> = compilation.executorSlotCompilation
                .getValue(executor to slots[1])
                .asMutable().monomials.map { it.symbol }
            assertEquals(nextExpectedVariables, nextActualVariables)
            assertEquals(
                slots.size,
                model.constraintsOfGroup(selection).count {
                    it.args is CapacityColumnSelectionShadowPriceKey<*>
                }
            )

            val firstConstraint = model.constraintsOfGroup(selection).first()
            val duals = selection.extractShadowPrices(
                model = model,
                shadowPrices = MetaDualSolution(
                    constraints = mapOf(firstConstraint to Flt64(2.5)),
                    symbols = emptyMap()
                )
            )
            assertEquals(Flt64(2.5), duals[CapacityColumnSelectionShadowPriceKey(executor, slots[0])])
        }
    }

    private fun timeWindow(): TimeWindow<Flt64> {
        return TimeWindow(
            window = TimeRange(slots.first().start, slots.last().end),
            continues = true,
            durationUnit = DurationUnit.HOURS,
            interval = 1.hours,
            fromDouble = { Flt64(it) },
            toDouble = { it.toDouble() }
        )
    }
}

@JvmInline
private value class SelectionExecutorId(val value: String) : ExecutorId {
    override fun toString(): String = value
}

private class SelectionAction(
    override val executor: Executor
) : ProductionAction {
    override val id = ProductionActionIdImpl("action")
    override val name: String = "action"
    override val discrete: Boolean = true
    override val batchDuration = 1.hours

    override fun <V : RealNumber<V>> unitCapacity(timeWindow: TimeWindow<V>): V {
        return timeWindow.fromDouble(1.0)
    }

    override fun <V : RealNumber<V>> unitCost(time: Instant, fromDouble: (Double) -> V): V {
        return fromDouble(1.0)
    }

    override fun <V : RealNumber<V>> upperBound(slot: TimeSlot, timeWindow: TimeWindow<V>): UInt64 {
        return UInt64.one
    }
}

private fun Try.assertOk() {
    when (this) {
        is Ok -> {}
        is Failed -> fail("Unexpected failure: $error")
        is Fatal -> fail("Unexpected fatal result: $errors")
    }
}

private fun <T> Ret<T>.valueOrFail(): T {
    return when (this) {
        is Ok -> value
        is Failed -> fail("Unexpected failure: $error")
        is Fatal -> fail("Unexpected fatal result: $errors")
    }
}
