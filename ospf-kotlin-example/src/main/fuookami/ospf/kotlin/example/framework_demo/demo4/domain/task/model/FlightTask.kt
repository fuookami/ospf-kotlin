@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.example.framework_demo.demo4.domain.task.model


import fuookami.ospf.kotlin.math.algebra.number.*
import kotlin.time.Duration
import kotlin.reflect.*
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.utils.concept.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.*

enum class FlightTaskCategory {
    Flight {
        override val isFlightType: Boolean get() = true
    },

    VirtualFlight {
        override val isFlightType: Boolean get() = true
    },

    Maintenance,

    AOG;

    open val isFlightType: Boolean get() = false
}

abstract class FlightTaskType(
    val category: FlightTaskCategory,
    cls: KClass<*>
) : TaskType(cls) {
    val isFlightType by category::isFlightType

    override fun hashCode(): Int {
        return cls.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as FlightTaskType

        return cls == other.cls
    }

    infix fun eq(type: FlightTaskType): Boolean {
        return this.cls == type.cls
    }

    infix fun neq(type: FlightTaskType): Boolean {
        return this.cls != type.cls
    }

    infix fun eq(category: FlightTaskCategory): Boolean {
        return this.category == category
    }

    infix fun neq(category: FlightTaskCategory): Boolean {
        return this.category != category
    }
}

enum class FlightTaskStatus {
    NotAdvance,
    NotDelay,
    NotCancel,
    NotCancelPreferred,
    NotAircraftChange,
    NotAircraftTypeChange,
    NotAircraftMinorTypeChange,
    NotTerminalChange,

    StrongLimitIgnored
}

open class FlightTaskAssignment(
    val aircraft: Aircraft? = null,
    time: TimeRange? = null,
    val route: Route? = null
) : AssignmentPolicy<Aircraft>() {
    override val empty by lazy {
        aircraft == null && time == null && route == null
    }
}

abstract class FlightTaskPlan(
    override val id: String,
    override val name: String,
    val flightTaskStatus: Set<FlightTaskStatus>
) : AbstractTaskPlan<Aircraft> {
    companion object {
        val NotFlightStaticConnectionTime = 5.minutes
    }

    override val status: Set<TaskStatus> by lazy {
        val status = mutableSetOf<TaskStatus>()
        if (flightTaskStatus.contains(FlightTaskStatus.NotAdvance)) {
            status.add(TaskStatus.NotAdvance)
        }
        if (flightTaskStatus.contains(FlightTaskStatus.NotDelay)) {
            status.add(TaskStatus.NotDelay)
        }
        if (flightTaskStatus.contains(FlightTaskStatus.NotCancel)) {
            status.add(TaskStatus.NotCancel)
        }
        if (flightTaskStatus.contains(FlightTaskStatus.NotCancelPreferred)) {
            status.add(TaskStatus.NotCancelPreferred)
        }
        if (flightTaskStatus.contains(FlightTaskStatus.NotAircraftChange)) {
            status.add(TaskStatus.NotExecutorChange)
        }
        status
    }

    abstract val aircraft: Aircraft?
    abstract val enabledAircrafts: Set<Aircraft>
    abstract val dep: Airport
    abstract val arr: Airport
    open val depBackup: List<Airport> get() = listOf()
    open val arrBackup: List<Airport> get() = listOf()
    open fun actualArr(dep: Airport): Airport? = arr

    override val executor = aircraft
    override val enabledExecutors = enabledAircrafts

    open fun connectionTime(succTask: FlightTask?): Duration? {
        return aircraft?.let { connectionTime(it, succTask) }
    }

    open fun connectionTime(aircraft: Aircraft, succTask: FlightTask?): Duration {
        return if (succTask != null) {
            if (succTask.isFlight) {
                aircraft.connectionTime[arr] ?: aircraft.maxConnectionTime
            } else {
                NotFlightStaticConnectionTime
            }
        } else {
            0L.minutes
        }
    }

    val aircraftChangeEnabled: Boolean get() = !flightTaskStatus.contains(FlightTaskStatus.NotAircraftChange)
    val aircraftTypeChangeEnabled: Boolean get() = !flightTaskStatus.contains(FlightTaskStatus.NotAircraftTypeChange)
    val aircraftMinorTypeChangeEnabled: Boolean get() = !flightTaskStatus.contains(FlightTaskStatus.NotAircraftMinorTypeChange)
    val terminalChangeEnabled: Boolean get() = !flightTaskStatus.contains(FlightTaskStatus.NotTerminalChange)

    open val weight: FltX get() = FltX.one
    val strongLimitIgnored: Boolean get() = flightTaskStatus.contains(FlightTaskStatus.StrongLimitIgnored)
}

