package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.service

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.dto.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.*

data object AggregationInitializer {
    operator fun invoke(
        aircraftAggregation: AircraftAggregation,
        input: RequestDTO
    ): Ret<Aggregation> {
        TODO("not implemented yet")
    }
}
