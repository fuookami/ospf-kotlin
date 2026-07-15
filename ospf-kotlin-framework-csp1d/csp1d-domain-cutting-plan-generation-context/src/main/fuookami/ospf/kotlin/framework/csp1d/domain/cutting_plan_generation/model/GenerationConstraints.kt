package fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.model

import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.quantities.quantity.Quantity

/**
 * 切割方案生成约束配置 / Cutting plan generation constraint configuration
 *
 * 同时作为便捷工厂：[toConstraints] 将此 data class 的字段转换为
 * [CuttingPlanConstraint] 列表，供生成器统一使用。
 *
 * @param V 数值类型 / Numeric value type
 * @property maxKnifeCount 最大刀数（切片总数上限）/ Max knife count (upper bound on total slices)
 * @property minKnifeCount 最小刀数（切片总数下限）/ Min knife count (lower bound on total slices)
 * @property maxOverProduceLength 最大超产长度 / Max over-produce length
 * @property parallelism 按物料并行生成的协程并发度，1 表示关闭 / Coroutine parallelism by material, 1 means disabled
 * @property enableDominancePruning 是否启用同贡献候选 dominance 剪枝 / Whether to enable dominance pruning for same-contribution candidates
 * @property dominanceStrategy dominance 剪枝策略 / Dominance pruning strategy
*/
data class GenerationConstraints<V : RealNumber<V>>(
    val maxKnifeCount: UInt64? = null,
    val minKnifeCount: UInt64? = null,
    val maxOverProduceLength: Quantity<V>? = null,
    val parallelism: Int64 = Int64.one,
    val enableDominancePruning: Boolean = false,
    val dominanceStrategy: DominanceStrategy = DominanceStrategy.SameContribution
) {
    companion object {
        /**
         * Returns unconstrained generation constraints with all limits disabled.
         * 返回所有限制均禁用的无约束生成配置。
         *
         * @return unconstrained generation constraints / 无约束的生成配置
        */
        fun <V : RealNumber<V>> unconstrained(): GenerationConstraints<V> = GenerationConstraints()
    }

    /**
     * 转换为约束 predicate 列表 / Convert to constraint predicate list
     *
     * 包含：
     * - [MaxKnifeCountConstraint]（当 maxKnifeCount 非空时）
     * - [MinKnifeCountConstraint]（当 minKnifeCount 非空时）
     * - [MaxOverProduceLengthConstraint]（当 maxOverProduceLength 非空时）
     * - [WidthUpperBoundConstraint]（始终包含，由生成器根据物料上界传入 context）
     *
     * @return list of cutting plan constraint predicates derived from this configuration / 由当前配置生成的切割方案约束谓词列表
    */
    fun toConstraints(): List<CuttingPlanConstraint<V>> {
        val constraints = mutableListOf<CuttingPlanConstraint<V>>()
        maxKnifeCount?.let { constraints.add(MaxKnifeCountConstraint(it)) }
        minKnifeCount?.let { constraints.add(MinKnifeCountConstraint(it)) }
        maxOverProduceLength?.let { constraints.add(MaxOverProduceLengthConstraint(it)) }
        constraints.add(WidthUpperBoundConstraint())
        return constraints
    }
}

/**
 * dominance 剪枝策略 / Dominance pruning strategy
 *
 * - [SameContribution]：按精确需求贡献分组，只比较相同贡献的方案余宽（当前默认行为）
 * - [CrossContribution]：按产品集合分组，贡献超集 + 余宽更优的方案 dominate 旧方案
*/
enum class DominanceStrategy {
    SameContribution,
    CrossContribution
}
