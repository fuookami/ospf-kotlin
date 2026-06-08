package fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.service

import kotlin.time.Duration
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.Csp1dInitialCuttingPlanGenerator
import fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.CuttingPlanGenerationInput
import fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.model.CuttingPlanConstraint
import fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.model.CuttingPlanConstraintContext
import fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.model.GenerationConstraints
import fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.model.MaxKnifeCountConstraint
import fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.model.MaxOverProduceLengthConstraint
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.CuttingPlan
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.CuttingPlanSlice
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.Material
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.Product
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.QuantityArithmetic
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.contribution
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.quantities.quantity.partialOrd
import fuookami.ospf.kotlin.utils.functional.Order

/**
 * N-Same 方案生成器，为每个产品-宽度组合生成单产品方案 / N-Same generator producing single-product plans per product-width combination
 *
 * @param V 数值类型 / Numeric value type
 * @property constraints 约束列表 / Constraint list
 * @property arithmetic 物理量算术策略 / Quantity arithmetic strategy
 * @property allAmount 是否生成所有可行数量（1 到 max），默认只生成 max / Whether to generate all amounts (1 to max)
 * @property timeout 超时限制 / Timeout limit
 */
class NSameGenerator<V : RealNumber<V>>(
    private val constraints: List<CuttingPlanConstraint<V>> = emptyList(),
    private val arithmetic: QuantityArithmetic<V>,
    private val allAmount: Boolean = false,
    private val timeout: Duration? = null
) : Csp1dInitialCuttingPlanGenerator<V> {

    constructor(
        constraints: GenerationConstraints<V>,
        arithmetic: QuantityArithmetic<V>,
        allAmount: Boolean = false,
        timeout: Duration? = null
    ) : this(
        constraints = constraints.toConstraints(),
        arithmetic = arithmetic,
        allAmount = allAmount,
        timeout = timeout
    )

    /** 从约束中提取 maxKnifeCount，用于枚举范围限制 */
    private val maxKnifeCount: UInt64? = constraints.filterIsInstance<MaxKnifeCountConstraint<V>>().firstOrNull()?.value

    /** 从约束中提取 maxOverProduceLength，用于枚举范围限制 */
    private val maxOverProduceLength: Quantity<V>? = constraints.filterIsInstance<MaxOverProduceLengthConstraint<V>>().firstOrNull()?.value

    override fun generate(input: CuttingPlanGenerationInput<V>): List<CuttingPlan<V>> {
        val plans = ArrayList<CuttingPlan<V>>()
        val planIndex = java.util.concurrent.atomic.AtomicInteger(0)
        val deadline = timeout?.let { System.nanoTime() + it.inWholeNanoseconds }

        for (material in input.materials) {
            if (deadline != null && System.nanoTime() > deadline) break

            for (demand in input.demands) {
                for (productWidth in demand.product.width) {
                    if (!material.widthRange.canCut(productWidth)) continue

                    val maxAmount = computeMaxAmount(
                        productWidth = productWidth,
                        material = material,
                        product = demand.product
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
                        if (deadline != null && System.nanoTime() > deadline) break

                        val slices = listOf(
                            CuttingPlanSlice(
                                production = demand.product,
                                width = productWidth,
                                amount = amount
                            )
                        )
                        val usedWidth = repeatWidth(productWidth, amount)

                        // 约束检查 / Constraint check
                        if (!satisfiesConstraints(slices, usedWidth, material.widthRange.upperBound, material)) {
                            continue
                        }

                        val contribution = demand.contribution(
                            width = productWidth,
                            amount = amount,
                            arithmetic = arithmetic
                        )

                        val plan = CuttingPlan(
                            id = "nsame-${material.id}-${demand.product.id}-${planIndex.getAndIncrement()}",
                            material = material,
                            slices = slices,
                            demandContributions = listOf(contribution),
                            arithmetic = arithmetic
                        )
                        if (material.enabled(plan, input.machines)) {
                            plans.add(plan)
                        }
                    }
                }
            }
        }
        return plans
    }

    private fun computeMaxAmount(
        productWidth: Quantity<V>,
        material: Material<V>,
        product: Product<V>
    ): UInt64 {
        var maxAmount = UInt64.maximum

        // 宽度约束 / Width constraint
        val upperBound = material.widthRange.upperBound
        if ((productWidth.value partialOrd upperBound.value) is Order.Greater) return UInt64.zero
        val maxWidthAmount = computeMaxByWidth(productWidth, upperBound)
        if (maxWidthAmount < maxAmount) maxAmount = maxWidthAmount

        // 刀数约束 / Knife count constraint
        maxKnifeCount?.let { if (it < maxAmount) maxAmount = it }

        // 超产长度约束 / Over-produce length constraint
        maxOverProduceLength?.let { maxLen ->
            product.length?.let { productLength ->
                if ((productLength.value partialOrd maxLen.value) is Order.Greater) {
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

        var remaining = upperBound
        var count = UInt64.zero
        while ((remaining.value partialOrd productWidth.value) !is Order.Less) {
            remaining = arithmetic.subtract(remaining, productWidth)
            count = count + UInt64.one
        }
        return count
    }

    private fun satisfiesConstraints(
        slices: List<CuttingPlanSlice<V>>,
        totalWidth: Quantity<V>,
        upperBound: Quantity<V>,
        material: Material<V>
    ): Boolean {
        if (constraints.isEmpty()) return true
        val context = CuttingPlanConstraintContext(slices, totalWidth, upperBound, material)
        return constraints.all { it.isSatisfied(context) }
    }

    private fun repeatWidth(width: Quantity<V>, times: UInt64): Quantity<V> {
        var result = arithmetic.zero(width.unit)
        repeat(times.toInt()) {
            result = arithmetic.add(result, width)
        }
        return result
    }
}
