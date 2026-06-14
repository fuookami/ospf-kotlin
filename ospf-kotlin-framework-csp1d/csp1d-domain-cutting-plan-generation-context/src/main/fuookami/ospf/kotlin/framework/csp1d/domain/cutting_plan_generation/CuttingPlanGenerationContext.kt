package fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation

import java.util.Comparator
import fuookami.ospf.kotlin.utils.functional.Order
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.Int64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.quantities.quantity.partialOrd
import fuookami.ospf.kotlin.quantities.unit.PhysicalUnit
import fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.model.CuttingPlanCanonicalKey
import fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.model.canonicalKey
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.Costar
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.CuttingPlan
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.CuttingPlanDemandContribution
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.CuttingPlanSlice
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.Machine
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.MachineBatchShadowPriceKey
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.MachineCapacityShadowPriceKey
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.Material
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.MaterialUsageShadowPriceKey
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.Product
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.ProductDemand
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.ProductDemandShadowPriceKey
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.ShadowPriceMap
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.Csp1dDomainPolicy
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.SimpleDomainCalculationContext
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.allFeasible
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.allWidthFeasible
import fuookami.ospf.kotlin.quantities.quantity.Quantity

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
    val existingPlans: List<CuttingPlan<V>> = emptyList(),
    val domainPolicies: List<Csp1dDomainPolicy<V>> = emptyList(),
    /** 候选方案过滤器列表，每个返回 true 表示接受 / Candidate filter list, each returns true if accepted */
    val candidateFilters: List<(CuttingPlan<V>, List<CuttingPlan<V>>) -> Boolean> = emptyList(),
    /**
     * 宽度可行性判断函数，返回 true 表示该产品宽度在该物料上可切。
     * 默认使用 material.widthRange.canCut(productWidth)。
     * 下游可通过此函数放宽或替代原始宽度判断逻辑。
     *
     * Width feasibility check function, returns true if the product width is cuttable on the material.
     * Defaults to material.widthRange.canCut(productWidth).
     * Downstream can relax or replace the original width judgment logic through this function.
     *
     * @param material 物料 / Material
     * @param product 产品 / Product
     * @param productWidth 当前枚举的产品宽度 / Current enumerated product width
     */
    val widthFeasibilityCheck: ((Material<V>, Product<V>, Quantity<V>) -> Boolean)? = null,
    /**
     * 自定义 canonical key 函数列表，每个返回非 null 时替代默认 canonicalKey()。
     * 按列表顺序尝试，首个返回非 null 的值作为自定义 key。
     * Custom canonical key function list; when any returns non-null, it replaces the default canonicalKey().
     * Tried in list order; the first non-null result is used as the custom key.
     */
    val canonicalKeyOverrides: List<(CuttingPlan<V>) -> String?> = emptyList(),
    /**
     * 自定义 dominance 接受函数列表，每个返回 true 表示新候选应被接受。
     * 默认空列表表示不额外过滤。
     * Custom dominance acceptance function list; each returns true if the new candidate should be accepted.
     * Default empty list means no extra filtering.
     */
    val dominanceAcceptOverrides: List<(CuttingPlan<V>, List<CuttingPlan<V>>) -> Boolean> = emptyList()
)

/**
 * 切割方案生成终止原因 / Cutting plan generation stop reason
 */
enum class CuttingPlanGenerationStopReason {
    Exhausted,
    MaxPlans,
    Timeout
}

/**
 * 切割方案生成统计 / Cutting plan generation statistics
 *
 * @property visitedNodes 搜索访问节点数 / Visited search nodes
 * @property generatedCandidates 产出的候选方案数 / Generated candidate plan count
 * @property acceptedPlans 接受的方案数 / Accepted plan count
 * @property infeasibleCandidates 被基础可行性拒绝的候选数 / Candidate count rejected by basic feasibility
 * @property duplicateCandidates 被结构化去重过滤的候选数 / Candidate count filtered by structural deduplication
 * @property dominatedCandidates 被 dominance 剪枝过滤的候选数 / Candidate count filtered by dominance pruning
 * @property widthBoundPrunedNodes 被剩余宽度上界剪枝的搜索节点数 / Search node count pruned by remaining-width upper bound
 * @property knifeBoundPrunedNodes 被刀数下界可达性剪枝的搜索节点数 / Search node count pruned by knife-count reachability
 * @property lengthBoundPrunedEntries 被长度上界剪枝的搜索入口数 / Search entry count pruned by length upper bound
 * @property materialWidthIndexCacheHits 物料等价宽度入口缓存命中数 / Material-equivalent width-entry cache hit count
 * @property materialSliceTemplateCacheHits 物料等价切片模板缓存命中数 / Material-equivalent slice-template cache hit count
 * @property quantityCacheHits 数量缓存命中数 / Quantity cache hit count
 * @property quantityCacheMisses 数量缓存未命中数 / Quantity cache miss count
 * @property materialSliceTemplateCacheMisses 物料等价切片模板缓存未命中数 / Material-equivalent slice-template cache miss count
 * @property crossWorkerDuplicateCandidates 并行合并时跨 worker 重复候选数 / Cross-worker duplicate candidate count during parallel merge
 * @property crossContributionDominated 跨贡献 dominance 剪枝过滤的候选数 / Candidate count filtered by cross-contribution dominance pruning
 * @property elapsedMilliseconds 生成耗时毫秒数 / Generation elapsed time in milliseconds
 * @property stopReason 终止原因 / Stop reason
 */
