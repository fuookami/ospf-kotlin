package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model

import kotlin.time.*
import kotlin.reflect.*
import kotlinx.datetime.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.concept.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrasturcutre.*

enum class StepRelation {
    And,
    Or
}

abstract class TaskStep<T: Task<E>, E: Executor>(
    val id: String,
    val enabledExecutors: Set<E>,
    val parallelable: Boolean
) {
    abstract fun duration(task: T, executor: E): Duration
}

data class TaskStepVector<T: Task<E>, E: Executor>(
    val from: TaskStep<T, E>,
    val to: TaskStep<T, E>,
    val relation: StepRelation
)

open class TaskStepBunch<T: Task<E>, E: Executor>(
    val id: String,
    val steps: List<TaskStep<T, E>>,
    // must be a DAG
    val stepRelation: Map<TaskStep<T, E>, TaskStepVector<T, E>>
) {
    companion object {
        fun <T: Task<E>, E: Executor> build(ctx: TaskStepBunchBuilder<T, E>.() -> Unit): TaskStepBunch<T, E> {
            val builder = TaskStepBunchBuilder<T, E>()
            return builder()
        }
    }
}

data class TaskStepBunchBuilder<T: Task<E>, E: Executor>(
    val id: String? = null,
    val steps: List<TaskStep<T, E>> = emptyList(),
    val stepRelation: Map<TaskStep<T, E>, TaskStepVector<T, E>> = emptyMap()
) {
    val setSteps: MutableSet<TaskStep<T, E>> = HashSet()

    operator fun invoke(): TaskStepBunch<T, E> {
        return TaskStepBunch(
            id = id!!,
            steps = steps,
            stepRelation = stepRelation
        )
    }

    fun <T: Task<E>, E: Executor> TaskStep<T, E>.succ(
        step: TaskStep<T, E>
    ) {
        TODO("not implement yet")
    }

    fun <T: Task<E>, E: Executor> TaskStep<T, E>.succ(
        relation: StepRelation,
        steps: List<TaskStep<T, E>>
    ) {
        TODO("not implement yet")
    }
}

enum class TaskStatus {
    NotAdvance,
    NotDelay,
    NotCancel,
    NotCancelPreferred,
    NotExecutorChange
}

