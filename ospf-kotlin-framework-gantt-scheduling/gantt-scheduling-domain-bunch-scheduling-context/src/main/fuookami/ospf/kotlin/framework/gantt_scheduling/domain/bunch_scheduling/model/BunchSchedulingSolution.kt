package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.bunch_scheduling.model

import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.*

data class BunchSolution<T : AbstractTask<E, A>, E : Executor, A : AssignmentPolicy<E>>(
    val bunches: List<AbstractTaskBunch<T, E, A>>,
    val canceledTasks: List<T>
)

fun <T : AbstractTask<E, A>, E : Executor, A : AssignmentPolicy<E>> Solution.Companion.invoke(bunchSolution: BunchSolution<T, E, A>): Solution<T, E, A> {
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
