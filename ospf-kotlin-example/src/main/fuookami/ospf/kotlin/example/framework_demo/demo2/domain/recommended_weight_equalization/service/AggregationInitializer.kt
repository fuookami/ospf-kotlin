package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.recommended_weight_equalization.service

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.recommended_weight_equalization.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.dto.*

/** 从飞机和装载上下文初始化推荐重量均衡聚合。Initializes the recommended weight equalization aggregation from aircraft and stowage contexts. */
data object AggregationInitializer {
    operator fun invoke(
        aircraftAggregation: AircraftAggregation,
        stowageAggregation: StowageAggregation,
        input: RequestDTO
    ): Ret<Aggregation> {
        TODO("not implemented yet")
    }
}