data class CuttingPlanGenerationStatistics(
    val visitedNodes: Int64 = Int64.zero,
    val generatedCandidates: Int64 = Int64.zero,
    val acceptedPlans: Int64 = Int64.zero,
    val infeasibleCandidates: Int64 = Int64.zero,
    val duplicateCandidates: Int64 = Int64.zero,
    val dominatedCandidates: Int64 = Int64.zero,
    val widthBoundPrunedNodes: Int64 = Int64.zero,
    val knifeBoundPrunedNodes: Int64 = Int64.zero,
    val lengthBoundPrunedEntries: Int64 = Int64.zero,
    val materialWidthIndexCacheHits: Int64 = Int64.zero,
    val materialSliceTemplateCacheHits: Int64 = Int64.zero,
    val quantityCacheHits: Int64 = Int64.zero,
    val quantityCacheMisses: Int64 = Int64.zero,
    val materialSliceTemplateCacheMisses: Int64 = Int64.zero,
    val crossWorkerDuplicateCandidates: Int64 = Int64.zero,
    val crossContributionDominated: Int64 = Int64.zero,
    val elapsedMilliseconds: Int64 = Int64.zero,
    val stopReason: CuttingPlanGenerationStopReason = CuttingPlanGenerationStopReason.Exhausted
)

/**
 * 切割方案生成 benchmark 快照 / Cutting plan generation benchmark snapshot
 *
 * 该快照只包含确定性的数量类统计，适合测试和文档中做稳定比较；耗时仍保留在原始 statistics 中作为趋势观察。
 * This snapshot only contains deterministic count statistics for stable comparison in tests and docs;
 * elapsed time remains in the raw statistics for trend observation.
 *
 * @property generatorName 生成器名称 / Generator name
 * @property visitedNodes 搜索访问节点数 / Visited search nodes
 * @property generatedCandidates 产出的候选方案数 / Generated candidate plan count
 * @property acceptedPlans 接受的方案数 / Accepted plan count
 * @property infeasibleCandidates 被基础可行性拒绝的候选数 / Candidate count rejected by basic feasibility
 * @property duplicateCandidates 被结构化去重过滤的候选数 / Candidate count filtered by structural deduplication
 * @property dominatedCandidates 被 dominance 剪枝过滤的候选数 / Candidate count filtered by dominance pruning
 * @property widthBoundPrunedNodes 被剩余宽度上界剪枝的搜索节点数 / Search node count pruned by remaining-width upper bound
 * @property knifeBoundPrunedNodes 被刀数下界可达性剪枝的搜索节点数 / Search node count pruned by knife-count reachability
 * @property lengthBoundPrunedEntries 被长度上界剪枝的搜索入口数 / Search entry count pruned by length upper bound
 * @property materialWidthIndexCacheHits 物料等价宽度入口缓存命中数 / Material-equivalent width-entry cache hit count
 * @property materialSliceTemplateCacheHits 物料等价切片模板缓存命中数 / Material-equivalent slice-template cache hit count
 * @property quantityCacheHits 数量缓存命中数 / Quantity cache hit count
 * @property quantityCacheMisses 数量缓存未命中数 / Quantity cache miss count
 * @property materialSliceTemplateCacheMisses 物料等价切片模板缓存未命中数 / Material-equivalent slice-template cache miss count
 * @property crossWorkerDuplicateCandidates 并行合并时跨 worker 重复候选数 / Cross-worker duplicate candidate count during parallel merge
 * @property crossContributionDominated 跨贡献 dominance 剪枝过滤的候选数 / Candidate count filtered by cross-contribution dominance pruning
 * @property stopReason 终止原因 / Stop reason
 */
