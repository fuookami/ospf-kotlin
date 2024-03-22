package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model

data class Solution<
    out E : Executor,
    out A : AssignmentPolicy<E>
>(
    val assignedTasks: List<AbstractTask<E, A>>,
    val canceledTasks: List<AbstractTask<E, A>>
) {
    companion object {}
}
