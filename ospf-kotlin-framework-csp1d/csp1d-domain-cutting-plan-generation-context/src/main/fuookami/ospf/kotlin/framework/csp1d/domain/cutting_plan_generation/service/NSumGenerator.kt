package fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.service

import kotlin.time.Duration
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.Csp1dInitialCuttingPlanGenerator
import fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.CuttingPlanGenerationInput
import fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.CuttingPlanGenerationReport
import fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.model.CuttingPlanConstraint
import fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.model.CuttingPlanConstraintContext
import fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.model.DominanceStrategy
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

/**
 * N-Sum 方案生成器，深度受限 DFS 枚举多产品组合方案 / N-Sum generator: depth-limited DFS for multi-product combination plans
 *
 * @param V 数值类型 / Numeric value type
 * @property constraints 约束列表 / Constraint list
 * @property arithmetic 物理量算术策略 / Quantity arithmetic strategy
 * @property maxDepth 最大深度（最大切片总数）/ Max depth (max total slices)
 * @property maxPlans 最大方案数（提前终止）/ Max plans (early termination)
 * @property timeout 超时限制 / Timeout limit
 * @property parallelism 按物料并行生成的协程并发度，1 表示关闭 / Coroutine parallelism by material, 1 means disabled
 * @property enableDominancePruning 是否启用同贡献候选 dominance 剪枝 / Whether to enable dominance pruning for same-contribution candidates
 */
class NSumGenerator<V : RealNumber<V>>(
    private val constraints: List<CuttingPlanConstraint<V>> = emptyList(),
    private val arithmetic: QuantityArithmetic<V>,
    private val maxDepth: UInt64 = UInt64(7UL),
    private val maxPlans: Int = 1000,
    private val timeout: Duration? = null,
    private val parallelism: Int = 1,
    private val enableDominancePruning: Boolean = false,
    private val dominanceStrategy: DominanceStrategy = DominanceStrategy.SameContribution
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
        timeout = timeout,
        parallelism = constraints.parallelism,
        enableDominancePruning = constraints.enableDominancePruning,
        dominanceStrategy = constraints.dominanceStrategy
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
            enableDominancePruning = enableDominancePruning,
            dominanceStrategy = dominanceStrategy
        )
        val quantityCache = GenerationQuantityCache(arithmetic)

        val widthIndex = GenerationWidthIndex.fromDemands(input.demands)
        if (widthIndex.isEmpty) return collector.report()
        val materialWidthIndexCache = GenerationMaterialWidthIndexCache(
            baseIndex = widthIndex,
            maxOverProduceLength = maxOverProduceLength
        )
        val materialSliceTemplateCache: GenerationSliceTemplateCache<V>? = if (canReuseMaterialSliceTemplates(constraints)) {
            if (parallelism == 1) {
                SequentialGenerationSliceTemplateCache<V>()
            } else {
                ConcurrentGenerationSliceTemplateCache<V>()
            }
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
                            enableDominancePruning = enableDominancePruning,
                            dominanceStrategy = dominanceStrategy
                        )
                        val localQuantityCache = GenerationQuantityCache(arithmetic)
                        val materialWidthIndex = materialWidthIndexCache.get(
                            material = material,
                            collector = localCollector
                        )
                        if (!materialWidthIndex.isEmpty) {
                            val templates = materialSliceTemplateCache?.get(material, localCollector)
                            if (templates != null) {
                                emitTemplates(
                                    material = material,
                                    widthIndex = materialWidthIndex,
                                    machines = input.machines,
                                    planIndex = planIndex,
                                    collector = localCollector,
                                    templates = templates
                                )
                            } else {
                                val templateRecorder = GenerationSliceTemplateRecorder<V>()
                                nSumSearch(
                                    material = material,
                                    widthIndex = materialWidthIndex,
                                    machines = input.machines,
                                    planIndex = planIndex,
                                    collector = localCollector,
                                    quantityCache = localQuantityCache,
                                    templateRecorder = templateRecorder
                                )
                                if (!localCollector.shouldStop()) {
                                    materialSliceTemplateCache?.put(material, templateRecorder.templates)
                                }
                            }
                        }
                        val localReport = localCollector.report()
                        localReport.copy(
                            statistics = localReport.statistics.copy(
                                quantityCacheHits = localQuantityCache.totalHits,
                                quantityCacheMisses = localQuantityCache.totalMisses
                            )
                        )
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
                nSumSearch(
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

        val report = collector.report()
        return report.copy(
            statistics = report.statistics.copy(
                quantityCacheHits = quantityCache.totalHits,
                quantityCacheMisses = quantityCache.totalMisses
            )
        )
    }

    private fun nSumSearch(
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
        val cutStack = ArrayDeque<UInt64>()
        val indexStack = ArrayDeque<Int>()

        stack.addLast(ArrayList())
        widthStack.addLast(arithmetic.zero(upperBound.unit))
        cutStack.addLast(UInt64.zero)
        indexStack.addLast(0)

        while (stack.isNotEmpty() && !collector.shouldStop()) {
            if (collector.isTimedOut()) break

            collector.visitNode()
            val currentSlices = stack.removeLast()
            val currentWidth = widthStack.removeLast()
            val currentCuts = cutStack.removeLast()
            val currentIndex = indexStack.removeLast()
            val remainingWidth = arithmetic.subtract(upperBound, currentWidth)
            val remainingDepth = maxDepth - currentCuts
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
                    currentCuts = currentCuts,
                    searchCutCapacity = remainingDepth
                )
            )

            if (cannotReachMinKnifeCount) {
                collector.recordKnifeBoundPrunedNode()
                continue
            }

            if (
                currentIndex >= entries.size ||
                remainingDepth == UInt64.zero ||
                noFittableRemainingEntry
            ) {
                if (noFittableRemainingEntry && currentSlices.isEmpty()) {
                    collector.recordWidthBoundPrunedNode()
                }
                if (currentSlices.isNotEmpty() && satisfiesLeafConstraints(currentSlices, currentWidth, upperBound, material)) {
                    templateRecorder?.record(currentSlices)
                    val plan = buildPlan(
                        material = material,
                        slices = currentSlices,
                        widthIndex = widthIndex,
                        planId = "nsum-${material.id}-${planIndex.getAndIncrement()}"
                    )
                    collector.record(
                        plan = plan,
                        feasible = material.enabled(plan, machines)
                    )
                }
                continue
            }

            val entry = entries[currentIndex]

            val maxByWidth = quantityCache.maxRepeatCount(entry.width, remainingWidth)
            val maxAmount = minOf(maxByWidth, remainingDepth)

            // 尝试数量 0 / Try amount 0
            stack.addLast(ArrayList(currentSlices))
            widthStack.addLast(currentWidth)
            cutStack.addLast(currentCuts)
            indexStack.addLast(currentIndex + 1)

            // 尝试数量 1..maxAmount / Try amounts 1..maxAmount
            var amount = UInt64.one
            while (amount <= maxAmount) {
                val addedWidth = quantityCache.repeatWidth(entry.width, amount)
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
                planId = "nsum-${material.id}-${planIndex.getAndIncrement()}"
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
