package fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.service

import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.quantities.quantity.partialOrd
import fuookami.ospf.kotlin.utils.functional.Order
import fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.CuttingPlanGenerationReport
import fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.CuttingPlanGenerationStatistics
import fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.CuttingPlanGenerationStopReason
import fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.model.CuttingPlanCanonicalKey
import fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.model.canonicalKey
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.CuttingPlan

internal class GenerationCollector<V : RealNumber<V>>(
    private val maxPlans: Int,
    private val deadline: Long?,
    private val enableDominancePruning: Boolean = false
) {
    private val startTime = System.nanoTime()
    private val canonicalKeys = HashSet<CuttingPlanCanonicalKey>()
    private val dominanceIndex = HashMap<DominanceKey, Int>()
    private val acceptedPlans = ArrayList<CuttingPlan<V>>()
    private var generatedCandidates = 0L
    private var infeasibleCandidates = 0L
    private var duplicateCandidates = 0L
    private var dominatedCandidates = 0L
    private var widthBoundPrunedNodes = 0L
    private var knifeBoundPrunedNodes = 0L
    private var lengthBoundPrunedEntries = 0L
    private var materialWidthIndexCacheHits = 0L
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
                elapsedMilliseconds = (System.nanoTime() - startTime) / 1_000_000L,
                stopReason = stopReason()
            )
        )
    }

    private fun applyDominancePruning(plan: CuttingPlan<V>): DominanceAction {
        val dominanceKey = plan.dominanceKey()
        val existingIndex = dominanceIndex[dominanceKey] ?: return DominanceAction.Accept
        val existingPlan = acceptedPlans.getOrNull(existingIndex) ?: return DominanceAction.Accept
        return when (compareRestWidth(plan, existingPlan)) {
            DominanceComparison.NewDominates -> {
                canonicalKeys.remove(existingPlan.canonicalKey())
                acceptedPlans[existingIndex] = plan
                dominanceIndex[dominanceKey] = existingIndex
                DominanceAction.Replace
            }

            DominanceComparison.ExistingDominates -> DominanceAction.Reject
            DominanceComparison.Incomparable -> DominanceAction.Accept
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
