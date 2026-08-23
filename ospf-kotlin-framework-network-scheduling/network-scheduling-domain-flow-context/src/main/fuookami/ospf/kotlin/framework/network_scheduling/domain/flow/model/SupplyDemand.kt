package fuookami.ospf.kotlin.framework.network_scheduling.domain.flow.model

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.framework.network_scheduling.infrastructure.*

/** 节点供需绑定。正值为供给，负值为需求。 / Node balance binding. */
class SupplyDemand<V : RealNumber<V>> private constructor(
    val nodeId: NetworkNodeId,
    val balance: NodeBalance<V>
) {
    /** 净供需量。 / Net supply-demand quantity. */
    val net: Quantity<V> get() = balance.net

    companion object {
        /** 绑定节点供需量。 / Bind a node to a supply-demand balance. */
        operator fun <V : RealNumber<V>> invoke(
            nodeId: NetworkNodeId,
            balance: NodeBalance<V>
        ): Ret<SupplyDemand<V>> {
            return if (nodeId.value.isBlank()) {
                networkSchedulingFailure(
                    "绑定供需失败：节点 ID 不能为空 / Failed to bind supply-demand: node ID cannot be blank"
                )
            } else {
                ok(SupplyDemand(nodeId, balance))
            }
        }

        /** 直接使用净供需量绑定节点。 / Bind a node directly to a net balance quantity. */
        operator fun <V : RealNumber<V>> invoke(
            nodeId: NetworkNodeId,
            net: Quantity<V>
        ): Ret<SupplyDemand<V>> {
            return when (val balance = NodeBalance(net)) {
                is Ok -> invoke(nodeId, balance.value)
                is Failed -> Failed(balance.error)
                is Fatal -> Fatal(balance.errors)
            }
        }
    }
}