data class CuttingPlanGenerationBenchmarkSnapshot(
    val generatorName: String,
    val visitedNodes: Int64,
    val generatedCandidates: Int64,
    val acceptedPlans: Int64,
    val infeasibleCandidates: Int64,
    val duplicateCandidates: Int64,
    val dominatedCandidates: Int64,
    val widthBoundPrunedNodes: Int64,
    val knifeBoundPrunedNodes: Int64,
    val lengthBoundPrunedEntries: Int64,
    val materialWidthIndexCacheHits: Int64,
    val materialSliceTemplateCacheHits: Int64,
    val quantityCacheHits: Int64,
    val quantityCacheMisses: Int64,
    val materialSliceTemplateCacheMisses: Int64,
    val crossWorkerDuplicateCandidates: Int64,
    val crossContributionDominated: Int64,
    val stopReason: CuttingPlanGenerationStopReason
) {
    /**
     * 输出稳定文本行 / Render stable text line
     *
     * @return 可比较的稳定文本行 / Comparable stable text line
     */
    fun toStableLine(): String {
        return listOf(
            "generator=$generatorName",
            "visitedNodes=$visitedNodes",
            "generatedCandidates=$generatedCandidates",
            "acceptedPlans=$acceptedPlans",
            "infeasibleCandidates=$infeasibleCandidates",
            "duplicateCandidates=$duplicateCandidates",
            "dominatedCandidates=$dominatedCandidates",
            "widthBoundPrunedNodes=$widthBoundPrunedNodes",
            "knifeBoundPrunedNodes=$knifeBoundPrunedNodes",
            "lengthBoundPrunedEntries=$lengthBoundPrunedEntries",
            "materialWidthIndexCacheHits=$materialWidthIndexCacheHits",
            "materialSliceTemplateCacheHits=$materialSliceTemplateCacheHits",
            "quantityCacheHits=$quantityCacheHits",
            "quantityCacheMisses=$quantityCacheMisses",
            "materialSliceTemplateCacheMisses=$materialSliceTemplateCacheMisses",
            "crossWorkerDuplicateCandidates=$crossWorkerDuplicateCandidates",
            "crossContributionDominated=$crossContributionDominated",
            "stopReason=${stopReason.name}"
        ).joinToString(";")
    }

    companion object {
        /**
         * 从生成统计构造快照 / Build snapshot from generation statistics
         *
         * @param generatorName 生成器名称 / Generator name
         * @param statistics 生成统计 / Generation statistics
         * @return benchmark 快照 / Benchmark snapshot
         */
        fun from(
            generatorName: String,
            statistics: CuttingPlanGenerationStatistics
        ): CuttingPlanGenerationBenchmarkSnapshot {
            return CuttingPlanGenerationBenchmarkSnapshot(
                generatorName = generatorName,
                visitedNodes = statistics.visitedNodes,
                generatedCandidates = statistics.generatedCandidates,
                acceptedPlans = statistics.acceptedPlans,
                infeasibleCandidates = statistics.infeasibleCandidates,
                duplicateCandidates = statistics.duplicateCandidates,
                dominatedCandidates = statistics.dominatedCandidates,
                widthBoundPrunedNodes = statistics.widthBoundPrunedNodes,
                knifeBoundPrunedNodes = statistics.knifeBoundPrunedNodes,
                lengthBoundPrunedEntries = statistics.lengthBoundPrunedEntries,
                materialWidthIndexCacheHits = statistics.materialWidthIndexCacheHits,
                materialSliceTemplateCacheHits = statistics.materialSliceTemplateCacheHits,
                quantityCacheHits = statistics.quantityCacheHits,
                quantityCacheMisses = statistics.quantityCacheMisses,
                materialSliceTemplateCacheMisses = statistics.materialSliceTemplateCacheMisses,
                crossWorkerDuplicateCandidates = statistics.crossWorkerDuplicateCandidates,
                crossContributionDominated = statistics.crossContributionDominated,
                stopReason = statistics.stopReason
            )
        }
    }
}

/**
 * 切割方案生成报告 / Cutting plan generation report
 *
 * @param V 数值类型 / Numeric value type
 * @property plans 生成并接受的切割方案 / Generated and accepted cutting plans
 * @property statistics 生成统计 / Generation statistics
 */
