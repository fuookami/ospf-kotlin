package fuookami.ospf.kotlin.example.framework_demo.demo2

import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.dto.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft.*

data object LoadingOrderAlgorithm {
    suspend operator fun invoke(
        request: RequestDTO
    ): LoadingOrderResponseDTO {
        val algo = LoadingOrderAlgorithmImpl()
        return algo(request)
    }
}

private class LoadingOrderAlgorithmImpl {
    private val aircraftContext = AircraftContext()

    operator fun invoke(request: RequestDTO): LoadingOrderResponseDTO {
        when (val result = aircraftContext.init(
            input = request
        )) {
            is Ok -> {}

            is Failed -> {
                return LoadingOrderResponseDTO(request, result.error)
            }

            is Fatal -> {
                return LoadingOrderResponseDTO(request, result.firstError ?: Err(ErrorCode.ApplicationFailed))
            }
        }

        val loadingOrders = when (val result = aircraftContext.exportLoadingOrders(
            input = request
        )) {
            is Ok -> {
                result.value
            }

            is Failed -> {
                return LoadingOrderResponseDTO(request, result.error)
            }

            is Fatal -> {
                return LoadingOrderResponseDTO(request, result.firstError ?: Err(ErrorCode.ApplicationFailed))
            }
        }

        return loadingOrders
    }
}




