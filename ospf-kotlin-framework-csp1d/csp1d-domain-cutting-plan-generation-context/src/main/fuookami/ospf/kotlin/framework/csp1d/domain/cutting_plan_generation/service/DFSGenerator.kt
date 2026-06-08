package fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.service

import kotlin.time.Duration
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.Csp1dInitialCuttingPlanGenerator
import fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.CuttingPlanGenerationInput
import fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.CuttingPlanGenerationReport
import fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.model.CuttingPlanConstraint
import fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.model.CuttingPlanConstraintContext
import fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.model.GenerationConstraints
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.CuttingPlan
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.CuttingPlanDemandContribution
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.CuttingPlanSlice
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.Machine
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.Material
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.Product
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.ProductDemand
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.QuantityArithmetic
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.quantities.quantity.partialOrd
import fuookami.ospf.kotlin.quantities.unit.PhysicalUnit
import fuookami.ospf.kotlin.utils.functional.Order

/**
 * DFS 方案生成器，栈式深度优先搜索枚举多产品组合方案 / DFS generator: stack-based depth-first search for multi-product combination plans
 *
 * @param V 数值类型 / Numeric value type
 * @property constraints 约束列表 / Constraint list
 * @property arithmetic 物理量算术策略 / Quantity arithmetic strategy
 * @property maxPlans 最大方案数（提前终止）/ Max plans (early termination)
 * @property timeout 超时限制 / Timeout limit
 * @property parallelism 按物料并行生成的协程并发度，1 表示关闭 / Coroutine parallelism by material, 1 means disabled
 * @property enableDominancePruning 是否启用同贡献候选 dominance 剪枝 / Whether to enable dominance pruning for same-contribution candidates
 */
