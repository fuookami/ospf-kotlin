package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model

data class Solution<E : Executor>(
    val assignedTasks: List<Task<E>>,
    val canceledTasks: List<Task<E>>
)
