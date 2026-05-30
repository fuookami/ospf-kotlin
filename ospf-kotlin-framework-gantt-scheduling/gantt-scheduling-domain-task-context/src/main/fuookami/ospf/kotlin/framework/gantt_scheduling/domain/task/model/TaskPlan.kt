@file:Suppress("DEPRECATION")

@file:OptIn(kotlin.time.ExperimentalTime::class)

/**
 * 任务计划模型，定义任务的调度属性和约束 / Task plan model defining task scheduling properties and constraints
 */
package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model

import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.TimeRange
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import kotlinx.datetime.Instant
import kotlin.time.Duration

/**
 * 任务状态枚举 / Task status enumeration
 */
enum class TaskStatus {
    /** 不允许提前 / Not allowed to advance */
    NotAdvance,
    /** 不允许延迟 / Not allowed to delay */
    NotDelay,
    /** 不允许取消 / Not allowed to cancel */
    NotCancel,
    /** 不优先取消 / Not preferred to cancel */
    NotCancelPreferred,
    /** 不允许变更执行者 / Not allowed to change executor */
    NotExecutorChange,

    /** 可并行执行（由不同执行者执行）/ Can be done by different executors */
    Parallelable,

    /** 可分割（由不同执行者在连续时间范围执行）/ Can be done by different executors at successive time range */
    Divisible
}

/**
 * 抽象任务计划接口，定义任务计划的基本属性 / Abstract task plan interface defining basic task plan properties
 *
 * @param E 执行者类型 / The executor type
 */
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

/**
 * 单步任务计划 / Single step task plan
 *
 * @param E 执行者类型 / The executor type
 * @property id 任务ID / The task ID
 * @property name 任务名称 / The task name
 * @property enabledExecutors 启用的执行者集合 / The set of enabled executors
 * @property status 任务状态集合 / The set of task statuses
 */
open class SingleStepTaskPlan<out E : Executor>(
    override val id: String,
    override val name: String,
    override val enabledExecutors: Set<E>,
    override val status: Set<TaskStatus>
) : AbstractTaskPlan<E>

/**
 * 抽象任务步骤计划接口 / Abstract task step plan interface
 *
 * @param Self 自身类型 / The self type
 * @param T 多步任务类型 / The multi-step task type
 * @param E 执行者类型 / The executor type
 */
interface AbstractTaskStepPlan<
        out Self : AbstractTaskStepPlan<Self, T, E>,
        out T : AbstractMultiStepTask<T, Self, E>,
        out E : Executor
        > : AbstractTaskPlan<E> {
    val parent: AbstractMultiStepTask<T, Self, E>
    val step: TaskStep<T, Self, E>
}

/**
 * 任务步骤计划 / Task step plan
 *
 * @param T 多步任务类型 / The multi-step task type
 * @param E 执行者类型 / The executor type
 * @property parent 父任务 / The parent task
 * @property step 任务步骤 / The task step
 * @param status 任务状态集合 / The set of task statuses
 */
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

/**
 * 抽象多步任务接口 / Abstract multi-step task interface
 *
 * @param Self 自身类型 / The self type
 * @param S 步骤计划类型 / The step plan type
 * @param E 执行者类型 / The executor type
 */
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

/**
 * 多步任务 / Multi-step task
 *
 * @param E 执行者类型 / The executor type
 * @property id 任务ID / The task ID
 * @property name 任务名称 / The task name
 * @property stepGraph 步骤图 / The step graph
 * @property status 任务状态集合 / The set of task statuses
 */
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


