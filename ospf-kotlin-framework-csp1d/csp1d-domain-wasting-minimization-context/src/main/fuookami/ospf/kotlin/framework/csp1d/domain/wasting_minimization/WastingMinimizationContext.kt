package fuookami.ospf.kotlin.framework.csp1d.domain.wasting_minimization

import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.CuttingPlan
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.QuantityArithmetic
import fuookami.ospf.kotlin.framework.csp1d.domain.produce.model.CuttingPlanUsage
import fuookami.ospf.kotlin.framework.csp1d.domain.wasting_minimization.model.RestMaterialWaste
import fuookami.ospf.kotlin.framework.csp1d.domain.wasting_minimization.model.RestWidthWaste
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.quantities.quantity.times

/**
 * 浪费分析结果 / Waste analysis result
 *
 * @param V 数值类型 / Numeric value type
 * @property restWidthWastes 余宽浪费列表 / Rest width waste list
 * @property restMaterialWastes 余料浪费列表 / Rest material waste list
 * @property totalRestWidth 总余宽 / Total rest width
 * @property totalRestMaterial 总余料面积代理 / Total rest material area proxy
 */
data class WasteAnalysis<V : RealNumber<V>>(
    val restWidthWastes: List<RestWidthWaste<V>>,
    val restMaterialWastes: List<RestMaterialWaste<V>>,
    val totalRestWidth: Quantity<V>?,
    val totalRestMaterial: Quantity<V>?
)

/**
 * 浪费最小化上下文，负责分析和量化切割方案集合中的各种浪费 / Waste minimization context: analyze and quantify various wastes across cutting plans
 *
 * @param V 数值类型 / Numeric value type
 */
class WastingMinimizationContext<V : RealNumber<V>>(
    private val arithmetic: QuantityArithmetic<V>
) {
    /**
     * 分析切割方案集合的浪费 / Analyze waste across selected cutting plans
     *
     * @param selectedPlans 选中的切割方案及其使用车次 / Selected cutting plans with usage amounts
     * @return 浪费分析结果 / Waste analysis result
     */
    fun analyze(selectedPlans: List<CuttingPlanUsage<V>>): WasteAnalysis<V> {
        val restWidthWastes = ArrayList<RestWidthWaste<V>>()
        val restMaterialWastes = ArrayList<RestMaterialWaste<V>>()
        var totalRestWidth: Quantity<V>? = null
        var totalRestMaterial: Quantity<V>? = null

        for (usage in selectedPlans) {
            val plan = usage.plan
            val restWidth = plan.restWidth ?: continue

            // 计入批次数倍数 / Account for batch multiplier
            val batchRestWidth = repeatQuantity(restWidth, usage.amount)
            restWidthWastes.add(RestWidthWaste(plan = plan, restWidth = batchRestWidth))
            totalRestWidth = if (totalRestWidth == null) {
                batchRestWidth
            } else {
                arithmetic.add(totalRestWidth, batchRestWidth)
            }

            // 余料面积代理 = 余宽 * 物料长度 / Rest material area proxy = rest width * material length
            val materialLength = plan.material.length
            if (materialLength != null) {
                val batchRestMaterial = repeatQuantity(
                    multiplyQuantities(restWidth, materialLength),
                    usage.amount
                )
                restMaterialWastes.add(
                    RestMaterialWaste(plan = plan, restMaterial = batchRestMaterial)
                )
                totalRestMaterial = if (totalRestMaterial == null) {
                    batchRestMaterial
                } else {
                    arithmetic.add(totalRestMaterial, batchRestMaterial)
                }
            }
        }

        return WasteAnalysis(
            restWidthWastes = restWidthWastes,
            restMaterialWastes = restMaterialWastes,
            totalRestWidth = totalRestWidth,
            totalRestMaterial = totalRestMaterial
        )
    }

    private fun repeatQuantity(q: Quantity<V>, times: UInt64): Quantity<V> {
        var result = Quantity(q.value.constants.zero, q.unit)
        var count = UInt64.zero
        while (count < times) {
            result = arithmetic.add(result, q)
            count += UInt64.one
        }
        return result
    }

    private fun multiplyQuantities(a: Quantity<V>, b: Quantity<V>): Quantity<V> {
        return a * b
    }
}
