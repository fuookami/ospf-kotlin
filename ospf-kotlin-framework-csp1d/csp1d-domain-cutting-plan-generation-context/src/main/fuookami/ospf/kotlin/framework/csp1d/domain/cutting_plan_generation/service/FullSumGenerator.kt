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
import fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.model.MaxKnifeCountConstraint
import fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.model.MinKnifeCountConstraint
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.CuttingPlan
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.CuttingPlanDemandContribution
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.CuttingPlanSlice
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.Machine
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.Material
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.Product
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.QuantityArithmetic
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.quantities.quantity.partialOrd
import fuookami.ospf.kotlin.utils.functional.Order

/**
 * FullSum 方案生成器，枚举所有宽度求和组合 / FullSum generator: enumerate all width-sum combinations
 *
 * 对每种物料幅宽值，枚举所有产品幅宽组合使得总宽度不超过物料幅宽上界。
 * 与 DFS 生成器的区别：DFS 按 product index 枚举，FullSum 按宽度值递归求和枚举，
 * 更适合产品宽度集合较小但组合方式多样的场景。
 *
 * @param V 数值类型 / Numeric value type
 * @property constraints 约束列表 / Constraint list
 * @property arithmetic 物理量算术策略 / Quantity arithmetic strategy
 * @property maxPlans 最大方案数（提前终止）/ Max plans (early termination)
 * @property timeout 超时限制 / Timeout limit
 * @property parallelism 按物料并行生成的协程并发度，1 表示关闭 / Coroutine parallelism by material, 1 means disabled
 * @property enableDominancePruning 是否启用同贡献候选 dominance 剪枝 / Whether to enable dominance pruning for same-contribution candidates
 */