abstract class TaskType(
    val cls: KClass<*>
) {
    abstract val type: String

    override fun hashCode(): Int {
        return cls.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TaskType

        if (cls != other.cls) return false

        return true
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

open class TaskPlan<E : Executor>(
    val id: String,
    val name: String,
    val enabledExecutors: Set<E>,
    val status: Set<TaskStatus>,
    val parallelable: Boolean
) {
    open val actualId: String get() = id
    open val displayName: String get() = name

    open val executor: E? = null

    open val timeWindow: TimeRange? get() = expirationTime?.let { TimeRange(end = it) }
    open val scheduledTime: TimeRange? = null
    open val time: TimeRange? get() = scheduledTime
    open val expirationTime: Instant? = null
    open val latestStartTime: Instant?
        get() = if (time != null && status.contains(TaskStatus.NotDelay)) {
            time!!.start
        } else if (timeWindow != null && duration != null) {
            timeWindow!!.end - duration!!
        } else if (expirationTime != null && duration != null) {
            expirationTime!! - duration!!
        } else {
            null
        }

    val latestBeginTime: Instant?
        get() = if (time != null && status.contains(TaskStatus.NotDelay)) {
            time!!.start
        } else if (timeWindow != null && duration != null) {
            timeWindow!!.end - duration!!
        } else if (expirationTime != null && duration != null) {
            expirationTime!! - duration!!
        } else {
            null
        }

    open fun latestBeginTime(executor: E): Instant? {
        return if (time != null && status.contains(TaskStatus.NotDelay)) {
            time!!.start
        } else if (timeWindow != null && duration != null) {
            timeWindow!!.end - duration!!
        } else if (expirationTime != null) {
            expirationTime!! - duration(executor)
        } else {
            null
        }
    }

    open val duration: Duration? get() = time?.duration ?: scheduledTime?.duration
    open fun duration(executor: E): Duration {
        return duration!!
    }

    open fun connectionTime(prevTask: Task<E>?, succTask: Task<E>?): Duration? {
        return Duration.ZERO
    }

    open fun connectionTime(executor: E, prevTask: Task<E>?, succTask: Task<E>?): Duration {
        return Duration.ZERO
    }

    val cancelEnabled: Boolean get() = !status.contains(TaskStatus.NotCancel)
    val notCancelPreferred: Boolean get() = status.contains(TaskStatus.NotCancelPreferred)
    val advanceEnabled: Boolean get() = !status.contains(TaskStatus.NotAdvance)
    val delayEnabled: Boolean get() = !status.contains(TaskStatus.NotDelay)
    val executorChangeEnabled: Boolean get() = !status.contains(TaskStatus.NotExecutorChange)
}

abstract class Task<E : Executor>(
    private val origin: Task<E>? = null
) : ManualIndexed() {
    abstract val type: TaskType
    abstract val key: TaskKey
    abstract val plan: TaskPlan<E>

    open val id: String get() = plan.id
    open val actualId: String get() = plan.actualId
    open val name: String get() = plan.name
    open val displayName: String get() = plan.displayName

    open val executor: E? get() = plan.executor
    open val enabledExecutors: Set<E> get() = plan.enabledExecutors

    open val timeWindow: TimeRange? get() = plan.timeWindow
    open val scheduledTime: TimeRange? get() = plan.scheduledTime
    open val time: TimeRange? get() = plan.time

    open val expirationTime: Instant? get() = plan.expirationTime
    open val latestBeginTime: Instant? get() = plan.latestBeginTime
    open fun latestBeginTime(executor: E): Instant? {
        return plan.latestBeginTime(executor)
    }

    open val duration: Duration? get() = plan.duration
    open fun duration(executor: E): Duration {
        return plan.duration(executor)
    }

    open fun connectionTime(prevTask: Task<E>?, succTask: Task<E>?): Duration? = plan.connectionTime(prevTask, succTask)
    open fun connectionTime(executor: E, prevTask: Task<E>?, succTask: Task<E>?): Duration = plan.connectionTime(executor, prevTask, succTask)

    open fun latestNormalStartTime(executor: E): Instant {
        return if (scheduledTime != null) {
            scheduledTime!!.start
        } else {
            timeWindow!!.end - duration(executor)
        }
    }

    open fun scheduleEnabled(timeWindow: TimeRange): Boolean {
        return plan.executor == null || plan.time == null
    }

    // it is disabled to recovery if there is actual time or out time
    // it is necessary to be participated in the problem, but it is unallowed to set recovery policy
    open fun recoveryEnabled(timeWindow: TimeRange): Boolean {
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
    val cancelEnabled get() = plan.cancelEnabled
    val notCancelPreferred get() = plan.notCancelPreferred
    val delayEnabled get() = plan.delayEnabled
    val advanceEnabled get() = plan.advanceEnabled
    open val executorChangeEnabled get() = plan.executorChangeEnabled

    abstract val assigned: Boolean
    abstract val assignmentPolicy: AssignmentPolicy<E>

    open fun scheduleNeeded(timeWindow: TimeRange): Boolean {
        return scheduleEnabled(timeWindow)
    }

    open fun recoveryNeeded(timeWindow: TimeRange): Boolean {
        return recoveryEnabled(timeWindow)
                && time == null || timeWindow.withIntersection(time!!)
    }

    open fun assigningEnabled(policy: AssignmentPolicy<E>): Boolean {
        return true
    }

    open val originTask: Task<E> get() = origin ?: this
    abstract fun assign(policy: AssignmentPolicy<E>): Task<E>?

    open val advance: Duration get() = advance(plan.time)
    open val actualAdvance: Duration get() = advance(scheduledTime)
    open val delay: Duration get() = delay(plan.time)
    open val actualDelay: Duration get() = delay(scheduledTime)
    open val overMaxDelay: Duration
        get() = if (maxDelay == null || actualDelay <= maxDelay!!) {
            Duration.ZERO
        } else {
            actualDelay - maxDelay!!
        }
    open val executorChanged: Boolean
        get() = if (!executorChangeEnabled) {
            false
        } else {
            assignmentPolicy.executor != null
        }
    open val onTime: Boolean
        get() = if (expirationTime != null && time != null && time!!.end > expirationTime!!) {
            false
        } else {
            true
        }
    open val overExpirationTime: Duration
        get() = if (expirationTime != null && time != null && time!!.end > expirationTime!!) {
            time!!.end - expirationTime!!
        } else {
            Duration.ZERO
        }

    override fun hashCode(): Int {
        return key.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Task<*>

        if (key != other.key) return false

        return true
    }

    override fun toString() = displayName

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
