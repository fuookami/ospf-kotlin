package fuookami.ospf.kotlin.framework.network_scheduling.infrastructure

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.quantities.quantity.*

/**
 * 弧容量上下界。 / Lower and upper bounds for an arc capacity.
 *
 * @property lower 下界（非负） / Non-negative lower bound
 * @property upper 上界（不小于下界） / Upper bound not smaller than the lower bound
 */
class CapacityBounds<V : RealNumber<V>> private constructor(
    val lower: Quantity<V>,
    val upper: Quantity<V>
) {
    companion object {
        /** 创建并校验容量上下界。 / Create and validate capacity bounds. */
        operator fun <V : RealNumber<V>> invoke(
            lower: Quantity<V>,
            upper: Quantity<V>
        ): Ret<CapacityBounds<V>> {
            if (lower.unit.quantity != upper.unit.quantity) {
                return networkSchedulingFailure(
                    "创建容量上下界失败：上下界量纲必须一致 / " +
                            "Failed to create capacity bounds: lower and upper bounds must have the same dimension"
                )
            }
            if (lower.value ls lower.value.constants.zero || upper.value ls upper.value.constants.zero) {
                return networkSchedulingFailure(
                    "创建容量上下界失败：上下界不能为负 / " +
                            "Failed to create capacity bounds: bounds cannot be negative"
                )
            }
            val normalizedUpper = upper.convertTo(lower.unit)
                ?: return networkSchedulingFailure(
                    "创建容量上下界失败：上界无法转换到下界单位 / " +
                            "Failed to create capacity bounds: upper bound cannot be converted to lower-bound unit"
                )
            if (lower.value gr normalizedUpper.value) {
                return networkSchedulingFailure(
                    "创建容量上下界失败：下界不能大于上界 / " +
                            "Failed to create capacity bounds: lower bound cannot exceed upper bound"
                )
            }
            return ok(CapacityBounds(lower = lower, upper = upper))
        }
    }
}
