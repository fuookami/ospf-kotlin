@file:OptIn(kotlin.time.ExperimentalTime::class)

/**
 * 任务模型，包含任务类型、任务键和任务接口 / Task model containing task type, task key, and task interface
*/
package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model

import kotlin.reflect.*
import kotlin.reflect.jvm.jvmName
import kotlin.time.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.error.GanttSchedulingSolvingError
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.TimeRange
import fuookami.ospf.kotlin.math.algebra.number.Int64
import fuookami.ospf.kotlin.utils.concept.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*

/**
 * 任务类型，基于KClass / Task type based on KClass
 *
 * @property cls 关联的KClass / The associated KClass
*/
open class TaskType(
    val cls: KClass<*>
) {
    open val type: String by cls::jvmName

    override fun hashCode(): Int {
        return cls.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TaskType

        return cls == other.cls
    }

/**
 * eq.
 * eq。
 * @param type The task type to compare with / 要比较的任务类型
*/

    infix fun eq(type: TaskType) = this.cls == type.cls

/**
 * neq.
 * neq。
 * @param type The task type to compare with / 要比较的任务类型
*/
    infix fun neq(type: TaskType) = this.cls != type.cls

/**
 * eq.
 * eq。
 * @param cls The KClass to compare with / 要比较的KClass类型
*/

    infix fun eq(cls: KClass<*>) = this.cls == cls

/**
 * neq.
 * neq。
 * @param cls The KClass to compare with / 要比较的KClass类型
*/
    infix fun neq(cls: KClass<*>) = this.cls != cls
}

/**
 * 任务键，由ID和类型组成 / Task key composed of ID and type
 *
 * @property id 任务ID / The task ID
 * @property type 任务类型 / The task type
*/
open class TaskKey(
    val id: TaskId,
    val type: TaskType
) {
    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + type.hashCode()
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TaskKey) return false

        if (id != other.id) return false
        if (type != other.type) return false

        return true
    }
}

/**
 * 计算提前时间 / Calculate advance time
 *
 * @param time 实际时间 / The actual time
 * @param timeWindow 时间窗口 / The time window
 * @param targetTime 目标时间 / The target time
 * @return 提前时间 / The advance duration
*/
private fun advance(time: TimeRange?, timeWindow: TimeRange?, targetTime: TimeRange?): Duration {
    return if (targetTime != null && time != null) {
        val advance = targetTime.start - time.start
        if (advance.isNegative()) {
            Duration.ZERO
        } else {
            advance
        }
    } else if (timeWindow != null && time != null) {
        val advance = timeWindow.start - time.start
        if (advance.isNegative()) {
            Duration.ZERO
        } else {
            advance
        }
    } else {
        Duration.ZERO
    }
}

/**
 * 计算延迟时间 / Calculate delay time
 *
 * @param time 实际时间 / The actual time
 * @param timeWindow 时间窗口 / The time window
 * @param targetTime 目标时间 / The target time
 * @return 延迟时间 / The delay duration
*/
private fun delay(time: TimeRange?, timeWindow: TimeRange?, targetTime: TimeRange?): Duration {
    return if (targetTime != null && time != null) {
        val delay = time.start - targetTime.start
        if (delay.isNegative()) {
            Duration.ZERO
        } else {
            delay
        }
    } else if (timeWindow != null && time != null) {
        val delay = time.start - timeWindow.end
        if (delay.isNegative()) {
            Duration.ZERO
        } else {
            delay
        }
    } else {
        Duration.ZERO
    }
}

