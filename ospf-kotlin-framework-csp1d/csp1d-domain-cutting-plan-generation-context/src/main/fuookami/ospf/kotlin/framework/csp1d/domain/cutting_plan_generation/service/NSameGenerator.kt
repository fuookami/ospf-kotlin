package fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.service

import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.Csp1dInitialCuttingPlanGenerator
import fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.CuttingPlanGenerationInput
import fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.model.GenerationConstraints
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.CuttingPlan
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.CuttingPlanDemandContribution
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.CuttingPlanSlice
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.Material
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.Product
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.QuantityArithmetic
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.quantities.quantity.partialOrd
import fuookami.ospf.kotlin.utils.functional.Order

/**
 * N-Same 方案生成器，为每个产品-宽度组合生成单产品方案 / N-Same generator producing single-product plans per product-width combination
 *
 * @param V 数值类型 / Numeric value type
 * @property constraints 生成约束 / Generation constraints
 * @property arithmetic 物理量算术策略 / Quantity arithmetic strategy
 * @property allAmount 是否生成所有可行数量（1 到 max），默认只生成 max / Whether to generate all amounts (1 to max)
 */
class NSameGenerator<V : RealNumber<V>>(
    private val constraints: GenerationConstraints<V> = GenerationConstraints.unconstrained(),
    private val arithmetic: QuantityArithmetic<V>,
    private val allAmount: Boolean = false
) : Csp1dInitialCuttingPlanGenerator<V> {
    override fun generate(input: CuttingPlanGenerationInput<V>): List<CuttingPlan<V>> {
        val plans = ArrayList<CuttingPlan<V>>()
        val planIndex = java.util.concurrent.atomic.AtomicInteger(0)

        for (material in input.materials) {
            for (demand in input.demands) {
                for (productWidth in demand.product.width) {
                    if (!material.widthRange.canCut(productWidth)) continue

                    val maxAmount = computeMaxAmount(
                        productWidth = productWidth,
                        material = material,
                        product = demand.product,
                        demandQuantity = demand.quantity
                    )
                    if (maxAmount == UInt64.zero) continue

                    val amounts = if (allAmount) {
                        val generatedAmounts = ArrayList<UInt64>()
                        var amount = UInt64.one
                        while (amount <= maxAmount) {
                            generatedAmounts.add(amount)
                            amount += UInt64.one
                        }
                        generatedAmounts
                    } else {
                        listOf(maxAmount)
                    }

                    for (amount in amounts) {
                        val slices = listOf(
                            CuttingPlanSlice(
                                production = demand.product,
                                width = productWidth,
                                amount = amount
                            )
                        )
                        val contribution = CuttingPlanDemandContribution(
                            product = demand.product,
                            quantity = computeContributionQuantity(demand.product, productWidth, amount, material)
                        )

                        val plan = CuttingPlan(
                            id = "nsame-${material.id}-${demand.product.id}-${planIndex.getAndIncrement()}",
                            material = material,
                            slices = slices,
                            demandContributions = listOf(contribution),
                            arithmetic = arithmetic
                        )
                        plans.add(plan)
                    }
                }
            }
        }
        return plans
    }

    private fun computeMaxAmount(
        productWidth: Quantity<V>,
        material: Material<V>,
        product: Product<V>,
        demandQuantity: Quantity<V>
    ): UInt64 {
        var maxAmount = UInt64.maximum

        // Width constraint: productWidth * amount <= material upper bound
        val upperBound = material.widthRange.upperBound
        if ((productWidth.value partialOrd upperBound.value) is Order.Greater) return UInt64.zero
        val maxWidthAmount = computeMaxByWidth(productWidth, upperBound)
        if (maxWidthAmount < maxAmount) maxAmount = maxWidthAmount

        // Knife count constraint
        constraints.maxKnifeCount?.let { maxKnife ->
            if (maxKnife < maxAmount) maxAmount = maxKnife
        }

        // Over-produce length constraint
        constraints.maxOverProduceLength?.let { maxOverLength ->
            product.length?.let { productLength ->
                if ((productLength.value partialOrd maxOverLength.value) is Order.Greater) {
                    return UInt64.zero
                }
            }
        }

        return maxAmount
    }

    private fun computeMaxByWidth(productWidth: Quantity<V>, upperBound: Quantity<V>): UInt64 {
        val comparison = upperBound.value partialOrd productWidth.value
        if (comparison is Order.Less) return UInt64.zero
        if (comparison is Order.Equal) return UInt64.one

        // Integer division: upperBound / productWidth
        var remaining = upperBound
        var count = UInt64.zero
        while ((remaining.value partialOrd productWidth.value) !is Order.Less) {
            remaining = arithmetic.subtract(remaining, productWidth)
            count = count + UInt64.one
        }
        return count
    }

    private fun computeContributionQuantity(
        product: Product<V>,
        width: Quantity<V>,
        amount: UInt64,
        material: Material<V>
    ): Quantity<V> {
        // For discrete demands (rolls/sheets): contribution = 1 roll/sheet per piece
        // For continuous demands (weight): contribution = width * length * unitWeight * amount
        // The actual quantity calculation depends on the demand unit; we delegate to the caller to interpret
        val unitContribution = product.unitWeight?.let { unitWeight ->
            product.length?.let { length ->
                // width * length * unitWeight gives a mass-like quantity
                // This is a simplified calculation; real units need proper Quantity multiplication
                val areaValue = width.value * length.value
                val weightValue = areaValue * unitWeight.value
                Quantity(weightValue, unitWeight.unit)
            }
        }
        if (unitContribution != null) {
            return repeatQuantity(unitContribution, amount)
        }
        // Default: 1 per piece (for roll/sheet count demands)
        val onePerPiece = Quantity(width.value.constants.one, width.unit)
        return repeatQuantity(onePerPiece, amount)
    }

    private fun repeatQuantity(q: Quantity<V>, times: UInt64): Quantity<V> {
        var result = arithmetic.zero(q.unit)
        repeat(times.toInt()) {
            result = arithmetic.add(result, q)
        }
        return result
    }
}
