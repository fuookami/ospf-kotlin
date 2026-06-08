package fuookami.ospf.kotlin.framework.csp1d.application.service

import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.CuttingPlanGenerationStatistics
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.CuttingPlan
import fuookami.ospf.kotlin.framework.csp1d.application.model.Csp1dSolution
import fuookami.ospf.kotlin.framework.csp1d.application.model.Csp1dSolutionStatus

internal fun <V : RealNumber<V>> topCuttingPlans(
    plans: List<CuttingPlan<V>>,
    limit: Int?
): List<CuttingPlan<V>> {
    if (limit == null || limit <= 0) {
        return emptyList()
    }
    val topK = TopKCuttingPlans<V>(limit)
    topK.offerAll(plans)
    return topK.toSortedList()
}

internal fun <V : RealNumber<V>> enrichSolution(
    solution: Csp1dSolution<V>,
    topPlans: List<CuttingPlan<V>>,
    status: Csp1dSolutionStatus,
    failureMessage: String?,
    terminationReason: Csp1dTerminationReason? = null,
    finalMilpStatus: Csp1dFinalMilpStatus? = null,
    partialSolutionAvailable: Boolean = false,
    initialGenerationStatistics: CuttingPlanGenerationStatistics? = null
): Csp1dSolution<V> {
    val kpi = solution.kpi.copy(
        topPlanCount = UInt64(topPlans.size),
        yieldMetricCount = UInt64(yieldMetricCount(solution)),
        wasteMetricCount = UInt64(wasteMetricCount(solution)),
        lengthMetricCount = UInt64(lengthMetricCount(solution))
    )
    val renderKpi = LinkedHashMap(solution.render.kpi)
    renderKpi["topPlanCount"] = kpi.topPlanCount.toString()
    renderKpi["yieldMetricCount"] = kpi.yieldMetricCount.toString()
    renderKpi["wasteMetricCount"] = kpi.wasteMetricCount.toString()
    renderKpi["lengthMetricCount"] = kpi.lengthMetricCount.toString()
    renderKpi["solutionStatus"] = status.name
    if (terminationReason != null) {
        renderKpi["terminationReason"] = terminationReason.name
    }
    if (finalMilpStatus != null) {
        renderKpi["finalMilpStatus"] = finalMilpStatus.name
    }
    renderKpi["partialSolutionAvailable"] = partialSolutionAvailable.toString()
    if (initialGenerationStatistics != null) {
        renderKpi["initialVisitedNodes"] = initialGenerationStatistics.visitedNodes.toString()
        renderKpi["initialGeneratedCandidates"] = initialGenerationStatistics.generatedCandidates.toString()
        renderKpi["initialAcceptedPlans"] = initialGenerationStatistics.acceptedPlans.toString()
        renderKpi["initialInfeasibleCandidates"] = initialGenerationStatistics.infeasibleCandidates.toString()
        renderKpi["initialDuplicateCandidates"] = initialGenerationStatistics.duplicateCandidates.toString()
        renderKpi["initialGenerationElapsedMilliseconds"] = initialGenerationStatistics.elapsedMilliseconds.toString()
        renderKpi["initialGenerationStopReason"] = initialGenerationStatistics.stopReason.name
    }
    if (failureMessage != null) {
        renderKpi["failureMessage"] = failureMessage
    } else {
        renderKpi.remove("failureMessage")
    }
    return solution.copy(
        kpi = kpi,
        render = solution.render.copy(kpi = renderKpi),
        status = status,
        failureMessage = failureMessage,
        topPlans = topPlans
    )
}

private fun <V : RealNumber<V>> yieldMetricCount(solution: Csp1dSolution<V>): Int {
    val result = solution.yieldResult ?: return 0
    return result.underProductions.size + result.overProductions.size
}

private fun <V : RealNumber<V>> wasteMetricCount(solution: Csp1dSolution<V>): Int {
    val result = solution.wasteResult ?: return 0
    var count = result.materialCosts.size
    if (result.totalTrimWidth != null) {
        ++count
    }
    if (result.totalRestMaterial != null) {
        ++count
    }
    if (result.overProductionArea != null) {
        ++count
    }
    return count
}

private fun <V : RealNumber<V>> lengthMetricCount(solution: Csp1dSolution<V>): Int {
    val result = solution.lengthResult ?: return 0
    return result.assignedLengths.size + result.overLengths.size
}