abstract class FlightTask(
    override val type: FlightTaskType,
    private val origin: FlightTask? = null
) : ManualIndexed(), AbstractTask<Aircraft, FlightTaskAssignment> {
    val isFlight: Boolean = type.isFlightType

    abstract val plan: FlightTaskPlan

    override val id: String get() = plan.id
    override val actualId: String get() = plan.actualId
    override val name: String get() = plan.name
    override val displayName: String get() = plan.displayName
    override val key: TaskKey get() = TaskKey(id, type)

    open val aircraft: Aircraft? get() = plan.aircraft
    open val capacity: AircraftCapacity?
        get() = if (isFlight) {
            aircraft?.capacity
        } else {
            null
        }
    open val dep: Airport get() = plan.dep
    open val arr: Airport get() = plan.arr
    open val depBackup: List<Airport> get() = plan.depBackup
    open val arrBackup: List<Airport> get() = plan.arrBackup
    open fun actualArr(dep: Airport): Airport? {
        return plan.actualArr(dep)
    }

    override val timeWindow: TimeRange? get() = null
    override val scheduledTime: TimeRange? get() = plan.scheduledTime
    override val time: TimeRange? get() = plan.time
    override val duration: Duration? get() = plan.duration
    open fun duration(aircraft: Aircraft): Duration {
        return plan.duration(aircraft)
    }

    open val flightHour: FlightHour?
        get() = if (isFlight) {
            duration?.let { FlightHour(it) }
        } else {
            null
        }

    open fun flightHour(aircraft: Aircraft) = FlightHour(
        if (isFlight) {
            duration(aircraft)
        } else {
            Duration.ZERO
        }
    )

    open fun flightHour(aircraft: Aircraft, expirationTime: Instant) = FlightHour(
        if (isFlight && time!!.start < expirationTime) {
            duration(aircraft)
        } else {
            Duration.ZERO
        }
    )

    open val flightCycle
        get() = FlightCycle(
            if (isFlight) {
                UInt64.one
            } else {
                UInt64.zero
            }
        )

    open fun flightCycle(expirationTime: Instant) = FlightCycle(
        if (isFlight && time!!.start < expirationTime) {
            UInt64.one
        } else {
            UInt64.zero
        }
    )

    open fun connectionTime(succTask: FlightTask?): Duration? {
        return plan.connectionTime(succTask)
    }

    open fun connectionTime(aircraft: Aircraft, succTask: FlightTask?): Duration {
        return plan.connectionTime(aircraft, succTask)
    }

    open fun latestNormalStartTime(aircraft: Aircraft): Instant {
        return if (scheduledTime != null) {
            scheduledTime!!.start
        } else {
            timeWindow!!.end - duration(aircraft)
        }
    }

    // it is disabled to recovery if there is actual time or out time
    // it is necessary to be participated in the problem, but it is unallowed to set recovery policy
    open fun recoveryEnabled(timeWindow: TimeRange): Boolean {
        return plan.time?.start?.let { timeWindow.contains(it) }
            ?: this.timeWindow?.let { timeWindow.withIntersection(it) }
            ?: true
    }
    override val maxDelay: Duration?
        get() = if (!delayEnabled) {
            0L.minutes
        } else {
            null
        }
    override val cancelEnabled get() = plan.cancelEnabled
    override val notCancelPreferred get() = plan.notCancelPreferred
    open val aircraftChangeEnabled get() = plan.aircraftChangeEnabled
    open val aircraftTypeChangeEnabled get() = plan.aircraftTypeChangeEnabled
    open val aircraftMinorTypeChangeEnabled get() = plan.aircraftMinorTypeChangeEnabled
    override val delayEnabled get() = plan.delayEnabled
    override val advanceEnabled get() = plan.advanceEnabled
    open val routeChangeEnabled get() = plan.terminalChangeEnabled
    open val weight get() = plan.weight
    open val strongLimitIgnored: Boolean get() = plan.strongLimitIgnored

    abstract val recovered: Boolean
    abstract val recoveryPolicy: FlightTaskAssignment
    open fun recoveryNeeded(timeWindow: TimeRange): Boolean {
        return recoveryEnabled(timeWindow)
                && (time == null || timeWindow.withIntersection(time!!))
    }

    open fun recoveryEnabled(policy: FlightTaskAssignment): Boolean {
        return true
    }

    open val originTask: FlightTask get() = origin ?: this
    abstract fun recovery(policy: FlightTaskAssignment): FlightTask

    override val advance: Duration get() = advance(plan.time)
    override val actualAdvance: Duration get() = advance(scheduledTime)
    override val delay: Duration get() = delay(plan.time)
    override val actualDelay: Duration get() = delay(scheduledTime)
    override val overMaxDelay: Duration
        get() = if (maxDelay == null || actualDelay <= maxDelay!!) {
            0.minutes
        } else {
            actualDelay - maxDelay!!
        }
    open val aircraftChanged: Boolean
        get() = if (!aircraftChangeEnabled) {
            false
        } else {
            recoveryPolicy.aircraft != null
        }
    open val aircraftTypeChanged: Boolean get() = aircraftTypeChange != null
    open val aircraftMinorTypeChanged: Boolean get() = aircraftMinorTypeChange != null
    open val routeChanged: Boolean
        get() = if (!routeChangeEnabled) {
            false
        } else {
            recoveryPolicy.route != null
        }
    open val aircraftChange: AircraftChange?
        get() = if (!aircraftChangeEnabled) {
            null
        } else {
            val policy = recoveryPolicy
            if (plan.aircraft != null
                && policy.aircraft != null
                && policy.aircraft != plan.aircraft
            ) {
                AircraftChange(plan.aircraft!!, policy.aircraft)
            } else {
                null
            }
        }
    open val aircraftTypeChange: AircraftTypeChange?
        get() = if (!aircraftTypeChangeEnabled) {
            null
        } else {
            val policy = recoveryPolicy
            if ((plan.aircraft != null)
                && (policy.aircraft != null)
                && (policy.aircraft.type != plan.aircraft!!.type)
            ) {
                AircraftTypeChange(plan.aircraft!!.type, policy.aircraft.type)
            } else {
                null
            }
        }
    open val aircraftMinorTypeChange: AircraftMinorTypeChange?
        get() = if (!aircraftMinorTypeChangeEnabled) {
            null
        } else {
            val policy = recoveryPolicy
            if ((plan.aircraft != null)
                && (policy.aircraft != null)
                && (policy.aircraft.minorType != plan.aircraft!!.minorType)
            ) {
                AircraftMinorTypeChange(plan.aircraft!!.minorType, policy.aircraft.minorType)
            } else {
                null
            }
        }
    open val routeChange: RouteChange?
        get() = if (!routeChangeEnabled) {
            null
        } else {
            val policy = recoveryPolicy
            if (policy.route != null
                && (policy.route.dep != dep || policy.route.arr != arr)
            ) {
                RouteChange(Route(dep, arr), policy.route)
            } else {
                null
            }
        }

    open fun arrivedWhen(airport: Airport, timeWindow: TimeRange): Boolean {
        return isFlight && time != null
                && arr == airport
                && timeWindow.contains(time!!)
    }

    open fun departedWhen(airport: Airport, timeWindow: TimeRange): Boolean {
        return isFlight && time != null
                && dep == airport
                && timeWindow.contains(time!!)
    }

    open fun locatedWhen(prevTask: FlightTask, airport: Airport, timeWindow: TimeRange): Boolean {
        val prevTime = prevTask.time
        return prevTime != null && time != null
                && prevTask.arr == airport
                && timeWindow.contains(
            TimeRange(
                start = prevTime.end,
                end = if (isFlight) {
                    time!!.start
                } else {
                    time!!.end
                }
            )
        )
    }

    override fun partialEq(rhs: AbstractTask<Aircraft, FlightTaskAssignment>): Boolean? {
        return if (rhs is FlightTask) {
            plan == rhs.plan && recoveryPolicy == rhs.recoveryPolicy
        } else {
            false
        }
    }

    override fun hashCode(): Int {
        return key.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as FlightTask

        return key == other.key
    }

    override fun toString() = name

    private fun advance(targetTime: TimeRange?): Duration {
        return if (targetTime != null && time != null) {
            val advance = targetTime.start - time!!.start
            if (advance.isNegative()) {
                0.minutes
            } else {
                advance
            }
        } else if (timeWindow != null && time != null) {
            val advance = timeWindow!!.start - time!!.start
            if (advance.isNegative()) {
                0.minutes
            } else {
                advance
            }
        } else {
            0.minutes
        }
    }

    private fun delay(targetTime: TimeRange?): Duration {
        return if (targetTime != null && time != null) {
            val delay = time!!.start - targetTime.start
            if (delay.isNegative()) {
                0.minutes
            } else {
                delay
            }
        } else if (timeWindow != null && time != null) {
            val delay = time!!.start - timeWindow!!.end
            if (delay.isNegative()) {
                0.minutes
            } else {
                delay
            }
        } else {
            0.minutes
        }
    }
}
