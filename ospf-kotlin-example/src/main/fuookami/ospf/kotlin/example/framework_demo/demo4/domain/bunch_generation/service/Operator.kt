@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.example.framework_demo.demo4.domain.bunch_generation.service

import kotlin.time.Duration
import kotlin.time.Instant
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.task.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.*

/** Checks if a rule allows the connection between two tasks for an aircraft. */
typealias RuleChecker = (Aircraft, FlightTask?, FlightTask) -> Boolean

/** Calculates the connection time between two tasks for an aircraft. */
typealias ConnectionTimeCalculator = (Aircraft, FlightTask, FlightTask?) -> Duration

/** Calculates the minimum departure time given an arrival time, connection time, and task. */
typealias MinimumDepartureTimeCalculator = (Instant, Aircraft, FlightTask, Duration) -> Instant

/** Calculates the cost of assigning a task to an aircraft, or null if not feasible. */
typealias CostCalculator = (Aircraft, FlightTask?, FlightTask, FlightHour, FlightCycle) -> Cost<*>?

/** Calculates the total cost of a sequence of tasks for an aircraft, or null if not feasible. */
typealias TotalCostCalculator = (Aircraft, List<FlightTask>) -> Cost<*>?
