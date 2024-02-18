package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model

import kotlinx.datetime.*
import fuookami.ospf.kotlin.utils.concept.*

open class ExecutorInitialUsability<T : AbstractTask<E, A>, E : Executor, A : AssignmentPolicy<E>>(
    open val lastTask: T?,
    val enabledTime: Instant
) {
    open val on: Boolean get() = lastTask != null
}

open class Executor(
    val id: String,
    val name: String
) : ManualIndexed() {
    open val actualId: String by ::id
    open val displayName: String by ::name
}
