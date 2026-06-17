package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.payload_maximization

import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.Position

/**
 * 聚合飞机模型和载荷数据用于载荷最大化优化。Aggregates aircraft model and payload data for payload maximization optimization.
 *
 * @property internal val aircraftModel 参数。
 * @property internal val payload 参数。
 */
class Aggregation(
    internal val aircraftModel: AircraftModel,
    internal val payload: Payload
)
