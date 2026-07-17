@file:OptIn(kotlin.time.ExperimentalTime::class)
package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.bunch_compilation.model

import kotlin.test.*
import kotlin.time.Instant
import kotlin.time.Duration.Companion.hours
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.core.model.mechanism.LinearMetaModel
import fuookami.ospf.kotlin.framework.model.ShadowPrice
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.bunch_compilation.service.limits.*

class SlotBasedBunchCompilationContractTest {
    private val executor = Executor(SlotExecutorId("executor"), "executor")
    private val slots = listOf(
        TestSlot(
            id = "slot-1",
            time = TimeRange(
                start = Instant.parse("2026-07-16T00:00:00Z"),
                end = Instant.parse("2026-07-16T01:00:00Z")
            )
        ),
        TestSlot(
            id = "slot-2",
            time = TimeRange(
                start = Instant.parse("2026-07-16T01:00:00Z"),
                end = Instant.parse("2026-07-16T02:00:00Z")
            )
        )
    )

    @Test
    fun addColumnsShouldKeepBunchSlotAndVariableMappingsAligned() = runBlocking {
        val tasks = (0 until 4).map { index ->
            TestTask(
                index = index,
                id = SlotTaskId("task-$index"),
                time = slots[index / 2].time
            )
        }
        val bunches = tasks.mapIndexed { index, task ->
            TestBunch(
                executor = executor,
                slot = slots[index / 2],
                slotIndex = index / 2,
                task = task
            )
        }
        val compilation = SlotBasedBunchCompilation<TestBunch, FltX, TestTask, Executor, AssignmentPolicy<Executor>>(
            tasks = tasks,
            executors = listOf(executor),
            slots = slots,
            withExecutorLeisure = false
        )

        LinearMetaModel<Flt64>("slot-column-mapping", converter = schedulingSolverValueAdapter).use { model ->
            compilation.register(model).assertOk()
            val added = compilation.addColumnsBySlot(
                iteration = UInt64.zero,
                newBunches = bunches,
                model = model
            ).valueOrFail()

            assertEquals(listOf(bunches[0], bunches[1]), added[slots[0]])
            assertEquals(listOf(bunches[2], bunches[3]), added[slots[1]])
            assertEquals(compilation.bunchesBySlot.keys, compilation.xBySlot.keys)
            for (slot in slots) {
                val slotBunches = assertNotNull(compilation.bunchesBySlot[slot])
                val slotVariables = assertNotNull(compilation.xBySlot[slot])
                assertEquals(slotBunches.size, slotVariables.size)
                for ((index, bunch) in slotBunches.withIndex()) {
                    assertSame(compilation.variableByBunch[bunch], slotVariables[index])
                    assertSame(compilation.x.last()[bunch], slotVariables[index])
                }

                val expressionVariables = assertNotNull(
                    compilation.executorSlotCompilation[executor to slot]
                ).asMutable().monomials.map { it.symbol }
                assertEquals(slotVariables, expressionVariables)
            }

            compilation.aggregation.removeColumn(bunches[0])
            assertFalse(compilation.variableByBunch.containsKey(bunches[0]))
            assertEquals(listOf(bunches[1]), compilation.bunchesBySlot[slots[0]])
            assertEquals(listOf(compilation.x.last()[bunches[1]]), compilation.xBySlot[slots[0]])
        }
    }

    @Test
    fun executorSlotConstraintShouldRegisterAndExposeTypedShadowPrice() {
        val compilation = SlotBasedBunchCompilation<TestBunch, FltX, TestTask, Executor, AssignmentPolicy<Executor>>(
            tasks = emptyList(),
            executors = listOf(executor),
            slots = slots,
            withExecutorLeisure = false
        )
        val constraint = ExecutorSlotCompilationConstraint<
                SlotBunchGanttSchedulingShadowPriceArguments<Executor, AssignmentPolicy<Executor>>,
                TestBunch,
                FltX,
                TestTask,
                Executor,
                AssignmentPolicy<Executor>
                >(
            executors = listOf(executor),
            slots = slots,
            compilation = compilation
        )

        LinearMetaModel<Flt64>("slot-constraint", converter = schedulingSolverValueAdapter).use { model ->
            compilation.register(model).assertOk()
            constraint.register(model)
            constraint.invoke(model).assertOk()
            assertEquals(
                slots.size,
                model.constraintsOfGroup(constraint).count { it.args is ExecutorSlotCompilationShadowPriceKey<*> }
            )
        }

        val shadowPriceMap = AbstractGanttSchedulingShadowPriceMap<
                SlotBunchGanttSchedulingShadowPriceArguments<Executor, AssignmentPolicy<Executor>>,
                Executor,
                AssignmentPolicy<Executor>
                >()
        val key = ExecutorSlotCompilationShadowPriceKey(executor, slots[0])
        shadowPriceMap.put(ShadowPrice(key, Flt64(3.0)))
        val price = constraint.extractor().invoke(
            shadowPriceMap,
            SlotBunchGanttSchedulingShadowPriceArguments(
                executor = executor,
                slot = slots[0]
            )
        )
        assertEquals(Flt64(3.0), price)
    }
}

@JvmInline
private value class SlotExecutorId(val value: String) : ExecutorId {
    override fun toString(): String = value
}

@JvmInline
private value class SlotTaskId(val value: String) : TaskId {
    override fun toString(): String = value
}

private data class TestSlot(
    val id: String,
    override val time: TimeRange
) : TimeSlot {
    override fun subOf(subTime: TimeRange): TimeSlot? {
        return if (subTime == time) this else null
    }

    override fun toString(): String = id
}

private data class TestTask(
    override val index: Int,
    override val id: TaskId,
    override val name: String = id.toString(),
    override val time: TimeRange
) : AbstractTask<Executor, AssignmentPolicy<Executor>> {
    override val duration = 1.hours

    override fun partialEq(rhs: AbstractTask<Executor, AssignmentPolicy<Executor>>): Boolean? {
        return this === rhs
    }
}

private class TestBunch(
    executor: Executor,
    override val slot: TimeSlot,
    override val slotIndex: Int,
    task: TestTask
) : AbstractTaskBunch<TestTask, Executor, AssignmentPolicy<Executor>, FltX>(
    executor = executor,
    initialUsability = ExecutorInitialUsability(
        lastTask = null,
        enabledTime = slot.start
    ),
    tasks = listOf(task),
    cost = ImmutableCost(emptyList()),
    iteration = Int64.zero
), SlotBasedBunch<TestTask, Executor, AssignmentPolicy<Executor>>

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
