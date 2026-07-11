@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.example.framework_demo.demo4.domain.bunch_generation.service

import kotlin.time.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.*
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.rule.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.task.model.*

/**
 * Generates initial flight task bunches for each aircraft.
 * 为每架飞机生成初始航班任务束。
 *
 * @property feasibilityJudger The feasibility judger for checking task-aircraft compatibility / 用于检查任务-飞机兼容性的可行性判断器
 * @property connectionTimeCalculator Function to calculate connection time between tasks / 计算任务间连接时间的函数
 * @property minimumDepartureTimeCalculator Function to calculate minimum departure time / 计算最小出发时间的函数
 * @property costCalculator Function to calculate total cost of a bunch / 计算批次总成本的函数
*/
class InitialFlightTaskBunchGenerator(
    val feasibilityJudger: FlightTaskFeasibilityJudger,
    val connectionTimeCalculator: ConnectionTimeCalculator,
    val minimumDepartureTimeCalculator: MinimumDepartureTimeCalculator,
    val costCalculator: TotalCostCalculator
) {
    companion object {
        val config = FlightTaskFeasibilityJudger.Config(
            timeExtractor = { it.time }
        )
    }

    /**
     * Generates an initial bunch for the given aircraft.
     * 为给定飞机生成初始束。
     *
     * @param aircraft The aircraft for which to generate the bunch / 要生成束的飞机
     * @param aircraftUsability The usability constraints of the aircraft / 飞机的可用性约束
     * @param lockedFlightTasks The list of locked flight tasks that must be included / 必须包含的锁定航班任务列表
     * @param originBunch The original flight task bunch / 原始航班任务束
     * @return The generated flight task bunch, or null if generation fails / 生成的航班任务束，如果生成失败则为 null
    */
    operator fun invoke(
        aircraft: Aircraft,
        aircraftUsability: AircraftUsability,
        lockedFlightTasks: List<FlightTask>,
        originBunch: FlightTaskBunch
    ): FlightTaskBunch? {
        return softRecovery(aircraft, aircraftUsability, lockedFlightTasks, originBunch)
            ?: emptyBunch(aircraft, aircraftUsability, lockedFlightTasks)
    }

    /**
     * Generates an empty bunch with only locked tasks.
     * 生成仅包含锁定任务的空束。
     *
     * @param aircraft The aircraft for which to generate the bunch / 要生成束的飞机
     * @param aircraftUsability The usability constraints of the aircraft / 飞机的可用性约束
     * @param lockedFlightTasks The list of locked flight tasks / 锁定航班任务列表
     * @return The generated flight task bunch, or null if generation fails / 生成的航班任务束，如果生成失败则为 null
    */
    fun emptyBunch(
        aircraft: Aircraft,
        aircraftUsability: AircraftUsability,
        lockedFlightTasks: List<FlightTask>
    ): FlightTaskBunch? {
        if (lockedFlightTasks.isEmpty()) {
            return null
        }

        val flightTasks = recoveryFlightTasks(aircraft, aircraftUsability, lockedFlightTasks)
        return if (flightTasks.isEmpty()) {
            null
        } else {
            val cost = costCalculator(aircraft, flightTasks)
            if (cost == null) {
                null
            } else {
                @Suppress("UNCHECKED_CAST")
                FlightTaskBunch(aircraft, flightTasks, Int64.zero, cost as Cost<FltX>)
            }
        }
    }

/**
 * Attempts soft recovery by reusing tasks from the original bunch while preserving locked tasks.
 * 尝试软恢复，在保留锁定任务的同时复用原始束中的任务。
 * @param aircraft The aircraft for which to recover the bunch / 要恢复束的飞机
 * @param aircraftUsability The usability constraints of the aircraft / 飞机的可用性约束
 * @param lockedFlightTasks The list of locked flight tasks that must be included / 必须包含的锁定航班任务列表
 * @param originBunch The original flight task bunch to recover from / 要从中恢复的原始航班任务束
 * @return The recovered flight task bunch, or null if recovery fails / 恢复后的航班任务束，如果恢复失败则为 null
*/
    private fun softRecovery(
        aircraft: Aircraft,
        aircraftUsability: AircraftUsability,
        lockedFlightTasks: List<FlightTask>,
        originBunch: FlightTaskBunch
    ): FlightTaskBunch? {
        val flightTasks = if (lockedFlightTasks.isEmpty()) {
            recoveryFlightTasks(aircraft, aircraftUsability, originBunch.tasks)
        } else {
            var currentLocation = aircraftUsability.location
            val lastFlightTask = aircraftUsability.lastTask as? FlightTask
            var currentTime = if (lastFlightTask != null) {
                lastFlightTask.time!!.end
            } else {
                aircraftUsability.enabledTime
            }

            val softFlightTasks = ArrayList<FlightTask>()
            val insertedFlightTasks = HashSet<FlightTask>()

            for (lockedFlightTask in lockedFlightTasks) {
                val prevFlightTask = if (softFlightTasks.isEmpty()) {
                    lastFlightTask
                } else {
                    softFlightTasks.last()
                }

                var flag = false
                for (flightTask in originBunch.tasks) {
                    if (insertedFlightTasks.contains(flightTask)) {
                        continue
                    }

                    val connectionTime = if (prevFlightTask != null) {
                        connectionTimeCalculator(aircraft, prevFlightTask, flightTask)
                    } else {
                        Duration.ZERO
                    }
                    val departureTime = minimumDepartureTimeCalculator(currentTime, aircraft, flightTask, connectionTime)
                    val actualTime = TimeRange(departureTime, departureTime + connectionTime)
                    val recoveryPolicy = if (actualTime == flightTask.scheduledTime!!) {
                        FlightTaskAssignment()
                    } else {
                        FlightTaskAssignment(time = actualTime)
                    }
                    val recoveredFlightTask = if (recoveryPolicy.empty) {
                        flightTask
                    } else if (flightTask.recoveryEnabled(recoveryPolicy)) {
                        flightTask.recovery(recoveryPolicy)
                    } else {
                        null
                    }

                    if (recoveredFlightTask != null) {
                        if (currentLocation == recoveredFlightTask.dep && recoveredFlightTask.arr == lockedFlightTask.dep) {
                            val thisConnectionTime = connectionTimeCalculator(aircraft, recoveredFlightTask, lockedFlightTask)
                            val thisDepartureTime = minimumDepartureTimeCalculator(actualTime.end, aircraft, lockedFlightTask, thisConnectionTime)
                            if (thisDepartureTime == lockedFlightTask.time!!.start) {
                                flag = true
                                softFlightTasks.add(flightTask)
                                softFlightTasks.add(lockedFlightTask)
                                currentTime = lockedFlightTask.time!!.end
                                currentLocation = lockedFlightTask.arr
                                break
                            }
                        } else if (currentLocation == flightTask.dep) {
                            softFlightTasks.add(flightTask)
                            currentTime = actualTime.end
                            currentLocation = flightTask.arr
                            insertedFlightTasks.add(flightTask)
                        }
                    }
                }

                if (!flag && currentLocation == lockedFlightTask.dep) {
                    softFlightTasks.add(lockedFlightTask)
                    currentTime = lockedFlightTask.time!!.end
                    currentLocation = lockedFlightTask.arr
                }
            }

            for (flightTask in originBunch.tasks) {
                if (insertedFlightTasks.contains(flightTask)) {
                    continue
                }

                val prevFlightTask = if (softFlightTasks.isEmpty()) {
                    lastFlightTask
                } else {
                    softFlightTasks.last()
                }

                val connectionTime = if (prevFlightTask != null) {
                    connectionTimeCalculator(aircraft, prevFlightTask, flightTask)
                } else {
                    Duration.ZERO
                }
                val departureTime = minimumDepartureTimeCalculator(currentTime, aircraft, flightTask, connectionTime)
                val actualTime = TimeRange(departureTime, departureTime + flightTask.duration(aircraft))
                val recoveryPolicy = if (actualTime == flightTask.scheduledTime!!) {
                    FlightTaskAssignment()
                } else {
                    FlightTaskAssignment(time = actualTime)
                }
                val recoveredFlightTask = if (recoveryPolicy.empty) {
                    flightTask
                } else if (flightTask.recoveryEnabled(recoveryPolicy)) {
                    flightTask.recovery(recoveryPolicy)
                } else {
                    null
                }

                if (recoveredFlightTask != null && currentLocation == flightTask.dep) {
                    softFlightTasks.add(flightTask)
                    insertedFlightTasks.add(flightTask)
                    currentTime = actualTime.end
                    currentLocation = flightTask.arr
                }
            }

            recoveryFlightTasks(aircraft, aircraftUsability, softFlightTasks)
        }

        if (flightTasks.isEmpty()) {
            return null
        }

        val cost = costCalculator(aircraft, flightTasks)
        return if (cost == null) {
            null
        } else {
            @Suppress("UNCHECKED_CAST")
            FlightTaskBunch(aircraft, flightTasks, Int64.zero, cost as Cost<FltX>)
        }
    }

/**
 * Recovers flight tasks by recalculating departure times and verifying feasibility for each task.
 * 通过重新计算出发时间并验证每个任务的可行性来恢复航班任务。
 * @param aircraft The aircraft for which to recover tasks / 要恢复任务的飞机
 * @param aircraftUsability The usability constraints of the aircraft / 飞机的可用性约束
 * @param lockedFlightTasks The list of flight tasks to recover / 要恢复的航班任务列表
 * @return The list of recovered flight tasks that passed feasibility checks / 通过可行性检查的恢复后航班任务列表
*/
    private fun recoveryFlightTasks(
        aircraft: Aircraft,
        aircraftUsability: AircraftUsability,
        lockedFlightTasks: List<FlightTask>
    ): List<FlightTask> {
        val flightTasks = ArrayList<FlightTask>()
        if (lockedFlightTasks.isEmpty()) {
            return flightTasks
        }

        val lastFlightTask = aircraftUsability.lastTask as? FlightTask
        var time = if (lastFlightTask != null) {
            lastFlightTask.time!!.end
        } else {
            aircraftUsability.enabledTime
        }
        for (i in lockedFlightTasks.indices) {
            val flightTask = lockedFlightTasks[i]
            val prevFlightTask = if (flightTasks.isEmpty()) {
                lastFlightTask
            } else {
                flightTasks.last()
            }

            val connectionTime = if (prevFlightTask != null) {
                connectionTimeCalculator(aircraft, prevFlightTask, flightTask)
            } else {
                Duration.ZERO
            }
            time = minimumDepartureTimeCalculator(time, aircraft, flightTask, connectionTime)
            val recoveredTime = TimeRange(time, time + flightTask.duration(aircraft))
            val recoveryPolicy = if (recoveredTime == flightTask.scheduledTime!!) {
                FlightTaskAssignment()
            } else {
                FlightTaskAssignment(time = recoveredTime)
            }

            val recoveredFlightTask = if (recoveryPolicy.empty) {
                flightTask
            } else if (flightTask.recoveryEnabled(recoveryPolicy)) {
                flightTask.recovery(recoveryPolicy)
            } else {
                null
            }

            if (recoveredFlightTask != null) {
                if (!feasibilityJudger(aircraft, prevFlightTask, recoveredFlightTask, config)) {
                    continue
                }
                flightTasks.add(recoveredFlightTask)
                time += recoveredFlightTask.duration(aircraft)
            }
        }

        return flightTasks
    }
}
