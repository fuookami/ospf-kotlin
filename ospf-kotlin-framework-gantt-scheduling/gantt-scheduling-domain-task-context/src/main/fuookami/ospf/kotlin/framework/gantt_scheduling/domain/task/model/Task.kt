package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model

import kotlin.time.*
import kotlin.reflect.*
import kotlin.reflect.jvm.*
import kotlinx.datetime.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.concept.*
import fuookami.ospf.kotlin.utils.operator.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.*

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

    infix fun eq(type: TaskType) = this.cls == type.cls
    infix fun neq(type: TaskType) = this.cls != type.cls

    infix fun eq(cls: KClass<*>) = this.cls == cls
    infix fun neq(cls: KClass<*>) = this.cls != cls
}

open class TaskKey(
    val id: String,
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

interface AbstractTask<out E : Executor, out A : AssignmentPolicy<E>> : Indexed,
    Eq<AbstractTask<@UnsafeVariance E, @UnsafeVariance A>> {
    val type: TaskType get() = TaskType(AbstractTask::class)
    val key: TaskKey get() = TaskKey(id, type)

    val assignmentPolicy: A? get() = null

    val id: String
    val actualId: String get() = id
    val name: String
    val displayName: String get() = name

    val status: Set<TaskStatus> get() = emptySet()

    val executor: E? get() = null
    val enabledExecutors: Set<E> get() = emptySet()

    val scheduledTime: TimeRange? get() = null
    val time: TimeRange? get() = null

    val earliestEndTime: Instant? get() = null
    val lastEndTime: Instant? get() = null

    val duration: Duration? get() = time?.duration
    fun duration(executor: @UnsafeVariance E, time: Instant? = null): Duration = duration!!

    val minDuration: Duration? get() = null
    val maxDuration: Duration? get() = null

    val timeWindow: TimeRange? get() = null

    val earliestStartTime: Instant? get() = null
    fun earliestStartTime(executor: @UnsafeVariance E): Instant? = null

    val lastStartTime: Instant? get() = null
    fun lastStartTime(executor: @UnsafeVariance E): Instant? = null

    fun connectionTime(
        prevTask: AbstractTask<@UnsafeVariance E, @UnsafeVariance A>?,
        succTask: AbstractTask<@UnsafeVariance E, @UnsafeVariance A>?
    ): Duration? = null

    fun connectionTime(
        executor: @UnsafeVariance E,
        prevTask: AbstractTask<@UnsafeVariance E, @UnsafeVariance A>?,
        succTask: AbstractTask<@UnsafeVariance E, @UnsafeVariance A>?
    ): Duration {
        return Duration.ZERO
    }

    fun scheduleEnabled(timeWindow: TimeRange): Boolean {
        return false
    }

    val maxDelay: Duration? get() = null
    val maxAdvance: Duration? get() = null

    val cancelEnabled: Boolean get() = false
    val notCancelPreferred: Boolean get() = false
    val delayEnabled: Boolean get() = false
    val advanceEnabled: Boolean get() = false
    val executorChangeEnabled: Boolean get() = false
    val parallelable: Boolean get() = false
    val divisible: Boolean get() = false

    fun scheduleNeeded(timeWindow: TimeRange): Boolean {
        return false
    }

    fun assigningEnabled(policy: @UnsafeVariance A): Boolean {
        return false
    }

    fun assign(policy: @UnsafeVariance A): Ret<AbstractTask<E, A>> {
        return Failed(Err(ErrorCode.ApplicationFailed, "infeasible policy"))
    }

    val advance: Duration get() = advance(time, timeWindow, scheduledTime)
    val actualAdvance: Duration get() = advance(time, timeWindow, scheduledTime)
    val delay: Duration get() = delay(time, timeWindow, scheduledTime)
    val actualDelay: Duration get() = delay(time, timeWindow, scheduledTime)

    val overMaxDelay: Duration
        get() {
            return if (maxDelay == null || actualDelay <= maxDelay!!) {
                Duration.ZERO
            } else {
                actualDelay - maxDelay!!
            }
        }
    val overMaxAdvance: Duration
        get() {
            return if (maxAdvance == null || actualAdvance <= maxAdvance!!) {
                Duration.ZERO
            } else {
                actualAdvance - maxAdvance!!
            }
        }

    val executorChanged: Boolean get() = false

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

    val delayLastEndTime: Duration
        get() {
            return if (lastEndTime != null && time != null && time!!.end > lastEndTime!!) {
                time!!.end - lastEndTime!!
            } else {
                Duration.ZERO
            }
        }

    val advanceEarliestEndTime: Duration
        get() {
            return if (earliestEndTime != null && time != null && time!!.end < earliestEndTime!!) {
                earliestEndTime!! - time!!.end
            } else {
                Duration.ZERO
            }
        }
}

open class AbstractUnplannedTask<out E : Executor, out A : AssignmentPolicy<E>>(
    override val id: String,
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
            return Failed(Err(ErrorCode.ApplicationFailed, "infeasible policy"))
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

open class AbstractPlannedTask<out E : Executor, out A : AssignmentPolicy<E>>(
    open val plan: AbstractTaskPlan<E>,
    override val assignmentPolicy: A?,
) : AbstractTask<E, A>, ManualIndexed() {
    constructor(plan: AbstractTaskPlan<E>) : this(
        plan = plan,
        assignmentPolicy = null
    )

    constructor(origin: AbstractPlannedTask<E, A>, assignmentPolicy: A? = null) : this(
        plan = origin.plan,
        assignmentPolicy = assignmentPolicy
    )

    override val id: String get() = plan.id
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
        return plan.connectionTime(executor, prevTask, succTask)
    }

    // it is disabled to schedule if there is actual time or time
    // it is necessary to be participated in the problem, but it is disallowed to set recovery policy
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
            return Failed(Err(ErrorCode.ApplicationFailed, "infeasible policy"))
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

        rhs as AbstractPlannedTask<E, A>

        if (this.plan != rhs.plan) return false

        return assignmentPolicy eq rhs.assignmentPolicy
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AbstractPlannedTask<*, *>

        return key == other.key
    }

    override fun hashCode(): Int {
        return key.hashCode()
    }

    override fun toString(): String {
        return displayName.ifEmpty { name }
    }
}

typealias Task<E> = AbstractPlannedTask<E, AssignmentPolicy<E>>

interface IterativeAbstractTask<
    out E : Executor,
    out A : AssignmentPolicy<E>
>: AbstractTask<E, A> {
    val iteration: Int64
}
