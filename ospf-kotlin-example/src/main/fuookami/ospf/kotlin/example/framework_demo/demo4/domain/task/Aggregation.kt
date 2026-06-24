@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.example.framework_demo.demo4.domain.task

import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.*
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.task.model.*

/**
 * 组合机场、飞机、航段和原始批次的任务域聚合。Aggregation for the task domain combining airports, aircraft, flight legs, and origin bunches.
 *
 * @property timeWindow 参数。
 * @property airports 参数。
 * @property aircrafts 参数。
 * @property aircraftUsability 参数。
 * @property legs 参数。
 * @property maintenances 参数。
 * @property aogs 参数。
 * @property transferFlights 参数。
 * @property originBunches 参数。
 */
class Aggregation(
    val timeWindow: TimeWindow<*>,
    val airports: List<Airport>,
    val aircrafts: List<Aircraft>,
    val aircraftUsability: Map<Aircraft, AircraftUsability>,

    val legs: List<FlightLeg>,
    val maintenances: List<Maintenance>,
    val aogs: List<AOG>,
    val transferFlights: List<Transfer>,

    val originBunches: List<FlightTaskBunch>
) {
    val flightTasks: List<FlightTask>

    init {
        val flightTasks = ArrayList<FlightTask>()
        flightTasks.addAll(legs)
        flightTasks.addAll(maintenances)
        flightTasks.addAll(aogs)
        flightTasks.addAll(transferFlights)
        this.flightTasks = flightTasks
    }

    /**
     * 检查飞机是否对给定的恢复策略和任务启用。Checks whether the aircraft is enabled for the given recovery policy and task.
 *
     * @param aircraft 参数。
     * @param recoveryPolicy 参数。
     * @param task 参数。
     * @return 返回结果。
     */
    fun enabled(
        aircraft: Aircraft,
        recoveryPolicy: FlightTaskAssignment,
        task: FlightTask? = null
    ): Boolean {
        return aircraftUsability[aircraft]?.enabledTime?.let { enabledTime ->
            (recoveryPolicy.time?.end ?: task?.time?.end)?.let { time ->
                enabledTime <= time
            } == true
        } ?: false
    }
}
