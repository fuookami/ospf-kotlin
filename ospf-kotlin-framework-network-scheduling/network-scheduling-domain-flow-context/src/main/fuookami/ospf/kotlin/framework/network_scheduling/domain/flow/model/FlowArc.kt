package fuookami.ospf.kotlin.framework.network_scheduling.domain.flow.model

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.quantities.quantity.*
import fuookami.ospf.kotlin.framework.network_scheduling.infrastructure.*

/** 网络流弧，不持有任何模型注册状态。 / Flow arc without model registration state.
 * @property networkArc 业务服务弧 / Business service
 * @property capacity 运行容量；设计问题中上界是有限建设包络 / Operating bounds; the upper bound is a finite construction envelope in design models
 */
class FlowArc<V : RealNumber<V>> private constructor(
    val networkArc: NetworkArc<V>,
    val capacity: CapacityBounds<V>
) {
    /** 弧起点。 / Arc origin. */
    val from: NetworkNodeId get() = networkArc.from

    /** 弧终点。 / Arc destination. */
    val to: NetworkNodeId get() = networkArc.to

    /** 弧成本。 / Arc cost. */
    val cost: Quantity<V> get() = networkArc.cost

    /** 通用弧属性。 / Generic arc attributes. */
    val attributes: Map<String, Quantity<V>> get() = networkArc.attributes

    companion object {
        /** 包装网络弧与容量上下界。 / Wrap a network arc and its capacity bounds.
         * @param networkArc 业务服务弧 / Business service arc
         * @param capacity 服务容量 / Service capacity bounds
         * @return 网络流弧或错误 / Flow arc or error
         */
        operator fun <V : RealNumber<V>> invoke(
            networkArc: NetworkArc<V>,
            capacity: CapacityBounds<V>
        ): Ret<FlowArc<V>> {
            return ok(FlowArc(networkArc, capacity))
        }

        /** 创建网络流弧。 / Create a network-flow arc.
         * @param from 起点 / Origin
         * @param to 终点 / Destination
         * @param cost 单位流量费用 / Unit-flow cost
         * @param capacity 服务容量 / Service capacity bounds
         * @param attributes 通用弧属性 / Generic arc attributes
         * @return 网络流弧或错误 / Flow arc or error
         */
        operator fun <V : RealNumber<V>> invoke(
            from: NetworkNodeId,
            to: NetworkNodeId,
            cost: Quantity<V>,
            capacity: CapacityBounds<V>,
            attributes: Map<String, Quantity<V>> = emptyMap()
        ): Ret<FlowArc<V>> {
            return when (val arc = NetworkArc(
                from = from,
                to = to,
                cost = cost,
                attributes = attributes
            )) {
                is Ok -> Ok(FlowArc(arc.value, capacity))
                is Failed -> Failed(arc.error)
                is Fatal -> Fatal(arc.errors)
            }
        }

        internal fun <V : RealNumber<V>> bind(
            networkArc: NetworkArc<V>,
            capacity: CapacityBounds<V>
        ): FlowArc<V> {
            return FlowArc(networkArc, capacity)
        }
    }
}
