@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.example.framework_demo.demo4.domain.task.model

import kotlin.time.Instant
import java.util.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.*
import fuookami.ospf.kotlin.example.framework_demo.demo4.infrastructure.*

/** 枚举维护类别及其稳定状态标志。Enumerates the maintenance categories with their stable status flags. */
enum class MaintenanceCategory {
    Line {
        override val stableStatus = setOf(
            FlightTaskStatus.NotAdvance,
            FlightTaskStatus.NotAircraftChange,
            FlightTaskStatus.NotAircraftTypeChange,
            FlightTaskStatus.NotAircraftMinorTypeChange
        )

        override fun toString() = "line"
    },
    Schedule {
        override val stableStatus = setOf(
            FlightTaskStatus.NotDelay,
            FlightTaskStatus.NotAdvance,
            FlightTaskStatus.NotAircraftChange,
            FlightTaskStatus.NotAircraftTypeChange,
            FlightTaskStatus.NotAircraftMinorTypeChange
        )

        override fun toString() = "schedule"
    };

    abstract val stableStatus: Set<FlightTaskStatus>
}

/** 具有飞机、时间、机场、类别和过期信息的维护计划。A maintenance plan with aircraft, time, airport, category, and expiration information. */
class MaintenancePlan internal constructor(
    override val aircraft: Aircraft,
    override val scheduledTime: TimeRange,
    val airport: Airport,
    val airportBackup: List<Airport>,
    val category: MaintenanceCategory,
    val expirationTime: Instant,
    status: Set<FlightTaskStatus>,
    override val actualId: String = UUID.randomUUID().toString()
) : FlightTaskPlan(
    id = "${prefix}_${actualId.replace("-", "")}",
    name = "${aircraft.regNo}_${category}_${scheduledTime.start.toShortString()}",
    flightTaskStatus = status
) {
    companion object {
        private const val prefix = "m"

        /**
         * Creates a [MaintenancePlan] with appropriate status based on category and expiration.
 *
         * @param aircraft 参数。
         * @param scheduledTime 参数。
         * @param airports 参数。
         * @param expirationTime 参数。
         * @param category 参数。
         * @param timeWindow 参数。
         * @return 返回结果。
         */
        operator fun invoke(
            aircraft: Aircraft,
            scheduledTime: TimeRange,
            airports: List<Airport>,
            expirationTime: Instant,
            category: MaintenanceCategory,
            timeWindow: TimeRange
        ): MaintenancePlan {
            assert(airports.isNotEmpty())
            val status = category.stableStatus.toMutableSet()
            if (expirationTime <= timeWindow.end) {
                status.add(FlightTaskStatus.NotCancel)
            }

            return if (airports.size == 1) {
                status.add(FlightTaskStatus.NotTerminalChange)
                MaintenancePlan(
                    aircraft = aircraft,
                    scheduledTime = scheduledTime,
                    airport = airports.first(),
                    airportBackup = emptyList(),
                    category = category,
                    expirationTime = expirationTime,
                    status = status
                )
            } else {
                MaintenancePlan(
                    aircraft = aircraft,
                    scheduledTime = scheduledTime,
                    airport = airports.first(),
                    airportBackup = airports.subList(1, airports.size),
                    category = category,
                    expirationTime = expirationTime,
                    status = status
                )
            }
        }
    }

    override val displayName = "${category}-${aircraft.regNo}"
    override val enabledAircrafts = setOf(aircraft)
    override val dep = airport
    override val arr = airport
    override val depBackup = airportBackup
    override fun actualArr(dep: Airport): Airport? {
        return if (dep == this.dep) {
            arr
        } else if (depBackup.contains(dep)) {
            dep
        } else {
            null
        }
    }

    override val duration get() = scheduledTime.duration
    override fun duration(executor: Aircraft) = scheduledTime.duration

    override fun connectionTime(succTask: FlightTask?) = NotFlightStaticConnectionTime
    override fun connectionTime(aircraft: Aircraft, succTask: FlightTask?) = NotFlightStaticConnectionTime
}

