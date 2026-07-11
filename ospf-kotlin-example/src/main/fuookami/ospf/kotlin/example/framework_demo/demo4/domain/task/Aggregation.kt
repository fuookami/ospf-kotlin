@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.example.framework_demo.demo4.domain.task

import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.*
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.task.model.*

/**
 * 组合机场、飞机、航段和原始批次的任务域聚合。Aggregation for the task domain combining airports, aircraft, flight legs, and origin bunches.
 *
 * @property timeWindow The time window for the scheduling period / 调度周期的时间窗口
 * @property airports The list of airports / 机场列表
 * @property aircrafts The list of aircraft / 飞机列表
 * @property aircraftUsability The map of aircraft to their usability / 飞机到其可用性的映射
 * @property legs The list of flight legs / 航段列表
 * @property maintenances The list of maintenance tasks / 维护任务列表
 * @property aogs The list of AOG events / AOG事件列表
 * @property transferFlights The list of transfer flights / 中转航班列表
 * @property originBunches The list of original flight task bunches / 原始航班任务束列表
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
     * @param aircraft The aircraft to check / 要检查的飞机
     * @param recoveryPolicy The recovery policy assignment / 恢复策略分配
     * @param task The optional flight task / 可选的航班任务
     * @return true if the aircraft is enabled, false otherwise / 如果飞机启用则为true，否则为false
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
