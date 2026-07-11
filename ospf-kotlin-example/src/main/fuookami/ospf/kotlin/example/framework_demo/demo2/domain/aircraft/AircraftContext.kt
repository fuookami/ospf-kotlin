package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft.service.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.dto.*

/**
 * Context for managing aircraft domain data, initializing aggregation from input and exporting loading orders.
 * 管理飞机域数据的上下文，从输入初始化聚合并导出装载顺序。
*/
class AircraftContext {
    lateinit var aggregation: Aggregation

    /**
     * Initialize aggregation with input data.
     * 使用输入数据初始化聚合。
     *
     * @param input The request input data. / 请求输入数据
     * @return The result of the operation. / 操作结果
    */
    fun init(
        input: RequestDTO
    ): Try {
        when (val result = AggregationInitializer(input)) {
            is Ok<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                aggregation = result.value!!
            }

            is Failed<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                    return Failed(result.error)
                }

                is Fatal<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                    return Fatal(result.errors)
                }
        }

        return ok
    }

    /**
     * Export loading orders.
     * 导出装载顺序。
     *
     * @param input The request input data. / 请求输入数据
     * @return The loading order response. / 装载顺序响应
    */
    fun exportLoadingOrders(
        input: RequestDTO
    ): Ret<LoadingOrderResponseDTO> {
        val exporter = LoadingOrderOutputExporter(aggregation)
        return exporter(input)
    }
}
