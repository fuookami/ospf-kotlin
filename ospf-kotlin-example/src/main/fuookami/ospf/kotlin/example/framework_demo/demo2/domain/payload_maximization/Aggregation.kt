package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.payload_maximization

import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.Position

/**
 * Aggregates aircraft model and payload data for payload maximization optimization.
 * 聚合飞机模型和载荷数据用于载荷最大化优化。
 *
 * @property aircraftModel The aircraft model used for payload maximization / 用于载荷最大化的飞机模型
 * @property payload The payload data to be maximized / 待最大化的载荷数据
*/
class Aggregation(
    internal val aircraftModel: AircraftModel,
    internal val payload: Payload
)
