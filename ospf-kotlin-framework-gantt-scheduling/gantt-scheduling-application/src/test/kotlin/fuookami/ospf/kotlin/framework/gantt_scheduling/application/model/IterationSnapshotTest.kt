package fuookami.ospf.kotlin.framework.gantt_scheduling.application.model

import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.math.algebra.number.Int64
import fuookami.ospf.kotlin.framework.gantt_scheduling.application.model.bunch.Iteration as BunchIteration
import fuookami.ospf.kotlin.framework.gantt_scheduling.application.model.task.Iteration as TaskIteration
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.AbstractTask
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.AssignmentPolicy
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.Executor
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.GenericSolverValueAdapter
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.IterativeAbstractTask
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.TaskId

@JvmInline
private value class TestTaskId(val value: String) : TaskId {
    override fun toString(): String = value
}

class IterationSnapshotTest {
    private val adapter = GenericSolverValueAdapter(FltX)

    @Test
    fun taskIterationSnapshotShouldExposeQuantityFltXObjectives() {
        val iteration = TaskIteration<SnapshotTask, Executor, AssignmentPolicy<Executor>>()

        iteration.refreshLpObj(Flt64(80.0))
        iteration.refreshIpObj(Flt64(100.0))

        val snapshot = iteration.snapshot(adapter)

        assertTrue(snapshot.bestObjective.value eq FltX("100.0"))
        assertTrue(snapshot.bestLpObjective.value eq FltX("80.0"))
        assertTrue(snapshot.lowerBound.value eq FltX("0.0"))
        assertTrue(snapshot.upperBound!!.value eq FltX("100.0"))
        assertNotNull(snapshot.slowLpImprovementStep)
    }

    @Test
    fun bunchIterationSnapshotShouldExposeQuantityFltXObjectives() {
        val iteration = BunchIteration<SnapshotPlainTask, Executor, AssignmentPolicy<Executor>, FltX>()

        iteration.refreshLpObj(Flt64(60.0))
        iteration.refreshIpObj(Flt64(75.0))

        val snapshot = iteration.snapshot(adapter)

        assertTrue(snapshot.bestObjective.value eq FltX("75.0"))
        assertTrue(snapshot.bestLpObjective.value eq FltX("60.0"))
        assertTrue(snapshot.lowerBound.value eq FltX("0.0"))
        assertTrue(snapshot.upperBound!!.value eq FltX("75.0"))
        assertNotNull(snapshot.slowLpImprovementStep)
    }
}

private data class SnapshotTask(
    override val index: Int = 0,
    override val id: TaskId = TestTaskId("task"),
    override val name: String = "task",
    override val iteration: Int64 = Int64.zero
) : IterativeAbstractTask<Executor, AssignmentPolicy<Executor>> {
    override fun partialEq(rhs: AbstractTask<Executor, AssignmentPolicy<Executor>>): Boolean? {
        return this === rhs
    }
}

private data class SnapshotPlainTask(
    override val index: Int = 0,
    override val id: TaskId = TestTaskId("task"),
    override val name: String = "task"
) : AbstractTask<Executor, AssignmentPolicy<Executor>> {
    override fun partialEq(rhs: AbstractTask<Executor, AssignmentPolicy<Executor>>): Boolean? {
        return this === rhs
    }
}