/**
 * 抽象任务接口，定义任务的基本属性和行为 / Abstract task interface defining basic task properties and behaviors
 *
 * @param E 执行者类型 / The executor type
 * @param A 分配策略类型 / The assignment policy type
*/
interface AbstractTask<out E : Executor, out A : AssignmentPolicy<E>> : Indexed,
    Eq<AbstractTask<@UnsafeVariance E, @UnsafeVariance A>> {

    /** Task type classification / 任务类型分类 */
    val type: TaskType get() = TaskType(AbstractTask::class)

    /** Composite key combining id and type / 组合键，结合 id 和 type */
    val key: TaskKey get() = TaskKey(id, type)

    /** Assignment policy, or null if not applicable / 分配策略，不适用时为 null */
    val assignmentPolicy: A? get() = null

    val id: TaskId

    /** Actual task ID, defaults to id / 实际任务 ID，默认等于 id */
    val actualId: TaskId get() = id
    val name: String

    /** Display name, defaults to name / 显示名称，默认等于 name */
    val displayName: String get() = name

    /** Set of task status constraints / 任务状态约束集合 */
    val status: Set<TaskStatus> get() = emptySet()

    /** Assigned executor, or null if unassigned / 已分配的执行者，未分配时为 null */
    val executor: E? get() = null

    /** Set of executors allowed for this task / 此任务允许的执行者集合 */
    val enabledExecutors: Set<E> get() = emptySet()

    /** Scheduled time range, or null if not yet scheduled / 计划时间范围，未调度时为 null */
    val scheduledTime: TimeRange? get() = null

    /** Time range alias / 时间范围别名 */
    val time: TimeRange? get() = null

    /** Earliest allowed end time, or null if unrestricted / 最早允许结束时间，无限制时为 null */
    val earliestEndTime: Instant? get() = null

    /** Latest allowed end time, or null if unrestricted / 最晚允许结束时间，无限制时为 null */
    val lastEndTime: Instant? get() = null

    /** Task duration, derived from time range / 任务持续时间，从时间范围派生 */
    val duration: Duration? get() = time?.duration

/**
 * duration.
 * duration。
 * @param executor The executor for which to calculate duration / 用于计算持续时间的执行者
 * @param time The reference start time for duration calculation, or null to use default / 用于持续时间计算的参考开始时间，为null时使用默认值
 * @return The task duration for the given executor and time / 给定执行者和时间下的任务持续时间
*/
    fun duration(executor: @UnsafeVariance E, time: Instant? = null): Duration = duration!!

    /** Minimum allowed duration, or null if same as duration / 最小允许持续时间，与 duration 相同时为 null */
    val minDuration: Duration? get() = null

    /** Maximum allowed duration, or null if same as duration / 最大允许持续时间，与 duration 相同时为 null */
    val maxDuration: Duration? get() = null

    /** Time window constraining start and end times / 约束开始和结束时间的时间窗口 */
    val timeWindow: TimeRange? get() = null

    /** Earliest allowed start time, or null if unrestricted / 最早允许开始时间，无限制时为 null */
    val earliestStartTime: Instant? get() = null

/**
 * earliestStartTime.
 * earliestStartTime。
 * @param executor The executor for which to calculate earliest start time / 用于计算最早开始时间的执行者
 * @return The earliest allowed start time for the given executor, or null if unrestricted / 给定执行者的最早允许开始时间，无限制时为null
*/
    fun earliestStartTime(executor: @UnsafeVariance E): Instant? = null

    /** Latest allowed start time, or null if unrestricted / 最晚允许开始时间，无限制时为 null */
    val lastStartTime: Instant? get() = null

/**
 * lastStartTime.
 * lastStartTime。
 * @param executor The executor for which to calculate latest start time / 用于计算最晚开始时间的执行者
 * @return The latest allowed start time for the given executor, or null if unrestricted / 给定执行者的最晚允许开始时间，无限制时为null
*/
    fun lastStartTime(executor: @UnsafeVariance E): Instant? = null

/**
 * connectionTime.
 * connectionTime。
 * @param prevTask The preceding task in the sequence, or null if this is the first / 前序任务，若为首个任务则为null
 * @param succTask The succeeding task in the sequence, or null if this is the last / 后续任务，若为末个任务则为null
 * @return The connection (transition) time between the two tasks, or null if not applicable / 两个任务之间的衔接（过渡）时间，不适用时为null
*/
    fun connectionTime(
        prevTask: AbstractTask<@UnsafeVariance E, @UnsafeVariance A>?,
        succTask: AbstractTask<@UnsafeVariance E, @UnsafeVariance A>?
    ): Duration? = null

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
        prevTask: AbstractTask<@UnsafeVariance E, @UnsafeVariance A>?,
        succTask: AbstractTask<@UnsafeVariance E, @UnsafeVariance A>?
    ): Duration {
        return Duration.ZERO
    }

