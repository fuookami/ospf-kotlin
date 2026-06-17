@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.example.framework_demo.demo4.domain.bunch_generation

import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.bunch_generation.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.task.model.*

/**
 * 批次生成域对象聚合。Aggregation of bunch generation domain objects.
 *
 * @property graphs 参数。
 * @property reverse 参数。
 * @property initialFlightBunches 参数。
 */
class Aggregation(
    val graphs: Map<Aircraft, Graph>,
    val reverse: FlightTaskReverse,
    val initialFlightBunches: List<FlightTaskBunch>
)
