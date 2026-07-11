@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.example.framework_demo.demo4.domain.task.model

import kotlin.reflect.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import fuookami.ospf.kotlin.utils.concept.*
import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.*

/** 枚举航班任务类别。Enumerates the flight task categories. */
enum class FlightTaskCategory {
    /** Actual flight / 实际航班 */
    Flight {
        override val isFlightType: Boolean get() = true
    },

    /** Virtual flight / 虚拟航班 */
    VirtualFlight {
        override val isFlightType: Boolean get() = true
    },

    /** Maintenance task / 维修任务 */
    Maintenance,

    /** Aircraft on ground / 停场任务 */
    AOG;

    open val isFlightType: Boolean get() = false
}

/**
 * 航班任务类型的抽象基类（将类别链接到 Kotlin 类）。
 * Abstract base for flight task types, linking a category to a Kotlin class.
 *
 * @property category the flight task category this type belongs to / 此类型所属的航班任务类别
 * @property isFlightType whether this task type represents a flight / 此任务类型是否表示航班
*/
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

    /**
     * Checks equality with another flight task type by class.
     * 按类检查与另一个航班任务类型的相等性。
     *
     * @param type the flight task type to compare with / 要比较的航班任务类型
     * @return true if the classes are equal / 如果类相等则为 true
    */
    infix fun eq(type: FlightTaskType): Boolean {
        return this.cls == type.cls
    }

    /**
     * Checks inequality with another flight task type by class.
     * 按类检查与另一个航班任务类型的不等性。
     *
     * @param type the flight task type to compare with / 要比较的航班任务类型
     * @return true if the classes are not equal / 如果类不相等则为 true
    */
    infix fun neq(type: FlightTaskType): Boolean {
        return this.cls != type.cls
    }

    /**
     * Checks equality with a flight task category.
     * 检查与航班任务类别的相等性。
     *
     * @param category the flight task category to compare with / 要比较的航班任务类别
     * @return true if the category matches / 如果类别匹配则为 true
    */
    infix fun eq(category: FlightTaskCategory): Boolean {
        return this.category == category
    }

    /**
     * Checks inequality with a flight task category.
     * 检查与航班任务类别的不等性。
     *
     * @param category the flight task category to compare with / 要比较的航班任务类别
     * @return true if the category does not match / 如果类别不匹配则为 true
    */
    infix fun neq(category: FlightTaskCategory): Boolean {
        return this.category != category
    }
}

/** 枚举可在航班任务上设置的状态标志。Enumerates the status flags that can be set on flight tasks. */
enum class FlightTaskStatus {
    /** Do not advance the task / 不允许提前 */
    NotAdvance,
    /** Do not delay the task / 不允许延误 */
    NotDelay,
    /** Do not cancel the task / 不允许取消 */
    NotCancel,
    /** Prefer not to cancel the task / 不取消偏好 */
    NotCancelPreferred,
    /** Do not change the aircraft / 不允许换飞机 */
    NotAircraftChange,
    /** Do not change the aircraft type / 不允许换机型 */
    NotAircraftTypeChange,
    /** Do not change the aircraft minor type / 不允许换子机型 */
    NotAircraftMinorTypeChange,
    /** Do not change the terminal / 不允许换航站楼 */
    NotTerminalChange,

    /** Ignore strong limits / 忽略强约束 */
    StrongLimitIgnored
}

/**
 * 指定可选飞机、时间和航线变更的恢复策略。
 * Recovery policy specifying optional aircraft, time, and route changes.
 *
 * @property aircraft the optional aircraft assignment / 可选的飞机分配
 * @property route the optional route assignment / 可选的航线分配
*/
open class FlightTaskAssignment(
    val aircraft: Aircraft? = null,
    time: TimeRange? = null,
    val route: Route? = null
) : AssignmentPolicy<Aircraft>() {
    override val empty by lazy {
        aircraft == null && time == null && route == null
    }
}