/**
 * scheduleEnabled.
 * scheduleEnabled。
 * @param timeWindow The time window to check scheduling eligibility against / 用于检查调度资格的时间窗口
 * @return Whether the task can be scheduled within the given time window / 任务是否可以在给定时间窗口内调度
*/
    fun scheduleEnabled(timeWindow: TimeRange): Boolean {
        return false
    }

    /** Maximum allowed delay duration / 最大允许延迟持续时间 */
    val maxDelay: Duration? get() = null

    /** Maximum allowed advance duration / 最大允许提前持续时间 */
    val maxAdvance: Duration? get() = null

    /** Whether task cancellation is enabled / 是否启用任务取消 */
    val cancelEnabled: Boolean get() = false

    /** Whether task is not preferred to be cancelled / 是否不优先取消 */
    val notCancelPreferred: Boolean get() = false

    /** Whether delaying the task is enabled / 是否允许延迟 */
    val delayEnabled: Boolean get() = false

    /** Whether advancing the task is enabled / 是否允许提前 */
    val advanceEnabled: Boolean get() = false

    /** Whether changing the executor is enabled / 是否允许变更执行者 */
    val executorChangeEnabled: Boolean get() = false

    /** Whether the task can be executed in parallel / 是否可并行执行 */
    val parallelable: Boolean get() = false

    /** Whether the task can be divided / 是否可分割 */
    val divisible: Boolean get() = false

/**
 * scheduleNeeded.
 * scheduleNeeded。
 * @param timeWindow The time window to check scheduling necessity against / 用于检查调度必要性的时间窗口
 * @return Whether the task needs to be scheduled within the given time window / 任务是否需要在给定时间窗口内调度
*/
    fun scheduleNeeded(timeWindow: TimeRange): Boolean {
        return false
    }

/**
 * assigningEnabled.
 * assigningEnabled。
 * @param policy The assignment policy to check eligibility for / 用于检查分配资格的分配策略
 * @return Whether the task can be assigned with the given policy / 任务是否可以使用给定策略进行分配
*/
    fun assigningEnabled(policy: @UnsafeVariance A): Boolean {
        return false
    }

/**
 * assign.
 * assign。
 * @param policy The assignment policy to apply / 要应用的分配策略
 * @return The newly assigned task, or failure if the policy is infeasible / 分配后的新任务，策略不可行时返回失败
*/
    fun assign(policy: @UnsafeVariance A): Ret<AbstractTask<E, A>> {
        return Failed(GanttSchedulingSolvingError("infeasible policy"))
    }

    /** Advance duration from scheduled time / 相对计划时间的提前时长 */
    val advance: Duration
        get() = advance(
            time = time,
            timeWindow = timeWindow,
            targetTime = scheduledTime
        )

    /** Actual advance duration / 实际提前时长 */
    val actualAdvance: Duration
        get() = advance(
            time = time,
            timeWindow = timeWindow,
            targetTime = scheduledTime
        )

    /** Delay duration from scheduled time / 相对计划时间的延迟时长 */
    val delay: Duration
        get() = delay(
            time = time,
            timeWindow = timeWindow,
            targetTime = scheduledTime
        )

    /** Actual delay duration / 实际延迟时长 */
    val actualDelay: Duration
        get() = delay(
            time = time,
            timeWindow = timeWindow,
            targetTime = scheduledTime
        )

    /** Duration exceeding maximum delay / 超出最大延迟的时长 */
    val overMaxDelay: Duration
        get() {
            return if (maxDelay == null || actualDelay <= maxDelay!!) {
                Duration.ZERO
            } else {
                actualDelay - maxDelay!!
            }
        }

    /** Duration exceeding maximum advance / 超出最大提前的时长 */
    val overMaxAdvance: Duration
        get() {
            return if (maxAdvance == null || actualAdvance <= maxAdvance!!) {
                Duration.ZERO
            } else {
                actualAdvance - maxAdvance!!
            }
        }

    /** Whether the executor has been changed / 执行者是否已变更 */
    val executorChanged: Boolean get() = false

    /** Whether the task is on time / 任务是否准时 */
    val onTime: Boolean
        get() {
            return if (time != null) {
                if (lastEndTime != null && time!!.end > lastEndTime!!) {
                    false
                } else if (earliestEndTime != null && time!!.end < earliestEndTime!!) {
                    false
                } else {
                    true
                }
            } else {
                true
            }
        }

    /** Delay beyond the latest end time / 超出最晚结束时间的延迟 */
    val delayLastEndTime: Duration
        get() {
            return if (lastEndTime != null && time != null && time!!.end > lastEndTime!!) {
                time!!.end - lastEndTime!!
            } else {
                Duration.ZERO
            }
        }

    /** Advance before the earliest end time / 早于最早结束时间的提前 */
    val advanceEarliestEndTime: Duration
        get() {
            return if (earliestEndTime != null && time != null && time!!.end < earliestEndTime!!) {
                earliestEndTime!! - time!!.end
            } else {
                Duration.ZERO
            }
        }
}

