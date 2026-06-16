@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.example.framework_demo.demo4.domain.bunch_compilation.model

import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.task.model.*

import fuookami.ospf.kotlin.math.algebra.number.FltX

import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.bunch_compilation.model.*

/** Type alias for bunch compilation specialized with flight task types. */
typealias Compilation = BunchCompilation<FlightTaskBunch, FltX, FlightTask, Aircraft, FlightTaskAssignment>

/** Type alias for task time specialized with flight task types. */
typealias TaskTime = BunchSchedulingTaskTime<FlightTaskBunch, FltX, FlightTask, Aircraft, FlightTaskAssignment>
