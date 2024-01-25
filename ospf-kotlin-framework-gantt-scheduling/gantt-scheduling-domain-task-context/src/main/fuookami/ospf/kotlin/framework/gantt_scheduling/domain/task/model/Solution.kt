package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model

data class Solution<T : AbstractTask<E, A>, E : Executor, A : AssignmentPolicy<E>>(
    val assignedTasks: List<T>,
    val canceledTasks: List<T>
)
