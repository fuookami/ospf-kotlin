@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.example.framework_demo.demo4.domain.task

import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.task.model.*

import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.*

/** Aggregation for the task domain combining airports, aircraft, flight legs, and origin bunches. */
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

    /** Checks whether the aircraft is enabled for the given recovery policy and task. */
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
