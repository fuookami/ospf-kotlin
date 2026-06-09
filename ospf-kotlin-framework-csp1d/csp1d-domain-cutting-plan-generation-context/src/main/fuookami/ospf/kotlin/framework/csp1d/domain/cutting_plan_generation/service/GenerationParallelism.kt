package fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.service

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.CuttingPlanGenerationReport
import fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.CuttingPlanGenerationStatistics
import fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.CuttingPlanGenerationStopReason
import fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.model.CuttingPlanCanonicalKey
import fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.model.canonicalKey
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.CuttingPlan

@OptIn(ExperimentalCoroutinesApi::class)
internal fun <T> runGenerationTasks(
    parallelism: Int,
    tasks: List<() -> T>
): List<T> {
    val normalizedParallelism = parallelism.coerceAtLeast(1)
    if (normalizedParallelism == 1 || tasks.size <= 1) {
        return tasks.map { task -> task() }
    }

    return runBlocking {
        val dispatcher = Dispatchers.Default.limitedParallelism(normalizedParallelism)
        tasks.map { task ->
            async(dispatcher) {
                task()
            }
        }.awaitAll()
    }
}

internal fun <V : RealNumber<V>> mergeGenerationReports(
    reports: List<CuttingPlanGenerationReport<V>>,
    maxPlans: Int,
    startedAt: Long,
    deadline: Long?
): CuttingPlanGenerationReport<V> {
    val canonicalKeys = HashSet<CuttingPlanCanonicalKey>()
    val plans = ArrayList<CuttingPlan<V>>()
    var duplicateCandidates = reports.sumOf { it.statistics.duplicateCandidates }
    val dominatedCandidates = reports.sumOf { it.statistics.dominatedCandidates }
    val widthBoundPrunedNodes = reports.sumOf { it.statistics.widthBoundPrunedNodes }
    val lengthBoundPrunedEntries = reports.sumOf { it.statistics.lengthBoundPrunedEntries }
    val materialWidthIndexCacheHits = reports.sumOf { it.statistics.materialWidthIndexCacheHits }

    for (report in reports) {
        for (plan in report.plans) {
            if (plans.size >= maxPlans) {
                break
            }
            if (!canonicalKeys.add(plan.canonicalKey())) {
                ++duplicateCandidates
                continue
            }
            plans.add(plan)
        }
        if (plans.size >= maxPlans) {
            break
        }
    }

    val timedOut = reports.any {
        it.statistics.stopReason == CuttingPlanGenerationStopReason.Timeout
    } || (deadline != null && System.nanoTime() > deadline)
    val stopReason = when {
        timedOut -> CuttingPlanGenerationStopReason.Timeout
        plans.size >= maxPlans -> CuttingPlanGenerationStopReason.MaxPlans
        else -> CuttingPlanGenerationStopReason.Exhausted
    }

    return CuttingPlanGenerationReport(
        plans = plans,
        statistics = CuttingPlanGenerationStatistics(
            visitedNodes = reports.sumOf { it.statistics.visitedNodes },
            generatedCandidates = reports.sumOf { it.statistics.generatedCandidates },
            acceptedPlans = plans.size,
            infeasibleCandidates = reports.sumOf { it.statistics.infeasibleCandidates },
            duplicateCandidates = duplicateCandidates,
            dominatedCandidates = dominatedCandidates,
            widthBoundPrunedNodes = widthBoundPrunedNodes,
            lengthBoundPrunedEntries = lengthBoundPrunedEntries,
            materialWidthIndexCacheHits = materialWidthIndexCacheHits,
            elapsedMilliseconds = (System.nanoTime() - startedAt) / 1_000_000L,
            stopReason = stopReason
        )
    )
}
