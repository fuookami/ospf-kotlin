package fuookami.ospf.kotlin.framework.csp1d.application.service

import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.CuttingPlanGenerationStatistics
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.CuttingPlan
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.ProductDemand
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.Material
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.Machine
import fuookami.ospf.kotlin.framework.csp1d.domain.produce.model.Csp1dExtractionPolicy
import fuookami.ospf.kotlin.framework.csp1d.domain.produce.model.Produce
import fuookami.ospf.kotlin.framework.csp1d.application.model.Csp1dKpiKeys
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
    pricingGenerationStatistics: CuttingPlanGenerationStatistics? = null,
    lpFailureMessage: String? = null,
    iterationRecords: List<Csp1dIterationRecord> = emptyList(),
    extractionPolicies: List<Csp1dExtractionPolicy<V>> = emptyList(),
    demands: List<ProductDemand<V>> = emptyList(),
    materials: List<Material<V>> = emptyList(),
    machines: List<Machine<V>> = emptyList()
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
    renderKpi[Csp1dKpiKeys.TopPlanCount] = kpi.topPlanCount.toString()
    renderKpi[Csp1dKpiKeys.YieldMetricCount] = kpi.yieldMetricCount.toString()
    renderKpi[Csp1dKpiKeys.WasteMetricCount] = kpi.wasteMetricCount.toString()
    renderKpi[Csp1dKpiKeys.LengthMetricCount] = kpi.lengthMetricCount.toString()
    renderKpi[Csp1dKpiKeys.SolutionStatus] = status.name
    if (terminationReason != null) {
        renderKpi[Csp1dKpiKeys.TerminationReason] = terminationReason.name
    }
    if (finalMilpStatus != null) {
        renderKpi[Csp1dKpiKeys.FinalMilpStatus] = finalMilpStatus.name
    }
    renderKpi[Csp1dKpiKeys.PartialSolutionAvailable] = partialSolutionAvailable.toString()
    if (initialGenerationStatistics != null) {
        renderKpi[Csp1dKpiKeys.InitialVisitedNodes] = initialGenerationStatistics.visitedNodes.toString()
        renderKpi[Csp1dKpiKeys.InitialGeneratedCandidates] = initialGenerationStatistics.generatedCandidates.toString()
        renderKpi[Csp1dKpiKeys.InitialAcceptedPlans] = initialGenerationStatistics.acceptedPlans.toString()
        renderKpi[Csp1dKpiKeys.InitialInfeasibleCandidates] = initialGenerationStatistics.infeasibleCandidates.toString()
        renderKpi[Csp1dKpiKeys.InitialDuplicateCandidates] = initialGenerationStatistics.duplicateCandidates.toString()
        renderKpi[Csp1dKpiKeys.InitialDominatedCandidates] = initialGenerationStatistics.dominatedCandidates.toString()
        renderKpi[Csp1dKpiKeys.InitialWidthBoundPrunedNodes] =
            initialGenerationStatistics.widthBoundPrunedNodes.toString()
        renderKpi[Csp1dKpiKeys.InitialKnifeBoundPrunedNodes] =
            initialGenerationStatistics.knifeBoundPrunedNodes.toString()
        renderKpi[Csp1dKpiKeys.InitialLengthBoundPrunedEntries] =
            initialGenerationStatistics.lengthBoundPrunedEntries.toString()
        renderKpi[Csp1dKpiKeys.InitialMaterialWidthIndexCacheHits] =
            initialGenerationStatistics.materialWidthIndexCacheHits.toString()
        renderKpi[Csp1dKpiKeys.InitialMaterialSliceTemplateCacheHits] =
            initialGenerationStatistics.materialSliceTemplateCacheHits.toString()
        renderKpi[Csp1dKpiKeys.InitialGenerationQuantityCacheHits] =
            initialGenerationStatistics.quantityCacheHits.toString()
        renderKpi[Csp1dKpiKeys.InitialGenerationQuantityCacheMisses] =
            initialGenerationStatistics.quantityCacheMisses.toString()
        renderKpi[Csp1dKpiKeys.InitialGenerationMaterialSliceTemplateCacheMisses] =
            initialGenerationStatistics.materialSliceTemplateCacheMisses.toString()
        renderKpi[Csp1dKpiKeys.InitialGenerationCrossWorkerDuplicateCandidates] =
            initialGenerationStatistics.crossWorkerDuplicateCandidates.toString()
        renderKpi[Csp1dKpiKeys.InitialGenerationCrossContributionDominated] =
            initialGenerationStatistics.crossContributionDominated.toString()
        renderKpi[Csp1dKpiKeys.InitialGenerationElapsedMillisecondsRender] =
            initialGenerationStatistics.elapsedMilliseconds.toString()
        renderKpi[Csp1dKpiKeys.InitialGenerationStopReasonRender] = initialGenerationStatistics.stopReason.name
    }
    if (pricingGenerationStatistics != null) {
        renderKpi[Csp1dKpiKeys.PricingVisitedNodes] = pricingGenerationStatistics.visitedNodes.toString()
        renderKpi[Csp1dKpiKeys.PricingGeneratedCandidates] = pricingGenerationStatistics.generatedCandidates.toString()
        renderKpi[Csp1dKpiKeys.PricingAcceptedPlans] = pricingGenerationStatistics.acceptedPlans.toString()
        renderKpi[Csp1dKpiKeys.PricingInfeasibleCandidates] = pricingGenerationStatistics.infeasibleCandidates.toString()
        renderKpi[Csp1dKpiKeys.PricingDuplicateCandidates] = pricingGenerationStatistics.duplicateCandidates.toString()
        renderKpi[Csp1dKpiKeys.PricingDominatedCandidates] = pricingGenerationStatistics.dominatedCandidates.toString()
        renderKpi[Csp1dKpiKeys.PricingElapsedMilliseconds] = pricingGenerationStatistics.elapsedMilliseconds.toString()
        renderKpi[Csp1dKpiKeys.PricingStopReason] = pricingGenerationStatistics.stopReason.name
    }
    if (lpFailureMessage != null) {
        renderKpi[Csp1dKpiKeys.LpFailureMessage] = lpFailureMessage
    } else {
        renderKpi.remove(Csp1dKpiKeys.LpFailureMessage)
    }
    if (failureMessage != null) {
        renderKpi[Csp1dKpiKeys.FailureMessage] = failureMessage
    } else {
        renderKpi.remove(Csp1dKpiKeys.FailureMessage)
    }

    // Apply extraction policies to enrich output
    if (extractionPolicies.isNotEmpty()) {
        val extractionDetails = LinkedHashMap(details)
        for (policy in extractionPolicies) {
            try {
                policy.enrichOutput(
                    details = extractionDetails,
                    renderKpi = renderKpi,
                    produce = solution.produce,
                    demands = demands,
                    materials = materials,
                    machines = machines,
                    generatedPlans = solution.generatedPlans,
                    iterationCount = iterationRecords.size,
                    terminationReason = terminationReason?.name,
                    finalMilpStatus = finalMilpStatus?.name,
                    pricingStatistics = pricingGenerationStatistics
                )
            } catch (_: Exception) {
                // Extraction policy failure must not escape or break the enrichment pipeline
            }
        }
        // Merge extraction details into kpi.details and renderKpi
        val extraKeys = extractionDetails.keys - details.keys
        for (key in extraKeys) {
            extractionDetails[key]?.let { value ->
                renderKpi[key] = value
            }
        }
        return solution.copy(
            kpi = kpi.copy(details = extractionDetails),
            render = solution.render.copy(kpi = renderKpi),
            status = status,
            failureMessage = failureMessage,
            topPlans = topPlans
        )
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

    details[Csp1dKpiKeys.GeneratedPlanCount] = solution.generatedPlans.size.toString()
    details[Csp1dKpiKeys.SelectedPlanCount] = solution.produce.cuttingPlans.size.toString()
    details[Csp1dKpiKeys.SelectedBatchCount] = solution.produce.cuttingPlans.fold(UInt64.zero) { acc, usage ->
        acc + usage.amount
    }.toString()
    details[Csp1dKpiKeys.TopPlanCount] = topPlans.size.toString()
    details[Csp1dKpiKeys.PartialSolutionAvailable] = partialSolutionAvailable.toString()

    terminationReason?.let { details[Csp1dKpiKeys.ColumnGenerationTerminationReason] = it.name }
    finalMilpStatus?.let { details[Csp1dKpiKeys.FinalMilpStatus] = it.name }
    details[Csp1dKpiKeys.ColumnGenerationIterationCount] = iterationRecords.size.toString()
    details[Csp1dKpiKeys.ColumnGenerationPricedPlanCount] = iterationRecords.fold(UInt64.zero) { acc, record ->
        acc + record.pricedPlanCount
    }.toString()
    iterationRecords.lastOrNull()?.let { record ->
        details[Csp1dKpiKeys.ColumnGenerationLastLpObjective] = record.lpObjective.toString()
        details[Csp1dKpiKeys.ColumnGenerationLastPlanCount] = record.planCountAfter.toString()
    }

    if (initialGenerationStatistics != null) {
        details[Csp1dKpiKeys.InitialGenerationVisitedNodes] = initialGenerationStatistics.visitedNodes.toString()
        details[Csp1dKpiKeys.InitialGenerationGeneratedCandidates] =
            initialGenerationStatistics.generatedCandidates.toString()
        details[Csp1dKpiKeys.InitialGenerationAcceptedPlans] = initialGenerationStatistics.acceptedPlans.toString()
        details[Csp1dKpiKeys.InitialGenerationInfeasibleCandidates] =
            initialGenerationStatistics.infeasibleCandidates.toString()
        details[Csp1dKpiKeys.InitialGenerationDuplicateCandidates] =
            initialGenerationStatistics.duplicateCandidates.toString()
        details[Csp1dKpiKeys.InitialGenerationDominatedCandidates] =
            initialGenerationStatistics.dominatedCandidates.toString()
        details[Csp1dKpiKeys.InitialGenerationWidthBoundPrunedNodes] =
            initialGenerationStatistics.widthBoundPrunedNodes.toString()
        details[Csp1dKpiKeys.InitialGenerationKnifeBoundPrunedNodes] =
            initialGenerationStatistics.knifeBoundPrunedNodes.toString()
        details[Csp1dKpiKeys.InitialGenerationLengthBoundPrunedEntries] =
            initialGenerationStatistics.lengthBoundPrunedEntries.toString()
        details[Csp1dKpiKeys.InitialGenerationMaterialWidthIndexCacheHits] =
            initialGenerationStatistics.materialWidthIndexCacheHits.toString()
        details[Csp1dKpiKeys.InitialGenerationMaterialSliceTemplateCacheHits] =
            initialGenerationStatistics.materialSliceTemplateCacheHits.toString()
        details[Csp1dKpiKeys.InitialGenerationQuantityCacheHits] =
            initialGenerationStatistics.quantityCacheHits.toString()
        details[Csp1dKpiKeys.InitialGenerationQuantityCacheMisses] =
            initialGenerationStatistics.quantityCacheMisses.toString()
        details[Csp1dKpiKeys.InitialGenerationMaterialSliceTemplateCacheMisses] =
            initialGenerationStatistics.materialSliceTemplateCacheMisses.toString()
        details[Csp1dKpiKeys.InitialGenerationCrossWorkerDuplicateCandidates] =
            initialGenerationStatistics.crossWorkerDuplicateCandidates.toString()
        details[Csp1dKpiKeys.InitialGenerationCrossContributionDominated] =
            initialGenerationStatistics.crossContributionDominated.toString()
        details[Csp1dKpiKeys.InitialGenerationElapsedMilliseconds] =
            initialGenerationStatistics.elapsedMilliseconds.toString()
        details[Csp1dKpiKeys.InitialGenerationStopReason] = initialGenerationStatistics.stopReason.name
    }

    for (materialUsage in solution.produce.materialUsages) {
        details[Csp1dKpiKeys.materialUsageBatchCount(materialUsage.material.id)] = materialUsage.amount.toString()
    }
    for (machineUsage in solution.produce.machineUsages) {
        val used = machineUsage.used ?: continue
        details[Csp1dKpiKeys.machineCapacityUsed(machineUsage.machine.id)] = used.toString()
    }
    for (underProduction in solution.yieldResult?.underProductions.orEmpty()) {
        details[Csp1dKpiKeys.underProduction(
            productId = underProduction.productId,
            unitSymbol = underProduction.unitSymbol
        )] = underProduction.amount.toString()
    }
    for (overProduction in solution.yieldResult?.overProductions.orEmpty()) {
        details[Csp1dKpiKeys.overProduction(
            productId = overProduction.productId,
            unitSymbol = overProduction.unitSymbol
        )] = overProduction.amount.toString()
    }

    val wasteResult = solution.wasteResult
    if (wasteResult != null) {
        wasteResult.totalTrimWidth?.let { details[Csp1dKpiKeys.TotalTrimWidth] = it.toString() }
        wasteResult.totalRestMaterial?.let { details[Csp1dKpiKeys.TotalRestMaterial] = it.toString() }
        wasteResult.overProductionArea?.let { details[Csp1dKpiKeys.OverProductionArea] = it.toString() }
        details[Csp1dKpiKeys.OverProductionAreaMeasure] = wasteResult.overProductionAreaMeasure.name
        details[Csp1dKpiKeys.RestMaterialMeasure] = wasteResult.restMaterialMeasure.name
        for (materialCost in wasteResult.materialCosts) {
            details[Csp1dKpiKeys.materialCost(materialCost.materialId)] = materialCost.cost.toString()
        }
    }

    for (assignedLength in solution.lengthResult?.assignedLengths.orEmpty()) {
        details[Csp1dKpiKeys.assignedLength(assignedLength.productId)] = assignedLength.assignedLength.toString()
    }
    for (overLength in solution.lengthResult?.overLengths.orEmpty()) {
        details[Csp1dKpiKeys.overLength(overLength.productId)] = overLength.overLength.toString()
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
