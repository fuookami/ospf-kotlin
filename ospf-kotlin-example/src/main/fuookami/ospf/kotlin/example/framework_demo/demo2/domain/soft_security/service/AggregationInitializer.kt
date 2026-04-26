package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.soft_security.service

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.dto.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.soft_security.*

data object AggregationInitializer {
    operator fun invoke(
        aircraftAggregation: AircraftAggregation,
        stowageAggregation: StowageAggregation,
        input: RequestDTO
    ): Ret<Aggregation> {
        TODO("not implemented yet")
    }
}
