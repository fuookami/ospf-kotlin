package fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.service

import fuookami.ospf.kotlin.utils.functional.Order
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.Int64
import fuookami.ospf.kotlin.quantities.quantity.partialOrd
import fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.*
import fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.model.*
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.CuttingPlan
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.MaterialId
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.MachineId
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.ProductId

/**
 * Collects, deduplicates, and applies dominance pruning to generated cutting plan candidates.
 * 收集、去重并对生成的切割方案候选应用支配剪枝。
 *
 * @param maxPlans the maximum number of plans to accept
 * 最大接受方案数
 * @param deadline optional nano-time deadline after which generation stops
 * 可选的纳秒截止时间，超过后停止生成
 * @param enableDominancePruning whether to enable dominance-based plan pruning
 * 是否启用基于支配的方案剪枝
 * @param dominanceStrategy the dominance strategy to use when pruning
 * 剪枝时使用的支配策略
 * @param canonicalKeyOverride custom canonical key resolver; when returns non-null, replaces the default canonicalKey()
 * 自定义 canonical key 解析函数，返回非 null 时替代默认 canonicalKey()
 * @param dominanceAcceptOverride custom dominance acceptance function; returns true if the new candidate should be accepted
 * 自定义 dominance 接受函数，返回 true 表示新候选应被接受
*/
internal class GenerationCollector<V : RealNumber<V>>(
    private val maxPlans: Int64,
    private val deadline: Int64?,
    private val enableDominancePruning: Boolean = false,
    private val dominanceStrategy: DominanceStrategy = DominanceStrategy.SameContribution,

    /**
     * 自定义 canonical key 解析函数，返回非 null 时替代默认 canonicalKey()。
     * Custom canonical key resolver; when returns non-null, replaces the default canonicalKey().
    */
    private val canonicalKeyOverride: ((CuttingPlan<V>) -> String?)? = null,

    /**
     * 自定义 dominance 接受函数，返回 true 表示新候选应被接受。
     * Custom dominance acceptance function; returns true if the new candidate should be accepted.
    */
    private val dominanceAcceptOverride: ((CuttingPlan<V>, List<CuttingPlan<V>>) -> Boolean)? = null
) {
    private val startTime = System.nanoTime()
    private val canonicalKeys = HashSet<CuttingPlanCanonicalKey>()
    private val dominanceIndex = HashMap<DominanceKey, Int>()
    private val relaxedDominanceIndex = HashMap<RelaxedDominanceKey, Int>()
    private val acceptedPlans = ArrayList<CuttingPlan<V>>()
    private var generatedCandidates = Int64.zero
    private var infeasibleCandidates = Int64.zero
    private var duplicateCandidates = Int64.zero
    private var dominatedCandidates = Int64.zero
    private var crossContributionDominated = Int64.zero
    private var widthBoundPrunedNodes = Int64.zero
    private var knifeBoundPrunedNodes = Int64.zero
    private var lengthBoundPrunedEntries = Int64.zero
    private var materialWidthIndexCacheHits = Int64.zero
    private var materialSliceTemplateCacheHits = Int64.zero
    private var materialSliceTemplateCacheMisses = Int64.zero
    private var visitedNodes = Int64.zero
    private var timedOut = false

    /**
     * The collected cutting plan candidates accepted so far.
     * 迄今为止收集到的已接受切割方案候选。
    */
    val plans: List<CuttingPlan<V>> get() = acceptedPlans

    /**
     * Records a visited node during the generation search tree traversal.
     * 记录生成搜索树遍历过程中的已访问节点。
    */
    fun visitNode() {
        visitedNodes = visitedNodes + Int64.one
    }

    /**
     * Records a node pruned due to width bound constraints.
     * 记录因宽度边界约束而被剪枝的节点。
    */
    fun recordWidthBoundPrunedNode() {
        widthBoundPrunedNodes = widthBoundPrunedNodes + Int64.one
    }

    /**
     * Records a node pruned due to knife bound constraints.
     * 记录因刀口边界约束而被剪枝的节点。
    */
    fun recordKnifeBoundPrunedNode() {
        knifeBoundPrunedNodes = knifeBoundPrunedNodes + Int64.one
    }

    /**
     * Records entries pruned due to length bound constraints.
     * 记录因长度边界约束而被剪枝的条目。
     *
     * @param count the number of pruned entries to record, defaults to 1
     * 要记录的剪枝条目数量，默认为 1
    */
    fun recordLengthBoundPrunedEntries(count: Int64 = Int64.one) {
        lengthBoundPrunedEntries = lengthBoundPrunedEntries + count
    }

    /**
     * Records a cache hit when looking up material width index.
     * 记录查找材料宽度索引时的缓存命中。
    */
    fun recordMaterialWidthIndexCacheHit() {
        materialWidthIndexCacheHits = materialWidthIndexCacheHits + Int64.one
    }

    /**
     * Records a cache hit when looking up material slice templates.
     * 记录查找材料切片模板时的缓存命中。
    */
    fun recordMaterialSliceTemplateCacheHit() {
        materialSliceTemplateCacheHits = materialSliceTemplateCacheHits + Int64.one
    }

    /**
     * Records a cache miss when looking up material slice templates.
     * 记录查找材料切片模板时的缓存未命中。
    */
    fun recordMaterialSliceTemplateCacheMiss() {
        materialSliceTemplateCacheMisses = materialSliceTemplateCacheMisses + Int64.one
    }

    /**
     * Checks whether generation should stop due to capacity or timeout.
     * 检查是否因容量已满或超时而应停止生成。
     *
     * @return true if generation should stop / 如果应停止生成则返回 true
    */
    fun shouldStop(): Boolean {
        return isFull() || isTimedOut()
    }

    /**
     * Checks whether the generation deadline has been exceeded.
     * 检查是否已超过生成截止时间。
     *
     * @return true if the deadline has been exceeded / 如果已超过截止时间则返回 true
    */
    fun isTimedOut(): Boolean {
        if (timedOut) {
            return true
        }
        if (deadline != null && System.nanoTime() > deadline.toLong()) {
            timedOut = true
        }
        return timedOut
    }

    /**
     * Records a cutting plan candidate, applying deduplication and dominance pruning.
     * 记录切割方案候选，应用去重和支配剪枝。
     *
     * @param plan the cutting plan candidate to record
     * 要记录的切割方案候选
     * @param feasible whether the candidate is feasible
     * 候选是否可行
     * @return true if the plan was accepted / 如果方案被接受则返回 true
    */
    fun record(plan: CuttingPlan<V>, feasible: Boolean): Boolean {
        generatedCandidates = generatedCandidates + Int64.one
        if (!feasible) {
            infeasibleCandidates = infeasibleCandidates + Int64.one
            return false
        }

        val key = resolveCanonicalKey(plan)
        if (!canonicalKeys.add(key)) {
            duplicateCandidates = duplicateCandidates + Int64.one
            return false
        }

        // Apply custom dominance acceptance override regardless of enableDominancePruning.
        // This allows acceptDominance to act as a general acceptance filter
        // even when built-in dominance pruning is disabled (the default).
        if (dominanceAcceptOverride != null && !dominanceAcceptOverride(plan, acceptedPlans.toList())) {
            canonicalKeys.remove(key)
            dominatedCandidates = dominatedCandidates + Int64.one
            return false
        }

        if (enableDominancePruning) {
            when (applyDominancePruning(plan)) {
                DominanceAction.Accept -> {}
                DominanceAction.Reject -> {
                    canonicalKeys.remove(key)
                    dominatedCandidates = dominatedCandidates + Int64.one
                    return false
                }

                DominanceAction.Replace -> {
                    dominatedCandidates = dominatedCandidates + Int64.one
                    return true
                }
            }
        }

        if (isFull()) {
            return false
        }

        acceptedPlans.add(plan)
        if (enableDominancePruning) {
            dominanceIndex[plan.dominanceKey()] = acceptedPlans.lastIndex
            if (dominanceStrategy == DominanceStrategy.CrossContribution) {
                relaxedDominanceIndex[plan.relaxedDominanceKey()] = acceptedPlans.lastIndex
            }
        }
        return true
    }

    /**
     * Resolves the canonical key for a cutting plan, using the custom override if provided.
     * 解析切割方案的 canonical key，如果提供了自定义覆盖则使用自定义覆盖。
     *
     * @param plan the cutting plan to resolve the key for
     * 要解析 key 的切割方案
     * @return the resolved canonical key / 解析后的 canonical key
    */
    private fun resolveCanonicalKey(plan: CuttingPlan<V>): CuttingPlanCanonicalKey {
        val customKey = canonicalKeyOverride?.invoke(plan)
        return if (customKey != null) CuttingPlanCanonicalKey(customKey) else plan.canonicalKey()
    }

    /**
     * Generates a report summarizing the generation results and statistics.
     * 生成汇总生成结果和统计信息的报告。
     *
     * @return the cutting plan generation report / 切割方案生成报告
    */
    fun report(): CuttingPlanGenerationReport<V> {
        return CuttingPlanGenerationReport(
            plans = acceptedPlans,
            statistics = CuttingPlanGenerationStatistics(
                visitedNodes = visitedNodes,
                generatedCandidates = generatedCandidates,
                acceptedPlans = Int64(acceptedPlans.size.toLong()),
                infeasibleCandidates = infeasibleCandidates,
                duplicateCandidates = duplicateCandidates,
                dominatedCandidates = dominatedCandidates,
                widthBoundPrunedNodes = widthBoundPrunedNodes,
                knifeBoundPrunedNodes = knifeBoundPrunedNodes,
                lengthBoundPrunedEntries = lengthBoundPrunedEntries,
                materialWidthIndexCacheHits = materialWidthIndexCacheHits,
                materialSliceTemplateCacheHits = materialSliceTemplateCacheHits,
                materialSliceTemplateCacheMisses = materialSliceTemplateCacheMisses,
                crossContributionDominated = crossContributionDominated,
                elapsedMilliseconds = Int64((System.nanoTime() - startTime) / 1_000_000L),
                stopReason = stopReason()
            )
        )
    }

    /**
     * Applies dominance pruning to determine whether the new plan should be accepted, rejected, or used to replace an existing plan.
     * 应用支配剪枝，判断新方案应被接受、拒绝还是用于替换已有方案。
     *
     * @param plan the new cutting plan candidate
     * 新的切割方案候选
     * @return the dominance action to take / 要执行的支配动作
    */
    private fun applyDominancePruning(plan: CuttingPlan<V>): DominanceAction {
        // Note: dominanceAcceptOverride is checked in record() before this method is called,
        // so it does not need to be re-checked here.

        // Tier 1: Same-contribution dominance (existing behavior)
        val dominanceKey = plan.dominanceKey()
        val existingIndex = dominanceIndex[dominanceKey]
        if (existingIndex != null) {
            val existingPlan = acceptedPlans.getOrNull(existingIndex) ?: return DominanceAction.Accept
            return when (compareRestWidth(plan, existingPlan)) {
                DominanceComparison.NewDominates -> {
                    canonicalKeys.remove(resolveCanonicalKey(existingPlan))
                    acceptedPlans[existingIndex] = plan
                    dominanceIndex[dominanceKey] = existingIndex
                    if (dominanceStrategy == DominanceStrategy.CrossContribution) {
                        relaxedDominanceIndex[plan.relaxedDominanceKey()] = existingIndex
                    }
                    DominanceAction.Replace
                }

                DominanceComparison.ExistingDominates -> DominanceAction.Reject
                DominanceComparison.Incomparable -> DominanceAction.Accept
            }
        }

        // Tier 2: Cross-contribution dominance (new, opt-in)
        if (dominanceStrategy == DominanceStrategy.CrossContribution) {
            val relaxedKey = plan.relaxedDominanceKey()
            val relaxedIndex = relaxedDominanceIndex[relaxedKey]
            if (relaxedIndex != null) {
                val existingPlan = acceptedPlans.getOrNull(relaxedIndex) ?: return DominanceAction.Accept
                if (canCrossContributionDominate(plan, existingPlan)) {
                    crossContributionDominated = crossContributionDominated + Int64.one
                    return DominanceAction.Reject
                }
            }
        }

        return DominanceAction.Accept
    }

    /**
     * 跨贡献 dominance 判定 / Cross-contribution dominance check
     *
     * 新方案 dominate 旧方案当且仅当：
     * 1. 新方案覆盖旧方案全部产品（每个产品贡献 >= 旧方案）
     * 2. 新方案余宽 <= 旧方案余宽
     *
     * New plan dominates existing plan iff:
     * 1. New plan covers all products of existing plan (each product contribution >= existing)
     * 2. New plan rest width <= existing plan rest width
     *
     * @param newPlan the new cutting plan to compare / 待比较的新切割方案
     * @param existingPlan the existing cutting plan to compare against / 待比较的已有切割方案
     * @return whether the new plan dominates the existing plan / 新方案是否支配已有方案
    */
    private fun canCrossContributionDominate(
        newPlan: CuttingPlan<V>,
        existingPlan: CuttingPlan<V>
    ): Boolean {
        val newRestWidth = newPlan.restWidth ?: return false
        val existingRestWidth = existingPlan.restWidth ?: return false
        if (newRestWidth.unit != existingRestWidth.unit) return false

        // 余宽必须 <= 旧方案余宽
        if ((newRestWidth.value partialOrd existingRestWidth.value) !is Order.Less &&
            (newRestWidth.value partialOrd existingRestWidth.value) !is Order.Equal
        ) {
            return false
        }

        // 新方案对每个产品的贡献 >= 旧方案
        val newContributions = newPlan.demandContributions.associate { it.product.id to it.quantity.value }
        val existingContributions = existingPlan.demandContributions.associate { it.product.id to it.quantity.value }

        return existingContributions.all { (productId, quantity) ->
            val newQuantity = newContributions[productId] ?: return false
            (newQuantity partialOrd quantity) !is Order.Less
        }
    }

    /**
     * Compares the rest width of two plans to determine dominance.
     * 比较两个方案的余宽以确定支配关系。
     *
     * @param newPlan the new cutting plan candidate
     * 新的切割方案候选
     * @param existingPlan the existing cutting plan to compare against
     * 用于比较的已有切割方案
     * @return the dominance comparison result / 支配比较结果
    */
    private fun compareRestWidth(
        newPlan: CuttingPlan<V>,
        existingPlan: CuttingPlan<V>
    ): DominanceComparison {
        val newRestWidth = newPlan.restWidth ?: return DominanceComparison.Incomparable
        val existingRestWidth = existingPlan.restWidth ?: return DominanceComparison.Incomparable
        if (newRestWidth.unit != existingRestWidth.unit) {
            return DominanceComparison.Incomparable
        }
        return when (newRestWidth.value partialOrd existingRestWidth.value) {
            is Order.Less -> DominanceComparison.NewDominates
            is Order.Greater -> DominanceComparison.ExistingDominates
            else -> DominanceComparison.ExistingDominates
        }
    }

    /**
     * Checks whether the collector has reached the maximum plan capacity.
     * 检查收集器是否已达到最大方案容量。
     *
     * @return true if the capacity is full / 如果容量已满则返回 true
    */
    private fun isFull(): Boolean {
        return acceptedPlans.size.toLong() >= maxPlans.toLong()
    }

    /**
     * Determines the reason why generation stopped.
     * 确定生成停止的原因。
     *
     * @return the stop reason / 停止原因
    */
    private fun stopReason(): CuttingPlanGenerationStopReason {
        return if (timedOut) {
            CuttingPlanGenerationStopReason.Timeout
        } else if (isFull()) {
            CuttingPlanGenerationStopReason.MaxPlans
        } else {
            CuttingPlanGenerationStopReason.Exhausted
        }
    }

    /**
     * Computes the dominance key for a cutting plan based on material, machine, capacity, and demand contributions.
     * 根据材料、机器、产能和需求贡献计算切割方案的支配键。
     *
     * @receiver plan the cutting plan to compute the key for
     * 要计算 key 的切割方案
     * @return the dominance key / 支配键
    */
    private fun CuttingPlan<V>.dominanceKey(): DominanceKey {
        return DominanceKey(
            materialId = material.id,
            machineId = machineId,
            capacityConsumption = capacityConsumption?.let {
                QuantityKey(
                    value = it.value.toString(),
                    unit = it.unit.canonicalUnitKey()
                )
            },
            demandContributions = demandContributions.map {
                DemandContributionKey(
                    productId = it.product.id,
                    unit = it.quantity.unit.canonicalUnitKey(),
                    quantityValue = it.quantity.value.toString()
                )
            }.sortedWith(
                compareBy<DemandContributionKey> { it.productId.toString() }
                    .thenBy { it.unit }
                    .thenBy { it.quantityValue }
            )
        )
    }

    /**
     * Computes the relaxed dominance key for cross-contribution dominance, based on material, machine, and product set.
     * 根据材料、机器和产品集计算用于跨贡献支配的松弛支配键。
     *
     * @receiver plan the cutting plan to compute the key for
     * 要计算 key 的切割方案
     * @return the relaxed dominance key / 松弛支配键
    */
    private fun CuttingPlan<V>.relaxedDominanceKey(): RelaxedDominanceKey {
        return RelaxedDominanceKey(
            materialId = material.id,
            machineId = machineId,
            productSet = demandContributions.map { it.product.id }.toSet()
        )
    }

    /**
     * Result of comparing two plans for dominance based on rest width.
     * 基于余宽比较两个方案支配关系的结果。
    */
    private enum class DominanceComparison {
        /** New plan dominates / 新方案支配 */
        NewDominates,
        /** Existing plan dominates / 已有方案支配 */
        ExistingDominates,
        /** Plans are incomparable / 方案不可比较 */
        Incomparable
    }

    /**
     * Action to take after applying dominance pruning to a candidate plan.
     * 对候选方案应用支配剪枝后要执行的动作。
    */
    private enum class DominanceAction {
        /** Accept the candidate / 接受候选 */
        Accept,
        /** Reject the candidate / 拒绝候选 */
        Reject,
        /** Replace existing plan / 替换已有方案 */
        Replace
    }

    /**
     * Key for same-contribution dominance comparison, capturing material, machine, capacity, and demand contributions.
     * 用于同贡献支配比较的键，包含材料、机器、产能和需求贡献。
     *
     * @property materialId the material identifier / 材料标识符
     * @property machineId the machine identifier / 机器标识符
     * @property capacityConsumption the capacity consumption quantity key / 产能消耗量键
     * @property demandContributions the list of demand contribution keys / 需求贡献键列表
    */
    private data class DominanceKey(
        val materialId: MaterialId,
        val machineId: MachineId?,
        val capacityConsumption: QuantityKey?,
        val demandContributions: List<DemandContributionKey>
    )

    /**
     * Key for cross-contribution dominance comparison, capturing material, machine, and product set.
     * 用于跨贡献支配比较的键，包含材料、机器和产品集。
     *
     * @property materialId the material identifier / 材料标识符
     * @property machineId the machine identifier / 机器标识符
     * @property productSet the set of product identifiers / 产品标识符集合
    */
    private data class RelaxedDominanceKey(
        val materialId: MaterialId,
        val machineId: MachineId?,
        val productSet: Set<ProductId>
    )

    /**
     * Key representing a quantity value with its unit for dominance comparison.
     * 表示带有单位的量值键，用于支配比较。
     *
     * @property value the quantity value as string / 量值的字符串表示
     * @property unit the canonical unit key / 规范单位键
    */
    private data class QuantityKey(
        val value: String,
        val unit: String
    )

    /**
     * Key representing a single demand contribution entry for dominance comparison.
     * 表示单个需求贡献条目的键，用于支配比较。
     *
     * @property productId the product identifier / 产品标识符
     * @property unit the canonical unit key / 规范单位键
     * @property quantityValue the quantity value as string / 量值的字符串表示
    */
    private data class DemandContributionKey(
        val productId: ProductId,
        val unit: String,
        val quantityValue: String
    )
}