/**
 * 抽象未计划任务，表示尚未分配执行者和时间的任务 / Abstract unplanned task representing a task not yet assigned executor and time
 *
 * @param E 执行者类型 / The executor type
 * @param A 分配策略类型 / The assignment policy type
 * @property id 任务ID / The task ID
 * @property name 任务名称 / The task name
 * @property assignmentPolicy 分配策略 / The assignment policy
*/
open class AbstractUnplannedTask<out E : Executor, out A : AssignmentPolicy<E>>(
    override val id: TaskId,
    override val name: String,
    override val assignmentPolicy: A
) : AbstractTask<E, A>, ManualIndexed() {
    override val executor: E? get() = assignmentPolicy.executor
    override val time: TimeRange? get() = assignmentPolicy.time

    override val cancelEnabled: Boolean = true

    override fun assigningEnabled(policy: @UnsafeVariance A): Boolean {
        return policy.full
    }

    override fun assign(policy: @UnsafeVariance A): Ret<AbstractTask<E, A>> {
        if (!assigningEnabled(policy)) {
            return Failed(GanttSchedulingSolvingError("infeasible policy"))
        }
        return Ok(AbstractUnplannedTask(id, name, policy))
    }

    override fun partialEq(rhs: AbstractTask<@UnsafeVariance E, @UnsafeVariance A>): Boolean? {
        if (this === rhs) return true
        if (this::class != rhs::class) return false

        rhs as AbstractUnplannedTask<E, A>

        return assignmentPolicy eq rhs.assignmentPolicy
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AbstractUnplannedTask<*, *>

        return assignmentPolicy == other.assignmentPolicy
    }

    override fun hashCode(): Int {
        return assignmentPolicy.hashCode()
    }

    override fun toString(): String {
        return displayName.ifEmpty { name }
    }
}

