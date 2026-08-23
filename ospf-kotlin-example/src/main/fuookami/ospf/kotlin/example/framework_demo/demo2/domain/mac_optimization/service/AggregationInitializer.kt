package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.mac_optimization.service

import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.mac_optimization.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.dto.*

/**
 * Initializes the MAC optimization aggregation from aircraft, stowage, and MAC contexts.
 * 从飞机、装载和 MAC 上下文初始化 MAC 优化聚合。
*/
data object AggregationInitializer {
    operator fun invoke(
        aircraftAggregation: AircraftAggregation,
        stowageAggregation: StowageAggregation,
        macAggregation: MACAggregation,
        input: RequestDTO
    ): Ret<Aggregation> {
        if (stowageAggregation.positions.isEmpty()) {
            return Failed(
                ErrorCode.IllegalArgument,
                "MAC 优化聚合至少需要一个装载位置 / MAC optimization aggregation requires at least one stowage position"
            )
        }

        return Ok(Aggregation(
            aircraftModel = aircraftAggregation.aircraftModel,
            formula = aircraftAggregation.formula,
            totalWeight = stowageAggregation.totalWeight,
            torque = macAggregation.torque,
            horizontalStabilizers = macAggregation.horizontalStabilizers
        ))
    }
}
