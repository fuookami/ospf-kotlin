package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model

import fuookami.ospf.kotlin.utils.operator.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.*

open class AssignmentPolicy<out E : Executor>(
    open val executor: E? = null,
    open val time: TimeRange? = null,
) : Eq<AssignmentPolicy<@UnsafeVariance E>> {
    open val full: Boolean by lazy { executor != null && time != null }
    open val empty: Boolean by lazy { executor == null && time == null }

    override fun partialEq(rhs: AssignmentPolicy<@UnsafeVariance E>): Boolean? {
        if (this === rhs) return true
        if (this::class != rhs::class) return false

        if (executor != rhs.executor) return false
        if (time != rhs.time) return false

        return true
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AssignmentPolicy<*>

        if (executor != other.executor) return false
        if (time != other.time) return false

        return true
    }

    override fun hashCode(): Int {
        var result = executor?.hashCode() ?: 0
        result = 31 * result + (time?.hashCode() ?: 0)
        return result
    }
}

data class ExecutorChange<out E : Executor>(
    val from: E,
    val to: E
)