class FullSumGenerator<V : RealNumber<V>>(
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

    private val pruningConstraints = constraints.filter { it.isPruning }
    private val leafConstraints = constraints.filter { !it.isPruning }
    private val maxKnifeCount = constraints.filterIsInstance<MaxKnifeCountConstraint<V>>().firstOrNull()?.value
    private val minKnifeCount = constraints.filterIsInstance<MinKnifeCountConstraint<V>>().firstOrNull()?.value
    private val maxOverProduceLength = generationMaxOverProduceLength(constraints)

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

        val widthIndex = GenerationWidthIndex.fromDemands(input.demands)
        if (widthIndex.isEmpty) return collector.report()
        val materialWidthIndexCache = GenerationMaterialWidthIndexCache(
            baseIndex = widthIndex,
            maxOverProduceLength = maxOverProduceLength
        )
        val materialSliceTemplateCache = if (parallelism == 1 && canReuseMaterialSliceTemplates(constraints)) {
            GenerationMaterialSliceTemplateCache<V>()
        } else {
            null
        }

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
                        val materialWidthIndex = materialWidthIndexCache.get(
                            material = material,
                            collector = localCollector
                        )
                        if (!materialWidthIndex.isEmpty) {
                            fullSumSearch(
                                material = material,
                                widthIndex = materialWidthIndex,
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
            val materialWidthIndex = materialWidthIndexCache.get(
                material = material,
                collector = collector
            )
            if (materialWidthIndex.isEmpty) continue

            val templates = materialSliceTemplateCache?.get(
                material = material,
                collector = collector
            )
            if (templates != null) {
                emitTemplates(
                    material = material,
                    widthIndex = materialWidthIndex,
                    machines = input.machines,
                    planIndex = planIndex,
                    collector = collector,
                    templates = templates
                )
            } else {
                val templateRecorder = materialSliceTemplateCache?.let { GenerationSliceTemplateRecorder<V>() }
                fullSumSearch(
                    material = material,
                    widthIndex = materialWidthIndex,
                    machines = input.machines,
                    planIndex = planIndex,
                    collector = collector,
                    quantityCache = quantityCache,
                    templateRecorder = templateRecorder
                )
                if (!collector.shouldStop() && templateRecorder != null) {
                    materialSliceTemplateCache.put(
                        material = material,
                        templates = templateRecorder.templates
                    )
                }
            }

            if (collector.shouldStop()) break
        }

        return collector.report()
    }

    private fun fullSumSearch(
        material: Material<V>,
        widthIndex: GenerationWidthIndex<V>,
        machines: List<Machine<V>>,
        planIndex: java.util.concurrent.atomic.AtomicInteger,
        collector: GenerationCollector<V>,
        quantityCache: GenerationQuantityCache<V>,
        templateRecorder: GenerationSliceTemplateRecorder<V>? = null
    ) {
        val entries = widthIndex.entries
        val upperBound = material.widthRange.upperBound
        val stack = ArrayDeque<MutableList<CuttingPlanSlice<V>>>()
        val widthStack = ArrayDeque<Quantity<V>>()
        val indexStack = ArrayDeque<Int>()

        // 起点：零宽度 / Start: zero width
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
            val currentCuts = currentSlices.fold(UInt64.zero) { acc, slice -> acc + slice.amount }
            val canAddKnife = maxKnifeCount == null || currentCuts < maxKnifeCount
            val noFittableRemainingEntry = !widthIndex.hasFittableFrom(
                startIndex = currentIndex,
                remainingWidth = remainingWidth
            )
            val cannotReachMinKnifeCount = isMinKnifeCountUnreachable(
                minKnifeCount = minKnifeCount,
                currentCuts = currentCuts,
                startIndex = currentIndex,
                remainingWidth = remainingWidth,
                widthIndex = widthIndex,
                quantityCache = quantityCache,
                remainingCutCapacity = remainingGenerationCutCapacity(
                    maxKnifeCount = maxKnifeCount,
                    currentCuts = currentCuts
                )
            )

            if (cannotReachMinKnifeCount) {
                collector.recordKnifeBoundPrunedNode()
                continue
            }

            if (
                currentIndex >= entries.size ||
                !canAddKnife ||
                noFittableRemainingEntry
            ) {
                if (noFittableRemainingEntry && currentSlices.isEmpty()) {
                    collector.recordWidthBoundPrunedNode()
                }
                // 叶节点或无法继续扩展：产出方案 / Leaf node or no extensible entry: emit plan
                if (currentSlices.isNotEmpty() && satisfiesLeafConstraints(currentSlices, currentWidth, upperBound, material)) {
                    templateRecorder?.record(currentSlices)
                    val plan = buildPlan(
                        material = material,
                        slices = currentSlices,
                        widthIndex = widthIndex,
                        planId = "fullsum-${material.id}-${planIndex.getAndIncrement()}"
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
            val maxByWidth = quantityCache.maxRepeatCount(entry.width, remainingWidth)
            val maxByKnife = maxKnifeCount?.let { max ->
                if (currentCuts >= max) UInt64.zero else max - currentCuts
            } ?: maxByWidth
            val maxAmount = minOf(maxByWidth, maxByKnife)

            // 尝试数量 0，即跳过当前产品 / Try amount 0
            stack.addLast(ArrayList(currentSlices))
            widthStack.addLast(currentWidth)
            indexStack.addLast(currentIndex + 1)

            // 尝试数量 1..maxAmount / Try amounts 1..maxAmount
            var amount = UInt64.one
            while (amount <= maxAmount) {
                val addedWidth = quantityCache.repeatWidth(entry.width, amount)
                val newWidth = arithmetic.add(currentWidth, addedWidth)

                // 幅宽剪枝 / Width pruning
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

    private fun emitTemplates(
        material: Material<V>,
        widthIndex: GenerationWidthIndex<V>,
        machines: List<Machine<V>>,
        planIndex: java.util.concurrent.atomic.AtomicInteger,
        collector: GenerationCollector<V>,
        templates: List<List<CuttingPlanSlice<V>>>
    ) {
        for (slices in templates) {
            if (collector.shouldStop()) break
            val plan = buildPlan(
                material = material,
                slices = slices,
                widthIndex = widthIndex,
                planId = "fullsum-${material.id}-${planIndex.getAndIncrement()}"
            )
            collector.record(
                plan = plan,
                feasible = material.enabled(plan, machines)
            )
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

    private fun buildPlan(
        material: Material<V>,
        slices: List<CuttingPlanSlice<V>>,
        widthIndex: GenerationWidthIndex<V>,
        planId: String
    ): CuttingPlan<V> {
        val contributions = slices.map { slice ->
            val product = slice.production as Product<V>
            val demandUnit = widthIndex.demandUnitFor(
                product = product,
                width = slice.width
            ) ?: slice.width.unit
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

}