/** 维护事件的任务类型对象。Task type object for maintenance events. */
object MaintenanceFlightTask : FlightTaskType(FlightTaskCategory.Maintenance, MaintenanceFlightTask::class) {
    override val type get() = "Maintenance"
}

/** 具有可选恢复时间和机场的维护航班任务。A maintenance flight task with optional recovery time and airport. */
class Maintenance internal constructor(
    override val plan: MaintenancePlan,
    val recoveryTime: TimeRange? = null,
    val recoveryAirport: Airport? = null,
    origin: Maintenance? = null
) : FlightTask(MaintenanceFlightTask, origin) {
    companion object {
        /**
         * Creates a [Maintenance] from a plan.
 *
         * @param plan 参数。
         * @return 返回结果。
         */
        operator fun invoke(plan: MaintenancePlan): Maintenance {
            return Maintenance(plan = plan)
        }

        /**
         * Creates a recovered [Maintenance] applying the given recovery policy.
 *
         * @param origin 参数。
         * @param recoveryPolicy 参数。
         * @return 返回结果。
         */
        operator fun invoke(origin: Maintenance, recoveryPolicy: FlightTaskAssignment): Maintenance {
            val recoveryTime = if (recoveryPolicy.time == null || recoveryPolicy.time == origin.scheduledTime!!) {
                null
            } else {
                recoveryPolicy.time
            }
            val recoveryAirport = if (recoveryPolicy.route == null
                    || (recoveryPolicy.route.dep == origin.dep && recoveryPolicy.route.arr == origin.arr)
            ) {
                null
            } else {
                    assert(recoveryPolicy.route.dep == recoveryPolicy.route.arr)
                    recoveryPolicy.route.dep
            }

            assert(origin.recoveryEnabled(recoveryPolicy))
            return Maintenance(
                plan = origin.plan,
                recoveryTime = recoveryTime,
                recoveryAirport = recoveryAirport,
                origin = origin
            )
        }
    }

    override val dep get() = recoveryAirport ?: plan.dep
    override val arr get() = recoveryAirport ?: plan.arr

    override val time get() = recoveryTime ?: plan.time

    override fun recoveryEnabled(timeWindow: TimeRange): Boolean {
        return plan.expirationTime < timeWindow.end || timeWindow.contains(scheduledTime!!.start)
    }

    override fun recoveryNeeded(timeWindow: TimeRange): Boolean {
        return plan.expirationTime < timeWindow.end || timeWindow.contains(scheduledTime!!.start)
    }

    override val recovered get() = recoveryTime != null || recoveryAirport != null
    override val recoveryPolicy get() = FlightTaskAssignment(null, recoveryTime, recoveryAirport?.let { Route(it, it) })
    override fun recoveryEnabled(policy: FlightTaskAssignment): Boolean {
        if (policy.aircraft != null && aircraft!! != policy.aircraft) {
            return false
        }
        if (!delayEnabled && policy.time != null && scheduledTime!!.start < policy.time!!.start) {
            return false
        }
        if (!advanceEnabled && policy.time != null && scheduledTime!!.start > policy.time!!.start) {
            return false
        }
        if (!routeChangeEnabled && policy.route != null
            && (policy.route.dep != policy.route.arr)
            && (dep != policy.route.dep || !depBackup.contains(policy.route.dep))
        ) {
            return false
        }
        return true
    }

    override fun recovery(policy: FlightTaskAssignment): FlightTask {
        assert(recoveryEnabled(policy))
        return Maintenance(this, recoveryPolicy)
    }

    override val routeChanged get() = recoveryAirport != null
    override val routeChange
        get() = recoveryAirport?.let {
            RouteChange(
                Route(plan.airport, plan.airport),
                Route(it, it)
            )
        }
}
