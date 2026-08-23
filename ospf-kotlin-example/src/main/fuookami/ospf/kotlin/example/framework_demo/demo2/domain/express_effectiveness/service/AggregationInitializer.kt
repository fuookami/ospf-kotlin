package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.express_effectiveness.service

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.express_effectiveness.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.dto.*

/**
 * Initializes the express effectiveness aggregation from aircraft and stowage contexts.
 * 从飞机和装载上下文初始化快递效能聚合。
*/
data object AggregationInitializer {

    /**
     * Creates an express effectiveness aggregation from aircraft and stowage aggregations.
     * 从飞行器和配载聚合创建快递效能聚合。
     *
     * @param aircraftAggregation 飞行器域聚合 / The aircraft domain aggregation.
     * @param stowageAggregation 配载域聚合 / The stowage domain aggregation.
     * @param input 包含输入参数的请求 DTO / The request DTO containing input parameters.
     * @param stowageMode 当前配载模式 / The current stowage mode.
     * @return 快递效能聚合或错误 / The express effectiveness aggregation, or an error.
    */
    operator fun invoke(
        aircraftAggregation: AircraftAggregation,
        stowageAggregation: StowageAggregation,
        input: RequestDTO,
        stowageMode: StowageMode
    ): Ret<Aggregation> {
        return Ok(
            Aggregation(
                stowageMode = stowageMode,
                items = stowageAggregation.items,
                positions = stowageAggregation.positions,
                stowage = stowageAggregation.stowage,
                mustShipIndices = input.cargos.mapIndexedNotNull { index, cargo ->
                    index.takeIf { cargo.priority >= 8 }
                }
            )
        )
    }
}
