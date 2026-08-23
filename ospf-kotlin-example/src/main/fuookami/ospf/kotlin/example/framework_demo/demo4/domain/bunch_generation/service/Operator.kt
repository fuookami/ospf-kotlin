@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.example.framework_demo.demo4.domain.bunch_generation.service

import kotlin.time.Duration
import kotlin.time.Instant
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.*
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.task.model.*

/** 检查规则是否允许飞机两个任务之间的连接。Checks if a rule allows the connection between two tasks for an aircraft. */
fun interface RuleChecker {
    operator fun invoke(aircraft: Aircraft, prevTask: FlightTask?, task: FlightTask): Boolean
}

/** 计算飞机两个任务之间的连接时间。Calculates the connection time between two tasks for an aircraft. */
fun interface ConnectionTimeCalculator {
    operator fun invoke(aircraft: Aircraft, prevTask: FlightTask, task: FlightTask?): Duration
}

/** 给定到达时间、连接时间和任务计算最早出发时间。Calculates the minimum departure time given an arrival time, connection time, and task. */
fun interface MinimumDepartureTimeCalculator {
    operator fun invoke(arrivalTime: Instant, aircraft: Aircraft, task: FlightTask, connectionTime: Duration): Instant
}

/** 计算将任务分配给飞机的成本，如果不可行则返回 null。Calculates the cost of assigning a task to an aircraft, or null if not feasible. */
fun interface CostCalculator {
    operator fun invoke(aircraft: Aircraft, prevTask: FlightTask?, task: FlightTask, flightHour: FlightHour, flightCycle: FlightCycle): Cost<*>?
}

/** 计算飞机任务序列的总成本，如果不可行则返回 null。Calculates the total cost of a sequence of tasks for an aircraft, or null if not feasible. */
fun interface TotalCostCalculator {
    operator fun invoke(aircraft: Aircraft, tasks: List<FlightTask>): Cost<*>?
}
