package fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.service

import kotlin.time.Duration
import fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.*
import fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.model.*
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.*
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.quantities.quantity.*
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
    private val maxPlans: Int64 = Int64(1000),
    private val timeout: Duration? = null,
    private val parallelism: Int64 = Int64.one,
    private val enableDominancePruning: Boolean = false,
    private val dominanceStrategy: DominanceStrategy = DominanceStrategy.SameContribution
) : Csp1dInitialCuttingPlanGenerator<V> {

    /**
     * Constructs a FullSum generator using [GenerationConstraints].
     * 使用 [GenerationConstraints] 构建 FullSum 生成器。
     *
     * @param constraints 生成约束 / generation constraints
     * @param arithmetic 物理量算术策略 / quantity arithmetic strategy
     * @param maxPlans 最大方案数（提前终止） / max plans (early termination)
     * @param timeout 超时限制 / timeout limit
    */
    constructor(
        constraints: GenerationConstraints<V>,
        arithmetic: QuantityArithmetic<V>,
        maxPlans: Int64 = Int64(1000),
        timeout: Duration? = null
    ) : this(
        constraints = constraints.toConstraints(),
        arithmetic = arithmetic,
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
        val deadline = timeout?.let { Int64(System.nanoTime() + it.inWholeNanoseconds) }
        val canonicalKeyOverride = if (input.canonicalKeyOverrides.isNotEmpty()) {
            { plan: CuttingPlan<V> -> input.canonicalKeyOverrides.firstNotNullOfOrNull { it(plan) } }
        } else null
        val dominanceAcceptOverride = if (input.dominanceAcceptOverrides.isNotEmpty()) {
            { plan: CuttingPlan<V>, existing: List<CuttingPlan<V>> -> input.dominanceAcceptOverrides.all { it(plan, existing) } }
        } else null
        val collector = GenerationCollector<V>(
            maxPlans = maxPlans,
            deadline = deadline,
            enableDominancePruning = enableDominancePruning,
            dominanceStrategy = dominanceStrategy,
            canonicalKeyOverride = canonicalKeyOverride,
            dominanceAcceptOverride = dominanceAcceptOverride
        )
        val quantityCache = GenerationQuantityCache(arithmetic)
        val widthCheck = input.widthFeasibilityCheck

        val widthIndex = GenerationWidthIndex.fromDemands(input.demands)
        if (widthIndex.isEmpty) return collector.report()
        val materialWidthIndexCache = GenerationMaterialWidthIndexCache(
            baseIndex = widthIndex,
            maxOverProduceLength = maxOverProduceLength,
            widthCheck = input.widthFeasibilityCheck
        )
        val materialSliceTemplateCache: GenerationSliceTemplateCache<V>? = if (canReuseMaterialSliceTemplates(constraints)) {
            if (parallelism == Int64.one) {
                SequentialGenerationSliceTemplateCache<V>()
            } else {
                ConcurrentGenerationSliceTemplateCache<V>()
            }
        } else {
            null
        }

        if (parallelism > Int64.one && input.materials.size > 1) {
            val reports = runGenerationTasks(
                parallelism = parallelism,
                tasks = input.materials.map { material ->
                    {
                        val localCanonicalKeyOverride = if (input.canonicalKeyOverrides.isNotEmpty()) {
                            { plan: CuttingPlan<V> -> input.canonicalKeyOverrides.firstNotNullOfOrNull { it(plan) } }
                        } else null
                        val localDominanceAcceptOverride = if (input.dominanceAcceptOverrides.isNotEmpty()) {
                            { plan: CuttingPlan<V>, existing: List<CuttingPlan<V>> -> input.dominanceAcceptOverrides.all { it(plan, existing) } }
                        } else null
                        val localCollector = GenerationCollector<V>(
                            maxPlans = maxPlans,
                            deadline = deadline,
                            enableDominancePruning = enableDominancePruning,
                            dominanceStrategy = dominanceStrategy,
                            canonicalKeyOverride = localCanonicalKeyOverride,
                            dominanceAcceptOverride = localDominanceAcceptOverride
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
                                    templates = templates,
                                    widthCheck = widthCheck
                                )
                            } else {
                                val templateRecorder = GenerationSliceTemplateRecorder<V>()
                                fullSumSearch(
                                    material = material,
                                    widthIndex = materialWidthIndex,
                                    machines = input.machines,
                                    planIndex = planIndex,
                                    collector = localCollector,
                                    quantityCache = localQuantityCache,
                                    templateRecorder = templateRecorder,
                                    widthCheck = widthCheck
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
                startedAt = Int64(startTime),
                deadline = deadline,
                canonicalKeyOverride = canonicalKeyOverride
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
                    templates = templates,
                    widthCheck = widthCheck
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
                    templateRecorder = templateRecorder,
                    widthCheck = widthCheck
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

    /**
     * Performs full-sum search to enumerate all feasible cutting plan combinations by width summation.
     * 执行全量求和搜索，按宽度值递归求和枚举所有可行的切割方案组合。
     *
     * @param material 当前物料 / the current material
     * @param widthIndex 包含所有产品宽度条目的宽度索引 / width index containing all product width entries
     * @param machines 可用机器列表 / available machine list
     * @param planIndex 方案 ID 生成器原子索引 / atomic index for plan ID generation
     * @param collector 方案收集器 / plan collector
     * @param quantityCache 数量缓存 / quantity cache
     * @param templateRecorder 可选的切片模板记录器 / optional slice template recorder
     * @param widthCheck 可选宽度可行性检查 / optional width feasibility check
    */
    private fun fullSumSearch(
        material: Material<V>,
        widthIndex: GenerationWidthIndex<V>,
        machines: List<Machine<V>>,
        planIndex: java.util.concurrent.atomic.AtomicInteger,
        collector: GenerationCollector<V>,
        quantityCache: GenerationQuantityCache<V>,
        templateRecorder: GenerationSliceTemplateRecorder<V>? = null,
        widthCheck: ((Material<V>, Product<V>, Quantity<V>) -> Boolean)? = null
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
            val remainingWidth = arithmetic.subtractOrNull(upperBound, currentWidth) ?: continue
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
                        planId = CuttingPlanIdImpl("fullsum-${material.id}-${planIndex.getAndIncrement()}")
                    )
                    collector.record(
                        plan = plan,
                        feasible = if (widthCheck != null) material.enabledWithoutWidthCheck(plan, machines) else material.enabled(plan, machines)
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
                val newWidth = arithmetic.addOrNull(currentWidth, addedWidth)
                if (newWidth == null) {
                    amount += UInt64.one
                    continue
                }

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

/**
 * Emits cutting plans from pre-recorded slice templates, converting each template into a [CuttingPlan]
 * and recording it with the collector for feasibility evaluation.
 * 从预记录的切片模板生成切割方案，将每个模板转换为 [CuttingPlan] 并通过收集器记录其可行性。
 *
 * @param material 切割方案所对应的物料 / the material to which the cutting plans apply
 * @param widthIndex 用于解析每个切片需求单位的宽度索引 / width index for resolving demand units per slice
 * @param machines 用于可行性检查的可用机器列表 / available machines for feasibility checking
 * @param planIndex 用于生成唯一方案 ID 的原子计数器 / atomic counter for generating unique plan IDs
 * @param collector 记录已生成方案的收集器 / collector that records generated plans
 * @param templates 待作为方案输出的预记录切片模板列表 / pre-recorded slice templates to emit as plans
 * @param widthCheck 可选的宽度可行性检查回调 / optional callback to check width feasibility for each plan
*/
    private fun emitTemplates(
        material: Material<V>,
        widthIndex: GenerationWidthIndex<V>,
        machines: List<Machine<V>>,
        planIndex: java.util.concurrent.atomic.AtomicInteger,
        collector: GenerationCollector<V>,
        templates: List<List<CuttingPlanSlice<V>>>,
        widthCheck: ((Material<V>, Product<V>, Quantity<V>) -> Boolean)? = null
    ) {
        for (slices in templates) {
            if (collector.shouldStop()) break
            val plan = buildPlan(
                material = material,
                slices = slices,
                widthIndex = widthIndex,
                planId = CuttingPlanIdImpl("fullsum-${material.id}-${planIndex.getAndIncrement()}")
            )
            collector.record(
                plan = plan,
                feasible = if (widthCheck != null) material.enabledWithoutWidthCheck(plan, machines) else material.enabled(plan, machines)
            )
        }
    }

/**
 * Checks whether the given slices satisfy all pruning constraints (early-bound constraints
 * used to cut off infeasible branches during search).
 * 检查给定切片是否满足所有剪枝约束（搜索过程中用于剪除不可行分支的早期绑定约束）。
 *
 * @param slices 当前的切割方案切片列表 / current list of cutting plan slices
 * @param totalWidth 切片的累计总宽度 / accumulated total width of the slices
 * @param upperBound 物料宽度上界 / material width upper bound
 * @param material 正在切割的物料 / the material being cut
 * @return 是否满足所有剪枝约束 / true if all pruning constraints are satisfied, false otherwise
*/
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

/**
 * Checks whether the given slices satisfy all leaf constraints (constraints evaluated
 * at leaf nodes when no further expansion is possible).
 * 检查给定切片是否满足所有叶节点约束（在无法继续扩展时于叶节点处评估的约束）。
 *
 * @param slices 当前的切割方案切片列表 / current list of cutting plan slices
 * @param totalWidth 切片的累计总宽度 / accumulated total width of the slices
 * @param upperBound 物料宽度上界 / material width upper bound
 * @param material 正在切割的物料 / the material being cut
 * @return 是否满足所有叶节点约束 / true if all leaf constraints are satisfied, false otherwise
*/
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

/**
 * Builds a [CuttingPlan] from the given slices by computing each slice's demand contribution
 * using the width index and quantity arithmetic.
 * 根据给定切片构建 [CuttingPlan]，通过宽度索引和数量算术计算每个切片的需求贡献。
 *
 * @param material 方案所对应的物料 / the material to which the plan applies
 * @param slices 组成方案的切割切片列表 / the cutting plan slices composing the plan
 * @param widthIndex 用于解析每个切片需求单位的宽度索引 / width index for resolving demand units per slice
 * @param planId 生成方案的唯一标识符 / unique identifier for the generated plan
 * @return 构建完成的切割方案 / the constructed [CuttingPlan]
*/
    private fun buildPlan(
        material: Material<V>,
        slices: List<CuttingPlanSlice<V>>,
        widthIndex: GenerationWidthIndex<V>,
        planId: CuttingPlanId
    ): CuttingPlan<V> {
        val contributions = slices.mapNotNull { slice ->
            val product = slice.production as Product<V>
            val demandUnit = widthIndex.demandUnitFor(
                product = product,
                width = slice.width
            ) ?: slice.width.unit
            val quantity = CuttingPlanDemandContribution.quantityOf(
                product = product,
                width = slice.width,
                amount = slice.amount,
                demandUnit = demandUnit,
                arithmetic = arithmetic,
                length = generationContributionLength(
                    product = product,
                    material = material
                )
            ).value ?: return@mapNotNull null
            CuttingPlanDemandContribution(
                product = product,
                quantity = quantity
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
