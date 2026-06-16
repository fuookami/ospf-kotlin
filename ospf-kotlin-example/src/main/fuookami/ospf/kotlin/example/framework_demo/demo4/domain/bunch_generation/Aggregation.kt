@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.example.framework_demo.demo4.domain.bunch_generation

import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.task.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.bunch_generation.model.*

/** Aggregation of bunch generation domain objects. */
class Aggregation(
    val graphs: Map<Aircraft, Graph>,
    val reverse: FlightTaskReverse,
    val initialFlightBunches: List<FlightTaskBunch>
)
