@file:OptIn(kotlin.time.ExperimentalTime::class)

/**
 * 任务分配策略和执行者变更 / Task assignment policy and executor change
 */
package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model

import fuookami.ospf.kotlin.utils.functional.Eq
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.TimeRange

/**
 * 分配策略，描述任务的执行者和时间分配 / Assignment policy describing executor and time assignment for a task
 *
 * @param E 执行者类型 / The executor type
 * @property executor 分配的执行者 / The assigned executor
 * @property time 分配的时间范围 / The assigned time range
 */
open class AssignmentPolicy<out E : Executor>(
    open val executor: E? = null,
    open val time: TimeRange? = null,
) : Eq<AssignmentPolicy<@UnsafeVariance E>> {
    /** 是否完整分配（同时有执行者和时间）/ Whether the assignment is full (has both executor and time) */
    open val full: Boolean by lazy {
        executor != null && time != null
    }
    /** 是否为空分配（无执行者和时间）/ Whether the assignment is empty (no executor and time) */
    open val empty: Boolean by lazy {
        executor == null && time == null
    }

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

/**
 * 执行者变更记录 / Executor change record
 *
 * @param E 执行者类型 / The executor type
 * @property from 原执行者 / The original executor
 * @property to 新执行者 / The new executor
 */
data class ExecutorChange<out E : Executor>(
    val from: E,
    val to: E
)