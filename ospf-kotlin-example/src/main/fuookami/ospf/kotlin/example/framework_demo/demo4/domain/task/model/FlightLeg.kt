@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.example.framework_demo.demo4.domain.task.model

import kotlinx.datetime.LocalDate
import kotlin.math.*
import kotlin.time.Instant
import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.*
import fuookami.ospf.kotlin.example.framework_demo.demo4.infrastructure.*

/** 基于出发/到达机场类型枚举航班类型。Enumerates the flight types based on departure/arrival airport types. */
enum class FlightType {
    Domestic {
        override val isDomainType: Boolean get() = true
    },
    Regional,
    International;

    companion object {
        /**
         * Determines the flight type from departure and arrival airports.
 *
         * @param dep 参数。
         * @param arr 参数。
         * @return 返回结果。
         */
        operator fun invoke(dep: Airport, arr: Airport): FlightType {
            return invoke(dep.type, arr.type)
        }

        /**
         * Determines the flight type from departure and arrival airport types.
 *
         * @param dep 参数。
         * @param arr 参数。
         * @return 返回结果。
         */
        operator fun invoke(dep: AirportType, arr: AirportType): FlightType {
            return when (AirportType.entries.find { it.ordinal == max(dep.ordinal, arr.ordinal) }!!) {
                AirportType.Domestic -> {
                    Domestic
                }

                AirportType.Regional -> {
                    Regional
                }

                AirportType.International -> {
                    International
                }
            }
        }
    }

    open val isDomainType: Boolean get() = false
}

/**
 * 具有计划/估计/实际时间、飞机和航线信息的航段计划。A flight leg plan with scheduled/estimated/actual times, aircraft, and route information.
 *
 * @property no 参数。
 * @property type 参数。
 * @property date 参数。
 * @property estimatedTime 参数。
 * @property actualTime 参数。
 * @property outTime 参数。
 * @property flightTaskStatus 参数。
 */
class FlightLegPlan(
    override val actualId: String,
    val no: String,
    val type: FlightType,
    val date: LocalDate,
    override val aircraft: Aircraft,
    override val enabledAircrafts: Set<Aircraft>,
    override val dep: Airport,
    override val arr: Airport,
    override val scheduledTime: TimeRange,
    val estimatedTime: TimeRange?,
    val actualTime: TimeRange?,
    val outTime: Instant?,
    flightTaskStatus: Set<FlightTaskStatus>,
    override val weight: FltX = FltX.one,
) : FlightTaskPlan(
    id = "${prefix}_${actualId}",
    name = "${no}_${date}",
    flightTaskStatus = flightTaskStatus
) {
    companion object {
        const val prefix = "f"
    }

    override val displayName = no

    override val time: TimeRange? get() = actualTime ?: estimatedTime ?: super.time

    /**
     * Checks whether this flight leg is eligible for recovery (no actual time or out time).
 *
     * @return 返回结果。
     */
    fun recoveryEnabled(): Boolean {
        return actualTime == null && outTime == null
    }
}

/** 航段的任务类型对象。Task type object for flight legs. */
object FlightLegTaskType : FlightTaskType(FlightTaskCategory.Flight, FlightLegTaskType::class) {
    override val type get() = "flight"
}

/** 具有可选恢复飞机和时间的航段任务。A flight leg task with optional recovery aircraft and time. */
class FlightLeg internal constructor(
    override val plan: FlightLegPlan,
    val recoveryAircraft: Aircraft? = null,
    val recoveryTime: TimeRange? = null,
    origin: FlightLeg? = null
) : FlightTask(FlightLegTaskType, origin) {
    companion object {
        /**
         * Creates a [FlightLeg] from a plan.
 *
         * @param plan 参数。
         * @return 返回结果。
         */
        operator fun invoke(plan: FlightLegPlan): FlightLeg {
            return FlightLeg(plan = plan)
        }

        /**
         * Creates a recovered [FlightLeg] applying the given recovery policy.
 *
         * @param origin 参数。
         * @param recoveryPolicy 参数。
         * @return 返回结果。
         */
        operator fun invoke(origin: FlightLeg, recoveryPolicy: FlightTaskAssignment): FlightLeg {
            val recoveryAircraft = if (recoveryPolicy.aircraft == null || recoveryPolicy.aircraft == origin.aircraft) {
                null
            } else {
                recoveryPolicy.aircraft
            }
            val recoveryTime = if (recoveryPolicy.time == null || recoveryPolicy.time == origin.scheduledTime!!) {
                null
            } else {
                recoveryPolicy.time
            }

            assert(origin.recoveryEnabled(recoveryPolicy))
            return FlightLeg(
                plan = origin.plan,
                recoveryAircraft = recoveryAircraft,
                recoveryTime = recoveryTime,
                origin = origin
            )
        }
    }

    override val aircraft get() = recoveryAircraft ?: plan.aircraft
    override val time get() = recoveryTime ?: plan.time

    override fun recoveryEnabled(timeWindow: TimeRange): Boolean {
        return plan.recoveryEnabled() && super.recoveryEnabled(timeWindow)
    }

    override fun recoveryNeeded(timeWindow: TimeRange): Boolean {
        return plan.recoveryEnabled() && timeWindow.contains(time!!.start)
    }

    override val recovered get() = recoveryAircraft != null || recoveryTime != null
    override val recoveryPolicy get() = FlightTaskAssignment(recoveryAircraft, recoveryTime, null)
    override fun recoveryEnabled(policy: FlightTaskAssignment): Boolean {
        if (!aircraftChangeEnabled && policy.aircraft != null && aircraft != policy.aircraft) {
            return false
        }
        if (!aircraftTypeChangeEnabled && policy.aircraft != null && aircraft.type != policy.aircraft.type) {
            return false
        }
        if (!aircraftMinorTypeChangeEnabled && policy.aircraft != null && aircraft.minorType != policy.aircraft.minorType) {
            return false
        }
        if (!delayEnabled && policy.time != null && plan.time!!.start < policy.time!!.start) {
            return false
        }
        if (!advanceEnabled && policy.time != null && plan.time!!.start > policy.time!!.start) {
            return false
        }
        return true
    }

    override fun recovery(policy: FlightTaskAssignment): FlightTask {
        assert(recoveryEnabled(policy))
        return FlightLeg(this, policy)
    }

    override val aircraftChanged: Boolean get() = recoveryAircraft != null
    override val aircraftTypeChanged: Boolean get() = recoveryAircraft?.let { it.type != plan.aircraft.type } ?: false
    override val aircraftMinorTypeChanged: Boolean get() = recoveryAircraft?.let { it.minorType != plan.aircraft.minorType } ?: false
    override val aircraftChange: AircraftChange? get() = recoveryAircraft?.let { AircraftChange(plan.aircraft, it) }
    override val aircraftTypeChange: AircraftTypeChange?
        get() = recoveryAircraft?.let {
            if (it.type == plan.aircraft.type) {
                null
            } else {
                AircraftTypeChange(plan.aircraft.type, it.type)
            }
        }
    override val aircraftMinorTypeChange: AircraftMinorTypeChange?
        get() = recoveryAircraft?.let {
            if (it.minorType == plan.aircraft.minorType) {
                null
            } else {
                AircraftMinorTypeChange(plan.aircraft.minorType, it.minorType)
            }
        }

    override fun toString() = "${plan.no}, ${aircraft.regNo}, ${dep.icao} - ${arr.icao}, ${time!!.start.toShortString()} - ${time!!.end.toShortString()}"
}
