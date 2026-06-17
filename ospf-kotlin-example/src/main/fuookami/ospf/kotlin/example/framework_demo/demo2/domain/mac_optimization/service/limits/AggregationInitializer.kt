package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.mac_optimization.service.limits

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.mac_optimization.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.dto.*

/** 为 limits 子包初始化 MAC 优化聚合。Initializes the MAC optimization aggregation for the limits sub-package. */
data object AggregationInitializer {
    operator fun invoke(
        aircraftAggregation: AircraftAggregation,
        stowageAggregation: StowageAggregation,
        macAggregation: MACAggregation,
        input: RequestDTO
    ): Ret<Aggregation> {
        TODO("not implemented yet")
    }
}
