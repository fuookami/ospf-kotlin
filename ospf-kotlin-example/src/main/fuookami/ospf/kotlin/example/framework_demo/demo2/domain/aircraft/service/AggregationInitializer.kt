package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft.service

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.dto.*

/**
 * Initializes the aircraft aggregation from input request data.
 * 从输入请求数据初始化飞机聚合。
*/
data object AggregationInitializer {

    /**
     * Initialize the aircraft aggregation from the given request data.
     * 从给定的请求数据初始化飞机聚合。
     *
     * @param input The request input data. / 请求输入数据
     * @return The initialized aggregation result. / 初始化的聚合结果
    */
    operator fun invoke(input: RequestDTO): Ret<Aggregation> {
        TODO("not implemented yet")
    }
}
