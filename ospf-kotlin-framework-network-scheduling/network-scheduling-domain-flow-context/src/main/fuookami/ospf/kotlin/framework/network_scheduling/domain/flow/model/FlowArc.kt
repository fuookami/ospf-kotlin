package fuookami.ospf.kotlin.framework.network_scheduling.domain.flow.model

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.quantities.quantity.*
import fuookami.ospf.kotlin.framework.network_scheduling.infrastructure.*

/** 网络流弧，携带容量上下界。 / Network-flow arc with capacity bounds. */
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

    /** 变量在弧流量组合中的位置。 / Variable slot in the arc-flow combination. */
    internal var variableIndex: Int? = null

    companion object {
        /** 包装网络弧与容量上下界。 / Wrap a network arc and its capacity bounds. */
        operator fun <V : RealNumber<V>> invoke(
            networkArc: NetworkArc<V>,
            capacity: CapacityBounds<V>
        ): Ret<FlowArc<V>> {
            return ok(FlowArc(networkArc, capacity))
        }

        /** 创建网络流弧。 / Create a network-flow arc. */
        operator fun <V : RealNumber<V>> invoke(
            from: NetworkNodeId,
            to: NetworkNodeId,
            cost: Quantity<V>,
            capacity: CapacityBounds<V>,
            attributes: Map<String, Quantity<V>> = emptyMap()
        ): Ret<FlowArc<V>> {
            return when (val arc = NetworkArc(from, to, cost, attributes)) {
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
