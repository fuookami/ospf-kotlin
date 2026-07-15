package fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.model

import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.*

/**
 * 切割方案约束评估上下文 / Cutting plan constraint evaluation context
 *
 * 在生成器搜索过程中传递给约束 predicate 的上下文，
 * 包含当前已生成的 slices、累计宽度、物料幅宽上界和物料信息。
 *
 * @property slices 当前已生成的切片列表 / Currently generated slice list
 * @property totalWidth 累计总宽度 / Accumulated total width
 * @property upperBound 物料幅宽上界 / Material width upper bound
 * @property material 物料信息 / Material information
*/
data class CuttingPlanConstraintContext<V : RealNumber<V>>(
    val slices: List<CuttingPlanSlice<V>>,
    val totalWidth: Quantity<V>,
    val upperBound: Quantity<V>,
    val material: Material<V>
)

/**
 * 切割方案约束 predicate / Cutting plan constraint predicate
 *
 * 可组合的约束接口，在生成器搜索过程中用于剪枝和方案可行性判断。
 * 生成器应在每个搜索节点调用所有注册约束的 [isSatisfied]，
 * 以决定是否继续沿该分支搜索或剪枝。
 *
 * 约束分两类：
 * - 剪枝约束（[isPruning] = true）：在搜索中间节点检查，用于提前剪枝无效分支
 * - 叶节点约束（[isPruning] = false）：仅在产出方案时检查，中间节点可能尚未满足
*/
interface CuttingPlanConstraint<V : RealNumber<V>> {

    /**
     * 判断当前搜索上下文是否满足约束
     * @return true 如果约束满足，可以继续搜索或产出方案；false 如果违反约束，应剪枝
     *
     * @param context Current search context containing generated slices, accumulated width, and material info / 包含当前已生成切片、累计宽度和物料信息的搜索上下文
    */
    fun isSatisfied(context: CuttingPlanConstraintContext<V>): Boolean

    /**
     * 是否为剪枝约束 / Whether this is a pruning constraint
     *
     * 剪枝约束在搜索中间节点即可判定，用于提前剪枝。
     * 叶节点约束仅在最终产出方案时判定，中间节点可能尚未满足。
    */
    val isPruning: Boolean get() = true
}
