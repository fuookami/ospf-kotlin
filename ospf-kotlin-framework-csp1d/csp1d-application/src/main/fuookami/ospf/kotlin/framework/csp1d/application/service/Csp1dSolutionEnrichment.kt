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
    initialGenerationStatistics: CuttingPlanGenerationStatistics? = null,
    iterationRecords: List<Csp1dIterationRecord> = emptyList()
): Csp1dSolution<V> {
    val details = kpiDetails(
        solution = solution,
        topPlans = topPlans,
        terminationReason = terminationReason,
        finalMilpStatus = finalMilpStatus,
        partialSolutionAvailable = partialSolutionAvailable,
        initialGenerationStatistics = initialGenerationStatistics,
        iterationRecords = iterationRecords
    )
    val kpi = solution.kpi.copy(
        topPlanCount = UInt64(topPlans.size),
        yieldMetricCount = UInt64(yieldMetricCount(solution)),
        wasteMetricCount = UInt64(wasteMetricCount(solution)),
        lengthMetricCount = UInt64(lengthMetricCount(solution)),
        details = details
    )
    val renderKpi = LinkedHashMap(solution.render.kpi)
    renderKpi.putAll(details)
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
        renderKpi["initialDominatedCandidates"] = initialGenerationStatistics.dominatedCandidates.toString()
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

private fun <V : RealNumber<V>> kpiDetails(
    solution: Csp1dSolution<V>,
    topPlans: List<CuttingPlan<V>>,
    terminationReason: Csp1dTerminationReason?,
    finalMilpStatus: Csp1dFinalMilpStatus?,
    partialSolutionAvailable: Boolean,
    initialGenerationStatistics: CuttingPlanGenerationStatistics?,
    iterationRecords: List<Csp1dIterationRecord>
): Map<String, String> {
    val details = LinkedHashMap<String, String>()

    details["generatedPlanCount"] = solution.generatedPlans.size.toString()
    details["selectedPlanCount"] = solution.produce.cuttingPlans.size.toString()
    details["selectedBatchCount"] = solution.produce.cuttingPlans.fold(UInt64.zero) { acc, usage ->
        acc + usage.amount
    }.toString()
    details["topPlanCount"] = topPlans.size.toString()
    details["partialSolutionAvailable"] = partialSolutionAvailable.toString()

    terminationReason?.let { details["columnGeneration.terminationReason"] = it.name }
    finalMilpStatus?.let { details["finalMilpStatus"] = it.name }
    details["columnGeneration.iterationCount"] = iterationRecords.size.toString()
    details["columnGeneration.pricedPlanCount"] = iterationRecords.fold(UInt64.zero) { acc, record ->
        acc + record.pricedPlanCount
    }.toString()
    iterationRecords.lastOrNull()?.let { record ->
        details["columnGeneration.lastLpObjective"] = record.lpObjective.toString()
        details["columnGeneration.lastPlanCount"] = record.planCountAfter.toString()
    }

    if (initialGenerationStatistics != null) {
        details["initialGeneration.visitedNodes"] = initialGenerationStatistics.visitedNodes.toString()
        details["initialGeneration.generatedCandidates"] = initialGenerationStatistics.generatedCandidates.toString()
        details["initialGeneration.acceptedPlans"] = initialGenerationStatistics.acceptedPlans.toString()
        details["initialGeneration.infeasibleCandidates"] = initialGenerationStatistics.infeasibleCandidates.toString()
        details["initialGeneration.duplicateCandidates"] = initialGenerationStatistics.duplicateCandidates.toString()
        details["initialGeneration.dominatedCandidates"] = initialGenerationStatistics.dominatedCandidates.toString()
        details["initialGeneration.elapsedMilliseconds"] = initialGenerationStatistics.elapsedMilliseconds.toString()
        details["initialGeneration.stopReason"] = initialGenerationStatistics.stopReason.name
    }

    for (materialUsage in solution.produce.materialUsages) {
        details["materialUsage.${materialUsage.material.id}.batchCount"] = materialUsage.amount.toString()
    }
    for (machineUsage in solution.produce.machineUsages) {
        val used = machineUsage.used ?: continue
        details["machineCapacityUsed.${machineUsage.machine.id}"] = used.toString()
    }
    for (underProduction in solution.yieldResult?.underProductions.orEmpty()) {
        details["underProduction.${underProduction.productId}.${underProduction.unitSymbol}"] = underProduction.amount.toString()
    }
    for (overProduction in solution.yieldResult?.overProductions.orEmpty()) {
        details["overProduction.${overProduction.productId}.${overProduction.unitSymbol}"] = overProduction.amount.toString()
    }

    val wasteResult = solution.wasteResult
    if (wasteResult != null) {
        wasteResult.totalTrimWidth?.let { details["totalTrimWidth"] = it.toString() }
        wasteResult.totalRestMaterial?.let { details["totalRestMaterial"] = it.toString() }
        wasteResult.overProductionArea?.let { details["overProductionArea"] = it.toString() }
        details["overProductionAreaMeasure"] = wasteResult.overProductionAreaMeasure.name
        details["restMaterialMeasure"] = wasteResult.restMaterialMeasure.name
        for (materialCost in wasteResult.materialCosts) {
            details["materialCost.${materialCost.materialId}"] = materialCost.cost.toString()
        }
    }

    for (assignedLength in solution.lengthResult?.assignedLengths.orEmpty()) {
        details["assignedLength.${assignedLength.productId}"] = assignedLength.assignedLength.toString()
    }
    for (overLength in solution.lengthResult?.overLengths.orEmpty()) {
        details["overLength.${overLength.productId}"] = overLength.overLength.toString()
    }

    return details
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
