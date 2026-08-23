@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.example.framework_demo.demo4.domain.bunch_generation.service

import kotlin.time.Instant
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.*
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.rule.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.task.model.*

/**
 * The first failed stage in flight-task feasibility checking.
 * 航班任务可行性检查中首个失败阶段。
 */
enum class FlightTaskFeasibilityFailureStage(val message: String) {
    Aircraft("飞机不匹配 / aircraft mismatch"),
    AircraftType("机型不匹配 / aircraft type mismatch"),
    AircraftMinorType("子机型不匹配 / aircraft minor type mismatch"),
    Capacity("容量类别不匹配 / aircraft capacity mismatch"),
    Usability("飞机可用性不满足 / aircraft usability is not satisfied"),
    AirportConnection("机场连接不满足 / airport connection is not satisfied"),
    FlyTime("飞行时长超限 / flight duration exceeds the limit"),
    Time("任务时间不满足 / task time is not feasible"),
    TimeWindow("时间窗不满足 / task time window is not feasible"),
    Rules("运营规则不满足 / operational rule is not satisfied")
}

/**
 * Structured feasibility result for a flight task transition.
 * 航班任务转移的结构化可行性结果。
 *
 * @property aircraft 被检查的飞机 / The aircraft being checked.
 * @property previousFlightTask 前一任务 / The previous task.
 * @property flightTask 当前任务 / The current task.
 * @property feasible 是否可行 / Whether the transition is feasible.
 * @property failureStage 首个失败阶段 / The first failed stage.
 * @property message 失败原因 / The failure message.
 */
data class FlightTaskFeasibilityDiagnostic(
    val aircraft: Aircraft,
    val previousFlightTask: FlightTask?,
    val flightTask: FlightTask,
    val feasible: Boolean,
    val failureStage: FlightTaskFeasibilityFailureStage? = null,
    val message: String? = null
)

/**
 * 检查给定前一任务时航班任务对飞机是否可行。Checks if a flight task is feasible for an aircraft given the previous task.
 *
 * @property aircraftUsability 飞机到其可用性约束的映射 / Map of aircraft to their usability constraints
 * @property connectionTimeCalculator 计算任务间连接时间的函数 / Function to calculate connection time between tasks
 * @property ruleChecker 检查任务间规则合规性的函数 / Function to check rule compliance between tasks
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
     * @property checkEnabledTime 是否检查飞机启用时间 / Whether to check the aircraft enabled time
     * @property timeExtractor 从航班任务提取时间范围的函数 / Function to extract the time range from a flight task
     * @property departureTime 可选的出发时间覆盖 / Optional departure time override
    */
    data class Config(
        val checkEnabledTime: Boolean = true,
        val timeExtractor: (FlightTask) -> TimeRange? = { it.scheduledTime },
        val departureTime: Instant? = null
    )

    /**
     * 检查给定航班任务对飞机是否可行。Checks if the given flight task is feasible for the aircraft.
     *
     * @param aircraft 要检查可行性的飞机 / The aircraft to check feasibility for
     * @param prevFlightTask 前一个航班任务，如果没有则为 null / The previous flight task, or null if none
     * @param flightTask 要检查的航班任务 / The flight task to check
     * @param config 可行性检查配置 / The feasibility check configuration
     * @return 如果航班任务对飞机可行则为 true / true if the flight task is feasible for the aircraft
    */
    operator fun invoke(
        aircraft: Aircraft,
        prevFlightTask: FlightTask?,
        flightTask: FlightTask,
        config: Config = Config()
    ): Boolean {
        return diagnose(aircraft, prevFlightTask, flightTask, config).feasible
    }

    /**
     * Checks feasibility and returns the first failed stage with context.
     * 检查可行性并返回包含上下文的首个失败阶段。
     *
     * @param aircraft 要检查可行性的飞机 / The aircraft to check.
     * @param prevFlightTask 前一个航班任务 / The previous flight task.
     * @param flightTask 当前航班任务 / The current flight task.
     * @param config 可行性检查配置 / Feasibility checking configuration.
     * @return 结构化可行性诊断 / Structured feasibility diagnostic.
     */
    fun diagnose(
        aircraft: Aircraft,
        prevFlightTask: FlightTask?,
        flightTask: FlightTask,
        config: Config = Config()
    ): FlightTaskFeasibilityDiagnostic {
        val checks = listOf(
            FlightTaskFeasibilityFailureStage.Aircraft to { checkAircraft(aircraft, flightTask) },
            FlightTaskFeasibilityFailureStage.AircraftType to { checkAircraftType(aircraft, flightTask) },
            FlightTaskFeasibilityFailureStage.AircraftMinorType to { checkAircraftMinorType(aircraft, flightTask) },
            FlightTaskFeasibilityFailureStage.Capacity to { checkAircraftCapacity(aircraft, flightTask) },
            FlightTaskFeasibilityFailureStage.Usability to {
                checkAircraftUsability(aircraft, prevFlightTask, flightTask, config)
            },
            FlightTaskFeasibilityFailureStage.AirportConnection to {
                checkAirportConnection(aircraft, prevFlightTask, flightTask)
            },
            FlightTaskFeasibilityFailureStage.FlyTime to { checkFlyTime(aircraft, flightTask) },
            FlightTaskFeasibilityFailureStage.Time to {
                checkTime(aircraft, prevFlightTask, flightTask, config)
            },
            FlightTaskFeasibilityFailureStage.TimeWindow to {
                checkTimeWindow(aircraft, prevFlightTask, flightTask, config)
            },
            FlightTaskFeasibilityFailureStage.Rules to {
                checkRules(aircraft, prevFlightTask, flightTask)
            }
        )

        for ((stage, check) in checks) {
            if (!check()) {
                return FlightTaskFeasibilityDiagnostic(
                    aircraft = aircraft,
                    previousFlightTask = prevFlightTask,
                    flightTask = flightTask,
                    feasible = false,
                    failureStage = stage,
                    message = stage.message
                )
            }
        }

        return FlightTaskFeasibilityDiagnostic(
            aircraft = aircraft,
            previousFlightTask = prevFlightTask,
            flightTask = flightTask,
            feasible = true
        )
    }

