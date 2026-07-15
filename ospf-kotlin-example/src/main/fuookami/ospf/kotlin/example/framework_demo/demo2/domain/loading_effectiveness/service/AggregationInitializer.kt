package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.loading_effectiveness.service

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.loading_effectiveness.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.dto.*

/**
 * Initializes the loading effectiveness aggregation from aircraft and stowage contexts.
 * 从飞机和装载上下文初始化装载效能聚合。
*/
data object AggregationInitializer {

    /**
     * Creates a loading effectiveness aggregation from aircraft and stowage aggregations.
     * 从飞行器和配载聚合创建装车效能聚合。
     *
     * @param aircraftAggregation The aircraft domain aggregation. / 飞行器域聚合
     * @param stowageAggregation The stowage domain aggregation. / 配载域聚合
     * @param input The request DTO containing input parameters. / 包含输入参数的请求 DTO
     * @return The loading effectiveness aggregation, or an error. / 装车效能聚合或错误
    */
    operator fun invoke(
        aircraftAggregation: AircraftAggregation,
        stowageAggregation: StowageAggregation,
        input: RequestDTO
    ): Ret<Aggregation> {
        TODO("not implemented yet")
    }
}
