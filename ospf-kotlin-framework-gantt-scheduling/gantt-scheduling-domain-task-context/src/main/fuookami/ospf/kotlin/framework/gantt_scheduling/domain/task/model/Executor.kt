@file:Suppress("DEPRECATION")

@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model

import fuookami.ospf.kotlin.utils.concept.ManualIndexed
import kotlinx.datetime.Instant

open class ExecutorInitialUsability<
        out T : AbstractTask<E, A>,
        out E : Executor,
        out A : AssignmentPolicy<E>
        >(
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
