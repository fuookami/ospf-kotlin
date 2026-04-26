package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft.service

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.dto.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft.*

data class LoadingOrderOutputExporter(
    private val aggregation: Aggregation
) {
    operator fun invoke(
        input: RequestDTO
    ): Ret<LoadingOrderResponseDTO> {
        TODO("not implemented yet")
    }
}
