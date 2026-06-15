package fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.model

import fuookami.ospf.kotlin.utils.functional.Order
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.quantities.quantity.*

/**
 * 最大刀数约束：slices 中 amount 总和不超过 maxKnifeCount / Max knife count constraint
 *
 * @property value 最大刀数阈值 / Max knife count threshold
 */
class MaxKnifeCountConstraint<V : RealNumber<V>>(
    val value: UInt64
) : CuttingPlanConstraint<V> {
    override fun isSatisfied(context: CuttingPlanConstraintContext<V>): Boolean {
        val totalCuts = context.slices.fold(UInt64.zero) { acc, slice ->
            acc + slice.amount
        }
        return totalCuts <= value
    }
}

/**
 * 最小刀数约束：slices 中 amount 总和不小于 minKnifeCount / Min knife count constraint
 *
 * 仅在叶节点（方案产出）时检查有效；搜索中间节点不应使用此约束剪枝，
 * 因为后续可能添加更多切片满足最小刀数要求。
 * [isPruning] 为 false，表示此约束不在搜索中间节点剪枝。
 *
 * @property value 最小刀数阈值 / Min knife count threshold
 */
class MinKnifeCountConstraint<V : RealNumber<V>>(
    val value: UInt64
) : CuttingPlanConstraint<V> {
    override val isPruning: Boolean = false

    override fun isSatisfied(context: CuttingPlanConstraintContext<V>): Boolean {
        val totalCuts = context.slices.fold(UInt64.zero) { acc, slice ->
            acc + slice.amount
        }
        return totalCuts >= value
    }
}

/**
 * 最大超产长度约束：产品的 length 不超过 maxOverProduceLength / Max over-produce length constraint
 *
 * 检查 slices 中所有产品的 length 是否在允许范围内。
 *
 * @property value 最大超产长度阈值 / Max over-produce length threshold
 */
class MaxOverProduceLengthConstraint<V : RealNumber<V>>(
    val value: Quantity<V>
) : CuttingPlanConstraint<V> {
    override fun isSatisfied(context: CuttingPlanConstraintContext<V>): Boolean {
        for (slice in context.slices) {
            val productLength = slice.production.length ?: continue
            if ((productLength.value partialOrd value.value) is Order.Greater) {
                return false
            }
        }
        return true
    }
}

/**
 * 幅宽上界约束：总宽度不超过物料幅宽上界 / Width upper bound constraint
 */
class WidthUpperBoundConstraint<V : RealNumber<V>> : CuttingPlanConstraint<V> {
    override fun isSatisfied(context: CuttingPlanConstraintContext<V>): Boolean {
        return (context.totalWidth.value partialOrd context.upperBound.value) !is Order.Greater
    }
}