class DFSGenerator<V : RealNumber<V>>(
    private val constraints: List<CuttingPlanConstraint<V>> = emptyList(),
    private val arithmetic: QuantityArithmetic<V>,
    private val maxPlans: Int = 1000,
    private val timeout: Duration? = null,
    private val parallelism: Int = 1,
    private val enableDominancePruning: Boolean = false
) : Csp1dInitialCuttingPlanGenerator<V> {

    constructor(
        constraints: GenerationConstraints<V>,
        arithmetic: QuantityArithmetic<V>,
        maxPlans: Int = 1000,
        timeout: Duration? = null
    ) : this(
        constraints = constraints.toConstraints(),
        arithmetic = arithmetic,
        maxPlans = maxPlans,
        timeout = timeout,
        parallelism = constraints.parallelism,
        enableDominancePruning = constraints.enableDominancePruning
    )

    private data class ProductWidthEntry<V : RealNumber<V>>(
        val product: Product<V>,
        val width: Quantity<V>,
        val demandUnit: PhysicalUnit
    )

    private val pruningConstraints = constraints.filter { it.isPruning }
    private val leafConstraints = constraints.filter { !it.isPruning }

    override fun generate(input: CuttingPlanGenerationInput<V>): List<CuttingPlan<V>> {
        return generateWithReport(input).plans
    }

    override fun generateWithReport(input: CuttingPlanGenerationInput<V>): CuttingPlanGenerationReport<V> {
        val startTime = System.nanoTime()
        val planIndex = java.util.concurrent.atomic.AtomicInteger(0)
        val deadline = timeout?.let { System.nanoTime() + it.inWholeNanoseconds }
        val collector = GenerationCollector<V>(
            maxPlans = maxPlans,
            deadline = deadline,
            enableDominancePruning = enableDominancePruning
        )
        val quantityCache = GenerationQuantityCache(arithmetic)

        val entries = buildProductWidthEntries(input.demands)
        if (entries.isEmpty()) return collector.report()

        if (parallelism > 1 && input.materials.size > 1) {
            val reports = runGenerationTasks(
                parallelism = parallelism,
                tasks = input.materials.map { material ->
                    {
                        val localCollector = GenerationCollector<V>(
                            maxPlans = maxPlans,
                            deadline = deadline,
                            enableDominancePruning = enableDominancePruning
                        )
                        val materialEntries = entries.filter { material.widthRange.canCut(it.width) }
                        if (materialEntries.isNotEmpty()) {
                            dfsSearch(
                                material = material,
                                entries = materialEntries,
                                machines = input.machines,
                                planIndex = planIndex,
                                collector = localCollector,
                                quantityCache = GenerationQuantityCache(arithmetic)
                            )
                        }
                        localCollector.report()
                    }
                }
            )
            return mergeGenerationReports(
                reports = reports,
                maxPlans = maxPlans,
                startedAt = startTime,
                deadline = deadline
            )
        }

        for (material in input.materials) {
            val materialEntries = entries.filter { material.widthRange.canCut(it.width) }
            if (materialEntries.isEmpty()) continue

            dfsSearch(
                material = material,
                entries = materialEntries,
                machines = input.machines,
                planIndex = planIndex,
                collector = collector,
                quantityCache = quantityCache
            )

            if (collector.shouldStop()) break
        }

        return collector.report()
    }

    private fun dfsSearch(
        material: Material<V>,
        entries: List<ProductWidthEntry<V>>,
        machines: List<Machine<V>>,
        planIndex: java.util.concurrent.atomic.AtomicInteger,
        collector: GenerationCollector<V>,
        quantityCache: GenerationQuantityCache<V>
    ) {
        val upperBound = material.widthRange.upperBound
        val stack = ArrayDeque<MutableList<CuttingPlanSlice<V>>>()
        val widthStack = ArrayDeque<Quantity<V>>()
        val indexStack = ArrayDeque<Int>()

        // 起点：索引 0、零宽度 / Start: index 0, zero width
        stack.addLast(ArrayList())
        widthStack.addLast(arithmetic.zero(upperBound.unit))
        indexStack.addLast(0)

        while (stack.isNotEmpty() && !collector.shouldStop()) {
            if (collector.isTimedOut()) break

            collector.visitNode()
            val currentSlices = stack.removeLast()
            val currentWidth = widthStack.removeLast()
            val currentIndex = indexStack.removeLast()
            val remainingWidth = arithmetic.subtract(upperBound, currentWidth)

            if (
                currentIndex >= entries.size ||
                !hasFittableRemainingEntry(
                    entries = entries,
                    startIndex = currentIndex,
                    remainingWidth = remainingWidth
                )
            ) {
                // 叶节点或无法继续扩展：构造方案 / Leaf node or no extensible entry: build plan
                if (currentSlices.isNotEmpty() && satisfiesLeafConstraints(currentSlices, currentWidth, upperBound, material)) {
                    val plan = buildPlan(
                        material = material,
                        slices = currentSlices,
                        entries = entries,
                        planId = "dfs-${material.id}-${planIndex.getAndIncrement()}"
                    )
                    collector.record(
                        plan = plan,
                        feasible = material.enabled(plan, machines)
                    )
                }
                continue
            }

            val entry = entries[currentIndex]

            // 当前产品宽度的最大数量 / Max amount for this product-width
            val maxAmount = quantityCache.maxRepeatCount(entry.width, remainingWidth)

            // 尝试数量 0，即跳过当前产品 / Try amount 0, skipping this product
            stack.addLast(ArrayList(currentSlices))
            widthStack.addLast(currentWidth)
            indexStack.addLast(currentIndex + 1)

            // 尝试数量 1..maxAmount / Try amounts 1..maxAmount
            var amount = UInt64.one
            while (amount <= maxAmount) {
                val addedWidth = quantityCache.repeatWidth(entry.width, amount)
                val newWidth = arithmetic.add(currentWidth, addedWidth)

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
                indexStack.addLast(currentIndex + 1)
                amount += UInt64.one
            }
        }
    }

    private fun hasFittableRemainingEntry(
        entries: List<ProductWidthEntry<V>>,
        startIndex: Int,
        remainingWidth: Quantity<V>
    ): Boolean {
        for (index in startIndex until entries.size) {
            if ((remainingWidth.value partialOrd entries[index].width.value) !is Order.Less) {
                return true
            }
        }
        return false
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
                quantity = CuttingPlanDemandContribution.quantityOf(
                    product = product,
                    width = slice.width,
                    amount = slice.amount,
                    demandUnit = demandUnit,
                    arithmetic = arithmetic,
                    length = generationContributionLength(
                        product = product,
                        material = material
                    )
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

}
