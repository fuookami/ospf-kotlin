package fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.service

import fuookami.ospf.kotlin.utils.functional.Order
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.Int64
import fuookami.ospf.kotlin.quantities.quantity.partialOrd
import fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.*
import fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.model.*
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.CuttingPlan

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

    val plans: List<CuttingPlan<V>> get() = acceptedPlans

    fun visitNode() {
        visitedNodes = visitedNodes + Int64.one
    }

    fun recordWidthBoundPrunedNode() {
        widthBoundPrunedNodes = widthBoundPrunedNodes + Int64.one
    }

    fun recordKnifeBoundPrunedNode() {
        knifeBoundPrunedNodes = knifeBoundPrunedNodes + Int64.one
    }

    fun recordLengthBoundPrunedEntries(count: Int64 = Int64.one) {
        lengthBoundPrunedEntries = lengthBoundPrunedEntries + count
    }

    fun recordMaterialWidthIndexCacheHit() {
        materialWidthIndexCacheHits = materialWidthIndexCacheHits + Int64.one
    }

    fun recordMaterialSliceTemplateCacheHit() {
        materialSliceTemplateCacheHits = materialSliceTemplateCacheHits + Int64.one
    }

    fun recordMaterialSliceTemplateCacheMiss() {
        materialSliceTemplateCacheMisses = materialSliceTemplateCacheMisses + Int64.one
    }

    fun shouldStop(): Boolean {
        return isFull() || isTimedOut()
    }

    fun isTimedOut(): Boolean {
        if (timedOut) {
            return true
        }
        if (deadline != null && System.nanoTime() > deadline.toLong()) {
            timedOut = true
        }
        return timedOut
    }

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

    private fun resolveCanonicalKey(plan: CuttingPlan<V>): CuttingPlanCanonicalKey {
        val customKey = canonicalKeyOverride?.invoke(plan)
        return if (customKey != null) CuttingPlanCanonicalKey(customKey) else plan.canonicalKey()
    }

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

    private fun isFull(): Boolean {
        return acceptedPlans.size.toLong() >= maxPlans.toLong()
    }

    private fun stopReason(): CuttingPlanGenerationStopReason {
        return if (timedOut) {
            CuttingPlanGenerationStopReason.Timeout
        } else if (isFull()) {
            CuttingPlanGenerationStopReason.MaxPlans
        } else {
            CuttingPlanGenerationStopReason.Exhausted
        }
    }

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
                compareBy<DemandContributionKey> { it.productId }
                    .thenBy { it.unit }
                    .thenBy { it.quantityValue }
            )
        )
    }

    private fun CuttingPlan<V>.relaxedDominanceKey(): RelaxedDominanceKey {
        return RelaxedDominanceKey(
            materialId = material.id,
            machineId = machineId,
            productSet = demandContributions.map { it.product.id }.toSortedSet()
        )
    }

    private enum class DominanceComparison {
        NewDominates,
        ExistingDominates,
        Incomparable
    }

    private enum class DominanceAction {
        Accept,
        Reject,
        Replace
    }

    private data class DominanceKey(
        val materialId: String,
        val machineId: String?,
        val capacityConsumption: QuantityKey?,
        val demandContributions: List<DemandContributionKey>
    )

    private data class RelaxedDominanceKey(
        val materialId: String,
        val machineId: String?,
        val productSet: Set<String>
    )

    private data class QuantityKey(
        val value: String,
        val unit: String
    )

    private data class DemandContributionKey(
        val productId: String,
        val unit: String,
        val quantityValue: String
    )
}
