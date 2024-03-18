package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model

import kotlin.time.*
import kotlinx.datetime.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.*

enum class TaskStatus {
    NotAdvance,
    NotDelay,
    NotCancel,
    NotCancelPreferred,
    NotExecutorChange,

    /** done by different executors */
    Parallelable,

    /** done by different executors at successive time range */
    Divisible
}

interface AbstractTaskPlan<out E : Executor> {
    val id: String
    val actualId: String get() = id
    val name: String
    val displayName: String get() = name

    val status: Set<TaskStatus>

    val executor: E? get() = null
    val enabledExecutors: Set<E>

    val scheduledTime: TimeRange? get() = null
    val time: TimeRange? get() = scheduledTime

    val earliestEndTime: Instant? get() = null
    val lastEndTime: Instant? get() = null

    val duration: Duration? get() = time?.duration ?: scheduledTime?.duration
    fun duration(executor: @UnsafeVariance E): Duration {
        return duration!!
    }

    val minDuration: Duration get() = duration!!
    val maxDuration: Duration get() = duration!!

    val minExecutionTimes: UInt64 get() = UInt64.one
    val maxExecutionTimes: UInt64 get() = UInt64.one

    val timeWindow: TimeRange?
        get() = if (lastEndTime != null && earliestEndTime != null) {
            TimeRange(
                start = earliestEndTime!! - maxDuration,
                end = lastEndTime!!
            )
        } else if (lastEndTime != null) {
            TimeRange(
                end = lastEndTime!!
            )
        } else if (earliestEndTime != null) {
            TimeRange(
                start = earliestEndTime!! - maxDuration
            )
        } else {
            null
        }

    val earliestStartTime: Instant?
        get() = if (time != null && status.contains(TaskStatus.NotAdvance)) {
            time!!.start
        } else if (timeWindow != null) {
            timeWindow!!.start
        } else if (earliestEndTime != null) {
            earliestEndTime!! - maxDuration
        } else {
            null
        }

    fun earliestStartTime(executor: @UnsafeVariance E): Instant? {
        return if (time != null && status.contains(TaskStatus.NotAdvance)) {
            time!!.start
        } else if (timeWindow != null) {
            timeWindow!!.start
        } else if (earliestEndTime != null) {
            earliestEndTime!! - duration(executor)
        } else {
            null
        }
    }

    val lastStartTime: Instant?
        get() = if (time != null && status.contains(TaskStatus.NotDelay)) {
            time!!.start
        } else if (timeWindow != null && duration != null) {
            timeWindow!!.end - duration!!
        } else if (lastEndTime != null) {
            lastEndTime!! - minDuration
        } else {
            null
        }

    fun lastStartTime(executor: @UnsafeVariance E): Instant? {
        return if (time != null && status.contains(TaskStatus.NotDelay)) {
            time!!.start
        } else if (timeWindow != null) {
            timeWindow!!.end - duration(executor)
        } else if (lastEndTime != null) {
            lastEndTime!! - duration(executor)
        } else {
            null
        }
    }

    fun earliestNormalStartTime(executor: @UnsafeVariance E): Instant {
        return if (scheduledTime != null) {
            scheduledTime!!.start
        } else {
            timeWindow!!.start - duration(executor)
        }
    }

    fun connectionTime(
        prevTask: AbstractTask<@UnsafeVariance E, *>?,
        succTask: AbstractTask<@UnsafeVariance E, *>?
    ): Duration? {
        return Duration.ZERO
    }

    fun connectionTime(
        executor: @UnsafeVariance E,
        prevTask: AbstractTask<@UnsafeVariance E, *>?,
        succTask: AbstractTask<@UnsafeVariance E, *>?
    ): Duration {
        return Duration.ZERO
    }

    val cancelEnabled: Boolean get() = !status.contains(TaskStatus.NotCancel)
    val notCancelPreferred: Boolean get() = status.contains(TaskStatus.NotCancelPreferred)
    val advanceEnabled: Boolean get() = !status.contains(TaskStatus.NotAdvance)
    val delayEnabled: Boolean get() = !status.contains(TaskStatus.NotDelay)
    val executorChangeEnabled: Boolean get() = !status.contains(TaskStatus.NotExecutorChange)
    val parallelable: Boolean get() = !status.contains(TaskStatus.Parallelable)
    val divisible: Boolean get() = !status.contains(TaskStatus.Divisible)
}

open class SingleStepTaskPlan<out E : Executor>(
    override val id: String,
    override val name: String,
    override val enabledExecutors: Set<E>,
    override val status: Set<TaskStatus>
) : AbstractTaskPlan<E>

interface AbstractTaskStepPlan<
    out Self : AbstractTaskStepPlan<Self, T, E>,
    out T : AbstractMultiStepTask<T, Self, E>,
    out E : Executor
> : AbstractTaskPlan<E> {
    val parent: AbstractMultiStepTask<T, Self, E>
    val step: TaskStep<T, Self, E>
}

open class TaskStepPlan<
    out T : AbstractMultiStepTask<T, TaskStepPlan<T, E>, E>,
    out E : Executor
>(
    final override val parent: T,
    final override val step: TaskStep<T, TaskStepPlan<T, E>, E>,
    status: Set<TaskStatus> = parent.status
) : AbstractTaskStepPlan<TaskStepPlan<T, E>, T, E> {
    override val id get() = "${parent.id}-${step.id}"
    override val name get() = "${parent.name}-${step.name}"
    override val enabledExecutors by step::enabledExecutors
    override val status by lazy { status + step.status }
}

interface AbstractMultiStepTask<
    out Self : AbstractMultiStepTask<Self, S, E>, 
    out S : AbstractTaskStepPlan<S, Self, E>,
    out E : Executor
> {
    val id: String
    val name: String
    val stepGraph: TaskStepGraph<Self, S, E>
    val status: Set<TaskStatus>

    val steps: List<S>
}

open class MultiStepTask<
    out E : Executor
>(
    override val id: String,
    override val name: String,
    final override val stepGraph: TaskStepGraph<
        MultiStepTask<E>,
        TaskStepPlan<MultiStepTask<E>, E>, E
    >,
    override val status: Set<TaskStatus>,
) : AbstractMultiStepTask<MultiStepTask<E>, TaskStepPlan<MultiStepTask<E>, E>, E> {
    override val steps: List<TaskStepPlan<MultiStepTask<E>, E>> by lazy {
        stepGraph.steps.map {
            TaskStepPlan(
                this,
                it
            )
        }
    }
}
