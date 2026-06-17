package fuookami.ospf.kotlin.example.framework_demo.demo2

import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.dto.*

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
            is Ok<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {}

            is Failed<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                return LoadingOrderResponseDTO(request, result.error)
            }

            is Fatal<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                return LoadingOrderResponseDTO(request, result.firstError ?: Err(ErrorCode.ApplicationFailed))
            }
        }

        val loadingOrders = when (val result = aircraftContext.exportLoadingOrders(
            input = request
        )) {
            is Ok<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                result.value!!
            }

            is Failed<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                return LoadingOrderResponseDTO(request, result.error)
            }

            is Fatal<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                return LoadingOrderResponseDTO(request, result.firstError ?: Err(ErrorCode.ApplicationFailed))
            }
        }

        return loadingOrders
    }
}

