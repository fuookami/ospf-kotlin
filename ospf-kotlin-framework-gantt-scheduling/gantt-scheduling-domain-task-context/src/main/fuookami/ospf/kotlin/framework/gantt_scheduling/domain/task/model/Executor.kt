package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model

import kotlinx.datetime.*
import fuookami.ospf.kotlin.utils.concept.*

abstract class Executor(
    val id: String,
    val name: String,
) : ManualIndexed() {
    open val actualId: String get() = id
    open val displayName: String get() = name
}

abstract class ExecutorUsability<E : Executor>(
    val lastTask: Task<E>?,
    val enabledTime: Instant
)
