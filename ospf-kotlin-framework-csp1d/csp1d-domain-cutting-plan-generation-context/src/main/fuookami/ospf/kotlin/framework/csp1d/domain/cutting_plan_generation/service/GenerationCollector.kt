package fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.service

import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.quantities.quantity.partialOrd
import fuookami.ospf.kotlin.utils.functional.Order
import fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.CuttingPlanGenerationReport
import fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.CuttingPlanGenerationStatistics
import fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.CuttingPlanGenerationStopReason
import fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.model.CuttingPlanCanonicalKey
import fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.model.DominanceStrategy
import fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.model.canonicalKey
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.CuttingPlan

internal class GenerationCollector<V : RealNumber<V>>(
    private val maxPlans: Int,
    private val deadline: Long?,
    private val enableDominancePruning: Boolean = false,
    private val dominanceStrategy: DominanceStrategy = DominanceStrategy.SameContribution
) {
    private val startTime = System.nanoTime()
    private val canonicalKeys = HashSet<CuttingPlanCanonicalKey>()
    private val dominanceIndex = HashMap<DominanceKey, Int>()
    private val relaxedDominanceIndex = HashMap<RelaxedDominanceKey, Int>()
    private val acceptedPlans = ArrayList<CuttingPlan<V>>()
    private var generatedCandidates = 0L
    private var infeasibleCandidates = 0L
    private var duplicateCandidates = 0L
    private var dominatedCandidates = 0L
    private var crossContributionDominated = 0L
    private var widthBoundPrunedNodes = 0L
    private var knifeBoundPrunedNodes = 0L
    private var lengthBoundPrunedEntries = 0L
    private var materialWidthIndexCacheHits = 0L
    private var materialSliceTemplateCacheHits = 0L
    private var materialSliceTemplateCacheMisses = 0L
    private var visitedNodes = 0L
    private var timedOut = false

    val plans: List<CuttingPlan<V>> get() = acceptedPlans

    fun visitNode() {
        ++visitedNodes
    }

    fun recordWidthBoundPrunedNode() {
        ++widthBoundPrunedNodes
    }

    fun recordKnifeBoundPrunedNode() {
        ++knifeBoundPrunedNodes
    }

    fun recordLengthBoundPrunedEntries(count: Long = 1L) {
        lengthBoundPrunedEntries += count
    }

    fun recordMaterialWidthIndexCacheHit() {
        ++materialWidthIndexCacheHits
    }

    fun recordMaterialSliceTemplateCacheHit() {
        ++materialSliceTemplateCacheHits
    }

    fun recordMaterialSliceTemplateCacheMiss() {
        ++materialSliceTemplateCacheMisses
    }

    fun shouldStop(): Boolean {
        return isFull() || isTimedOut()
    }

    fun isTimedOut(): Boolean {
        if (timedOut) {
            return true
        }
        if (deadline != null && System.nanoTime() > deadline) {
            timedOut = true
        }
        return timedOut
    }

    fun record(plan: CuttingPlan<V>, feasible: Boolean): Boolean {
        ++generatedCandidates
        if (!feasible) {
            ++infeasibleCandidates
            return false
        }

        if (!canonicalKeys.add(plan.canonicalKey())) {
            ++duplicateCandidates
            return false
        }

        if (enableDominancePruning) {
            when (applyDominancePruning(plan)) {
                DominanceAction.Accept -> {}
                DominanceAction.Reject -> {
                    canonicalKeys.remove(plan.canonicalKey())
                    ++dominatedCandidates
                    return false
                }

                DominanceAction.Replace -> {
                    ++dominatedCandidates
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

    fun report(): CuttingPlanGenerationReport<V> {
        return CuttingPlanGenerationReport(
            plans = acceptedPlans,
            statistics = CuttingPlanGenerationStatistics(
                visitedNodes = visitedNodes,
                generatedCandidates = generatedCandidates,
                acceptedPlans = acceptedPlans.size,
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
                elapsedMilliseconds = (System.nanoTime() - startTime) / 1_000_000L,
                stopReason = stopReason()
            )
        )
    }

    private fun applyDominancePruning(plan: CuttingPlan<V>): DominanceAction {
        // Tier 1: Same-contribution dominance (existing behavior)
        val dominanceKey = plan.dominanceKey()
        val existingIndex = dominanceIndex[dominanceKey]
        if (existingIndex != null) {
            val existingPlan = acceptedPlans.getOrNull(existingIndex) ?: return DominanceAction.Accept
            return when (compareRestWidth(plan, existingPlan)) {
                DominanceComparison.NewDominates -> {
                    canonicalKeys.remove(existingPlan.canonicalKey())
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
                    ++crossContributionDominated
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
        return acceptedPlans.size >= maxPlans
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
