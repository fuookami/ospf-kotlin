package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.redundancy.service

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.redundancy.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.dto.*

/**
 * Initializes the redundancy aggregation from aircraft and stowage contexts.
 * 从飞机和装载上下文初始化冗余聚合。
*/
data object AggregationInitializer {
    operator fun invoke(
        aircraftAggregation: AircraftAggregation,
        stowageAggregation: StowageAggregation,
        input: RequestDTO
    ): Ret<Aggregation> {
        TODO("not implemented yet")
    }
}
