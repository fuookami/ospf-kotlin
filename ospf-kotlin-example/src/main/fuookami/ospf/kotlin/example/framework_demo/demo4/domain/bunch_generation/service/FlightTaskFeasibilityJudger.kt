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
 * @property aircraftUsability Map of aircraft to their usability constraints / 飞机到其可用性约束的映射
 * @property connectionTimeCalculator Function to calculate connection time between tasks / 计算任务间连接时间的函数
 * @property ruleChecker Function to check rule compliance between tasks / 检查任务间规则合规性的函数
*/
class FlightTaskFeasibilityJudger(
    val aircraftUsability: Map<Aircraft, AircraftUsability>,
    val connectionTimeCalculator: ConnectionTimeCalculator,
    val ruleChecker: RuleChecker
) {

    /**
     * Configuration for flight task feasibility checking.
     * 航班任务可行性检查的配置。
     *
     * @property checkEnabledTime Whether to check the aircraft enabled time / 是否检查飞机启用时间
     * @property timeExtractor Function to extract the time range from a flight task / 从航班任务提取时间范围的函数
     * @property departureTime Optional departure time override / 可选的出发时间覆盖
    */
    data class Config(
        val checkEnabledTime: Boolean = true,
        val timeExtractor: (FlightTask) -> TimeRange? = { it.scheduledTime },
        val departureTime: Instant? = null
    )

    /**
     * 检查给定航班任务对飞机是否可行。Checks if the given flight task is feasible for the aircraft.
     *
     * @param aircraft The aircraft to check feasibility for / 要检查可行性的飞机
     * @param prevFlightTask The previous flight task, or null if none / 前一个航班任务，如果没有则为 null
     * @param flightTask The flight task to check / 要检查的航班任务
     * @param config The feasibility check configuration / 可行性检查配置
     * @return true if the flight task is feasible for the aircraft / 如果航班任务对飞机可行则为 true
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

/**
 * Checks whether the flight task allows the given aircraft assignment.
 * 检查航班任务是否允许给定的飞机分配。
 * @param aircraft The aircraft to verify against the task / 要与任务验证的飞机
 * @param flightTask The flight task to check aircraft compatibility for / 要检查飞机兼容性的航班任务
 * @return true if the aircraft matches or aircraft change is enabled, false otherwise / 如果飞机匹配或允许换飞机则为 true，否则为 false
*/
    private fun checkAircraft(aircraft: Aircraft, flightTask: FlightTask): Boolean {
        return flightTask.aircraftChangeEnabled || flightTask.aircraft == aircraft
    }

/**
 * Checks whether the flight task allows the given aircraft type.
 * 检查航班任务是否允许给定的机型。
 * @param aircraft The aircraft whose type to verify / 要验证机型的飞机
 * @param flightTask The flight task to check aircraft type compatibility for / 要检查机型兼容性的航班任务
 * @return true if the aircraft type matches or aircraft type change is enabled, false otherwise / 如果机型匹配或允许换机型则为 true，否则为 false
*/
    private fun checkAircraftType(aircraft: Aircraft, flightTask: FlightTask): Boolean {
        return flightTask.aircraftTypeChangeEnabled || flightTask.aircraft?.type == aircraft.type
    }

/**
 * Checks whether the flight task allows the given aircraft minor type.
 * 检查航班任务是否允许给定的子机型。
 * @param aircraft The aircraft whose minor type to verify / 要验证子机型的飞机
 * @param flightTask The flight task to check aircraft minor type compatibility for / 要检查子机型兼容性的航班任务
 * @return true if the aircraft minor type matches or aircraft type change is enabled, false otherwise / 如果子机型匹配或允许换机型则为 true，否则为 false
*/
    private fun checkAircraftMinorType(aircraft: Aircraft, flightTask: FlightTask): Boolean {
        return flightTask.aircraftTypeChangeEnabled || flightTask.aircraft?.minorType == aircraft.minorType
    }

/**
 * Checks whether the aircraft capacity category matches the flight task requirement.
 * 检查飞机容量类别是否匹配航班任务要求。
 * @param aircraft The aircraft whose capacity to verify / 要验证容量的飞机
 * @param flightTask The flight task to check capacity compatibility for / 要检查容量兼容性的航班任务
 * @return true if the capacity category matches or the task is not a flight, false otherwise / 如果容量类别匹配或任务非航班则为 true，否则为 false
*/
    private fun checkAircraftCapacity(aircraft: Aircraft, flightTask: FlightTask): Boolean {
        return !flightTask.isFlight || flightTask.capacity?.let { it.category == aircraft.capacity.category } != false
    }

/**
 * Checks whether the aircraft usability constraints are satisfied for the flight task.
 * 检查航班任务是否满足飞机可用性约束。
 * @param aircraft The aircraft whose usability to check / 要检查可用性的飞机
 * @param prevFlightTask The previous flight task, or null if this is the first task / 前一个航班任务，如果是第一个任务则为 null
 * @param flightTask The flight task to check usability for / 要检查可用性的航班任务
 * @param config The feasibility check configuration / 可行性检查配置
 * @return true if the aircraft is at the required location and within the enabled time, false otherwise / 如果飞机在要求的位置且在启用时间内则为 true，否则为 false
*/
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

/**
 * Checks whether the airport connection between the previous and current flight task is valid.
 * 检查前序航班任务与当前航班任务之间的机场连接是否有效。
 * @param aircraft The aircraft performing the flight tasks / 执行航班任务的飞机
 * @param prevFlightTask The previous flight task, or null if this is the first task / 前一个航班任务，如果是第一个任务则为 null
 * @param flightTask The current flight task to check connection for / 要检查连接的当前航班任务
 * @return true if the arrival airport of the previous task matches the departure airport of the current task (including backups), false otherwise / 如果前序任务的到达机场与当前任务的出发机场匹配（含备选机场）则为 true，否则为 false
*/
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

/**
 * Checks whether the flight task duration is within the aircraft's maximum fly time limit.
 * 检查航班任务时长是否在飞机最大飞行时间限制内。
 * @param aircraft The aircraft whose maximum fly time to check against / 要检查最大飞行时间的飞机
 * @param flightTask The flight task to check fly time for / 要检查飞行时间的航班任务
 * @return true if the task is not a flight, has no duration, or the duration is within the limit, false otherwise / 如果任务非航班、无时长或时长在限制内则为 true，否则为 false
*/
    private fun checkFlyTime(aircraft: Aircraft, flightTask: FlightTask): Boolean {
        if (!flightTask.isFlight) {
            return true
        }

        val duration = flightTask.duration
        val maxFlyTime = aircraft.maxFlyTime
        return duration == null || maxFlyTime == null || duration <= maxFlyTime
    }

/**
 * Checks whether the flight task timing is valid relative to the previous task.
 * 检查航班任务时间相对于前序任务是否有效。
 * @param aircraft The aircraft performing the flight tasks / 执行航班任务的飞机
 * @param prevFlightTask The previous flight task, or null if this is the first task / 前一个航班任务，如果是第一个任务则为 null
 * @param flightTask The flight task to check timing for / 要检查时间的航班任务
 * @param config The feasibility check configuration / 可行性检查配置
 * @return true if the task starts after the previous task and before the latest normal start time, false otherwise / 如果任务在前序任务之后且在最新正常开始时间之前开始则为 true，否则为 false
*/
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

/**
 * Checks whether the time window constraints between the previous and current flight task are satisfiable.
 * 检查前序航班任务与当前航班任务之间的时间窗口约束是否可满足。
 * @param aircraft The aircraft performing the flight tasks / 执行航班任务的飞机
 * @param prevFlightTask The previous flight task, or null if this is the first task / 前一个航班任务，如果是第一个任务则为 null
 * @param flightTask The flight task to check time window for / 要检查时间窗口的航班任务
 * @param config The feasibility check configuration / 可行性检查配置
 * @return true if the time windows allow a valid schedule, false otherwise / 如果时间窗口允许有效排程则为 true，否则为 false
*/
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

/**
 * Checks whether the flight task pair complies with operational rules.
 * 检查航班任务对是否符合运营规则。
 * @param aircraft The aircraft performing the flight tasks / 执行航班任务的飞机
 * @param prevFlightTask The previous flight task, or null if this is the first task / 前一个航班任务，如果是第一个任务则为 null
 * @param flightTask The flight task to check rule compliance for / 要检查规则合规性的航班任务
 * @return true if both tasks are flights and the rule checker approves, or if either task is not a flight, false otherwise / 如果两个任务都是航班且规则检查器通过，或任一任务非航班则为 true，否则为 false
*/
    private fun checkRules(aircraft: Aircraft, prevFlightTask: FlightTask?, flightTask: FlightTask): Boolean {
        if (prevFlightTask == null || !prevFlightTask.isFlight || !flightTask.isFlight) {
            return true
        }
        return ruleChecker(aircraft, prevFlightTask, flightTask)
    }
}
