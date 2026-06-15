package fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.service

import kotlinx.coroutines.*
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.Int64
import fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.*
import fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.model.*
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.CuttingPlan

@OptIn(ExperimentalCoroutinesApi::class)
internal fun <T> runGenerationTasks(
    parallelism: Int64,
    tasks: List<() -> T>
): List<T> {
    val normalizedParallelism = parallelism.coerceAtLeast(Int64.one)
    if (normalizedParallelism == Int64.one || tasks.size <= 1) {
        return tasks.map { task -> task() }
    }

    return runBlocking {
        val dispatcher = Dispatchers.Default.limitedParallelism(normalizedParallelism.toInt())
        tasks.map { task ->
            async(dispatcher) {
                task()
            }
        }.awaitAll()
    }
}

internal fun <V : RealNumber<V>> mergeGenerationReports(
    reports: List<CuttingPlanGenerationReport<V>>,
    maxPlans: Int64,
    startedAt: Int64,
    deadline: Int64?,
    canonicalKeyOverride: ((CuttingPlan<V>) -> String?)? = null
): CuttingPlanGenerationReport<V> {
    val resolveCanonicalKey: (CuttingPlan<V>) -> CuttingPlanCanonicalKey = { plan ->
        val customKey = canonicalKeyOverride?.invoke(plan)
        if (customKey != null) CuttingPlanCanonicalKey(customKey) else plan.canonicalKey()
    }
    val canonicalKeys = HashSet<CuttingPlanCanonicalKey>()
    val plans = ArrayList<CuttingPlan<V>>()
    var duplicateCandidates = Int64(reports.sumOf { it.statistics.duplicateCandidates.toLong() })
    val dominatedCandidates = Int64(reports.sumOf { it.statistics.dominatedCandidates.toLong() })
    val widthBoundPrunedNodes = Int64(reports.sumOf { it.statistics.widthBoundPrunedNodes.toLong() })
    val knifeBoundPrunedNodes = Int64(reports.sumOf { it.statistics.knifeBoundPrunedNodes.toLong() })
    val lengthBoundPrunedEntries = Int64(reports.sumOf { it.statistics.lengthBoundPrunedEntries.toLong() })
    val materialWidthIndexCacheHits = Int64(reports.sumOf { it.statistics.materialWidthIndexCacheHits.toLong() })
    val materialSliceTemplateCacheHits = Int64(reports.sumOf { it.statistics.materialSliceTemplateCacheHits.toLong() })
    val materialSliceTemplateCacheMisses = Int64(reports.sumOf { it.statistics.materialSliceTemplateCacheMisses.toLong() })
    val quantityCacheHits = Int64(reports.sumOf { it.statistics.quantityCacheHits.toLong() })
    val quantityCacheMisses = Int64(reports.sumOf { it.statistics.quantityCacheMisses.toLong() })
    var crossWorkerDuplicateCandidates = Int64.zero

    for (report in reports) {
        for (plan in report.plans) {
            if (plans.size.toLong() >= maxPlans.toLong()) {
                break
            }
            if (!canonicalKeys.add(resolveCanonicalKey(plan))) {
                duplicateCandidates = duplicateCandidates + Int64.one
                crossWorkerDuplicateCandidates = crossWorkerDuplicateCandidates + Int64.one
                continue
            }
            plans.add(plan)
        }
        if (plans.size.toLong() >= maxPlans.toLong()) {
            break
        }
    }

    val timedOut = reports.any {
        it.statistics.stopReason == CuttingPlanGenerationStopReason.Timeout
    } || (deadline != null && System.nanoTime() > deadline.toLong())
    val stopReason = when {
        timedOut -> CuttingPlanGenerationStopReason.Timeout
        plans.size.toLong() >= maxPlans.toLong() -> CuttingPlanGenerationStopReason.MaxPlans
        else -> CuttingPlanGenerationStopReason.Exhausted
    }

    return CuttingPlanGenerationReport(
        plans = plans,
        statistics = CuttingPlanGenerationStatistics(
            visitedNodes = Int64(reports.sumOf { it.statistics.visitedNodes.toLong() }),
            generatedCandidates = Int64(reports.sumOf { it.statistics.generatedCandidates.toLong() }),
            acceptedPlans = Int64(plans.size.toLong()),
            infeasibleCandidates = Int64(reports.sumOf { it.statistics.infeasibleCandidates.toLong() }),
            duplicateCandidates = duplicateCandidates,
            dominatedCandidates = dominatedCandidates,
            widthBoundPrunedNodes = widthBoundPrunedNodes,
            knifeBoundPrunedNodes = knifeBoundPrunedNodes,
            lengthBoundPrunedEntries = lengthBoundPrunedEntries,
            materialWidthIndexCacheHits = materialWidthIndexCacheHits,
            materialSliceTemplateCacheHits = materialSliceTemplateCacheHits,
            quantityCacheHits = quantityCacheHits,
            quantityCacheMisses = quantityCacheMisses,
            materialSliceTemplateCacheMisses = materialSliceTemplateCacheMisses,
            crossWorkerDuplicateCandidates = crossWorkerDuplicateCandidates,
            elapsedMilliseconds = Int64((System.nanoTime() - startedAt.toLong()) / 1_000_000L),
            stopReason = stopReason
        )
    )
}
