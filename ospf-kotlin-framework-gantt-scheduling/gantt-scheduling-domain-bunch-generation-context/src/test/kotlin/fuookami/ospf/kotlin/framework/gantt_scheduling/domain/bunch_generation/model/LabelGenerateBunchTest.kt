@file:OptIn(kotlin.time.ExperimentalTime::class)
package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.bunch_generation.model

import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.math.algebra.number.Int64
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.quantities.unit.NoneUnit
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.TimeRange
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.AbstractUnplannedTask
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.AssignmentPolicy
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.CostItem
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.Executor
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.ExecutorId
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.ExecutorInitialUsability
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.ImmutableCost
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.TaskId

private typealias TestTask = AbstractUnplannedTask<Executor, AssignmentPolicy<Executor>>

@JvmInline
private value class TestExecutorId(val value: String) : ExecutorId {
    override fun toString(): String = value
}

@JvmInline
private value class TestTaskId(val value: String) : TaskId {
    override fun toString(): String = value
}

class LabelGenerateBunchTest {
    private val executor = Executor(
        id = TestExecutorId("executor-1"),
        name = "executor-1"
    )
    private val start = Instant.parse("2024-01-01T08:00:00Z")

    @Test
    fun labelShouldGenerateBunchFromTraceTasksWithSolverValue() {
        val first = task(
            id = TestTaskId("task-1"),
            start = start
        )
        val second = task(
            id = TestTaskId("task-2"),
            start = start + 1.hours
        )
        val endLabel = solverEndLabel(first, second)

        val bunch = endLabel.generateBunch(
            iteration = Int64(2),
            executor = executor,
            executorUsability = ExecutorInitialUsability(
                lastTask = null,
                enabledTime = start
            ),
            totalCostCalculator = { actualExecutor: Executor, lastTask: TestTask?, tasks: List<TestTask> ->
                assertEquals(executor, actualExecutor)
                assertEquals(null, lastTask)
                assertEquals(listOf(first, second), tasks)
                solverCost(Flt64(tasks.size.toDouble()))
            }
        )

        assertNotNull(bunch)
        assertEquals(listOf(first, second), bunch.tasks)
        assertEquals(Int64(2), bunch.iteration)
        assertTrue(bunch.cost.costSum!!.value eq Flt64(2.0))
    }

    @Test
    fun genericLabelShouldGenerateBunchFromTraceTasksWithFltX() {
        val first = task(
            id = TestTaskId("task-1"),
            start = start
        )
        val second = task(
            id = TestTaskId("task-2"),
            start = start + 1.hours
        )
        val endLabel = fltXEndLabel(first, second)

        val bunch = endLabel.generateBunch(
            iteration = Int64(3),
            executor = executor,
            executorUsability = ExecutorInitialUsability(
                lastTask = null,
                enabledTime = start
            ),
            totalCostCalculator = { _, _, tasks ->
                assertEquals(listOf(first, second), tasks)
                fltXCost(FltX("10.0"))
            }
        )

        assertNotNull(bunch)
        assertEquals(listOf(first, second), bunch.tasks)
        assertEquals(Int64(3), bunch.iteration)
        assertTrue(bunch.cost.costSum!!.value eq FltX("10.0"))
    }

    private fun task(id: TaskId, start: Instant): TestTask {
        return AbstractUnplannedTask(
            id = id,
            name = id.toString(),
            assignmentPolicy = AssignmentPolicy(
                executor = executor,
                time = TimeRange(
                    start = start,
                    end = start + 1.hours
                )
            )
        )
    }

    private fun solverEndLabel(
        first: TestTask,
        second: TestTask
    ): Label<TestTask, Executor, AssignmentPolicy<Executor>, Flt64> {
        val root = Label<TestTask, Executor, AssignmentPolicy<Executor>, Flt64>(
            cost = solverCost(Flt64.zero),
            shadowPrice = Flt64.zero,
            node = RootNode
        )
        val firstLabel = Label<TestTask, Executor, AssignmentPolicy<Executor>, Flt64>(
            cost = solverCost(Flt64.zero),
            shadowPrice = Flt64.zero,
            prevLabel = root,
            task = first
        )
        val secondLabel = Label<TestTask, Executor, AssignmentPolicy<Executor>, Flt64>(
            cost = solverCost(Flt64.zero),
            shadowPrice = Flt64.zero,
            prevLabel = firstLabel,
            task = second
        )
        return Label(
            cost = solverCost(Flt64.zero),
            shadowPrice = Flt64.zero,
            prevLabel = secondLabel,
            node = EndNode
        )
    }

    private fun fltXEndLabel(
        first: TestTask,
        second: TestTask
    ): Label<TestTask, Executor, AssignmentPolicy<Executor>, FltX> {
        val zero = FltX("0")
        val root = Label<TestTask, Executor, AssignmentPolicy<Executor>, FltX>(
            cost = fltXCost(zero),
            shadowPrice = zero,
            node = RootNode
        )
        val firstLabel = Label<TestTask, Executor, AssignmentPolicy<Executor>, FltX>(
            cost = fltXCost(zero),
            shadowPrice = zero,
            prevLabel = root,
            task = first
        )
        val secondLabel = Label<TestTask, Executor, AssignmentPolicy<Executor>, FltX>(
            cost = fltXCost(zero),
            shadowPrice = zero,
            prevLabel = firstLabel,
            task = second
        )
        return Label(
            cost = fltXCost(zero),
            shadowPrice = zero,
            prevLabel = secondLabel,
            node = EndNode
        )
    }

    private fun solverCost(value: Flt64): ImmutableCost<Flt64> {
        return ImmutableCost(
            items = listOf(
                CostItem(
                    tag = "total",
                    costQuantity = Quantity(value, NoneUnit)
                )
            )
        )
    }

    private fun fltXCost(value: FltX): ImmutableCost<FltX> {
        return ImmutableCost(
            items = listOf(
                CostItem(
                    tag = "total",
                    costQuantity = Quantity(value, NoneUnit)
                )
            )
        )
    }
}
