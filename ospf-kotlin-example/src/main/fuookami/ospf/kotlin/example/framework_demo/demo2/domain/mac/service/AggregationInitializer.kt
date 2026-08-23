package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.mac.service

import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.mac.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.dto.*

/**
 * Initializes the MAC aggregation from aircraft and stowage contexts.
 * 从飞机和装载上下文初始化 MAC 聚合。
*/
data object AggregationInitializer {
    operator fun invoke(
        aircraftAggregation: AircraftAggregation,
        stowageAggregation: StowageAggregation,
        input: RequestDTO
    ): Ret<Aggregation> {
        if (stowageAggregation.positions.isEmpty()) {
            return Failed(
                ErrorCode.IllegalArgument,
                "MAC 聚合至少需要一个装载位置 / MAC aggregation requires at least one stowage position"
            )
        }

        return Ok(Aggregation(
            aircraftModel = aircraftAggregation.aircraftModel,
            fuselage = aircraftAggregation.fuselage,
            fuel = aircraftAggregation.fuel,
            formula = aircraftAggregation.formula,
            positions = stowageAggregation.positions,
            load = stowageAggregation.load,
            totalWeight = stowageAggregation.totalWeight,
            horizontalStabilizers = hashMapOf()
        ))
    }
}
