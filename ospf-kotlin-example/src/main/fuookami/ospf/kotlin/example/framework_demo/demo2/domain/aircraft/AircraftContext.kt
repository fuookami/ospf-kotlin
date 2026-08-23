package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft

import fuookami.ospf.kotlin.utils.error.*
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
     * @param input 请求输入数据 / The request input data.
     * @return 操作结果 / The result of the operation.
    */
    fun init(
        input: RequestDTO
    ): Try {
        when (val result = AggregationInitializer(input)) {
            is Ok -> {
                aggregation = result.value!!
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

    /**
     * Export loading orders.
     * 导出装载顺序。
     *
     * @param input 请求输入数据 / The request input data.
     * @return 装载顺序响应 / The loading order response.
    */
    fun exportLoadingOrders(
        input: RequestDTO
    ): Ret<LoadingOrderResponseDTO> {
        val exporter = LoadingOrderOutputExporter(aggregation)
        return exporter(input)
    }
}