/**
 * 抽象已计划任务，基于任务计划和可选的分配策略 / Abstract planned task based on task plan and optional assignment policy
 *
 * @param P 任务计划类型 / The task plan type
 * @param E 执行者类型 / The executor type
 * @param A 分配策略类型 / The assignment policy type
 * @property plan 任务计划 / The task plan
 * @property assignmentPolicy 分配策略 / The assignment policy
*/
open class AbstractPlannedTask<out P : AbstractTaskPlan<E>, out E : Executor, out A : AssignmentPolicy<E>>(
    open val plan: P,
    override val assignmentPolicy: A?,
) : AbstractTask<E, A>, ManualIndexed() {
    constructor(plan: P) : this(
        plan = plan,
        assignmentPolicy = null
    )

    constructor(origin: AbstractPlannedTask<P, E, A>, assignmentPolicy: A? = null) : this(
        plan = origin.plan,
        assignmentPolicy = assignmentPolicy
    )

    override val id: TaskId get() = plan.id
    override val name: String get() = plan.name

    final override val status: Set<TaskStatus> get() = plan.status

    override val executor: E? by lazy { assignmentPolicy?.executor ?: plan.executor }
    final override val enabledExecutors: Set<E> get() = plan.enabledExecutors

    override val scheduledTime: TimeRange? get() = plan.scheduledTime
    override val time: TimeRange? by lazy { assignmentPolicy?.time ?: plan.time }

    override val earliestEndTime: Instant? get() = plan.earliestEndTime
    override val lastEndTime: Instant? get() = plan.lastEndTime

    override val duration: Duration? by lazy { assignmentPolicy?.time?.duration ?: plan.duration }
    override fun duration(executor: @UnsafeVariance E, time: Instant?): Duration {
        return assignmentPolicy?.time?.duration ?: plan.duration(executor)
    }

    override val minDuration: Duration? get() = plan.minDuration
    override val maxDuration: Duration? get() = plan.maxDuration

    final override val timeWindow: TimeRange? get() = plan.timeWindow

    override val earliestStartTime: Instant? get() = plan.earliestStartTime
    override fun earliestStartTime(executor: @UnsafeVariance E): Instant? {
        return plan.earliestStartTime(executor)
    }

    override val lastStartTime: Instant? get() = plan.lastStartTime
    override fun lastStartTime(executor: @UnsafeVariance E): Instant? {
        return plan.lastStartTime(executor)
    }

    override fun connectionTime(
        prevTask: AbstractTask<@UnsafeVariance E, @UnsafeVariance A>?,
        succTask: AbstractTask<@UnsafeVariance E, @UnsafeVariance A>?
    ): Duration? {
        return plan.connectionTime(prevTask, succTask)
    }

    override fun connectionTime(
        executor: @UnsafeVariance E,
        prevTask: AbstractTask<@UnsafeVariance E, @UnsafeVariance A>?,
        succTask: AbstractTask<@UnsafeVariance E, @UnsafeVariance A>?
    ): Duration {
        return plan.connectionTime(
            executor = executor,
            prevTask = prevTask,
            succTask = succTask
        )
    }

    // it is disabled to schedule if there is actual time or time / 如果已有实际时间或时间，则禁用调度
    // it is necessary to be participated in the problem, but it is disallowed to set recovery policy / 需要参与问题求解，但不允许设置恢复策略
    override fun scheduleEnabled(timeWindow: TimeRange): Boolean {
        return (plan.time?.start?.let { timeWindow.contains(it) }
            ?: this.timeWindow?.let { timeWindow.withIntersection(it) }
            ?: true)
                || plan.executor == null
    }

    override val maxDelay: Duration?
        get() = if (!delayEnabled) {
            Duration.ZERO
        } else {
            null
        }
    override val maxAdvance: Duration?
        get() = if (!advanceEnabled) {
            Duration.ZERO
        } else {
            null
        }

    final override val cancelEnabled get() = plan.cancelEnabled
    final override val notCancelPreferred get() = plan.notCancelPreferred
    final override val delayEnabled get() = plan.delayEnabled
    final override val advanceEnabled get() = plan.advanceEnabled
    final override val executorChangeEnabled get() = plan.executorChangeEnabled
    final override val parallelable get() = plan.parallelable
    final override val divisible get() = plan.divisible

    override fun scheduleNeeded(timeWindow: TimeRange): Boolean {
        return scheduleEnabled(timeWindow)
                && (time?.let { timeWindow.withIntersection(it) } ?: true)
    }

    override fun assigningEnabled(policy: @UnsafeVariance A): Boolean {
        if (time == null && policy.time == null) {
            return false
        }
        if (executor == null && policy.executor == null) {
            return false
        }
        return policy.executor?.let { enabledExecutors.contains(it) } ?: true
    }

    override fun assign(policy: @UnsafeVariance A): Ret<AbstractTask<E, A>> {
        if (!assigningEnabled(policy)) {
            return Failed(GanttSchedulingSolvingError("infeasible policy"))
        }
        return Ok(AbstractPlannedTask(this, policy))
    }

    override val executorChanged: Boolean
        get() {
            return if (!executorChangeEnabled) {
                false
            } else {
                assignmentPolicy?.executor != null
            }
        }
    override fun partialEq(rhs: AbstractTask<@UnsafeVariance E, @UnsafeVariance A>): Boolean? {
        if (this === rhs) return true
        if (this::class != rhs::class) return false

        rhs as AbstractPlannedTask<P, E, A>

        if (this.plan != rhs.plan) return false

        return assignmentPolicy eq rhs.assignmentPolicy
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AbstractPlannedTask<*, *, *>

        return key == other.key
    }

    override fun hashCode(): Int {
        return key.hashCode()
    }

    override fun toString(): String {
        return displayName.ifEmpty { name }
    }
}

/** 任务类型别名 / Task type alias */
typealias Task<P, E> = AbstractPlannedTask<P, E, AssignmentPolicy<E>>

/**
 * 迭代抽象任务接口 / Iterative abstract task interface
 *
 * @param E 执行者类型 / The executor type
 * @param A 分配策略类型 / The assignment policy type
*/
interface IterativeAbstractTask<
        out E : Executor,
        out A : AssignmentPolicy<E>
        > : AbstractTask<E, A> {

    /** Iteration index of this task / 此任务的迭代索引 */
    val iteration: Int64
}


