package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.dto.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft.service.*

class AircraftContext {
    lateinit var aggregation: Aggregation

    fun init(
        input: RequestDTO
    ): Try {
        when (val result = AggregationInitializer(input)) {
            is Ok -> {
                aggregation = result.value
            }

            is Failed -> {
                    return Failed(result.error)
                }

                is Fatal -> {
                    return Fatal(result.errors)
                }
        }

        return ok
    }
    
    fun exportLoadingOrders(
        input: RequestDTO
    ): Ret<LoadingOrderResponseDTO> {
        val exporter = LoadingOrderOutputExporter(aggregation)
        return exporter(input)
    }
}











