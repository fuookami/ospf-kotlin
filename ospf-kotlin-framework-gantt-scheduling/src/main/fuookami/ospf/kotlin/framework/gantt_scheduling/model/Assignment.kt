package fuookami.ospf.kotlin.framework.gantt_scheduling.model

open class AssignmentPolicy<E : Executor>(
    val executor: E? = null,
    val time: TimeRange? = null,
) {
    open val empty: Boolean = executor == null && time == null
}

data class ExecutorChange<E : Executor>(
    val from: E,
    val to: E
)