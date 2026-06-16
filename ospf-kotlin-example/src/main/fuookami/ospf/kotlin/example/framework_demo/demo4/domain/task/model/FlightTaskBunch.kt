@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.example.framework_demo.demo4.domain.task.model

import kotlin.time.Instant

import fuookami.ospf.kotlin.example.framework_demo.demo4.infrastructure.*

import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.math.algebra.number.*

import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.*

/** A bunch of flight tasks assigned to a single aircraft with cost and time tracking. */
class FlightTaskBunch(
    val aircraft: Aircraft,
    time: TimeRange,
    val dep: Airport,
    val arr: Airport,
    tasks: List<FlightTask>,
    cost: Cost<FltX>,
    iteration: Int64
) : AbstractTaskBunch<FlightTask, Aircraft, FlightTaskAssignment, FltX>(
    executor = aircraft,
    initialUsability = aircraft.usability,
    tasks = tasks,
    cost = cost,
    iteration = iteration
) {
    val aircraftChange: UInt64 by lazy { UInt64(tasks.count { it.aircraftChanged }.toULong()) }

    constructor(
        timeWindow: TimeWindow<*>,
        aircraft: Aircraft,
        airport: Airport,
        time: Instant,
        iteration: Int64
    ) : this(
        aircraft = aircraft,
        time = TimeRange(time, time + timeWindow.interval),
        dep = airport,
        arr = airport,
        tasks = emptyList(),
        iteration = iteration,
        cost = Cost(FltX)
    )

    constructor(
        aircraft: Aircraft,
        tasks: List<FlightTask>,
        iteration: Int64,
        cost: Cost<FltX> = Cost(FltX)
    ) : this(
        aircraft = aircraft,
        time = TimeRange(tasks.first().time!!.start, tasks.last().time!!.end),
        dep = tasks.first().dep,
        arr = tasks.last().arr,
        tasks = tasks,
        iteration = iteration,
        cost = cost
    )

    /** Checks whether this bunch contains the given task. */
    fun contains(task: FlightTask): Boolean {
        return keys.contains(task.key)
    }

    /** Checks whether this bunch contains two consecutive tasks. */
    fun contains(prevFlightTask: FlightTask, succFlightTask: FlightTask): Boolean {
        val prevTask = keys[prevFlightTask.key]
        val succTask = keys[succFlightTask.key]
        return if (prevTask != null && succTask != null) {
            (succTask - prevTask) == 1
        } else {
            false
        }
    }

    /** Returns the recovered version of the given origin task within this bunch. */
    fun get(originTask: FlightTask): FlightTask? {
        val task = keys[originTask.key]
        return if (task != null) {
            assert(tasks[task].originTask == originTask)
            tasks[task]
        } else {
            null
        }
    }

    /** Checks whether any task in this bunch arrives at the airport within the time window. */
    fun arrivedWhen(airport: Airport, timeWindow: TimeRange): Boolean {
        if (!time.withIntersection(timeWindow)) {
            return false
        }

        for (task in tasks) {
            if (task.arrivedWhen(airport, timeWindow)) {
                return true
            }
            if (task.time!!.end >= timeWindow.end) {
                break
            }
        }
        return false
    }

    /** Checks whether any task in this bunch departs from the airport within the time window. */
    fun departedWhen(airport: Airport, timeWindow: TimeRange): Boolean {
        if (!time.withIntersection(timeWindow)) {
            return false
        }

        for (task in tasks) {
            if (task.departedWhen(airport, timeWindow)) {
                return true
            }
            if (task.time!!.end >= timeWindow.end) {
                break
            }
        }
        return false
    }

    /** Checks whether the aircraft is located at the airport within the time window. */
    fun locatedWhen(airport: Airport, timeWindow: TimeRange): Boolean {
        if (tasks.first().departedWhen(airport, timeWindow)) {
            return true
        }
        if (tasks.last().arrivedWhen(airport, timeWindow)) {
            return true
        }
        if (!empty) {
            for (i in 1 until tasks.size) {
                if (tasks[i].locatedWhen(tasks[i - 1], airport, timeWindow)) {
                    return true
                }
            }
        }
        return false
    }

    override fun toString() = "${aircraft.regNo}, ${dep.icao} - ${arr.icao}, ${time.start.toShortString()} - ${time.end.toShortString()}, ${tasks.size} tasks"
}
