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
         * 根据出发和到达机场确定航班类型。Determines the flight type from departure and arrival airports.
         *
         * @param dep The departure airport / 出发机场
         * @param arr The arrival airport / 到达机场
         * @return The determined flight type / 确定的航班类型
        */
        operator fun invoke(dep: Airport, arr: Airport): FlightType {
            return invoke(dep.type, arr.type)
        }

        /**
         * 根据出发和到达机场类型确定航班类型。Determines the flight type from departure and arrival airport types.
         *
         * @param dep The departure airport type / 出发机场类型
         * @param arr The arrival airport type / 到达机场类型
         * @return The determined flight type / 确定的航班类型
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
 * @property no The flight number / 航班号
 * @property type The flight type / 航班类型
 * @property date The flight date / 航班日期
 * @property estimatedTime The estimated time range / 预计时间范围
 * @property actualTime The actual time range / 实际时间范围
 * @property outTime The out time / 推出时间
 * @property flightTaskStatus The set of flight task statuses / 航班任务状态集合
*/
class FlightLegPlan(
    actualId: String,
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

    override val actualId = FlightTaskPlanId(actualId)
    override val displayName = no

    override val time: TimeRange? get() = actualTime ?: estimatedTime ?: super.time

    /**
     * 检查此航段是否有资格进行恢复（无实际时间或推出时间）。Checks whether this flight leg is eligible for recovery (no actual time or out time).
     *
     * @return true if eligible for recovery, false otherwise / 如果有资格恢复则为true，否则为false
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
         * 从计划创建 [FlightLeg]。Creates a [FlightLeg] from a plan.
         *
         * @param plan The flight leg plan / 航段计划
         * @return The created flight leg / 创建的航段
        */
        operator fun invoke(plan: FlightLegPlan): FlightLeg {
            return FlightLeg(plan = plan)
        }

        /**
         * 创建应用给定恢复策略的已恢复 [FlightLeg]。Creates a recovered [FlightLeg] applying the given recovery policy.
         *
         * @param origin The original flight leg / 原始航段
         * @param recoveryPolicy The recovery policy assignment / 恢复策略分配
         * @return The recovered flight leg / 恢复后的航段
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
