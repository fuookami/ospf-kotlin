package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.loading_effectiveness.service

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.loading_effectiveness.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.dto.*

/** 从飞机和装载上下文初始化装载效能聚合。Initializes the loading effectiveness aggregation from aircraft and stowage contexts. */
data object AggregationInitializer {
    operator fun invoke(
        aircraftAggregation: AircraftAggregation,
        stowageAggregation: StowageAggregation,
        input: RequestDTO
    ): Ret<Aggregation> {
        TODO("not implemented yet")
    }
}