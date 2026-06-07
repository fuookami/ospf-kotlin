package fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.service

import kotlin.time.Duration
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.Csp1dInitialCuttingPlanGenerator
import fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.CuttingPlanGenerationInput
import fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.model.CuttingPlanConstraint
import fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.model.CuttingPlanConstraintContext
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
import fuookami.ospf.kotlin.quantities.unit.PhysicalUnit
import fuookami.ospf.kotlin.utils.functional.Order

/**
 * N-Sum 方案生成器，深度受限 DFS 枚举多产品组合方案 / N-Sum generator: depth-limited DFS for multi-product combination plans
 *
 * @param V 数值类型 / Numeric value type
 * @property constraints 约束列表 / Constraint list
 * @property arithmetic 物理量算术策略 / Quantity arithmetic strategy
 * @property maxDepth 最大深度（最大切片总数）/ Max depth (max total slices)
 * @property maxPlans 最大方案数（提前终止）/ Max plans (early termination)
 * @property timeout 超时限制 / Timeout limit
 */
class NSumGenerator<V : RealNumber<V>>(
    private val constraints: List<CuttingPlanConstraint<V>> = emptyList(),
    private val arithmetic: QuantityArithmetic<V>,
    private val maxDepth: UInt64 = UInt64(7UL),
    private val maxPlans: Int = 1000,
    private val timeout: Duration? = null
) : Csp1dInitialCuttingPlanGenerator<V> {

    constructor(
        constraints: GenerationConstraints<V>,
        arithmetic: QuantityArithmetic<V>,
        maxDepth: UInt64 = UInt64(7UL),
        maxPlans: Int = 1000,
        timeout: Duration? = null
    ) : this(
        constraints = constraints.toConstraints(),
        arithmetic = arithmetic,
        maxDepth = maxDepth,
        maxPlans = maxPlans,
        timeout = timeout
    )

    private data class ProductWidthEntry<V : RealNumber<V>>(
        val product: Product<V>,
        val width: Quantity<V>,
        val demandUnit: PhysicalUnit
    )

    private val pruningConstraints = constraints.filter { it.isPruning }
    private val leafConstraints = constraints.filter { !it.isPruning }

    override fun generate(input: CuttingPlanGenerationInput<V>): List<CuttingPlan<V>> {
        val plans = ArrayList<CuttingPlan<V>>()
        val planIndex = java.util.concurrent.atomic.AtomicInteger(0)
        val deadline = timeout?.let { System.nanoTime() + it.inWholeNanoseconds }

        val entries = buildProductWidthEntries(input.demands)
        if (entries.isEmpty()) return emptyList()

        for (material in input.materials) {
            val materialEntries = entries.filter { material.widthRange.canCut(it.width) }
            if (materialEntries.isEmpty()) continue

            nSumSearch(
                material = material,
                entries = materialEntries,
                planIndex = planIndex,
                plans = plans,
                deadline = deadline
            )

            if (plans.size >= maxPlans) break
            if (deadline != null && System.nanoTime() > deadline) break
        }

        return plans.take(maxPlans)
    }

    private fun nSumSearch(
        material: Material<V>,
        entries: List<ProductWidthEntry<V>>,
        planIndex: java.util.concurrent.atomic.AtomicInteger,
        plans: MutableList<CuttingPlan<V>>,
        deadline: Long?
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
            if (deadline != null && System.nanoTime() > deadline) break

            val currentSlices = stack.removeLast()
            val currentWidth = widthStack.removeLast()
            val currentCuts = cutStack.removeLast()
            val currentIndex = indexStack.removeLast()

            if (currentIndex >= entries.size) {
                if (currentSlices.isNotEmpty() && satisfiesLeafConstraints(currentSlices, currentWidth, upperBound, material)) {
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

            // 深度约束：最大数量受剩余深度限制 / Depth constraint
            val remainingDepth = maxDepth - currentCuts
            if (remainingDepth == UInt64.zero) {
                if (currentSlices.isNotEmpty() && satisfiesLeafConstraints(currentSlices, currentWidth, upperBound, material)) {
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

            val maxByWidth = computeMaxByWidth(entry.width, remainingWidth)
            val maxAmount = minOf(maxByWidth, remainingDepth)

            // 尝试数量 0 / Try amount 0
            stack.addLast(ArrayList(currentSlices))
            widthStack.addLast(currentWidth)
            cutStack.addLast(currentCuts)
            indexStack.addLast(currentIndex + 1)

            // 尝试数量 1..maxAmount / Try amounts 1..maxAmount
            var amount = UInt64.one
            while (amount <= maxAmount) {
                val addedWidth = repeatWidth(entry.width, amount)
                val newWidth = arithmetic.add(currentWidth, addedWidth)
                val newCuts = currentCuts + amount

                val newSlices = ArrayList(currentSlices)
                newSlices.add(
                    CuttingPlanSlice(
                        production = entry.product,
                        width = entry.width,
                        amount = amount
                    )
                )

                // 剪枝约束检查 / Pruning constraint check
                if (!satisfiesPruningConstraints(newSlices, newWidth, upperBound, material)) {
                    amount += UInt64.one
                    continue
                }

                stack.addLast(newSlices)
                widthStack.addLast(newWidth)
                cutStack.addLast(newCuts)
                indexStack.addLast(currentIndex + 1)
                amount += UInt64.one
            }
        }
    }

    private fun satisfiesPruningConstraints(
        slices: List<CuttingPlanSlice<V>>,
        totalWidth: Quantity<V>,
        upperBound: Quantity<V>,
        material: Material<V>
    ): Boolean {
        if (pruningConstraints.isEmpty()) return true
        val context = CuttingPlanConstraintContext(slices, totalWidth, upperBound, material)
        return pruningConstraints.all { it.isSatisfied(context) }
    }

    private fun satisfiesLeafConstraints(
        slices: List<CuttingPlanSlice<V>>,
        totalWidth: Quantity<V>,
        upperBound: Quantity<V>,
        material: Material<V>
    ): Boolean {
        if (leafConstraints.isEmpty()) return true
        val context = CuttingPlanConstraintContext(slices, totalWidth, upperBound, material)
        return leafConstraints.all { it.isSatisfied(context) }
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
            val demandUnit = entries.firstOrNull {
                it.product == slice.production && it.width == slice.width
            }?.demandUnit ?: slice.width.unit
            CuttingPlanDemandContribution(
                product = product,
                quantity = computeSliceContribution(
                    product = product,
                    width = slice.width,
                    amount = slice.amount,
                    demandUnit = demandUnit
                )
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

    private fun computeSliceContribution(
        product: Product<V>,
        width: Quantity<V>,
        amount: UInt64,
        demandUnit: PhysicalUnit
    ): Quantity<V> {
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
        val onePerPiece = Quantity(width.value.constants.one, demandUnit)
        return repeatQuantity(onePerPiece, amount)
    }

    private fun buildProductWidthEntries(demands: List<ProductDemand<V>>): List<ProductWidthEntry<V>> {
        val entries = ArrayList<ProductWidthEntry<V>>()
        for (demand in demands) {
            for (width in demand.product.width) {
                entries.add(
                    ProductWidthEntry(
                        product = demand.product,
                        width = width,
                        demandUnit = demand.quantity.unit
                    )
                )
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
