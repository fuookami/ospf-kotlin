package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.recommended_weight_equalization.service

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.dto.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.recommended_weight_equalization.*

data object AggregationInitializer {
    operator fun invoke(
        aircraftAggregation: AircraftAggregation,
        stowageAggregation: StowageAggregation,
        input: RequestDTO
    ): Ret<Aggregation> {
        TODO("not implemented yet")
    }
}
