package fuookami.ospf.kotlin.framework.network_scheduling.infrastructure

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.quantities.quantity.Quantity

/**
 * 网络流量。 / Network flow.
 *
 * @property quantity 非负流量物理量 / Non-negative flow quantity
 */
class Flow<V : RealNumber<V>> private constructor(val quantity: Quantity<V>) {
    companion object {
        /** 创建非负流量。 / Create a non-negative flow. */
        operator fun <V : RealNumber<V>> invoke(quantity: Quantity<V>): Ret<Flow<V>> {
            if (quantity.value ls quantity.value.constants.zero) {
                return networkSchedulingFailure(
                    "创建流量失败：流量不能为负 / Failed to create flow: flow cannot be negative"
                )
            }
            return ok(Flow(quantity))
        }
    }
}
