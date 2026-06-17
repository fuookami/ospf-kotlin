@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.example.framework_demo.demo4.domain.task.model

import java.util.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.*
import fuookami.ospf.kotlin.example.framework_demo.demo4.infrastructure.*

/**
 * 具有固定飞机、时间和机场的 AOG（飞机停场）计划。An AOG (Aircraft On Ground) plan with fixed aircraft, time, and airport.
 *
 * @property override val aircraft 参数。
 * @property override val scheduledTime 参数。
 * @property airport 参数。
 * @property status 参数。
 * @property override val actualId 参数。
 */
class AOGPlan(
    override val aircraft: Aircraft,
    override val scheduledTime: TimeRange,
    val airport: Airport,
    status: Set<FlightTaskStatus>,
    override val actualId: String = UUID.randomUUID().toString()
) : FlightTaskPlan(
    id = "${prefix}_${actualId.replace("-", "")}",
    name = "${aircraft.regNo}_AOG_${scheduledTime.start.toShortString()}",
    flightTaskStatus = status
) {
    companion object {
        val stableStatus = setOf(
            FlightTaskStatus.NotCancel,
            FlightTaskStatus.NotDelay,
            FlightTaskStatus.NotAdvance,
            FlightTaskStatus.NotAircraftChange,
            FlightTaskStatus.NotAircraftTypeChange,
            FlightTaskStatus.NotAircraftMinorTypeChange
        )

        private const val prefix = "a"

        /**
         * Creates an [AOGPlan] with stable status for the given aircraft, time, and airport.
 *
         * @param aircraft 参数。
         * @param scheduledTime 参数。
         * @param airport 参数。
         * @return 返回结果。
         */
        operator fun invoke(aircraft: Aircraft, scheduledTime: TimeRange, airport: Airport): AOGPlan {
            val status = stableStatus.toMutableSet()
            return AOGPlan(
                aircraft = aircraft,
                scheduledTime = scheduledTime,
                airport = airport,
                status = status
            )
        }
    }

    override val displayName = "AOG"
    override val enabledAircrafts = setOf(aircraft)
    override val dep = airport
    override val arr = airport
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

/** AOG（飞机停场）事件的任务类型对象。Task type object for AOG (Aircraft On Ground) events. */
object AOGFlightTask : FlightTaskType(FlightTaskCategory.Maintenance, AOGFlightTask::class) {
    override val type get() = "AOG"
}

/** 具有可选恢复机场的 AOG 航班任务。An AOG flight task with optional recovery airport. */
class AOG internal constructor(
    override val plan: AOGPlan,
    val recoveryAirport: Airport? = null,
    origin: AOG? = null
) : FlightTask(AOGFlightTask, origin) {
    companion object {
        /**
         * Creates an [AOG] from a plan.
 *
         * @param plan 参数。
         */
        operator fun invoke(plan: AOGPlan) = AOG(plan)

        /**
         * Creates a recovered [AOG] applying the given recovery policy.
 *
         * @param origin 参数。
         * @param recoveryPolicy 参数。
         * @return 返回结果。
         */
        operator fun invoke(origin: AOG, recoveryPolicy: FlightTaskAssignment): AOG {
            val recoveryAirport =
                if (recoveryPolicy.route == null || (recoveryPolicy.route.dep == origin.dep && recoveryPolicy.route.arr == origin.arr)) {
                    null
                } else {
                    assert(recoveryPolicy.route.dep == recoveryPolicy.route.arr)
                    recoveryPolicy.route.dep
                }

            assert(origin.recoveryEnabled(recoveryPolicy))
            return AOG(
                plan = origin.plan,
                recoveryAirport = recoveryAirport,
                origin = origin
            )
        }
    }

    override val dep get() = recoveryAirport ?: plan.dep
    override val arr get() = recoveryAirport ?: plan.arr

    override val recovered get() = recoveryAirport != null
    override val recoveryPolicy get() = FlightTaskAssignment()
    override fun recoveryEnabled(policy: FlightTaskAssignment): Boolean {
        if (policy.aircraft != null && aircraft!! != policy.aircraft) {
            return false
        }
        if (policy.time != null && time != policy.time) {
            return false
        }
        if (!routeChangeEnabled && policy.route != null
            && (policy.route.dep != policy.route.arr)
            && (dep != policy.route.dep || !depBackup.contains(policy.route.dep))
        ) {
            return false
        }
        return super.recoveryEnabled(policy)
    }

    override fun recovery(policy: FlightTaskAssignment): AOG {
        assert(recoveryEnabled(policy))
        return AOG(this, recoveryPolicy)
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
