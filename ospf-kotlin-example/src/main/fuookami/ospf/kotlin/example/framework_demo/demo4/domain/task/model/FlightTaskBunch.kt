@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.example.framework_demo.demo4.domain.task.model

import kotlin.time.Instant
import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.*
import fuookami.ospf.kotlin.example.framework_demo.demo4.infrastructure.*

/**
 * 分配给单架飞机的航班任务束（具有成本和时间跟踪）。A bunch of flight tasks assigned to a single aircraft with cost and time tracking.
 *
 * @property aircraft 分配的飞机 / The assigned aircraft
 * @property time 任务束的时间范围 / The time range of the bunch
 * @property dep 出发机场 / The departure airport
 * @property arr 到达机场 / The arrival airport
 * @property tasks 航班任务列表 / The list of flight tasks
 * @property cost 任务束的成本 / The cost of the bunch
 * @property iteration 迭代号 / The iteration number
*/
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

    /**
     * 检查此任务束是否包含给定任务。Checks whether this bunch contains the given task.
     *
     * @param task 要检查的航班任务 / The flight task to check
     * @return 如果包含该任务则为true，否则为false / true if the task is contained, false otherwise
    */
    fun contains(task: FlightTask): Boolean {
        return keys.contains(task.key)
    }

    /**
     * 检查此任务束是否包含两个连续任务。Checks whether this bunch contains two consecutive tasks.
     *
     * @param prevFlightTask 前一个航班任务 / The preceding flight task
     * @param succFlightTask 后一个航班任务 / The succeeding flight task
     * @return 如果两个任务在任务束中连续则为true，否则为false / true if both tasks are consecutive in the bunch, false otherwise
    */
    fun contains(prevFlightTask: FlightTask, succFlightTask: FlightTask): Boolean {
        val prevTask = keys[prevFlightTask.key]
        val succTask = keys[succFlightTask.key]
        return if (prevTask != null && succTask != null) {
            (succTask - prevTask) == 1
        } else {
            false
        }
    }

    /**
     * 返回此任务束中给定原始任务的恢复版本。Returns the recovered version of the given origin task within this bunch.
     *
     * @param originTask 要查找的原始航班任务 / The original flight task to look up
     * @return 任务的恢复版本，未找到则为null / The recovered version of the task, or null if not found
    */
    fun get(originTask: FlightTask): FlightTask? {
        val task = keys[originTask.key]
        return if (task != null) {
            assert(tasks[task].originTask == originTask)
            tasks[task]
        } else {
            null
        }
    }

    /**
     * 检查此任务束中是否有任务在时间窗口内到达机场。Checks whether any task in this bunch arrives at the airport within the time window.
     *
     * @param airport 要检查的机场 / The airport to check
     * @param timeWindow 要检查的时间窗口 / The time window to check
     * @return 如果有任务在窗口内到达机场则为true，否则为false / true if any task arrives at the airport within the window, false otherwise
    */
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

    /**
     * 检查此任务束中是否有任务在时间窗口内从机场出发。Checks whether any task in this bunch departs from the airport within the time window.
     *
     * @param airport 要检查的机场 / The airport to check
     * @param timeWindow 要检查的时间窗口 / The time window to check
     * @return 如果有任务在窗口内从机场出发则为true，否则为false / true if any task departs from the airport within the window, false otherwise
    */
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

    /**
     * 检查飞机是否在时间窗口内位于机场。Checks whether the aircraft is located at the airport within the time window.
     *
     * @param airport 要检查的机场 / The airport to check
     * @param timeWindow 要检查的时间窗口 / The time window to check
     * @return 如果飞机在窗口内位于机场则为true，否则为false / true if the aircraft is located at the airport within the window, false otherwise
    */
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
