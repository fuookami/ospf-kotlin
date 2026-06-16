package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.mac.service

import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.mac.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.dto.*

import fuookami.ospf.kotlin.utils.functional.*

/** Initializes the MAC aggregation from aircraft and stowage contexts. */
data object AggregationInitializer {
    operator fun invoke(
        aircraftAggregation: AircraftAggregation,
        stowageAggregation: StowageAggregation,
        input: RequestDTO
    ): Ret<Aggregation> {
        TODO("not implemented yet")
    }
}
