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
 * @property aircraft The assigned aircraft / 分配的飞机
 * @property time The time range of the bunch / 任务束的时间范围
 * @property dep The departure airport / 出发机场
 * @property arr The arrival airport / 到达机场
 * @property tasks The list of flight tasks / 航班任务列表
 * @property cost The cost of the bunch / 任务束的成本
 * @property iteration The iteration number / 迭代号
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
     * @param task The flight task to check / 要检查的航班任务
     * @return true if the task is contained, false otherwise / 如果包含该任务则为true，否则为false
    */
    fun contains(task: FlightTask): Boolean {
        return keys.contains(task.key)
    }

    /**
     * 检查此任务束是否包含两个连续任务。Checks whether this bunch contains two consecutive tasks.
     *
     * @param prevFlightTask The preceding flight task / 前一个航班任务
     * @param succFlightTask The succeeding flight task / 后一个航班任务
     * @return true if both tasks are consecutive in the bunch, false otherwise / 如果两个任务在任务束中连续则为true，否则为false
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
     * @param originTask The original flight task to look up / 要查找的原始航班任务
     * @return The recovered version of the task, or null if not found / 任务的恢复版本，未找到则为null
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
     * @param airport The airport to check / 要检查的机场
     * @param timeWindow The time window to check / 要检查的时间窗口
     * @return true if any task arrives at the airport within the window, false otherwise / 如果有任务在窗口内到达机场则为true，否则为false
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
     * @param airport The airport to check / 要检查的机场
     * @param timeWindow The time window to check / 要检查的时间窗口
     * @return true if any task departs from the airport within the window, false otherwise / 如果有任务在窗口内从机场出发则为true，否则为false
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
     * @param airport The airport to check / 要检查的机场
     * @param timeWindow The time window to check / 要检查的时间窗口
     * @return true if the aircraft is located at the airport within the window, false otherwise / 如果飞机在窗口内位于机场则为true，否则为false
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
