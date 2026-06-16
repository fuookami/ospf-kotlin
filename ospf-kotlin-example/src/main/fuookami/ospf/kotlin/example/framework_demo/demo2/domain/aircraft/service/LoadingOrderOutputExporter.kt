package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft.service

import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.dto.*

import fuookami.ospf.kotlin.utils.functional.*

/** Exports the computed loading order as a response DTO. */
data class LoadingOrderOutputExporter(
    private val aggregation: Aggregation
) {
    operator fun invoke(
        input: RequestDTO
    ): Ret<LoadingOrderResponseDTO> {
        TODO("not implemented yet")
    }
}
