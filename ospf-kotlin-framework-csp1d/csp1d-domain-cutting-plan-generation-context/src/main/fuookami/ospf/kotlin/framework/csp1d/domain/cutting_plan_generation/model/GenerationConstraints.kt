package fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.model

import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.quantities.quantity.Quantity

/**
 * 切割方案生成约束配置 / Cutting plan generation constraint configuration
 *
 * @param V 数值类型 / Numeric value type
 * @property maxKnifeCount 最大刀数（切片总数上限）/ Max knife count (upper bound on total slices)
 * @property minKnifeCount 最小刀数（切片总数下限）/ Min knife count (lower bound on total slices)
 * @property maxOverProduceLength 最大超产长度 / Max over-produce length
 */
data class GenerationConstraints<V : RealNumber<V>>(
    val maxKnifeCount: UInt64? = null,
    val minKnifeCount: UInt64? = null,
    val maxOverProduceLength: Quantity<V>? = null
) {
    companion object {
        fun <V : RealNumber<V>> unconstrained(): GenerationConstraints<V> = GenerationConstraints()
    }
}