/**
 * Checks whether the flight task allows the given aircraft assignment.
 * 检查航班任务是否允许给定的飞机分配。
 * @param aircraft 要与任务验证的飞机 / The aircraft to verify against the task
 * @param flightTask 要检查飞机兼容性的航班任务 / The flight task to check aircraft compatibility for
 * @return 如果飞机匹配或允许换飞机则为 true，否则为 false / true if the aircraft matches or aircraft change is enabled, false otherwise
*/
    private fun checkAircraft(aircraft: Aircraft, flightTask: FlightTask): Boolean {
        return flightTask.aircraftChangeEnabled || flightTask.aircraft == aircraft
    }

/**
 * Checks whether the flight task allows the given aircraft type.
 * 检查航班任务是否允许给定的机型。
 * @param aircraft 要验证机型的飞机 / The aircraft whose type to verify
 * @param flightTask 要检查机型兼容性的航班任务 / The flight task to check aircraft type compatibility for
 * @return 如果机型匹配或允许换机型则为 true，否则为 false / true if the aircraft type matches or aircraft type change is enabled, false otherwise
*/
    private fun checkAircraftType(aircraft: Aircraft, flightTask: FlightTask): Boolean {
        return flightTask.aircraftTypeChangeEnabled || flightTask.aircraft?.type == aircraft.type
    }

/**
 * Checks whether the flight task allows the given aircraft minor type.
 * 检查航班任务是否允许给定的子机型。
 * @param aircraft 要验证子机型的飞机 / The aircraft whose minor type to verify
 * @param flightTask 要检查子机型兼容性的航班任务 / The flight task to check aircraft minor type compatibility for
 * @return 如果子机型匹配或允许换机型则为 true，否则为 false / true if the aircraft minor type matches or aircraft type change is enabled, false otherwise
*/
    private fun checkAircraftMinorType(aircraft: Aircraft, flightTask: FlightTask): Boolean {
        return flightTask.aircraftTypeChangeEnabled || flightTask.aircraft?.minorType == aircraft.minorType
    }

