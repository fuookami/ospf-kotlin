@file:OptIn(kotlin.time.ExperimentalTime::class)
package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.capacity_scheduling

import kotlin.test.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.DurationUnit
import kotlin.time.Instant
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.math.symbol.operation.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.quantities.unit.NoneUnit
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.symbol.*
import fuookami.ospf.kotlin.core.variable.*
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
    fun `action indices keep first matches and executor capacities across repeated slots`(): Unit = runBlocking {
        val otherExecutor = Executor(SelectionExecutorId("other"), "other")
        val otherAction = SelectionAction(otherExecutor)
        val allExecutors = listOf(executor, otherExecutor)
        val allActions = listOf(action, action, otherAction)
        val allSlots = listOf(slots[0], slots[0], slots[1])
        val compilation = IterativeCapacityCompilation(
            executors = allExecutors,
            actions = allActions,
            slots = allSlots,
            timeWindow = timeWindow()
        )
        assertTrue(compilation.executorSlotCompilation.getValue(executor to slots[0]).name.endsWith("_0"))
        LinearMetaModel<Flt64>("capacity-index", converter = schedulingSolverValueAdapter).use { model ->
            compilation.register(model).assertOk()
            for (a in allActions.indices) for (s in allSlots.indices) {
                model.addConstraint(LinearPolynomial(compilation.operationTime[a, s]) leq Flt64(100), name = "op_${a}_$s").assertOk()
            }
            for (e in allExecutors.indices) for (s in allSlots.indices) {
                model.addConstraint(LinearPolynomial(compilation.capacity[e, s]) leq Flt64(100), name = "cap_${e}_$s").assertOk()
            }
            val active = ArrayList<CapacityColumn<Executor, SelectionAction, Flt64>>()
            for (iteration in 0..1) {
                val batch = allExecutors.mapIndexed { index, owner ->
                    CapacityColumn(
                        executor = owner,
                        slotIndex = if (iteration == 0) index else 2,
                        order = iteration,
                        allocations = mapOf((if (index == 0) action else otherAction) to UInt64(index + iteration + 1)),
                        columnCost = Quantity(Flt64.one, NoneUnit),
                        key = "index-$iteration-$index"
                    )
                }
                compilation.addColumns(
                    iteration = UInt64(iteration),
                    newColumns = batch,
                    model = model
                ).valueOrFail()
                active.addAll(batch)
                val snapshot = model.state.refresh().valueOrFail()
                for (a in allActions.indices) for (s in allSlots.indices) {
                    val row = snapshot.rows.single { it.sourceName == "op_${a}_$s" }
                    for (column in active) {
                        val amount = if (allActions.indexOf(allActions[a]) == a && column.slotIndex == s) column.amountFor(allActions[a]) else UInt64.zero
                        assertEquals(amount.toFlt64(), row.coefficients[compilation.variableByColumn.getValue(column)] ?: Flt64.zero)
                    }
                }
                for (e in allExecutors.indices) for (s in allSlots.indices) {
                    val row = snapshot.rows.single { it.sourceName == "cap_${e}_$s" }
                    for (column in active) {
                        val amount = if (column.executor == allExecutors[e] && column.slotIndex == s) {
                            allActions.distinct().filter { it.executor == allExecutors[e] }.fold(UInt64.zero) { sum, candidate -> sum + column.amountFor(candidate) }
                        } else UInt64.zero
                        assertEquals(amount.toFlt64(), row.coefficients[compilation.variableByColumn.getValue(column)] ?: Flt64.zero)
                    }
                }
                for (owner in allExecutors) for (slot in allSlots) {
                    val slotIndex = allSlots.indexOf(slot)
                    val expected = compilation.variableByColumn
                        .filter { (column, _) -> column.executor == owner && column.slotIndex == slotIndex }
                        .values
                        .toList()
                    val actual = compilation.executorSlotCompilation
                        .getValue(owner to slot)
                        .asMutable()
                        .monomials
                        .map { it.symbol }
                    assertEquals(expected.size, actual.size)
                    assertEquals(expected, actual)
                }
            }
        }
    }

    @Test
    fun `same iteration growth rebinds all variables and preserves bounds`() = runBlocking {
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
        val first = CapacityColumn(
            executor = executor,
            slotIndex = 0,
            order = 0,
            allocations = mapOf(action to UInt64.one),
            columnCost = Quantity(Flt64.one, NoneUnit),
            key = "same-iteration-first"
        )
        val second = CapacityColumn(
            executor = executor,
            slotIndex = 0,
            order = 1,
            allocations = mapOf(action to UInt64(2)),
            columnCost = Quantity(Flt64(2), NoneUnit),
            key = "same-iteration-second"
        )
        val idle = CapacityColumn<Executor, SelectionAction, Flt64>(
            executor = executor,
            slotIndex = 0,
            order = 2,
            allocations = emptyMap<SelectionAction, UInt64>(),
            columnCost = Quantity(Flt64.zero, NoneUnit),
            key = "same-iteration-idle"
        )

        LinearMetaModel<Flt64>("same-iteration-growth", converter = schedulingSolverValueAdapter).use { model ->
            compilation.register(model).assertOk()
            selection.register(model)
            selection(model).assertOk()
            model.addConstraint(
                LinearPolynomial(compilation.operationTime[0, 0]) leq Flt64(100),
                name = "operation_time"
            ).assertOk()

            assertEquals(
                listOf(first),
                compilation.addColumns(
                    iteration = UInt64.zero,
                    newColumns = listOf(first),
                    model = model
                ).valueOrFail()
            )
            val originalVariable = compilation.variableByColumn.getValue(first)
            assertEquals(Flt64.one, originalVariable.upperBound?.value?.unwrap())
            assertNotNull(model.tokens.find(originalVariable))

            assertEquals(
                listOf(second),
                compilation.addColumns(
                    iteration = UInt64.zero,
                    newColumns = listOf(second),
                    model = model
                ).valueOrFail()
            )
            val reboundVariable = compilation.variableByColumn.getValue(first)
            val secondVariable = compilation.variableByColumn.getValue(second)
            assertNotEquals(originalVariable.key, reboundVariable.key)
            assertNull(model.tokens.find(originalVariable))
            assertNotNull(model.tokens.find(reboundVariable))
            assertEquals(Flt64.one, reboundVariable.upperBound?.value?.unwrap())
            assertEquals(Flt64.zero, secondVariable.upperBound?.value?.unwrap())

            assertEquals(
                listOf(idle),
                compilation.addColumns(
                    iteration = UInt64.zero,
                    newColumns = listOf(idle, idle),
                    model = model
                ).valueOrFail()
            )
            val variableKeysBeforeNoOp = compilation.variableByColumn.values.map { it.key }
            assertTrue(compilation.addColumns(
                iteration = UInt64.zero,
                newColumns = emptyList(),
                model = model
            ).valueOrFail().isEmpty())
            assertTrue(compilation.addColumns(
                iteration = UInt64.zero,
                newColumns = listOf(idle, idle),
                model = model
            ).valueOrFail().isEmpty())
            assertEquals(variableKeysBeforeNoOp, compilation.variableByColumn.values.map { it.key })

            val expectedSelectionVariables = listOf(first, second, idle).map { compilation.variableByColumn.getValue(it) }
            val actualSelectionVariables = compilation.executorSlotCompilation
                .getValue(executor to slots[0])
                .asMutable()
                .monomials
                .map { it.symbol }
            assertEquals(expectedSelectionVariables.size, actualSelectionVariables.size)
            assertEquals(expectedSelectionVariables, actualSelectionVariables)
            assertEquals(
                listOf(first, second).map { compilation.variableByColumn.getValue(it) },
                compilation.cost!!.polynomial.monomials
                    .filter { it.coefficient != Flt64.zero }
                    .map { it.symbol }
            )

            val snapshot = model.state.refresh().valueOrFail()
            val operationRow = snapshot.rows.single { it.sourceName == "operation_time" }
            assertEquals(Flt64.one, operationRow.coefficients[compilation.variableByColumn.getValue(first)])
            assertEquals(Flt64(2), operationRow.coefficients[compilation.variableByColumn.getValue(second)])
            assertEquals(
                Flt64.zero,
                operationRow.coefficients[compilation.variableByColumn.getValue(idle)] ?: Flt64.zero
            )
        }
    }

    @Test
    fun `refresh observes mutable allocation duration and slot values on unchanged executor`() = runBlocking {
        val otherExecutor = Executor(SelectionExecutorId("other"), "other")
        val mutableAction = MutableSelectionAction(otherExecutor)
        val mutableSlot = MutableSelectionSlot(slots[0])
        val mutableAllocations = mutableMapOf<ProductionAction, UInt64>()
        val actions = listOf(action, mutableAction)
        val compilation = IterativeCapacityCompilation(
            executors = listOf(executor, otherExecutor),
            actions = actions,
            slots = listOf(mutableSlot),
            timeWindow = timeWindow()
        )
        val first = CapacityColumn<Executor, ProductionAction, Flt64>(
            executor = executor,
            slotIndex = 0,
            order = 0,
            allocations = mapOf<ProductionAction, UInt64>(action to UInt64.one),
            columnCost = Quantity(Flt64.one, NoneUnit),
            key = "mutable-input-first"
        )
        val other = CapacityColumn<Executor, ProductionAction, Flt64>(
            executor = otherExecutor,
            slotIndex = 0,
            order = 0,
            allocations = mutableAllocations,
            columnCost = Quantity(Flt64.one, NoneUnit),
            key = "mutable-input-other"
        )
        mutableAllocations[mutableAction] = UInt64.one

        LinearMetaModel<Flt64>("mutable-input-refresh", converter = schedulingSolverValueAdapter).use { model ->
            compilation.register(model).assertOk()
            compilation.addColumns(
                iteration = UInt64.zero,
                newColumns = listOf(first, other),
                model = model
            ).valueOrFail()
            val otherVariable = compilation.variableByColumn.getValue(other)
            assertEquals(Flt64.one, otherVariable.upperBound?.value?.unwrap())
            assertEquals(Flt64.one, coefficient(compilation.operationTime[1, 0].polynomial, otherVariable))

            mutableAllocations[mutableAction] = UInt64(2)
            mutableAction.batchDurationValue = 30.minutes
            mutableSlot.timeValue = TimeRange(mutableSlot.start, mutableSlot.end + 1.hours)
            val discreteReads = mutableAction.discreteReads
            val durationReads = mutableAction.batchDurationReads
            val next = CapacityColumn<Executor, ProductionAction, Flt64>(
                executor = executor,
                slotIndex = 0,
                order = 1,
                allocations = mapOf<ProductionAction, UInt64>(action to UInt64(2)),
                columnCost = Quantity(Flt64(2), NoneUnit),
                key = "mutable-input-next"
            )

            compilation.addColumns(
                iteration = UInt64.zero,
                newColumns = listOf(next),
                model = model
            ).valueOrFail()

            assertSame(otherVariable, compilation.variableByColumn.getValue(other))
            assertEquals(Flt64.one, coefficient(compilation.operationTime[1, 0].polynomial, otherVariable))
            assertEquals(discreteReads + 1, mutableAction.discreteReads)
            assertEquals(durationReads + 2, mutableAction.batchDurationReads)
            assertEquals(Flt64.one, compilation.variableByColumn.getValue(next).upperBound?.value?.unwrap())
        }
    }

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

