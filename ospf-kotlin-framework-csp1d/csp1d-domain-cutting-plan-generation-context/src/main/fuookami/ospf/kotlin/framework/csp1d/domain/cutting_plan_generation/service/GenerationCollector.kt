package fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.service

import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.CuttingPlanGenerationReport
import fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.CuttingPlanGenerationStatistics
import fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.CuttingPlanGenerationStopReason
import fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.model.CuttingPlanCanonicalKey
import fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.model.canonicalKey
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.CuttingPlan

internal class GenerationCollector<V : RealNumber<V>>(
    private val maxPlans: Int,
    private val deadline: Long?
) {
    private val startTime = System.nanoTime()
    private val canonicalKeys = HashSet<CuttingPlanCanonicalKey>()
    private val acceptedPlans = ArrayList<CuttingPlan<V>>()
    private var generatedCandidates = 0L
    private var infeasibleCandidates = 0L
    private var duplicateCandidates = 0L
    private var visitedNodes = 0L
    private var timedOut = false

    val plans: List<CuttingPlan<V>> get() = acceptedPlans

    fun visitNode() {
        ++visitedNodes
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

        if (isFull()) {
            return false
        }

        acceptedPlans.add(plan)
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
                elapsedMilliseconds = (System.nanoTime() - startTime) / 1_000_000L,
                stopReason = stopReason()
            )
        )
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
}
