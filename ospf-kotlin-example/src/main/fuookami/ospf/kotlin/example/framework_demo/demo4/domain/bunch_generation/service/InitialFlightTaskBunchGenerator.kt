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
 * 为每架飞机生成初始航班任务束。Generates initial flight task bunches for each aircraft.
 *
 * @property feasibilityJudger 参数。
 * @property connectionTimeCalculator 参数。
 * @property minimumDepartureTimeCalculator 参数。
 * @property costCalculator 参数。
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
 *
     * @param aircraft 参数。
     * @param aircraftUsability 参数。
     * @param lockedFlightTasks 参数。
     * @param originBunch 参数。
     * @return 返回结果。
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
 *
     * @param aircraft 参数。
     * @param aircraftUsability 参数。
     * @param lockedFlightTasks 参数。
     * @return 返回结果。
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
                FlightTaskBunch(aircraft, flightTasks, Int64.zero, cost as Cost<FltX>)
            }
        }
    }

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
            FlightTaskBunch(aircraft, flightTasks, Int64.zero, cost as Cost<FltX>)
        }
    }

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
