@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.example.framework_demo.demo4.domain.bunch_generation

import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.bunch_generation.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.task.model.*

/**
 * 批次生成域对象聚合。Aggregation of bunch generation domain objects.
 *
 * @property graphs route graphs keyed by aircraft / 按飞机索引的路线图
 * @property reverse flight task reverse pairs / 航班任务可反转对
 * @property initialFlightBunches initial flight task bunches / 初始航班任务束
*/
class Aggregation(
    val graphs: Map<Aircraft, Graph>,
    val reverse: FlightTaskReverse,
    val initialFlightBunches: List<FlightTaskBunch>
)