private class MutableSelectionAction(
    override val executor: Executor
) : ProductionAction {
    var discreteValue: Boolean = true
    var batchDurationValue: Duration? = 1.hours
    var discreteReads: Int = 0
        private set
    var batchDurationReads: Int = 0
        private set

    override val id = ProductionActionIdImpl("mutable-action")
    override val name: String = "mutable action"
    override val discrete: Boolean
        get() {
            discreteReads++
            return discreteValue
        }
    override val batchDuration: Duration?
        get() {
            batchDurationReads++
            return batchDurationValue
        }

    override fun <V : RealNumber<V>> unitCapacity(timeWindow: TimeWindow<V>): V = timeWindow.fromDouble(1.0)

    override fun <V : RealNumber<V>> unitCost(time: Instant, fromDouble: (Double) -> V): V = fromDouble(1.0)

    override fun <V : RealNumber<V>> upperBound(slot: TimeSlot, timeWindow: TimeWindow<V>): UInt64 = UInt64.one
}

private class MutableSelectionSlot(
    var timeValue: TimeRange
) : TimeSlot {
    override val time: TimeRange
        get() = timeValue

    override fun subOf(subTime: TimeRange): TimeSlot? = null
}

private fun coefficient(
    polynomial: LinearPolynomial<Flt64>,
    variable: CombinationVariableItem<UInt64, UInteger>
): Flt64 {
    return polynomial.monomials
        .filter { it.symbol == variable }
        .fold(Flt64.zero) { sum, monomial -> sum + monomial.coefficient }
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
