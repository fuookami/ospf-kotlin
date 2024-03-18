package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_compilation.model

import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.*

data class Solution<
    out T : AbstractTask<E, A>,
    out E : Executor,
    out A : AssignmentPolicy<E>
>(
    val assignedTasks: List<T>,
    val canceledTasks: List<T>
) {
    companion object {}
}
