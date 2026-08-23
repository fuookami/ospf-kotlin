package fuookami.ospf.kotlin.framework.network_scheduling.infrastructure

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.quantities.quantity.Quantity

/**
 * 可归一化的网络成本。 / Normalizable network cost.
 *
 * @property quantity 网络成本物理量（允许为负） / Network cost quantity (negative values allowed)
 */
class NetworkCost<V : RealNumber<V>> private constructor(val quantity: Quantity<V>) {
    companion object {
        /** 创建网络成本。 / Create a network cost. */
        operator fun <V : RealNumber<V>> invoke(quantity: Quantity<V>): Ret<NetworkCost<V>> {
            return ok(NetworkCost(quantity))
        }
    }
}
