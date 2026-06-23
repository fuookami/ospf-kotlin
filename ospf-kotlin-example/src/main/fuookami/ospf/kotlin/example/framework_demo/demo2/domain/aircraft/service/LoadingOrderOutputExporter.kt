package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft.service

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.dto.*

/**
 * 将计算的装载顺序导出为响应 DTO。Exports the computed loading order as a response DTO.
 *
 * @property aggregation 参数。
 */
data class LoadingOrderOutputExporter(
    private val aggregation: Aggregation
) {
    operator fun invoke(
        input: RequestDTO
    ): Ret<LoadingOrderResponseDTO> {
        TODO("not implemented yet")
    }
}
