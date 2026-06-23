@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.example.framework_demo.demo4.domain.bunch_generation.service

import kotlin.time.Instant
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.*
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.rule.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.task.model.*

/**
 * 检查给定前一任务时航班任务对飞机是否可行。Checks if a flight task is feasible for an aircraft given the previous task.
 *
 * @property aircraftUsability 参数。
 * @property connectionTimeCalculator 参数。
 * @property ruleChecker 参数。
 */
class FlightTaskFeasibilityJudger(
    val aircraftUsability: Map<Aircraft, AircraftUsability>,
    val connectionTimeCalculator: ConnectionTimeCalculator,
    val ruleChecker: RuleChecker
) {
    data class Config(
        val checkEnabledTime: Boolean = true,
        val timeExtractor: (FlightTask) -> TimeRange? = { it.scheduledTime },
        val departureTime: Instant? = null
    )

    /**
     * Checks if the given flight task is feasible for the aircraft.
 *
     * @param aircraft 参数。
     * @param prevFlightTask 参数。
     * @param flightTask 参数。
     * @param config 参数。
     */
    operator fun invoke(
        aircraft: Aircraft,
        prevFlightTask: FlightTask?,
        flightTask: FlightTask,
        config: Config = Config()
    ): Boolean {
        val subProcesses = listOf(
            { checkAircraft(aircraft, flightTask) },
            { checkAircraftType(aircraft, flightTask) },
            { checkAircraftMinorType(aircraft, flightTask) },
            { checkAircraftCapacity(aircraft, flightTask) },
            { checkAircraftUsability(aircraft, prevFlightTask, flightTask, config) },
            { checkAirportConnection(aircraft, prevFlightTask, flightTask) },
            { checkFlyTime(aircraft, flightTask) },
            { checkTime(aircraft, prevFlightTask, flightTask, config) },
            { checkTimeWindow(aircraft, prevFlightTask, flightTask, config) },
            { checkRules(aircraft, prevFlightTask, flightTask) },
        )

        for (process in subProcesses) {
            if (!process()) {
                return false
            }
        }
        return true
    }

    private fun checkAircraft(aircraft: Aircraft, flightTask: FlightTask): Boolean {
        return flightTask.aircraftChangeEnabled || flightTask.aircraft == aircraft
    }

    private fun checkAircraftType(aircraft: Aircraft, flightTask: FlightTask): Boolean {
        return flightTask.aircraftTypeChangeEnabled || flightTask.aircraft?.type == aircraft.type
    }

    private fun checkAircraftMinorType(aircraft: Aircraft, flightTask: FlightTask): Boolean {
        return flightTask.aircraftTypeChangeEnabled || flightTask.aircraft?.minorType == aircraft.minorType
    }

    private fun checkAircraftCapacity(aircraft: Aircraft, flightTask: FlightTask): Boolean {
        return !flightTask.isFlight || flightTask.capacity?.let { it.category == aircraft.capacity.category } != false
    }

    private fun checkAircraftUsability(
        aircraft: Aircraft,
        prevFlightTask: FlightTask?,
        flightTask: FlightTask,
        config: Config
    ): Boolean {
        if (prevFlightTask != null) {
            return true
        }
        val usability = aircraftUsability[aircraft] ?: return true
        if (flightTask.dep != usability.location) {
            return false
        }
        if (config.checkEnabledTime) {
            val time = config.timeExtractor(flightTask)
            if (time != null && time.start < usability.enabledTime) {
                return false
            }
        }
        return true
    }

    private fun checkAirportConnection(
        aircraft: Aircraft,
        prevFlightTask: FlightTask?,
        flightTask: FlightTask
    ): Boolean {
        if (prevFlightTask == null) {
            return true
        }
        if (prevFlightTask.arr == flightTask.dep) {
            return true
        } else {
            val arr = arrayListOf(prevFlightTask.arr)
            arr.addAll(prevFlightTask.arrBackup)

            val dep = arrayListOf(flightTask.dep)
            dep.addAll(flightTask.depBackup)

            for (airport in arr) {
                if (dep.contains(airport)) {
                    return true
                }
            }

            return false
        }
    }

    private fun checkFlyTime(aircraft: Aircraft, flightTask: FlightTask): Boolean {
        if (!flightTask.isFlight) {
            return true
        }

        val duration = flightTask.duration
        val maxFlyTime = aircraft.maxFlyTime
        return duration == null || maxFlyTime == null || duration <= maxFlyTime
    }

    private fun checkTime(
        aircraft: Aircraft,
        prevFlightTask: FlightTask?,
        flightTask: FlightTask,
        config: Config
    ): Boolean {
        if (prevFlightTask == null) {
            return true
        }

        val prevTime = config.timeExtractor(prevFlightTask)
        val time = config.timeExtractor(flightTask)
        if (prevTime == null || time == null) {
            return true
        }

        val lastBeginTime = flightTask.latestNormalStartTime(aircraft)
        if (time.start > lastBeginTime) {
            return false
        }

        return prevTime.start < time.start
    }

    private fun checkTimeWindow(
        aircraft: Aircraft,
        prevFlightTask: FlightTask?,
        flightTask: FlightTask,
        config: Config
    ): Boolean {
        if (prevFlightTask == null) {
            return true
        }

        val prevTime = config.timeExtractor(prevFlightTask)
        val time = config.timeExtractor(flightTask)

        if (prevTime != null && time != null) {
            return true
        }

        val prevTimeWindow = prevFlightTask.timeWindow
        val timeWindow = flightTask.timeWindow
        return if (prevTime != null && timeWindow != null) {
            val minimumDepartureTime = prevTime.start + prevFlightTask.duration(aircraft) + connectionTimeCalculator(aircraft, prevFlightTask, flightTask)
            val maximumDepartureTime = timeWindow.end - flightTask.duration(aircraft)
            minimumDepartureTime <= maximumDepartureTime
        } else if (time != null && prevTimeWindow != null) {
            prevTimeWindow.start <= time.start
        } else if (prevTimeWindow != null && timeWindow != null) {
            val minimumDepartureTime = prevTimeWindow.start + prevFlightTask.duration(aircraft) + connectionTimeCalculator(aircraft, prevFlightTask, flightTask)
            val maximumDepartureTime = timeWindow.end - flightTask.duration(aircraft)
            minimumDepartureTime <= maximumDepartureTime
        } else {
            false
        }
    }

    private fun checkRules(aircraft: Aircraft, prevFlightTask: FlightTask?, flightTask: FlightTask): Boolean {
        if (prevFlightTask == null || !prevFlightTask.isFlight || !flightTask.isFlight) {
            return true
        }
        return ruleChecker(aircraft, prevFlightTask, flightTask)
    }
}