data class CuttingPlanGenerationReport<V : RealNumber<V>>(
    val plans: List<CuttingPlan<V>>,
    val statistics: CuttingPlanGenerationStatistics
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

    /**
     * 生成初始切割方案并返回统计 / Generate initial cutting plans with statistics
     *
     * @param input 切割方案生成输入 / Cutting plan generation input
     * @return 切割方案生成报告 / Cutting plan generation report
     */
    fun generateWithReport(input: CuttingPlanGenerationInput<V>): CuttingPlanGenerationReport<V> {
        val startTime = System.nanoTime()
        val plans = generate(input)
        return CuttingPlanGenerationReport(
            plans = plans,
            statistics = CuttingPlanGenerationStatistics(
                generatedCandidates = Int64(plans.size.toLong()),
                acceptedPlans = Int64(plans.size.toLong()),
                elapsedMilliseconds = Int64((System.nanoTime() - startTime) / 1_000_000L)
            )
        )
    }
}

/**
 * 定价输入 / Pricing input
 *
 * @param V 数值类型 / Numeric value type
 */
data class Csp1dPricingInput<V : RealNumber<V>>(
    val generationInput: CuttingPlanGenerationInput<V>,
    val shadowPrices: ShadowPriceMap<V>,
    val maxGeneratedPlans: UInt64 = UInt64.one,
    val objectiveConfig: Csp1dPricingObjectiveConfig<V> = Csp1dPricingObjectiveConfig(),
    /** 定价成本修正器列表，每个接收 (candidate, baseCost) 返回修正后成本 / Pricing cost modifier list */
    val pricingCostModifiers: List<(CuttingPlan<V>, V) -> V> = emptyList(),
    /** 定价收益修正器列表，每个接收 (candidate, baseBenefit) 返回修正后收益 / Pricing benefit modifier list */
    val pricingBenefitModifiers: List<(CuttingPlan<V>, V) -> V> = emptyList(),
    /** 自定义 isImproving 判断器列表，返回 null 表示跳过 / Custom isImproving judges, null to skip */
    val isImprovingJudges: List<(CuttingPlan<V>, V, V) -> Boolean?> = emptyList(),
    /**
     * 自定义 canonical key 函数列表，覆盖 generationInput 中的同名字段。
     * 每个返回非 null 时替代默认 canonicalKey()。
     * Custom canonical key function list, overriding the same field in generationInput.
     * When any returns non-null, it replaces the default canonicalKey().
     */
    val canonicalKeyOverrides: List<(CuttingPlan<V>) -> String?> = emptyList()
)

/**
 * 定价候选的方案级目标提示 / Plan-level objective hints for pricing candidates
 *
 * 这些配置只用于候选筛选和排序，不改变 LP shadow price 提取口径。
 * These settings only affect candidate filtering and ordering, not LP shadow price extraction.
 *
 * @param V 数值类型 / Numeric value type
 * @property planUsagePenalty 单次方案使用惩罚 / Penalty per plan usage
 * @property trimWidthPenalty 余宽惩罚 / Trim width penalty
 * @property restMaterialPenalty 余料面积代理惩罚 / Rest material area proxy penalty
 * @property materialCostPenalty 按物料 ID 的成本惩罚 / Per-material cost penalty
 */
data class Csp1dPricingObjectiveConfig<V : RealNumber<V>>(
    val planUsagePenalty: V? = null,
    val trimWidthPenalty: V? = null,
    val restMaterialPenalty: V? = null,
    val materialCostPenalty: Map<String, V> = emptyMap()
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

    /**
     * 生成新列并返回统计 / Generate new columns with statistics
     *
     * @param input 定价输入 / Pricing input
     * @return 切割方案生成报告 / Cutting plan generation report
     */
    fun generateWithReport(input: Csp1dPricingInput<V>): CuttingPlanGenerationReport<V> {
        val startTime = System.nanoTime()
        val plans = generate(input)
        return CuttingPlanGenerationReport(
            plans = plans,
            statistics = CuttingPlanGenerationStatistics(
                generatedCandidates = Int64(plans.size.toLong()),
                acceptedPlans = Int64(plans.size.toLong()),
                elapsedMilliseconds = Int64((System.nanoTime() - startTime) / 1_000_000L)
            )
        )
    }
}

/**
 * 简单初始方案生成器 / Simple initial cutting plan generator
 *
 * @param V 数值类型 / Numeric value type
 */
