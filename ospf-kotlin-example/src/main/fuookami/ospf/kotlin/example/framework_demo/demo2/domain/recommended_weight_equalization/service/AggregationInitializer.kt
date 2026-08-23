package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.recommended_weight_equalization.service

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.recommended_weight_equalization.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.dto.*

/**
 * Initializes the recommended weight equalization aggregation from aircraft and stowage contexts.
 * 从飞机和装载上下文初始化推荐重量均衡聚合。
*/
data object AggregationInitializer {
    operator fun invoke(
        aircraftAggregation: AircraftAggregation,
        stowageAggregation: StowageAggregation,
        input: RequestDTO
    ): Ret<Aggregation> {
        return Ok(
            Aggregation(
                aircraftModel = aircraftAggregation.aircraftModel,
                items = stowageAggregation.items,
                positions = stowageAggregation.positions,
                appointment = stowageAggregation.appointment.appointment,
                stowage = stowageAggregation.stowage,
                load = stowageAggregation.load,
                payload = stowageAggregation.payload,
                totalWeight = stowageAggregation.totalWeight,
                ballast = stowageAggregation.ballast
            )
        )
    }
}
