package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_compilation.model

import kotlin.test.assertTrue
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.AbstractTask
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.AssignmentPolicy
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.Executor

class TaskSolutionSummaryTest {
    @Test
    fun taskSolutionSummaryShouldCountAssignedAndCanceledTasks() {
        val solution = TaskSolution(
            assignedTasks = listOf(
                SummaryTask(index = 0, id = "assigned-1"),
                SummaryTask(index = 1, id = "assigned-2")
            ),
            canceledTasks = listOf(
                SummaryTask(index = 2, id = "canceled-1")
            )
        )

        assertTrue(solution.summary.assignedTaskCount.toULong() == 2UL)
        assertTrue(solution.summary.canceledTaskCount.toULong() == 1UL)
        assertTrue(solution.summary.totalTaskCount.toULong() == 3UL)
    }
}

private data class SummaryTask(
    override val index: Int,
    override val id: String,
    override val name: String = id
) : AbstractTask<Executor, AssignmentPolicy<Executor>> {
    override fun partialEq(rhs: AbstractTask<Executor, AssignmentPolicy<Executor>>): Boolean? {
        return this === rhs
    }
}