class SimpleInitialCuttingPlanGenerator<V : RealNumber<V>> : Csp1dInitialCuttingPlanGenerator<V> {
    override fun generate(input: CuttingPlanGenerationInput<V>): List<CuttingPlan<V>> {
        val domainPolicies = input.domainPolicies
        val candidateFilters = input.candidateFilters
        val widthCheck = input.widthFeasibilityCheck
        val domainValueSample = input.demands.firstOrNull()?.quantity?.value
            ?: input.materials.firstOrNull()?.widthRange?.upperBound?.value
            ?: return emptyList()
        val plans = ArrayList<CuttingPlan<V>>()
        for (material in input.materials) {
            for (demand in input.demands) {
                val width = demand.product.width.firstOrNull { productWidth ->
                    // Use domain policy width check if provided, otherwise fall back to canCut
                    if (widthCheck != null) {
                        widthCheck(material, demand.product, productWidth)
                    } else {
                        material.widthRange.canCut(productWidth)
                    }
                } ?: continue
                val plan = CuttingPlan(
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
                if (if (widthCheck != null) material.enabledWithoutWidthCheck(plan, input.machines) else material.enabled(plan, input.machines)) {
                    // Apply domain policy feasibility checks
                    if (domainPolicies.isNotEmpty()) {
                        val ctx = SimpleDomainCalculationContext(
                            plan = plan,
                            planIndex = plans.size,
                            domainValueSample = domainValueSample
                        )
                        if (!allFeasible(domainPolicies, ctx) || !allWidthFeasible(domainPolicies, ctx)) {
                            continue
                        }
                    }
                    // Apply candidate filters
                    if (candidateFilters.isNotEmpty()) {
                        val accepted = candidateFilters.all { it(plan, input.existingPlans) }
                        if (!accepted) {
                            continue
                        }
                    }
                    plans.add(plan)
                }
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
        val domainPolicies = input.generationInput.domainPolicies
        val candidateFilters = input.generationInput.candidateFilters
        val widthCheck = input.generationInput.widthFeasibilityCheck
        val domainValueSample = input.generationInput.demands.firstOrNull()?.quantity?.value
            ?: input.generationInput.materials.firstOrNull()?.widthRange?.upperBound?.value
            ?: return emptyList()
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
                // Use domain policy width check if provided, otherwise fall back to contains
                if (widthCheck != null) {
                    widthCheck(material, demand.product, productWidth)
                } else {
                    material.widthRange.width.contains(productWidth)
                }
            } ?: continue
            val plan = CuttingPlan(
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
            if (if (widthCheck != null) material.enabledWithoutWidthCheck(plan, input.generationInput.machines) else material.enabled(plan, input.generationInput.machines)) {
                // Apply domain policy feasibility checks
                if (domainPolicies.isNotEmpty()) {
                    val ctx = SimpleDomainCalculationContext(
                        plan = plan,
                        planIndex = pricedPlans.size,
                        domainValueSample = domainValueSample
                    )
                    if (!allFeasible(domainPolicies, ctx) || !allWidthFeasible(domainPolicies, ctx)) {
                        continue
                    }
                }
                // Apply candidate filters
                if (candidateFilters.isNotEmpty()) {
                    val accepted = candidateFilters.all { it(plan, input.generationInput.existingPlans) }
                    if (!accepted) {
                        continue
                    }
                }
                pricedPlans.add(plan)
            }
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
        return generateWithReport(input).plans
    }

    override fun generateWithReport(input: Csp1dPricingInput<V>): CuttingPlanGenerationReport<V> {
        val report = enumerator.generateWithReport(input.generationInput)
        if (report.plans.isEmpty()) {
            return report
        }

        // Resolve canonical key overrides: pricing-level overrides take precedence, then generation-level
        val canonicalKeyOverrides = input.canonicalKeyOverrides.ifEmpty {
            input.generationInput.canonicalKeyOverrides
        }
        val resolveCanonicalKey: (CuttingPlan<V>) -> CuttingPlanCanonicalKey = { plan ->
            val customKey = canonicalKeyOverrides.firstNotNullOfOrNull { it(plan) }
            if (customKey != null) CuttingPlanCanonicalKey(customKey) else plan.canonicalKey()
        }

        val existingIds = input.generationInput.existingPlans.map { it.id }.toSet()
        val existingKeys = input.generationInput.existingPlans.map { resolveCanonicalKey(it) }.toSet()
        val maxGeneratedPlans = input.maxGeneratedPlans.toULong()
        val shadowPrices = input.shadowPrices

        val pricedPlans = report.plans
            .asSequence()
            .map { plan -> plan to resolveCanonicalKey(plan) }
            .filter { (plan, key) -> plan.id !in existingIds && key !in existingKeys }
            .distinctBy { (_, key) -> key }
            .map { (plan, _) ->
                val baseBenefit = computeDualBenefit(plan, shadowPrices)
                val benefit = if (input.pricingBenefitModifiers.isNotEmpty()) {
                    input.pricingBenefitModifiers.fold(baseBenefit) { b, modifier -> modifier(plan, b) }
                } else {
                    baseBenefit
                }
                PricedCandidate(
                    plan = plan,
                    benefit = benefit,
                    objectiveCost = computeObjectiveCost(plan, input.objectiveConfig).let { baseCost ->
                        // Apply pricing cost modifiers
                        input.pricingCostModifiers.fold(baseCost) { cost, modifier -> modifier(plan, cost) }
                    }
                )
            }
            .filter { candidate -> isImproving(candidate, input.isImprovingJudges) }
            .sortedWith(compareByScore())
            .map { it.plan }
            .take(maxGeneratedPlans.toInt())
            .toList()
        return report.copy(
            plans = pricedPlans,
            statistics = report.statistics.copy(
                acceptedPlans = Int64(pricedPlans.size.toLong())
            )
        )
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
            val machineBatchKey = MachineBatchShadowPriceKey(machineId)
            val machineBatchSp = shadowPrices[machineBatchKey]
            if (machineBatchSp != null) {
                benefit += machineBatchSp
            }

            val machineKey = MachineCapacityShadowPriceKey(machineId)
            val machineSp = shadowPrices[machineKey]
            val capacityConsumption = plan.capacityConsumption
            if (machineSp != null && capacityConsumption != null) {
                benefit += capacityConsumption.value * machineSp
            }
        }

        return benefit
    }

    private fun isPositive(value: V): Boolean {
        return when (value partialOrd value.constants.zero) {
            is Order.Greater -> true
            else -> false
        }
    }

    private fun computeObjectiveCost(
        plan: CuttingPlan<V>,
        objectiveConfig: Csp1dPricingObjectiveConfig<V>
    ): V {
        var cost = plan.material.widthRange.upperBound.value.constants.one

        val planUsagePenalty = objectiveConfig.planUsagePenalty
        if (planUsagePenalty != null) {
            cost += planUsagePenalty
        }

        val trimWidthPenalty = objectiveConfig.trimWidthPenalty
        val restWidthValue = plan.restWidth?.value
        if (trimWidthPenalty != null && restWidthValue != null && isPositive(restWidthValue)) {
            cost += restWidthValue * trimWidthPenalty
        }

        val restMaterialPenalty = objectiveConfig.restMaterialPenalty
        val materialLengthValue = plan.material.length?.value
        if (
            restMaterialPenalty != null &&
            restWidthValue != null &&
            materialLengthValue != null &&
            isPositive(restWidthValue) &&
            isPositive(materialLengthValue)
        ) {
            cost += restWidthValue * materialLengthValue * restMaterialPenalty
        }

        val materialCostPenalty = objectiveConfig.materialCostPenalty[plan.material.id]
        if (materialCostPenalty != null) {
            cost += materialCostPenalty
        }

        return cost
    }

    private fun isImproving(
        candidate: PricedCandidate<V>,
        judges: List<(CuttingPlan<V>, V, V) -> Boolean?> = emptyList()
    ): Boolean {
        // Check custom judges first
        for (judge in judges) {
            val result = judge(candidate.plan, candidate.benefit, candidate.objectiveCost)
            if (result != null) return result
        }
        // Default: benefit > objectiveCost
        return when (candidate.benefit partialOrd candidate.objectiveCost) {
            is Order.Greater -> true
            else -> false
        }
    }

    private fun compareByScore(): Comparator<PricedCandidate<V>> {
        return Comparator { left, right ->
            val leftScoreSide = left.benefit + right.objectiveCost
            val rightScoreSide = right.benefit + left.objectiveCost
            when (leftScoreSide partialOrd rightScoreSide) {
                is Order.Greater -> -1
                is Order.Less -> 1
                else -> 0
            }
        }
    }

    private data class PricedCandidate<V : RealNumber<V>>(
        val plan: CuttingPlan<V>,
        val benefit: V,
        val objectiveCost: V
    )
}
