package fuookami.ospf.kotlin.framework.csp1d.domain.produce.model

import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.CuttingPlan
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.CuttingPlanDemandContribution
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.Machine
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.Material
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.ProductDemand
import fuookami.ospf.kotlin.quantities.quantity.Quantity

/**
 * 切割方案使用量 / Cutting plan usage
 *
 * @param V 数值类型 / Numeric value type
 * @property plan 切割方案 / Cutting plan
 * @property amount 使用车次 / Usage amount
 */
data class CuttingPlanUsage<V : RealNumber<V>>(
    val plan: CuttingPlan<V>,
    val amount: UInt64
)

/**
 * 物料使用量 / Material usage
 *
 * @param V 数值类型 / Numeric value type
 * @property material 物料 / Material
 * @property amount 使用车次 / Used batches
 */
data class MaterialUsage<V : RealNumber<V>>(
    val material: Material<V>,
    val amount: UInt64
)

/**
 * 设备产能使用 / Machine capacity usage
 *
 * @param V 数值类型 / Numeric value type
 * @property machine 设备 / Machine
 * @property used 已使用产能 / Used capacity
 */
data class MachineCapacityUsage<V : RealNumber<V>>(
    val machine: Machine<V>,
    val used: Quantity<V>?
)

/**
 * 主问题求解产出 / Master problem output
 *
 * @param V 数值类型 / Numeric value type
 * @property cuttingPlans 选中切割方案 / Selected cutting plans
 * @property materialUsages 物料使用统计 / Material usage statistics
 * @property machineUsages 设备产能统计 / Machine capacity statistics
 * @property unmetDemands 未满足需求 / Unmet demands
 */
data class Produce<V : RealNumber<V>>(
    val cuttingPlans: List<CuttingPlanUsage<V>>,
    val materialUsages: List<MaterialUsage<V>>,
    val machineUsages: List<MachineCapacityUsage<V>>,
    val unmetDemands: List<ProductDemand<V>> = emptyList()
)

/**
 * 需求贡献聚合键（产品ID + 单位），确保同产品不同单位贡献不混算 / Contribution aggregation key (product ID + unit) ensuring same-product different-unit contributions are not mixed
 */
data class ContributionKey(
    val productId: String,
    val unit: fuookami.ospf.kotlin.quantities.unit.PhysicalUnit
)

/**
 * 汇总切割方案贡献 / Aggregate cutting plan contributions
 *
 * @param V 数值类型 / Numeric value type
 * @return 按产品+单位聚合的需求贡献 / Demand contribution grouped by product+unit
 */
fun <V : RealNumber<V>> Produce<V>.contributions(): Map<ContributionKey, List<CuttingPlanDemandContribution<V>>> {
    val contributions = LinkedHashMap<ContributionKey, MutableList<CuttingPlanDemandContribution<V>>>()
    for (usage in cuttingPlans) {
        for (contribution in usage.plan.demandContributions) {
            val key = ContributionKey(contribution.product.id, contribution.quantity.unit)
            contributions.getOrPut(key) { ArrayList() }.add(contribution)
        }
    }
    return contributions
}