/**
 * Checks whether the aircraft capacity category matches the flight task requirement.
 * 检查飞机容量类别是否匹配航班任务要求。
 * @param aircraft 要验证容量的飞机 / The aircraft whose capacity to verify
 * @param flightTask 要检查容量兼容性的航班任务 / The flight task to check capacity compatibility for
 * @return 如果容量类别匹配或任务非航班则为 true，否则为 false / true if the capacity category matches or the task is not a flight, false otherwise
*/
    private fun checkAircraftCapacity(aircraft: Aircraft, flightTask: FlightTask): Boolean {
        return !flightTask.isFlight || flightTask.capacity?.let { it.category == aircraft.capacity.category } != false
    }

/**
 * Checks whether the aircraft usability constraints are satisfied for the flight task.
 * 检查航班任务是否满足飞机可用性约束。
 * @param aircraft 要检查可用性的飞机 / The aircraft whose usability to check
 * @param prevFlightTask 前一个航班任务，如果是第一个任务则为 null / The previous flight task, or null if this is the first task
 * @param flightTask 要检查可用性的航班任务 / The flight task to check usability for
 * @param config 可行性检查配置 / The feasibility check configuration
 * @return 如果飞机在要求的位置且在启用时间内则为 true，否则为 false / true if the aircraft is at the required location and within the enabled time, false otherwise
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
 * @param aircraft 执行航班任务的飞机 / The aircraft performing the flight tasks
 * @param prevFlightTask 前一个航班任务，如果是第一个任务则为 null / The previous flight task, or null if this is the first task
 * @param flightTask 要检查连接的当前航班任务 / The current flight task to check connection for
 * @return 如果前序任务的到达机场与当前任务的出发机场匹配（含备选机场）则为 true，否则为 false / true if the arrival airport of the previous task matches the departure airport of the current task (including backups), false otherwise
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
 * @param aircraft 要检查最大飞行时间的飞机 / The aircraft whose maximum fly time to check against
 * @param flightTask 要检查飞行时间的航班任务 / The flight task to check fly time for
 * @return 如果任务非航班、无时长或时长在限制内则为 true，否则为 false / true if the task is not a flight, has no duration, or the duration is within the limit, false otherwise
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
 * @param aircraft 执行航班任务的飞机 / The aircraft performing the flight tasks
 * @param prevFlightTask 前一个航班任务，如果是第一个任务则为 null / The previous flight task, or null if this is the first task
 * @param flightTask 要检查时间的航班任务 / The flight task to check timing for
 * @param config 可行性检查配置 / The feasibility check configuration
 * @return 如果任务在前序任务之后且在最新正常开始时间之前开始则为 true，否则为 false / true if the task starts after the previous task and before the latest normal start time, false otherwise
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
 * @param aircraft 执行航班任务的飞机 / The aircraft performing the flight tasks
 * @param prevFlightTask 前一个航班任务，如果是第一个任务则为 null / The previous flight task, or null if this is the first task
 * @param flightTask 要检查时间窗口的航班任务 / The flight task to check time window for
 * @param config 可行性检查配置 / The feasibility check configuration
 * @return 如果时间窗口允许有效排程则为 true，否则为 false / true if the time windows allow a valid schedule, false otherwise
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
 * @param aircraft 执行航班任务的飞机 / The aircraft performing the flight tasks
 * @param prevFlightTask 前一个航班任务，如果是第一个任务则为 null / The previous flight task, or null if this is the first task
 * @param flightTask 要检查规则合规性的航班任务 / The flight task to check rule compliance for
 * @return 如果两个任务都是航班且规则检查器通过，或任一任务非航班则为 true，否则为 false / true if both tasks are flights and the rule checker approves, or if either task is not a flight, false otherwise
*/
    private fun checkRules(aircraft: Aircraft, prevFlightTask: FlightTask?, flightTask: FlightTask): Boolean {
        if (prevFlightTask == null || !prevFlightTask.isFlight || !flightTask.isFlight) {
            return true
        }
        return ruleChecker(aircraft, prevFlightTask, flightTask)
    }
}
