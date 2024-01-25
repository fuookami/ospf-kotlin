package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model

import kotlin.time.*
import kotlin.reflect.*
import kotlin.reflect.jvm.*
import kotlinx.datetime.*
import fuookami.ospf.kotlin.utils.concept.*
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

open class AbstractTask<E : Executor, A : AssignmentPolicy<E>>(
    open val plan: AbstractTaskPlan<E>,
    open val origin: AbstractTask<E, A>?,
    private val assignmentPolicy: A?,
) : ManualIndexed() {
    constructor(plan: AbstractTaskPlan<E>): this(
        plan = plan,
        origin = null,
        assignmentPolicy = null
    )

    constructor(origin: AbstractTask<E, A>, assignmentPolicy: A? = null): this(
        plan = origin.plan,
        origin = origin,
        assignmentPolicy = assignmentPolicy
    )

    open val type: TaskType get() = TaskType(AbstractTask::class)
    open val key: TaskKey get() = TaskKey(plan.id, type)

    open val id: String get() = plan.id
    open val actualId: String get() = plan.actualId
    open val name: String get() = plan.name
    open val displayName: String get() = plan.displayName

    val status: Set<TaskStatus> get() = plan.status

    open val executor: E? by lazy { assignmentPolicy?.executor ?: plan.executor }
    val enabledExecutors: Set<E> get() = plan.enabledExecutors

    open val scheduledTime: TimeRange? get() = plan.scheduledTime
    open val time: TimeRange? by lazy { assignmentPolicy?.time ?: plan.time }

    open val earliestEndTime: Instant? get() = plan.earliestEndTime
    open val lastEndTime: Instant? get() = plan.lastEndTime

    open val duration: Duration? by lazy { assignmentPolicy?.time?.duration ?: plan.duration }
    open fun duration(executor: E): Duration {
        return assignmentPolicy?.time?.duration ?: plan.duration(executor)
    }

    open val minDuration: Duration get() = plan.duration!!
    open val maxDuration: Duration get() = plan.duration!!

    val timeWindow: TimeRange? get() = plan.timeWindow

    open val earliestStartTime: Instant? get() = plan.earliestStartTime
    open fun earliestStartTime(executor: E): Instant? {
        return plan.earliestStartTime(executor)
    }

    open val lastStartTime: Instant? get() = plan.lastStartTime
    open fun lastStartTime(executor: E): Instant? {
        return plan.lastStartTime(executor)
    }

    open fun connectionTime(prevTask: AbstractTask<E, A>?, succTask: AbstractTask<E, A>?): Duration? {
        return plan.connectionTime(prevTask, succTask)
    }

    open fun connectionTime(executor: E, prevTask: AbstractTask<E, A>?, succTask: AbstractTask<E, A>?): Duration {
        return plan.connectionTime(executor, prevTask, succTask)
    }

    // it is disabled to schedule if there is actual time or out time
    // it is necessary to be participated in the problem, but it is disallowed to set recovery policy
    open fun scheduleEnabled(timeWindow: TimeRange): Boolean {
        return (plan.time?.start?.let { timeWindow.contains(it) }
            ?: this.timeWindow?.let { timeWindow.withIntersection(it) }
            ?: true)
                || plan.executor == null
    }

    open val maxDelay: Duration?
        get() = if (!delayEnabled) {
            Duration.ZERO
        } else {
            null
        }
    open val maxAdvance: Duration?
        get() = if (!advanceEnabled) {
            Duration.ZERO
        } else {
            null
        }

    val cancelEnabled get() = plan.cancelEnabled
    val notCancelPreferred get() = plan.notCancelPreferred
    val delayEnabled get() = plan.delayEnabled
    val advanceEnabled get() = plan.advanceEnabled
    val executorChangeEnabled get() = plan.executorChangeEnabled
    val parallelable get() = plan.parallelable
    val divisible get() = plan.divisible

    open fun scheduleNeeded(timeWindow: TimeRange): Boolean {
        return scheduleEnabled(timeWindow)
                && (time?.let { timeWindow.withIntersection(it) } ?: true)
    }

    open fun assigningEnabled(policy: A): Boolean {
        if (time == null && policy.time == null) {
            return false
        }
        if (executor == null && policy.executor == null) {
            return false
        }
        return policy.executor?.let { enabledExecutors.contains(it) } ?: true
    }

    open fun assign(policy: A): AbstractTask<E, A> {
        return AbstractTask(this, policy)
    }

    open val advance: Duration get() = advance(plan.time)
    open val actualAdvance: Duration get() = advance(scheduledTime)
    open val delay: Duration get() = delay(plan.time)
    open val actualDelay: Duration get() = delay(scheduledTime)

    open val overMaxDelay: Duration
        get() {
            return if (maxDelay == null || actualDelay <= maxDelay!!) {
                Duration.ZERO
            } else {
                actualDelay - maxDelay!!
            }
        }

    open val executorChanged: Boolean
        get() {
            return if (!executorChangeEnabled) {
                false
            } else {
                assignmentPolicy?.executor != null
            }
        }

    open val onTime: Boolean
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

    open val delayLastEndTime: Duration
        get() {
            return if (lastEndTime != null && time != null && time!!.end > lastEndTime!!) {
                time!!.end - lastEndTime!!
            } else {
                Duration.ZERO
            }
        }

    open val advanceEarliestEndTime: Duration
        get() {
            return if (earliestEndTime != null && time != null && time!!.end < earliestEndTime!!) {
                earliestEndTime!! - time!!.end
            } else {
                Duration.ZERO
            }
        }

    override fun hashCode(): Int {
        return key.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AbstractTask<*, *>

        return key == other.key
    }

    override fun toString() = displayName.ifEmpty { name }

    private fun advance(targetTime: TimeRange?): Duration {
        return if (targetTime != null && time != null) {
            val advance = targetTime.start - time!!.start
            if (advance.isNegative()) {
                Duration.ZERO
            } else {
                advance
            }
        } else if (timeWindow != null && time != null) {
            val advance = timeWindow!!.start - time!!.start
            if (advance.isNegative()) {
                Duration.ZERO
            } else {
                advance
            }
        } else {
            Duration.ZERO
        }
    }

    private fun delay(targetTime: TimeRange?): Duration {
        return if (targetTime != null && time != null) {
            val delay = time!!.start - targetTime.start
            if (delay.isNegative()) {
                Duration.ZERO
            } else {
                delay
            }
        } else if (timeWindow != null && time != null) {
            val delay = time!!.start - timeWindow!!.end
            if (delay.isNegative()) {
                Duration.ZERO
            } else {
                delay
            }
        } else {
            Duration.ZERO
        }
    }
}

typealias Task<E> = AbstractTask<E, AssignmentPolicy<E>>
