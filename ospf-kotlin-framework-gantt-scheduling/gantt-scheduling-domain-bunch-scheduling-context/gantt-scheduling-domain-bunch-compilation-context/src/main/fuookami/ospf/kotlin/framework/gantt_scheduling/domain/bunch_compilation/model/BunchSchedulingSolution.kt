package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.bunch_compilation.model

import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_compilation.model.*

data class BunchSolution<
    out B : AbstractTaskBunch<T, E, A>,
    out T : AbstractTask<E, A>,
    out E : Executor,
    out A : AssignmentPolicy<E>
>(
    val bunches: List<B>,
    val canceledTasks: List<T>
)

fun <
    B : AbstractTaskBunch<T, E, A>,
    T : AbstractTask<E, A>,
    E : Executor,
    A : AssignmentPolicy<E>
> Solution.Companion.invoke(
    bunchSolution: BunchSolution<B, T, E, A>
): Solution<T, E, A> {
    val assignedTasks = ArrayList<T>()
    for (bunch in bunchSolution.bunches) {
        for (task in bunch.tasks) {
            when (val policy = task.assignmentPolicy) {
                null -> {}

                else -> {
                    if (!policy.empty) {
                        assignedTasks.add(task)
                    }
                }
            }
        }
    }
    return Solution(assignedTasks, bunchSolution.canceledTasks)
}
