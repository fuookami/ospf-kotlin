@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.example.framework_demo.demo4.domain.bunch_generation

import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.bunch_generation.service.RouteGraphDiagnostics
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.bunch_generation.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.task.model.*

/**
 * 批次生成域对象聚合。Aggregation of bunch generation domain objects.
 *
 * @property graphs 按飞机索引的路线图 / route graphs keyed by aircraft
 * @property reverse 航班任务可反转对 / flight task reverse pairs
 * @property initialFlightBunches 初始航班任务束 / initial flight task bunches
*/
class Aggregation(
    val graphs: Map<Aircraft, Graph>,
    val reverse: FlightTaskReverse,
    val initialFlightBunches: List<FlightTaskBunch>,
    val routeGraphDiagnostics: Map<Aircraft, RouteGraphDiagnostics> = emptyMap()
)
