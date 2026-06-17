package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft.service

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.dto.*

/** 从输入请求数据初始化飞机聚合。Initializes the aircraft aggregation from input request data. */
data object AggregationInitializer {
    /**
     * 
     * @param input 请求数据。
     * @return 聚合结果。
     */
    operator fun invoke(input: RequestDTO): Ret<Aggregation> {
        TODO("not implemented yet")
    }
}
