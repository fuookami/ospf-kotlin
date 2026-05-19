@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.example.framework_demo.demo4.domain.bunch_compilation.model

import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.bunch_compilation.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.task.model.*

typealias Compilation = BunchCompilation<FlightTaskBunch, FlightTask, Aircraft, FlightTaskAssignment>
typealias TaskTime = BunchSchedulingTaskTime<FlightTaskBunch, FlightTask, Aircraft, FlightTaskAssignment>