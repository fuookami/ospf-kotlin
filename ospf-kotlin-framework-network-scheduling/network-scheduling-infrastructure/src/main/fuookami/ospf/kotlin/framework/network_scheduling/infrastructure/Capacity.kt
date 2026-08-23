package fuookami.ospf.kotlin.framework.network_scheduling.infrastructure

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.quantities.quantity.Quantity

/**
 * 网络容量。 / Network capacity.
 *
 * @property quantity 非负容量物理量 / Non-negative capacity quantity
 */
class Capacity<V : RealNumber<V>> private constructor(val quantity: Quantity<V>) {
    companion object {
        /** 创建非负容量。 / Create a non-negative capacity. */
        operator fun <V : RealNumber<V>> invoke(quantity: Quantity<V>): Ret<Capacity<V>> {
            if (quantity.value ls quantity.value.constants.zero) {
                return networkSchedulingFailure(
                    "创建容量失败：容量不能为负 / Failed to create capacity: capacity cannot be negative"
                )
            }
            return ok(Capacity(quantity))
        }
    }
}