/**
 * 具有状态、飞机、机场和连接时间逻辑的航班任务计划的抽象基类。
 * Abstract base for flight task plans with status, aircraft, airports, and connection time logic.
 *
 * @property flightTaskStatus the set of flight task status flags / 航班任务状态标志集合
*/
abstract class FlightTaskPlan(
    id: String,
    override val name: String,
    val flightTaskStatus: Set<FlightTaskStatus>
) : AbstractTaskPlan<Aircraft> {
    override val id = FlightTaskPlanId(id)

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

    /**
     * Returns the actual arrival airport for a given departure airport.
     * 返回给定出发机场的实际到达机场。
     *
     * @param dep the departure airport / 出发机场
     * @return the actual arrival airport, or null if not applicable / 实际到达机场，如果不适用则为 null
    */
    open fun actualArr(dep: Airport): Airport? = arr

    override val executor = aircraft
    override val enabledExecutors = enabledAircrafts

    /**
     * Calculates the connection time to a successor task using the plan's aircraft.
     * 使用计划的飞机计算到后续任务的连接时间。
     *
     * @param succTask the successor flight task, or null if none / 后续航班任务，如果没有则为 null
     * @return the connection time duration, or null if no aircraft is assigned / 连接时间持续时间，如果没有分配飞机则为 null
    */
    open fun connectionTime(succTask: FlightTask?): Duration? {
        return aircraft?.let { connectionTime(it, succTask) }
    }

    /**
     * Calculates the connection time for a specific aircraft to a successor task.
     * 计算特定飞机到后续任务的连接时间。
     *
     * @param aircraft the aircraft for which to calculate connection time / 要计算连接时间的飞机
     * @param succTask the successor flight task, or null if none / 后续航班任务，如果没有则为 null
     * @return the connection time duration / 连接时间持续时间
    */
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

    /** Whether aircraft change is allowed for this task. 中文此任务是否允许换飞机。 */
    val aircraftChangeEnabled: Boolean get() = !flightTaskStatus.contains(FlightTaskStatus.NotAircraftChange)

    /** Whether aircraft type change is allowed for this task. 中文此任务是否允许换机型。 */
    val aircraftTypeChangeEnabled: Boolean get() = !flightTaskStatus.contains(FlightTaskStatus.NotAircraftTypeChange)

    /** Whether aircraft minor type change is allowed for this task. 中文此任务是否允许换子机型。 */
    val aircraftMinorTypeChangeEnabled: Boolean get() = !flightTaskStatus.contains(FlightTaskStatus.NotAircraftMinorTypeChange)

    /** Whether terminal change is allowed for this task. 中文此任务是否允许换航站楼。 */
    val terminalChangeEnabled: Boolean get() = !flightTaskStatus.contains(FlightTaskStatus.NotTerminalChange)

    /** The weight of this task in the objective function. 中文此任务在目标函数中的权重。 */
    open val weight: FltX get() = FltX.one

    /** Whether strong limits are ignored for this task. 中文此任务是否忽略强约束。 */
    val strongLimitIgnored: Boolean get() = flightTaskStatus.contains(FlightTaskStatus.StrongLimitIgnored)
}

