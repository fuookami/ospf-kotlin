package fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation

import java.util.Comparator
import fuookami.ospf.kotlin.utils.functional.Order
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.quantities.quantity.partialOrd
import fuookami.ospf.kotlin.quantities.unit.PhysicalUnit
import fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.model.canonicalKey
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.Costar
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.CuttingPlan
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.CuttingPlanDemandContribution
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.CuttingPlanSlice
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.Machine
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.MachineCapacityShadowPriceKey
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.Material
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.MaterialUsageShadowPriceKey
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.Product
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.ProductDemand
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.ProductDemandShadowPriceKey
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.ShadowPriceMap

private fun shadowPriceUnitSymbol(unit: PhysicalUnit): String {
    return unit.symbol ?: unit.name ?: unit.toString()
}

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
                    material.widthRange.canCut(productWidth)
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
            val shadowPrice = input.shadowPrices[
                ProductDemandShadowPriceKey(
                    productId = demand.product.id,
                    unitSymbol = shadowPriceUnitSymbol(demand.quantity.unit)
                )
            ] ?: continue
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

/**
 * Reduced cost 定价生成器 / Reduced cost pricing generator
 *
 * 使用枚举子问题生成候选切割方案，通过 shadow price 计算 reduced cost，
 * 返回 reduced cost 为负的方案（即有潜力改善主问题目标的新列）。
 *
 * Uses enumeration sub-problem to generate candidate cutting plans,
 * computes reduced cost via shadow prices, and returns plans with
 * negative reduced cost (columns that can improve the master problem objective).
 *
 * reduced_cost = objective_coefficient - Σ(contribution_ij * shadow_price_i) - Σ(material_usage_m * shadow_price_m)
 * 当 reduced_cost < 0 时，该方案可能改善当前 LP 松弛目标。
 *
 * @param V 数值类型 / Numeric value type
 * @property enumerator 底层枚举生成器 / Underlying enumeration generator
 */
class ReducedCostPricingGenerator<V : RealNumber<V>>(
    private val enumerator: Csp1dInitialCuttingPlanGenerator<V>
) : Csp1dPricingGenerator<V> {
    override fun generate(input: Csp1dPricingInput<V>): List<CuttingPlan<V>> {
        val candidates = enumerator.generate(input.generationInput)
        if (candidates.isEmpty()) return emptyList()

        val existingIds = input.generationInput.existingPlans.map { it.id }.toSet()
        val existingKeys = input.generationInput.existingPlans.map { it.canonicalKey() }.toSet()
        val maxGeneratedPlans = input.maxGeneratedPlans.toULong()
        val shadowPrices = input.shadowPrices

        return candidates
            .asSequence()
            .map { plan -> plan to plan.canonicalKey() }
            .filter { (plan, key) -> plan.id !in existingIds && key !in existingKeys }
            .distinctBy { (_, key) -> key }
            .map { (plan, _) -> plan to computeDualBenefit(plan, shadowPrices) }
            .filter { (_, benefit) -> isGreaterThanOne(benefit) }
            .sortedWith(compareByDualBenefit())
            .map { it.first }
            .take(maxGeneratedPlans.toInt())
            .toList()
    }

    /**
     * 计算切割方案的对偶收益 / Compute dual benefit of a cutting plan
     *
     * benefit = Σ(contribution_ij * shadow_price_i) + Σ(material_usage_m * shadow_price_m)
     * 当 benefit > 1 时，等价于 reduced_cost < 0。
     *
     * benefit = Σ(contribution_ij * shadow_price_i) + Σ(material_usage_m * shadow_price_m)
     * When benefit > 1, it is equivalent to reduced_cost < 0.
     */
    private fun computeDualBenefit(plan: CuttingPlan<V>, shadowPrices: ShadowPriceMap<V>): V {
        var benefit = plan.material.widthRange.upperBound.value.constants.zero

        for (contribution in plan.demandContributions) {
            val key = ProductDemandShadowPriceKey(
                productId = contribution.product.id,
                unitSymbol = shadowPriceUnitSymbol(contribution.quantity.unit)
            )
            val sp = shadowPrices[key] ?: continue
            benefit += contribution.quantity.value * sp
        }

        val materialKey = MaterialUsageShadowPriceKey(plan.material.id)
        val materialSp = shadowPrices[materialKey]
        if (materialSp != null) {
            benefit += materialSp
        }

        val machineId = plan.machineId
        if (machineId != null) {
            val machineKey = MachineCapacityShadowPriceKey(machineId)
            val machineSp = shadowPrices[machineKey]
            if (machineSp != null) {
                benefit += machineSp
            }
        }

        return benefit
    }

    private fun isGreaterThanOne(value: V): Boolean {
        return when (value partialOrd value.constants.one) {
            is Order.Greater -> true
            else -> false
        }
    }

    private fun compareByDualBenefit(): Comparator<Pair<CuttingPlan<V>, V>> {
        return Comparator { left, right ->
            when (left.second partialOrd right.second) {
                is Order.Greater -> -1
                is Order.Less -> 1
                else -> 0
            }
        }
    }
}

