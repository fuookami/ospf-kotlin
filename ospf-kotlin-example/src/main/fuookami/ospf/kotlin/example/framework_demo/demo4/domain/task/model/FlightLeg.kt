@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.example.framework_demo.demo4.domain.task.model


import fuookami.ospf.kotlin.math.algebra.number.*
import kotlin.math.*
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.*
import fuookami.ospf.kotlin.example.framework_demo.demo4.infrastructure.*

enum class FlightType {
    Domestic {
        override val isDomainType: Boolean get() = true
    },
    Regional,
    International;

    companion object {
        operator fun invoke(dep: Airport, arr: Airport): FlightType {
            return invoke(dep.type, arr.type)
        }

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

    fun recoveryEnabled(): Boolean {
        return actualTime == null && outTime == null
    }
}

object FlightLegTaskType : FlightTaskType(FlightTaskCategory.Flight, FlightLegTaskType::class) {
    override val type get() = "flight"
}

class FlightLeg internal constructor(
    override val plan: FlightLegPlan,
    val recoveryAircraft: Aircraft? = null,
    val recoveryTime: TimeRange? = null,
    origin: FlightLeg? = null
) : FlightTask(FlightLegTaskType, origin) {
    companion object {
        operator fun invoke(plan: FlightLegPlan): FlightLeg {
            return FlightLeg(plan = plan)
        }

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
