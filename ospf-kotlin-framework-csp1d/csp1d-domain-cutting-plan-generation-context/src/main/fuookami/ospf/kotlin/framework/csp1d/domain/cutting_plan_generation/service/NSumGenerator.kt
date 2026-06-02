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
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.ProductDemand
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.QuantityArithmetic
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.quantities.quantity.partialOrd
import fuookami.ospf.kotlin.utils.functional.Order

/**
 * N-Sum 方案生成器，深度受限 DFS 枚举多产品组合方案 / N-Sum generator: depth-limited DFS for multi-product combination plans
 *
 * @param V 数值类型 / Numeric value type
 * @property constraints 生成约束 / Generation constraints
 * @property arithmetic 物理量算术策略 / Quantity arithmetic strategy
 * @property maxDepth 最大深度（最大切片总数）/ Max depth (max total slices)
 * @property maxPlans 最大方案数（提前终止）/ Max plans (early termination)
 */
class NSumGenerator<V : RealNumber<V>>(
    private val constraints: GenerationConstraints<V> = GenerationConstraints.unconstrained(),
    private val arithmetic: QuantityArithmetic<V>,
    private val maxDepth: UInt64 = UInt64(7UL),
    private val maxPlans: Int = 1000
) : Csp1dInitialCuttingPlanGenerator<V> {

    private data class ProductWidthEntry<V : RealNumber<V>>(
        val product: Product<V>,
        val width: Quantity<V>,
        val demandIndex: Int
    )

    override fun generate(input: CuttingPlanGenerationInput<V>): List<CuttingPlan<V>> {
        val plans = ArrayList<CuttingPlan<V>>()
        val planIndex = java.util.concurrent.atomic.AtomicInteger(0)

        val entries = buildProductWidthEntries(input.demands)
        if (entries.isEmpty()) return emptyList()

        for (material in input.materials) {
            val materialEntries = entries.filter { material.widthRange.width.contains(it.width) }
            if (materialEntries.isEmpty()) continue

            nSumSearch(
                material = material,
                entries = materialEntries,
                planIndex = planIndex,
                plans = plans
            )

            if (plans.size >= maxPlans) break
        }

        return plans.take(maxPlans)
    }

    private fun nSumSearch(
        material: Material<V>,
        entries: List<ProductWidthEntry<V>>,
        planIndex: java.util.concurrent.atomic.AtomicInteger,
        plans: MutableList<CuttingPlan<V>>
    ) {
        val upperBound = material.widthRange.upperBound
        val stack = ArrayDeque<MutableList<CuttingPlanSlice<V>>>()
        val widthStack = ArrayDeque<Quantity<V>>()
        val cutStack = ArrayDeque<UInt64>()
        val indexStack = ArrayDeque<Int>()

        stack.addLast(ArrayList())
        widthStack.addLast(arithmetic.zero(upperBound.unit))
        cutStack.addLast(UInt64.zero)
        indexStack.addLast(0)

        while (stack.isNotEmpty() && plans.size < maxPlans) {
            val currentSlices = stack.removeLast()
            val currentWidth = widthStack.removeLast()
            val currentCuts = cutStack.removeLast()
            val currentIndex = indexStack.removeLast()

            if (currentIndex >= entries.size) {
                if (currentSlices.isNotEmpty()) {
                    val plan = buildPlan(
                        material = material,
                        slices = currentSlices,
                        entries = entries,
                        planId = "nsum-${material.id}-${planIndex.getAndIncrement()}"
                    )
                    plans.add(plan)
                }
                continue
            }

            val entry = entries[currentIndex]
            val remainingWidth = arithmetic.subtract(upperBound, currentWidth)

            // Depth constraint: max amount is limited by remaining depth
            val remainingDepth = maxDepth - currentCuts
            if (remainingDepth == UInt64.zero) {
                // No more depth — treat as leaf
                if (currentSlices.isNotEmpty()) {
                    val plan = buildPlan(
                        material = material,
                        slices = currentSlices,
                        entries = entries,
                        planId = "nsum-${material.id}-${planIndex.getAndIncrement()}"
                    )
                    plans.add(plan)
                }
                continue
            }

            // Max amount for this product-width
            val maxByWidth = computeMaxByWidth(entry.width, remainingWidth)
            val maxByKnife = constraints.maxKnifeCount?.let { maxKnife ->
                if (currentCuts >= maxKnife) UInt64.zero else maxKnife - currentCuts
            } ?: remainingDepth
            val maxAmount = minOf(maxByWidth, remainingDepth, maxByKnife)

            // Try amount 0 (skip)
            stack.addLast(ArrayList(currentSlices))
            widthStack.addLast(currentWidth)
            cutStack.addLast(currentCuts)
            indexStack.addLast(currentIndex + 1)

            // Try amounts 1..maxAmount
            var amount = UInt64.one
            while (amount <= maxAmount) {
                val addedWidth = repeatWidth(entry.width, amount)
                val newWidth = arithmetic.add(currentWidth, addedWidth)
                val newCuts = currentCuts + amount

                if ((newWidth.value partialOrd upperBound.value) is Order.Greater) {
                    amount += UInt64.one
                    continue
                }

                val newSlices = ArrayList(currentSlices)
                newSlices.add(
                    CuttingPlanSlice(
                        production = entry.product,
                        width = entry.width,
                        amount = amount
                    )
                )

                stack.addLast(newSlices)
                widthStack.addLast(newWidth)
                cutStack.addLast(newCuts)
                indexStack.addLast(currentIndex + 1)
                amount += UInt64.one
            }
        }
    }

    private fun computeMaxByWidth(productWidth: Quantity<V>, remainingWidth: Quantity<V>): UInt64 {
        if ((remainingWidth.value partialOrd productWidth.value) is Order.Less) return UInt64.zero
        var count = UInt64.zero
        var w = remainingWidth
        while ((w.value partialOrd productWidth.value) !is Order.Less) {
            w = arithmetic.subtract(w, productWidth)
            count = count + UInt64.one
        }
        return count
    }

    private fun buildPlan(
        material: Material<V>,
        slices: List<CuttingPlanSlice<V>>,
        entries: List<ProductWidthEntry<V>>,
        planId: String
    ): CuttingPlan<V> {
        val contributions = slices.map { slice ->
            val product = slice.production as Product<V>
            CuttingPlanDemandContribution(
                product = product,
                quantity = computeSliceContribution(product, slice.width, slice.amount)
            )
        }

        return CuttingPlan(
            id = planId,
            material = material,
            slices = slices,
            demandContributions = contributions,
            arithmetic = arithmetic
        )
    }

    private fun computeSliceContribution(product: Product<V>, width: Quantity<V>, amount: UInt64): Quantity<V> {
        val unitContribution = product.unitWeight?.let { unitWeight ->
            product.length?.let { length ->
                val areaValue = width.value * length.value
                val weightValue = areaValue * unitWeight.value
                Quantity(weightValue, unitWeight.unit)
            }
        }
        if (unitContribution != null) {
            return repeatQuantity(unitContribution, amount)
        }
        val onePerPiece = Quantity(width.value.constants.one, width.unit)
        return repeatQuantity(onePerPiece, amount)
    }

    private fun buildProductWidthEntries(demands: List<ProductDemand<V>>): List<ProductWidthEntry<V>> {
        val entries = ArrayList<ProductWidthEntry<V>>()
        demands.forEachIndexed { index, demand ->
            for (width in demand.product.width) {
                entries.add(ProductWidthEntry(demand.product, width, index))
            }
        }
        return entries
    }

    private fun repeatWidth(width: Quantity<V>, times: UInt64): Quantity<V> {
        var result = arithmetic.zero(width.unit)
        repeat(times.toInt()) {
            result = arithmetic.add(result, width)
        }
        return result
    }

    private fun repeatQuantity(q: Quantity<V>, times: UInt64): Quantity<V> {
        var result = arithmetic.zero(q.unit)
        repeat(times.toInt()) {
            result = arithmetic.add(result, q)
        }
        return result
    }
}
