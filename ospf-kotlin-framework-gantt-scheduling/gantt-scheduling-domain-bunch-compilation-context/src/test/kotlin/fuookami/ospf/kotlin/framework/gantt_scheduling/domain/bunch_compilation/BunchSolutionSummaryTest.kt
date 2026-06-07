@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.bunch_compilation

import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlin.time.Duration.Companion.hours
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.math.algebra.number.Int64
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.bunch_compilation.model.BunchSolution
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.AbstractTask
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.AbstractTaskBunch
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.AssignmentPolicy
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.Executor
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.ExecutorInitialUsability
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.ImmutableCost
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.TimeRange

class BunchSolutionSummaryTest {
    private val executor = Executor("executor", "executor")
    private val time = TimeRange(
        start = Instant.parse("2026-06-07T00:00:00Z"),
        end = Instant.parse("2026-06-07T01:00:00Z")
    )

    @Test
    fun bunchSolutionSummaryShouldCountBunchesAssignedTasksAndCanceledTasks() {
        val bunch = AbstractTaskBunch(
            executor = executor,
            initialUsability = ExecutorInitialUsability(
                lastTask = null,
                enabledTime = time.start
            ),
            tasks = listOf(
                SummaryTask(index = 0, id = "assigned-1", time = time),
                SummaryTask(index = 1, id = "assigned-2", time = time)
            ),
            cost = ImmutableCost<FltX>(emptyList()),
            iteration = Int64.zero
        )
        val solution = BunchSolution(
            bunches = listOf(bunch),
            canceledTasks = listOf(
                SummaryTask(index = 2, id = "canceled-1", time = time)
            )
        )

        assertTrue(solution.summary.bunchCount.toULong() == 1UL)
        assertTrue(solution.summary.assignedTaskCount.toULong() == 2UL)
        assertTrue(solution.summary.canceledTaskCount.toULong() == 1UL)
        assertTrue(solution.summary.totalTaskCount.toULong() == 3UL)
    }
}

private data class SummaryTask(
    override val index: Int,
    override val id: String,
    override val name: String = id,
    override val time: TimeRange
) : AbstractTask<Executor, AssignmentPolicy<Executor>> {
    override val duration = 1.hours

    override fun partialEq(rhs: AbstractTask<Executor, AssignmentPolicy<Executor>>): Boolean? {
        return this === rhs
    }
}

