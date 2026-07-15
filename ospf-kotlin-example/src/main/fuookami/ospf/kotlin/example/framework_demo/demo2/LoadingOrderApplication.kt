package fuookami.ospf.kotlin.example.framework_demo.demo2

import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.dto.*

/**
 * Entry point for the loading order algorithm, encapsulating the full flow of algorithm initialization and execution.
 * 配载单算法入口，封装了算法初始化和执行的完整流程。
*/
data object LoadingOrderAlgorithm {
    suspend operator fun invoke(
        request: RequestDTO
    ): LoadingOrderResponseDTO {
        val algo = LoadingOrderAlgorithmImpl()
        return algo(request)
    }
}

/**
 * Implementation of the loading order algorithm, containing aircraft context initialization and loading order export logic.
 * 配载单算法实现，包含飞机上下文初始化和配载单导出逻辑。
*/
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
