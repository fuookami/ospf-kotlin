package fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.service

import kotlin.time.Duration
import fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.*
import fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.model.*
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.*
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.*
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
    private val maxPlans: Int64 = Int64(1000),
    private val timeout: Duration? = null,
    private val parallelism: Int64 = Int64.one,
    private val enableDominancePruning: Boolean = false,
    private val dominanceStrategy: DominanceStrategy = DominanceStrategy.SameContribution
) : Csp1dInitialCuttingPlanGenerator<V> {

    constructor(
        constraints: GenerationConstraints<V>,
        arithmetic: QuantityArithmetic<V>,
        maxDepth: UInt64 = UInt64(7UL),
        maxPlans: Int64 = Int64(1000),
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
                                nSumSearch(
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
                nSumSearch(
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
 * Depth-limited DFS search that enumerates multi-product slice combinations for a single material.
 * 深度受限 DFS 搜索，枚举单个物料的多产品切片组合。
 * @param material The raw material to generate cutting plans for / 待生成切割方案的原料
 * @param widthIndex Sorted index of demand entries by width for the current material / 当前物料按宽度排序的需求条目索引
 * @param machines Available machines for feasibility checking / 用于可行性检查的可用机器列表
 * @param planIndex Atomic counter for generating unique plan IDs / 用于生成唯一方案 ID 的原子计数器
 * @param collector Collects generated plans and enforces early-termination limits / 收集生成的方案并执行提前终止限制
 * @param quantityCache Cache for width arithmetic operations to avoid repeated computation / 宽度算术运算缓存，避免重复计算
 * @param templateRecorder Optional recorder that captures slice templates for reuse across materials / 可选的切片模板记录器，捕获模板以在物料间复用
 * @param widthCheck Optional callback to validate product width feasibility against material constraints / 可选回调，验证产品宽度相对于物料约束的可行性
*/
    private fun nSumSearch(
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
            val remainingWidth = arithmetic.subtractOrNull(upperBound, currentWidth) ?: continue
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
                        planId = CuttingPlanIdImpl("nsum-${material.id}-${planIndex.getAndIncrement()}")
                    )
                    collector.record(
                        plan = plan,
                        feasible = if (widthCheck != null) material.enabledWithoutWidthCheck(plan, machines) else material.enabled(plan, machines)
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
                val newWidth = arithmetic.addOrNull(currentWidth, addedWidth)
                if (newWidth == null) {
                    amount += UInt64.one
                    continue
                }
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

/**
 * Emits cutting plans from pre-recorded slice templates for a single material.
 * 从预录的切片模板中为单个物料生成切割方案。
 * @param material The raw material to generate cutting plans for / 待生成切割方案的原料
 * @param widthIndex Sorted index of demand entries by width for the current material / 当前物料按宽度排序的需求条目索引
 * @param machines Available machines for feasibility checking / 用于可行性检查的可用机器列表
 * @param planIndex Atomic counter for generating unique plan IDs / 用于生成唯一方案 ID 的原子计数器
 * @param collector Collects generated plans and enforces early-termination limits / 收集生成的方案并执行提前终止限制
 * @param templates Pre-recorded slice combinations to replay as cutting plans / 预录的切片组合，作为切割方案回放
 * @param widthCheck Optional callback to validate product width feasibility against material constraints / 可选回调，验证产品宽度相对于物料约束的可行性
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
                planId = CuttingPlanIdImpl("nsum-${material.id}-${planIndex.getAndIncrement()}")
            )
            collector.record(
                plan = plan,
                feasible = if (widthCheck != null) material.enabledWithoutWidthCheck(plan, machines) else material.enabled(plan, machines)
            )
        }
    }

/**
 * Checks whether the given slices satisfy all pruning constraints (applied during DFS traversal).
 * 检查给定切片是否满足所有剪枝约束（在 DFS 遍历过程中应用）。
 * @param slices Current slice combination being evaluated / 当前正在评估的切片组合
 * @param totalWidth Accumulated width of the current slice combination / 当前切片组合的累计宽度
 * @param upperBound Upper bound of the material width / 物料宽度的上界
 * @param material The raw material being cut / 正在切割的原料
 * @return true if all pruning constraints are satisfied, false otherwise / 若所有剪枝约束均满足则返回 true，否则返回 false
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
 * Checks whether the given slices satisfy all leaf constraints (applied at DFS leaf nodes).
 * 检查给定切片是否满足所有叶节点约束（在 DFS 叶节点处应用）。
 * @param slices Final slice combination at a leaf node / 叶节点处的最终切片组合
 * @param totalWidth Accumulated width of the slice combination / 切片组合的累计宽度
 * @param upperBound Upper bound of the material width / 物料宽度的上界
 * @param material The raw material being cut / 正在切割的原料
 * @return true if all leaf constraints are satisfied, false otherwise / 若所有叶节点约束均满足则返回 true，否则返回 false
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
 * Builds a CuttingPlan from the given slices, computing demand contributions.
 * 从给定切片构建切割方案，计算需求贡献量。
 * @param material The raw material for this cutting plan / 该切割方案对应的原料
 * @param slices Slice combination to include in the plan / 方案中包含的切片组合
 * @param widthIndex Width index used to resolve demand units for contribution calculation / 用于解析需求单位的宽度索引，以计算贡献量
 * @param planId Unique identifier for the generated cutting plan / 生成切割方案的唯一标识
 * @return The constructed CuttingPlan with computed demand contributions / 构建完成的切割方案，包含已计算的需求贡献量
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
