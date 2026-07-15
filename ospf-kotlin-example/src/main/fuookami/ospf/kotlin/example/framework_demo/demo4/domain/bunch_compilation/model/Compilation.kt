@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.example.framework_demo.demo4.domain.bunch_compilation.model

import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.bunch_compilation.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.task.model.*

/** 专门用于航班任务类型的批次编译的类型别名。Type alias for bunch compilation specialized with flight task types. */
typealias Compilation = BunchCompilation<FlightTaskBunch, FltX, FlightTask, Aircraft, FlightTaskAssignment>

/** 专门用于航班任务类型的任务时间的类型别名。Type alias for task time specialized with flight task types. */
typealias TaskTime = BunchSchedulingTaskTime<FlightTaskBunch, FltX, FlightTask, Aircraft, FlightTaskAssignment>
