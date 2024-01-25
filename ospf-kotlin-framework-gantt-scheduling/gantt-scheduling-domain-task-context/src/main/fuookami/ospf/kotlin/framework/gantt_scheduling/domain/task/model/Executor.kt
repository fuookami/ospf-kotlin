package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model

import kotlinx.datetime.*
import fuookami.ospf.kotlin.utils.concept.*

open class Executor(
    val id: String,
    val name: String,
) : ManualIndexed() {
    open val actualId: String by ::id
    open val displayName: String by ::name
}

abstract class ExecutorUsability<E : Executor, A : AssignmentPolicy<E>>(
    val lastTask: AbstractTask<E, A>?,
    val enabledTime: Instant
)
