package fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation

import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.Costar
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.CuttingPlan
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.CuttingPlanDemandContribution
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.CuttingPlanSlice
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.Machine
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.Material
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.Product
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.ProductDemand
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.ProductDemandShadowPriceKey
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.ShadowPriceMap
import fuookami.ospf.kotlin.quantities.quantity.partialOrd
import fuookami.ospf.kotlin.utils.functional.Order

/**
 * 切割方案生成输入 / Cutting plan generation input
 *
 * @param V 数值类型 / Numeric value type
 */
data class CuttingPlanGenerationInput<V : RealNumber<V>>(
    val products: List<Product<V>>,
    val materials: List<Material<V>>,
    val machines: List<Machine<V>>,
    val costars: List<Costar<V>> = emptyList(),
    val demands: List<ProductDemand<V>>,
    val existingPlans: List<CuttingPlan<V>> = emptyList()
)

/**
 * 初始切割方案生成器 / Initial cutting plan generator
 *
 * @param V 数值类型 / Numeric value type
 */
fun interface Csp1dInitialCuttingPlanGenerator<V : RealNumber<V>> {
    /**
     * 生成初始切割方案 / Generate initial cutting plans
     *
     * @param input 切割方案生成输入 / Cutting plan generation input
     * @return 初始切割方案列表 / Initial cutting plans
     */
    fun generate(input: CuttingPlanGenerationInput<V>): List<CuttingPlan<V>>
}

/**
 * 定价输入 / Pricing input
 *
 * @param V 数值类型 / Numeric value type
 */
data class Csp1dPricingInput<V : RealNumber<V>>(
    val generationInput: CuttingPlanGenerationInput<V>,
    val shadowPrices: ShadowPriceMap<V>,
    val maxGeneratedPlans: UInt64 = UInt64.one
)

/**
 * 定价子问题生成器 / Pricing sub-problem generator
 *
 * @param V 数值类型 / Numeric value type
 */
fun interface Csp1dPricingGenerator<V : RealNumber<V>> {
    /**
     * 生成新列 / Generate new columns
     *
     * @param input 定价输入 / Pricing input
     * @return 新切割方案列表 / New cutting plans
     */
    fun generate(input: Csp1dPricingInput<V>): List<CuttingPlan<V>>
}

/**
 * 简单初始方案生成器 / Simple initial cutting plan generator
 *
 * @param V 数值类型 / Numeric value type
 */
class SimpleInitialCuttingPlanGenerator<V : RealNumber<V>> : Csp1dInitialCuttingPlanGenerator<V> {
    override fun generate(input: CuttingPlanGenerationInput<V>): List<CuttingPlan<V>> {
        val plans = ArrayList<CuttingPlan<V>>()
        for (material in input.materials) {
            for (demand in input.demands) {
                val width = demand.product.width.firstOrNull { productWidth ->
                    material.widthRange.width.contains(productWidth)
                } ?: continue
                plans.add(
                    CuttingPlan(
                        id = "init-${material.id}-${demand.product.id}-${plans.size}",
                        material = material,
                        slices = listOf(
                            CuttingPlanSlice(
                                production = demand.product,
                                width = width
                            )
                        ),
                        demandContributions = listOf(
                            CuttingPlanDemandContribution(
                                product = demand.product,
                                quantity = demand.quantity
                            )
                        )
                    )
                )
            }
        }
        return plans
    }
}

/**
 * 简单定价生成器，按 shadow price 触发新列 / Simple pricing generator that triggers new columns by shadow prices
 *
 * @param V 数值类型 / Numeric value type
 */
class SimplePricingGenerator<V : RealNumber<V>> : Csp1dPricingGenerator<V> {
    override fun generate(input: Csp1dPricingInput<V>): List<CuttingPlan<V>> {
        val pricedPlans = ArrayList<CuttingPlan<V>>()
        val maxGeneratedPlans = input.maxGeneratedPlans
        val material = input.generationInput.materials.firstOrNull() ?: return emptyList()
        for (demand in input.generationInput.demands) {
            if (pricedPlans.size.toULong() >= maxGeneratedPlans.toULong()) {
                break
            }
            val shadowPrice = input.shadowPrices[ProductDemandShadowPriceKey(demand.product.id)] ?: continue
            if (!isPositive(shadowPrice)) {
                continue
            }
            val width = demand.product.width.firstOrNull { productWidth ->
                material.widthRange.width.contains(productWidth)
            } ?: continue
            pricedPlans.add(
                CuttingPlan(
                    id = "pricing-${material.id}-${demand.product.id}-${pricedPlans.size}",
                    material = material,
                    slices = listOf(
                        CuttingPlanSlice(
                            production = demand.product,
                            width = width
                        )
                    ),
                    demandContributions = listOf(
                        CuttingPlanDemandContribution(
                            product = demand.product,
                            quantity = demand.quantity
                        )
                    )
                )
            )
        }
        return pricedPlans
    }

    private fun isPositive(value: V): Boolean {
        return when (value partialOrd value.constants.zero) {
            is Order.Greater -> true
            else -> false
        }
    }
}

