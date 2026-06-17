package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.mac_optimization.service

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.mac_optimization.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.dto.*

/** 从飞机、装载和 MAC 上下文初始化 MAC 优化聚合。Initializes the MAC optimization aggregation from aircraft, stowage, and MAC contexts. */
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
