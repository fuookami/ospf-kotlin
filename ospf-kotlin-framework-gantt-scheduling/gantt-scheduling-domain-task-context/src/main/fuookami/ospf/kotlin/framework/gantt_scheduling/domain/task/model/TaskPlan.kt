@file:OptIn(kotlin.time.ExperimentalTime::class)

/**
 * 任务计划模型，定义任务的调度属性和约束 / Task plan model defining task scheduling properties and constraints
*/
package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model

import kotlin.time.*
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.TimeRange

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
    val id: TaskPlanId

    /** Actual task plan ID, defaults to id / 实际任务计划 ID，默认等于 id */
    val actualId: TaskPlanId get() = id
    val name: String

    /** Display name, defaults to name / 显示名称，默认等于 name */
    val displayName: String get() = name

    /** Set of task status constraints / 任务状态约束集合 */
    val status: Set<TaskStatus>

    /** Assigned executor, or null if unassigned / 已分配的执行者，未分配时为 null */
    val executor: E? get() = null

    /** Set of executors allowed for this task / 此任务允许的执行者集合 */
    val enabledExecutors: Set<E>

    /** Scheduled time range, or null if not yet scheduled / 计划时间范围，未调度时为 null */
    val scheduledTime: TimeRange? get() = null

    /** Time range alias, defaults to scheduledTime / 时间范围别名，默认等于 scheduledTime */
    val time: TimeRange? get() = scheduledTime

    /** Earliest allowed end time, or null if unrestricted / 最早允许结束时间，无限制时为 null */
    val earliestEndTime: Instant? get() = null

    /** Latest allowed end time, or null if unrestricted / 最晚允许结束时间，无限制时为 null */
    val lastEndTime: Instant? get() = null

    /** Task duration, derived from time range / 任务持续时间，从时间范围派生 */
    val duration: Duration? get() = time?.duration ?: scheduledTime?.duration

/**
 * duration.
 * duration。
 * @param executor The executor for which to calculate duration / 用于计算持续时间的执行者
 * @return The task duration for the given executor / 给定执行者下的任务持续时间
*/
    fun duration(executor: @UnsafeVariance E): Duration {
        return duration!!
    }

    /** Minimum allowed duration / 最小允许持续时间 */
    val minDuration: Duration get() = duration!!

    /** Maximum allowed duration / 最大允许持续时间 */
    val maxDuration: Duration get() = duration!!

    /** Minimum execution times / 最小执行次数 */
    val minExecutionTimes: UInt64 get() = UInt64.one

    /** Maximum execution times / 最大执行次数 */
    val maxExecutionTimes: UInt64 get() = UInt64.one

    /** Time window constraining start and end times / 约束开始和结束时间的时间窗口 */
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

    /** Earliest allowed start time, derived from constraints / 最早允许开始时间，从约束派生 */
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

/**
 * earliestStartTime.
 * earliestStartTime。
 * @param executor The executor for which to calculate earliest start time / 用于计算最早开始时间的执行者
 * @return The earliest allowed start time for the given executor, or null if unrestricted / 给定执行者的最早允许开始时间，无限制时为null
*/
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

    /** Latest allowed start time, derived from constraints / 最晚允许开始时间，从约束派生 */
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

/**
 * lastStartTime.
 * lastStartTime。
 * @param executor The executor for which to calculate latest start time / 用于计算最晚开始时间的执行者
 * @return The latest allowed start time for the given executor, or null if unrestricted / 给定执行者的最晚允许开始时间，无限制时为null
*/
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

/**
 * earliestNormalStartTime.
 * earliestNormalStartTime。
 * @param executor The executor for which to calculate earliest normal start time / 用于计算最早正常开始时间的执行者
 * @return The earliest normal start time based on scheduled time or time window / 基于计划时间或时间窗口的最早正常开始时间
*/
    fun earliestNormalStartTime(executor: @UnsafeVariance E): Instant {
        return if (scheduledTime != null) {
            scheduledTime!!.start
        } else {
            timeWindow!!.start - duration(executor)
        }
    }

/**
 * connectionTime.
 * connectionTime。
 * @param prevTask The preceding task in the sequence, or null if this is the first / 前序任务，若为首个任务则为null
 * @param succTask The succeeding task in the sequence, or null if this is the last / 后续任务，若为末个任务则为null
 * @return The connection (transition) time between the two tasks, or null if not applicable / 两个任务之间的衔接（过渡）时间，不适用时为null
*/
    fun connectionTime(
        prevTask: AbstractTask<@UnsafeVariance E, *>?,
        succTask: AbstractTask<@UnsafeVariance E, *>?
    ): Duration? {
        return Duration.ZERO
    }

/**
 * connectionTime.
 * connectionTime。
 * @param executor The executor performing the transition / 执行衔接过渡的执行者
 * @param prevTask The preceding task in the sequence, or null if this is the first / 前序任务，若为首个任务则为null
 * @param succTask The succeeding task in the sequence, or null if this is the last / 后续任务，若为末个任务则为null
 * @return The connection (transition) time between the two tasks for the given executor / 给定执行者下两个任务之间的衔接（过渡）时间
*/
    fun connectionTime(
        executor: @UnsafeVariance E,
        prevTask: AbstractTask<@UnsafeVariance E, *>?,
        succTask: AbstractTask<@UnsafeVariance E, *>?
    ): Duration {
        return Duration.ZERO
    }

    /** Whether task cancellation is enabled / 是否启用任务取消 */
    val cancelEnabled: Boolean get() = !status.contains(TaskStatus.NotCancel)

    /** Whether task is not preferred to be cancelled / 是否不优先取消 */
    val notCancelPreferred: Boolean get() = status.contains(TaskStatus.NotCancelPreferred)

    /** Whether advancing the task is enabled / 是否允许提前 */
    val advanceEnabled: Boolean get() = !status.contains(TaskStatus.NotAdvance)

    /** Whether delaying the task is enabled / 是否允许延迟 */
    val delayEnabled: Boolean get() = !status.contains(TaskStatus.NotDelay)

    /** Whether changing the executor is enabled / 是否允许变更执行者 */
    val executorChangeEnabled: Boolean get() = !status.contains(TaskStatus.NotExecutorChange)

    /** Whether the task can be executed in parallel / 是否可并行执行 */
    val parallelable: Boolean get() = !status.contains(TaskStatus.Parallelable)

    /** Whether the task can be divided / 是否可分割 */
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
    override val id: TaskPlanId,
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

    /** Parent multi-step task / 父多步任务 */
    val parent: AbstractMultiStepTask<T, Self, E>

    /** Task step within the parent / 父任务中的任务步骤 */
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
    override val id: TaskStepPlanId get() = CompositeTaskStepPlanId(parent.id, step.id)
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
    val id: TaskId
    val name: String

    /** Step dependency graph / 步骤依赖图 */
    val stepGraph: TaskStepGraph<Self, S, E>

    /** Set of task status constraints / 任务状态约束集合 */
    val status: Set<TaskStatus>

    /** Ordered list of task steps / 有序的任务步骤列表 */
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
    override val id: TaskId,
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