/**
 * 具有计划、恢复、延迟/提前跟踪和变更检测的航班任务的抽象基类。
 * Abstract base for flight tasks with plan, recovery, delay/advance tracking, and change detection.
 *
 * @property origin the original flight task before recovery, or null if this is the original / 恢复前的原始航班任务，如果这是原始任务则为 null
*/
abstract class FlightTask(
    override val type: FlightTaskType,
    private val origin: FlightTask? = null
) : ManualIndexed(), AbstractTask<Aircraft, FlightTaskAssignment> {

    /** Whether this task represents a flight type. 中文此任务是否为航班类型。 */
    val isFlight: Boolean = type.isFlightType

    abstract val plan: FlightTaskPlan

    override val id: TaskPlanId get() = plan.id
    override val actualId: TaskPlanId get() = plan.actualId
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

    /**
     * Returns the actual arrival airport for a given departure airport.
     * 返回给定出发机场的实际到达机场。
     *
     * @param dep the departure airport / 出发机场
     * @return the actual arrival airport, or null if not applicable / 实际到达机场，如果不适用则为 null
    */
    open fun actualArr(dep: Airport): Airport? {
        return plan.actualArr(dep)
    }

    override val timeWindow: TimeRange? get() = null
    override val scheduledTime: TimeRange? get() = plan.scheduledTime
    override val time: TimeRange? get() = plan.time
    override val duration: Duration? get() = plan.duration

    /**
     * Returns the flight duration for a specific aircraft.
     * 返回特定飞机的航班飞行时长。
     *
     * @param aircraft the aircraft for which to calculate duration / 要计算时长的飞机
     * @return the flight duration / 飞行时长
    */
    open fun duration(aircraft: Aircraft): Duration {
        return plan.duration(aircraft)
    }

    /** The flight hour derived from the task duration, or null if not a flight. 中文从任务时长派生的飞行小时数，如果不是航班则为 null。 */
    open val flightHour: FlightHour?
        get() = if (isFlight) {
            duration?.let { FlightHour(it) }
        } else {
            null
        }

    /**
     * Calculates the flight hour for a specific aircraft.
     * 计算特定飞机的飞行小时数。
     *
     * @param aircraft the aircraft for which to calculate flight hours / 要计算飞行小时的飞机
     * @return the flight hour / 飞行小时
    */
    open fun flightHour(aircraft: Aircraft) = FlightHour(
        if (isFlight) {
            duration(aircraft)
        } else {
            Duration.ZERO
        }
    )

    /**
     * Calculates the flight hour for a specific aircraft, considering an expiration time.
     * 计算特定飞机的飞行小时数，考虑过期时间。
     *
     * @param aircraft the aircraft for which to calculate flight hours / 要计算飞行小时的飞机
     * @param expirationTime the expiration time; only flights starting before this count / 过期时间；仅在此时间之前开始的航班计入
     * @return the flight hour / 飞行小时
    */
    open fun flightHour(aircraft: Aircraft, expirationTime: Instant) = FlightHour(
        if (isFlight && time!!.start < expirationTime) {
            duration(aircraft)
        } else {
            Duration.ZERO
        }
    )

    /** The flight cycle count (1 for flights, 0 otherwise). 中文飞行循环数（航班为1，否则为0）。 */
    open val flightCycle
        get() = FlightCycle(
            if (isFlight) {
                UInt64.one
            } else {
                UInt64.zero
            }
        )

    /**
     * Calculates the flight cycle count considering an expiration time.
     * 计算考虑过期时间的飞行循环数。
     *
     * @param expirationTime the expiration time; only flights starting before this count / 过期时间；仅在此时间之前开始的航班计入
     * @return the flight cycle count / 飞行循环数
    */
    open fun flightCycle(expirationTime: Instant) = FlightCycle(
        if (isFlight && time!!.start < expirationTime) {
            UInt64.one
        } else {
            UInt64.zero
        }
    )

    /**
     * Calculates the connection time to a successor task using the plan's aircraft.
     * 使用计划的飞机计算到后续任务的连接时间。
     *
     * @param succTask the successor flight task, or null if none / 后续航班任务，如果没有则为 null
     * @return the connection time duration, or null if no aircraft is assigned / 连接时间持续时间，如果没有分配飞机则为 null
    */
    open fun connectionTime(succTask: FlightTask?): Duration? {
        return plan.connectionTime(succTask)
    }

    /**
     * Calculates the connection time for a specific aircraft to a successor task.
     * 计算特定飞机到后续任务的连接时间。
     *
     * @param aircraft the aircraft for which to calculate connection time / 要计算连接时间的飞机
     * @param succTask the successor flight task, or null if none / 后续航班任务，如果没有则为 null
     * @return the connection time duration / 连接时间持续时间
    */
    open fun connectionTime(aircraft: Aircraft, succTask: FlightTask?): Duration {
        return plan.connectionTime(aircraft, succTask)
    }

    /**
     * Returns the latest normal start time for this task given an aircraft.
     * 返回给定飞机时此任务的最新正常开始时间。
     *
     * @param aircraft the aircraft for which to calculate the latest start time / 要计算最新开始时间的飞机
     * @return the latest normal start time / 最新正常开始时间
    */
    open fun latestNormalStartTime(aircraft: Aircraft): Instant {
        return if (scheduledTime != null) {
            scheduledTime!!.start
        } else {
            timeWindow!!.end - duration(aircraft)
        }
    }

    // it is disabled to recovery if there is actual time or out time
    // it is necessary to be participated in the problem, but it is unallowed to set recovery policy
    /**
     * Checks whether recovery is enabled for this task within the given time window.
     * 检查在给定时间窗口内此任务是否启用恢复。
     *
     * @param timeWindow the time window to check / 要检查的时间窗口
     * @return true if recovery is enabled within the time window / 如果在时间窗口内启用恢复则为 true
    */
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

    /**
     * Checks whether recovery is needed for this task within the given time window.
     * 检查在给定时间窗口内此任务是否需要恢复。
     *
     * @param timeWindow the time window to check / 要检查的时间窗口
     * @return true if recovery is both enabled and needed within the time window / 如果在时间窗口内恢复已启用且需要恢复则为 true
    */
    open fun recoveryNeeded(timeWindow: TimeRange): Boolean {
        return recoveryEnabled(timeWindow)
                && (time == null || timeWindow.withIntersection(time!!))
    }

    /**
     * Checks whether recovery is enabled for the given assignment policy.
     * 检查给定分配策略是否启用恢复。
     *
     * @param policy the flight task assignment policy to check / 要检查的航班任务分配策略
     * @return true if recovery is enabled for the policy / 如果该策略启用恢复则为 true
    */
    open fun recoveryEnabled(policy: FlightTaskAssignment): Boolean {
        return true
    }

    /** The original task before recovery, or this task if it is the original. 中文恢复前的原始任务，如果这是原始任务则返回自身。 */
    open val originTask: FlightTask get() = origin ?: this

    /**
     * Creates a recovered flight task based on the given assignment policy.
     * 根据给定分配策略创建恢复后的航班任务。
     *
     * @param policy the flight task assignment policy for recovery / 用于恢复的航班任务分配策略
     * @return the recovered flight task / 恢复后的航班任务
    */
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

    /** Whether the aircraft has been changed in the recovery policy. 中文恢复策略中飞机是否已更换。 */
    open val aircraftChanged: Boolean
        get() = if (!aircraftChangeEnabled) {
            false
        } else {
            recoveryPolicy.aircraft != null
        }

    /** Whether the aircraft type has been changed in the recovery policy. 中文恢复策略中机型是否已更换。 */
    open val aircraftTypeChanged: Boolean get() = aircraftTypeChange != null

    /** Whether the aircraft minor type has been changed in the recovery policy. 中文恢复策略中子机型是否已更换。 */
    open val aircraftMinorTypeChanged: Boolean get() = aircraftMinorTypeChange != null

    /** Whether the route has been changed in the recovery policy. 中文恢复策略中航线是否已更换。 */
    open val routeChanged: Boolean
        get() = if (!routeChangeEnabled) {
            false
        } else {
            recoveryPolicy.route != null
        }

    /** The aircraft change details, or null if no change. 中文飞机更换详情，如果未更换则为 null。 */
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

    /** The aircraft type change details, or null if no change. 中文机型更换详情，如果未更换则为 null。 */
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

    /** The aircraft minor type change details, or null if no change. 中文子机型更换详情，如果未更换则为 null。 */
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

    /** The route change details, or null if no change. 中文航线更换详情，如果未更换则为 null。 */
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

    /**
     * Checks whether this task arrives at the given airport within the time window.
     * 检查此任务是否在给定时间窗口内到达指定机场。
     *
     * @param airport the airport to check arrival at / 要检查到达的机场
     * @param timeWindow the time window to check / 要检查的时间窗口
     * @return true if the task arrives at the airport within the time window / 如果任务在时间窗口内到达该机场则为 true
    */
    open fun arrivedWhen(airport: Airport, timeWindow: TimeRange): Boolean {
        return isFlight && time != null
                && arr == airport
                && timeWindow.contains(time!!)
    }

    /**
     * Checks whether this task departs from the given airport within the time window.
     * 检查此任务是否在给定时间窗口内从指定机场出发。
     *
     * @param airport the airport to check departure from / 要检查出发的机场
     * @param timeWindow the time window to check / 要检查的时间窗口
     * @return true if the task departs from the airport within the time window / 如果任务在时间窗口内从该机场出发则为 true
    */
    open fun departedWhen(airport: Airport, timeWindow: TimeRange): Boolean {
        return isFlight && time != null
                && dep == airport
                && timeWindow.contains(time!!)
    }

    /**
     * Checks whether this task is located at the given airport between the previous task and this task within the time window.
     * 检查在给定时间窗口内，前序任务与此任务之间是否位于指定机场。
     *
     * @param prevTask the previous flight task / 前序航班任务
     * @param airport the airport to check location at / 要检查位置的机场
     * @param timeWindow the time window to check / 要检查的时间窗口
     * @return true if the task is located at the airport within the time window / 如果任务在时间窗口内位于该机场则为 true
    */
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

/**
 * Calculates how much earlier the task starts compared to the target time.
 * 计算任务比目标时间提前了多少。
 * @param targetTime The target time range to compare against, or null to use the time window / 要比较的目标时间范围，为 null 时使用时间窗口
 * @return The advance duration, or zero if there is no advance / 提前时长，如果没有提前则为零
*/
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

/**
 * Calculates how much later the task starts compared to the target time.
 * 计算任务比目标时间延迟了多少。
 * @param targetTime The target time range to compare against, or null to use the time window end / 要比较的目标时间范围，为 null 时使用时间窗口结束时间
 * @return The delay duration, or zero if there is no delay / 延误时长，如果没有延误则为零
*/
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
