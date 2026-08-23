package fuookami.ospf.kotlin.framework.network_scheduling.infrastructure

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.quantities.quantity.Quantity

/**
 * 节点净供需量。 / Net supply-demand balance at a node.
 *
 * 正值表示供给，负值表示需求。 / Positive values represent supply and negative values represent demand.
 *
 * @property net 净供需量 / Net balance
 */
class NodeBalance<V : RealNumber<V>> private constructor(
    val net: Quantity<V>
) {
    companion object {
        /** 创建节点净供需量，允许任意符号。 / Create a balance with either sign. */
        operator fun <V : RealNumber<V>> invoke(net: Quantity<V>): Ret<NodeBalance<V>> {
            return ok(NodeBalance(net))
        }
    }
}
