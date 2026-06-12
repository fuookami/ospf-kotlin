package fuookami.ospf.kotlin.framework.csp1d.application.service

import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.framework.solver.ColumnGenerationSolver
import fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.Csp1dInitialCuttingPlanGenerator
import fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.Csp1dPricingInput
import fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.Csp1dPricingGenerator
import fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.Csp1dPricingObjectiveConfig
import fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.CuttingPlanGenerationInput
import fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.CuttingPlanGenerationStatistics
import fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.ReducedCostPricingGenerator
import fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.SimpleInitialCuttingPlanGenerator
import fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.model.canonicalKey
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.CuttingPlan
import fuookami.ospf.kotlin.framework.csp1d.domain.produce.ProduceInput
import fuookami.ospf.kotlin.framework.csp1d.domain.produce.model.CuttingPlanUsage
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.Csp1dDomainPolicy
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.Material
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.Product
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.widthFeasibilityCheckFromPolicies
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.framework.csp1d.domain.produce.model.Csp1dFlowPolicy
import fuookami.ospf.kotlin.framework.csp1d.domain.produce.model.Csp1dFlowContext
import fuookami.ospf.kotlin.framework.csp1d.domain.produce.model.Csp1dExtractionPolicy
import fuookami.ospf.kotlin.framework.csp1d.domain.produce.model.Produce
import fuookami.ospf.kotlin.framework.csp1d.domain.produce.model.filterInitialPlansByPolicies
import fuookami.ospf.kotlin.framework.csp1d.domain.produce.model.isEquivalentByPolicies
import fuookami.ospf.kotlin.framework.csp1d.domain.produce.model.shouldStopByPolicies
import fuookami.ospf.kotlin.framework.csp1d.domain.produce.model.selectTerminationByPolicies
import fuookami.ospf.kotlin.framework.csp1d.domain.produce.model.acceptPartialByPolicies
import fuookami.ospf.kotlin.framework.csp1d.domain.length_assignment.model.LengthAssignmentModelingConfig
import fuookami.ospf.kotlin.framework.csp1d.domain.yield.model.YieldModelingConfig
import fuookami.ospf.kotlin.framework.csp1d.application.model.Csp1dConfiguration
import fuookami.ospf.kotlin.framework.csp1d.application.model.Csp1dProblem
import fuookami.ospf.kotlin.framework.csp1d.application.model.Csp1dSolveConfig
import fuookami.ospf.kotlin.framework.csp1d.application.model.Csp1dSolution
import fuookami.ospf.kotlin.framework.csp1d.application.model.Csp1dSolutionAnalyzer
import fuookami.ospf.kotlin.framework.csp1d.application.model.Csp1dSolutionStatus
import fuookami.ospf.kotlin.framework.csp1d.application.model.DefaultCsp1dSolutionAnalyzer

/**
 * 列生成终止原因 / Column generation termination reason
 */
enum class Csp1dTerminationReason {
    /** 达到迭代上限 / Iteration limit reached */
    IterationLimitReached,
    /** LP 求解失败（异常、超时等，有前序有效 LP 解） / LP solve failed (exception, timeout, etc., with a prior valid LP result) */
    LpSolveFailed,
    /** 首次 LP 求解即失败，疑似 LP 松弛不可行 / First LP solve failed, likely LP relaxation infeasible */
    LpInfeasible,
    /** 无负 reduced cost 新列，自然收敛 / No negative reduced cost columns, natural convergence */
    PricingConverged,
    /** 新列全部重复，无增量 / All new plans are duplicates, no improvement */
    AllDuplicates,
    /** 初始方案为空 / No initial plans */
    NoInitialPlans
}

/**
 * 列生成每轮迭代记录 / Column generation iteration record
 *
 * @property iteration 迭代号 / Iteration number
 * @property lpObjective LP 目标值 / LP objective value
 * @property planCountBefore 本轮开始时方案池大小 / Plan pool size before this iteration
 * @property pricedPlanCount 本轮定价新增方案数 / Number of plans added by pricing this iteration
 * @property planCountAfter 本轮结束时方案池大小 / Plan pool size after this iteration
 */
data class Csp1dIterationRecord(
    val iteration: Int,
    val lpObjective: Flt64,
    val planCountBefore: Int,
    val pricedPlanCount: UInt64,
    val planCountAfter: Int
)

/**
 * 列生成求解追踪信息 / Column generation solve trace
 *
 * @property initialPlanCount 初始方案数 / Initial plan count
 * @property finalPlanCount 最终方案池大小 / Final plan pool size
 * @property pricedPlanCount 每轮新增定价方案数 / Priced plan count per iteration
 * @property terminationReason 列生成终止原因 / Column generation termination reason
 * @property iterations 每轮迭代记录 / Iteration records
 * @property initialGenerationStatistics 初始生成统计 / Initial generation statistics
 * @property finalMilpStatus 最终 MILP 状态 / Final MILP status
 * @property partialSolutionAvailable 是否存在部分解 / Whether partial solution is available
 * @property failureMessage 失败信息 / Failure message
 * @property pricingGenerationStatistics pricing 生成统计 / Pricing generation statistics
 * @property lpFailureMessage LP 失败的详细错误信息 / LP failure detail message
 */
data class Csp1dColumnGenerationTrace(
    val initialPlanCount: UInt64,
    val finalPlanCount: UInt64,
    val pricedPlanCount: List<UInt64>,
    val terminationReason: Csp1dTerminationReason = Csp1dTerminationReason.PricingConverged,
    val iterations: List<Csp1dIterationRecord> = emptyList(),
    val initialGenerationStatistics: CuttingPlanGenerationStatistics? = null,
    val finalMilpStatus: Csp1dFinalMilpStatus = Csp1dFinalMilpStatus.NotAttempted,
    val partialSolutionAvailable: Boolean = false,
    val failureMessage: String? = null,
    val pricingGenerationStatistics: CuttingPlanGenerationStatistics? = null,
    val lpFailureMessage: String? = null
)

class Csp1dColumnGeneration<V : RealNumber<V>>(
    private val solver: ColumnGenerationSolver,
    private val initialGenerator: Csp1dInitialCuttingPlanGenerator<V> = SimpleInitialCuttingPlanGenerator(),
    private val pricingGenerator: Csp1dPricingGenerator<V> = ReducedCostPricingGenerator(initialGenerator),
    private val analyzer: Csp1dSolutionAnalyzer<V> = DefaultCsp1dSolutionAnalyzer(),
    private val yieldConfig: YieldModelingConfig<V>? = null,
    private val wasteConfig: WasteMinimizationConfig<V>? = null,
    private val lengthConfig: LengthAssignmentModelingConfig<V>? = null,
    private val warmStartPlanUsages: List<CuttingPlanUsage<V>> = emptyList()
) {
    suspend fun solve(
        problem: Csp1dProblem<V>,
        solveConfig: Csp1dSolveConfig<V>? = null
    ): Csp1dSolution<V> {
        return solveWithTrace(
            problem = problem,
            solveConfig = solveConfig
        ).solution
    }

    suspend fun solveWithTrace(
        problem: Csp1dProblem<V>,
        solveConfig: Csp1dSolveConfig<V>? = null
    ): Csp1dColumnGenerationResult<V> {
        val resolvedConfig = resolveSolveConfig(
            problem = problem,
            solveConfig = solveConfig
        )
        val columnConfig = resolvedConfig.columnGeneration
        val candidateFilters = resolvedConfig.extensionSet.generationStrategies.map { strategy ->
            { candidate: CuttingPlan<V>, existing: List<CuttingPlan<V>> -> strategy.acceptCandidate(candidate, existing) }
        }
        val domainPolicies = resolvedConfig.extensionSet.domainPolicies
        val vSample = problem.demands.firstOrNull()?.quantity?.value
            ?: problem.materials.firstOrNull()?.widthRange?.upperBound?.value
        val widthCheck = if (vSample != null) widthFeasibilityCheckFromPolicies(domainPolicies, vSample) else null
        val initialPlanPool = initialPlanPool(
            problem = problem,
            configuration = columnConfig,
            domainPolicies = domainPolicies,
            candidateFilters = candidateFilters,
            flowPolicies = resolvedConfig.extensionSet.flowPolicies,
            widthFeasibilityCheck = widthCheck
        )
        val initialPlans = initialPlanPool.plans
        val initialCount = initialPlans.size

        if (initialPlans.isEmpty()) {
            val failureMessage = "No initial cutting plans generated"
            val emptyProduce = emptyProduce(problem)
            val baseSolution = analyzer.analyze(
                problem = problem,
                produce = emptyProduce,
                generatedPlans = emptyList()
            )
            val solution = enrichSolution(
                solution = baseSolution,
                topPlans = emptyList(),
                status = Csp1dSolutionStatus.NoInitialPlans,
                failureMessage = failureMessage,
                terminationReason = Csp1dTerminationReason.NoInitialPlans,
                finalMilpStatus = Csp1dFinalMilpStatus.NotAttempted,
                partialSolutionAvailable = false,
                initialGenerationStatistics = initialPlanPool.statistics,
                iterationRecords = emptyList(),
                extractionPolicies = resolvedConfig.extensionSet.extractionPolicies,
                demands = problem.demands,
                materials = problem.materials,
                machines = problem.machines
            )
            return Csp1dColumnGenerationResult(
                solution = solution,
                trace = Csp1dColumnGenerationTrace(
                    initialPlanCount = UInt64.zero,
                    finalPlanCount = UInt64.zero,
                    pricedPlanCount = emptyList(),
                    terminationReason = Csp1dTerminationReason.NoInitialPlans,
                    iterations = emptyList(),
                    initialGenerationStatistics = initialPlanPool.statistics,
                    finalMilpStatus = Csp1dFinalMilpStatus.NotAttempted,
                    partialSolutionAvailable = false,
                    failureMessage = failureMessage
                )
            )
        }

        var currentPlans = initialPlans
        val pricedPlanCounts = ArrayList<UInt64>()
        val iterationRecords = ArrayList<Csp1dIterationRecord>()
        var terminationReason: Csp1dTerminationReason = Csp1dTerminationReason.PricingConverged
        var hasValidLpResult = false
        var lpFailureMessage: String? = null
        var pricingGenerationStatistics: CuttingPlanGenerationStatistics? = null
        val flowPolicies = resolvedConfig.extensionSet.flowPolicies

        for (iteration in 0 until columnConfig.iterationLimit) {
            val planCountBefore = currentPlans.size
            val lpResult = Csp1dMilpSolver(solver).solveLP(
                ProduceInput(
                    cuttingPlans = currentPlans,
                    demands = problem.demands,
                    materials = problem.materials,
                    machines = problem.machines
                ),
                extensions = resolvedConfig.allExtensions
            )
            if (lpResult == null) {
                pricedPlanCounts.add(UInt64.zero)
                iterationRecords.add(
                    Csp1dIterationRecord(
                        iteration = iteration,
                        lpObjective = Flt64.zero,
                        planCountBefore = planCountBefore,
                        pricedPlanCount = UInt64.zero,
                        planCountAfter = planCountBefore
                    )
                )
                terminationReason = if (!hasValidLpResult) {
                    Csp1dTerminationReason.LpInfeasible
                } else {
                    Csp1dTerminationReason.LpSolveFailed
                }
                lpFailureMessage = "LP solve returned null at iteration $iteration"
                // Apply flow policy selectTermination
                if (flowPolicies.isNotEmpty()) {
                    val flowCtx = buildFlowContext(
                        iteration = iteration,
                        currentPlans = currentPlans,
                        iterationLimit = columnConfig.iterationLimit,
                        allowPartialSolution = resolvedConfig.allowPartialSolution,
                        hasValidLpResult = hasValidLpResult,
                        pricingStatistics = pricingGenerationStatistics
                    )
                    val (customReason, customMessage) = selectTerminationByPolicies(
                        flowPolicies, flowCtx, terminationReason.name, lpFailureMessage
                    )
                    lpFailureMessage = customMessage ?: lpFailureMessage
                }
                break
            }

            hasValidLpResult = true

            val lpObjective = lpResult.lpOutput.result.obj
            val shadowPrices = lpResult.shadowPrices

            val pricingCandidateFilters = resolvedConfig.extensionSet.generationStrategies.map { strategy ->
                { candidate: CuttingPlan<V>, existing: List<CuttingPlan<V>> -> strategy.acceptCandidate(candidate, existing) }
            }
            val pricingCostModifiers = resolvedConfig.extensionSet.pricingPolicies.map { policy ->
                { candidate: CuttingPlan<V>, baseCost: V -> policy.modifyCost(candidate, baseCost) }
            }
            val pricingBenefitModifiers = resolvedConfig.extensionSet.pricingPolicies.map { policy ->
                { candidate: CuttingPlan<V>, baseBenefit: V -> policy.modifyBenefit(candidate, baseBenefit) }
            }
            val isImprovingJudges = resolvedConfig.extensionSet.pricingPolicies.map { policy ->
                { candidate: CuttingPlan<V>, benefit: V, cost: V -> policy.isImproving(candidate, benefit, cost) }
            }
            val pricingInput = Csp1dPricingInput(
                generationInput = CuttingPlanGenerationInput(
                    products = problem.products,
                    materials = problem.materials,
                    machines = problem.machines,
                    costars = problem.costars,
                    demands = problem.demands,
                    existingPlans = currentPlans,
                    domainPolicies = domainPolicies,
                    candidateFilters = pricingCandidateFilters,
                    widthFeasibilityCheck = widthCheck
                ),
                shadowPrices = shadowPrices,
                maxGeneratedPlans = UInt64(columnConfig.maxPricingPlans.coerceAtLeast(0)),
                objectiveConfig = pricingObjectiveConfig(resolvedConfig),
                pricingCostModifiers = pricingCostModifiers,
                pricingBenefitModifiers = pricingBenefitModifiers,
                isImprovingJudges = isImprovingJudges
            )
            val pricingReport = pricingGenerator.generateWithReport(pricingInput)
            pricingGenerationStatistics = mergeGenerationStatistics(
                left = pricingGenerationStatistics,
                right = pricingReport.statistics
            )
            val newPlans = pricingReport.plans

            if (newPlans.isEmpty()) {
                pricedPlanCounts.add(UInt64.zero)
                iterationRecords.add(
                    Csp1dIterationRecord(
                        iteration = iteration,
                        lpObjective = lpObjective,
                        planCountBefore = planCountBefore,
                        pricedPlanCount = UInt64.zero,
                        planCountAfter = planCountBefore
                    )
                )
                terminationReason = Csp1dTerminationReason.PricingConverged
                break
            }

            // Build flow context for deduplication and iteration control
            val flowContext = buildFlowContext(
                iteration = iteration,
                currentPlans = currentPlans,
                iterationLimit = columnConfig.iterationLimit,
                allowPartialSolution = resolvedConfig.allowPartialSolution,
                newPlans = newPlans,
                hasValidLpResult = hasValidLpResult,
                pricingStatistics = pricingGenerationStatistics
            )

            val addedPlans = deduplicatePlans(currentPlans, newPlans, flowPolicies, flowContext)
            if (addedPlans.isEmpty()) {
                pricedPlanCounts.add(UInt64.zero)
                iterationRecords.add(
                    Csp1dIterationRecord(
                        iteration = iteration,
                        lpObjective = lpObjective,
                        planCountBefore = planCountBefore,
                        pricedPlanCount = UInt64.zero,
                        planCountAfter = planCountBefore
                    )
                )
                terminationReason = Csp1dTerminationReason.AllDuplicates
                break
            }

            currentPlans = currentPlans + addedPlans
            pricedPlanCounts.add(UInt64(addedPlans.size))
            iterationRecords.add(
                Csp1dIterationRecord(
                    iteration = iteration,
                    lpObjective = lpObjective,
                    planCountBefore = planCountBefore,
                    pricedPlanCount = UInt64(addedPlans.size),
                    planCountAfter = currentPlans.size
                )
            )

            if (iteration == columnConfig.iterationLimit - 1) {
                terminationReason = Csp1dTerminationReason.IterationLimitReached
            }

            // Check flow policy early stop condition
            if (iteration < columnConfig.iterationLimit - 1 && flowPolicies.isNotEmpty()) {
                val stopContext = buildFlowContext(
                    iteration = iteration,
                    currentPlans = currentPlans,
                    iterationLimit = columnConfig.iterationLimit,
                    allowPartialSolution = resolvedConfig.allowPartialSolution,
                    hasValidLpResult = hasValidLpResult,
                    pricingStatistics = pricingGenerationStatistics
                )
                if (shouldStopByPolicies(flowPolicies, stopContext)) {
                    terminationReason = Csp1dTerminationReason.PricingConverged
                    // Apply custom termination message from flow policy
                    val (customReason, customMessage) = selectTerminationByPolicies(
                        flowPolicies, stopContext, terminationReason.name, null
                    )
                    if (customMessage != null) {
                        lpFailureMessage = customMessage
                    }
                    break
                }
            }
        }

        val finalMilp = solveFinalMilp(
            problem = problem,
            cuttingPlans = currentPlans,
            solveConfig = resolvedConfig
        )
        val produce = finalMilp.milpResult?.produce ?: emptyProduce(problem)
        val baseSolution = analyzer.analyze(
            problem = problem,
            produce = produce,
            generatedPlans = currentPlans
        )
        val topPlans = topCuttingPlans(
            plans = currentPlans,
            limit = resolvedConfig.topKPlanLimit
        )
        // Apply flow policy acceptPartial when final MILP fails
        val allowPartial = if (finalMilp.status == Csp1dFinalMilpStatus.Failed && flowPolicies.isNotEmpty()) {
            val postFlowContext = buildFlowContext(
                iteration = iterationRecords.size,
                currentPlans = currentPlans,
                iterationLimit = columnConfig.iterationLimit,
                allowPartialSolution = resolvedConfig.allowPartialSolution,
                hasValidLpResult = hasValidLpResult,
                pricingStatistics = pricingGenerationStatistics
            )
            acceptPartialByPolicies(flowPolicies, postFlowContext, resolvedConfig.allowPartialSolution)
        } else {
            resolvedConfig.allowPartialSolution
        }
        val solutionStatus = when (finalMilp.status) {
            Csp1dFinalMilpStatus.Solved -> Csp1dSolutionStatus.Feasible
            Csp1dFinalMilpStatus.Failed -> if (allowPartial) Csp1dSolutionStatus.Partial else Csp1dSolutionStatus.Failed
            Csp1dFinalMilpStatus.NotAttempted -> Csp1dSolutionStatus.Partial
        }
        val combinedFailureMessage = when {
            lpFailureMessage != null && finalMilp.failureMessage != null ->
                "$lpFailureMessage; ${finalMilp.failureMessage}"
            lpFailureMessage != null -> lpFailureMessage
            else -> finalMilp.failureMessage
        }
        val solution = enrichSolution(
            solution = baseSolution.copy(
                yieldResult = finalMilp.milpResult?.yieldResult,
                wasteResult = finalMilp.milpResult?.wasteResult,
                lengthResult = finalMilp.milpResult?.lengthResult
            ),
            topPlans = topPlans,
            status = solutionStatus,
            failureMessage = combinedFailureMessage,
            terminationReason = terminationReason,
            finalMilpStatus = finalMilp.status,
            partialSolutionAvailable = finalMilp.status == Csp1dFinalMilpStatus.Failed,
            initialGenerationStatistics = initialPlanPool.statistics,
            pricingGenerationStatistics = pricingGenerationStatistics,
            lpFailureMessage = lpFailureMessage,
            iterationRecords = iterationRecords,
            extractionPolicies = resolvedConfig.extensionSet.extractionPolicies,
            demands = problem.demands,
            materials = problem.materials,
            machines = problem.machines
        )
        return Csp1dColumnGenerationResult(
            solution = solution,
            trace = Csp1dColumnGenerationTrace(
                initialPlanCount = UInt64(initialCount),
                finalPlanCount = UInt64(currentPlans.size),
                pricedPlanCount = pricedPlanCounts,
                terminationReason = terminationReason,
                iterations = iterationRecords,
                initialGenerationStatistics = initialPlanPool.statistics,
                finalMilpStatus = finalMilp.status,
                partialSolutionAvailable = finalMilp.status == Csp1dFinalMilpStatus.Failed,
                failureMessage = finalMilp.failureMessage,
                pricingGenerationStatistics = pricingGenerationStatistics,
                lpFailureMessage = lpFailureMessage
            )
        )
    }

    private fun initialPlanPool(
        problem: Csp1dProblem<V>,
        configuration: Csp1dConfiguration<V>,
        domainPolicies: List<Csp1dDomainPolicy<V>> = emptyList(),
        candidateFilters: List<(CuttingPlan<V>, List<CuttingPlan<V>>) -> Boolean> = emptyList(),
        flowPolicies: List<Csp1dFlowPolicy<V>> = emptyList(),
        widthFeasibilityCheck: ((Material<V>, Product<V>, Quantity<V>) -> Boolean)? = null
    ): InitialPlanPool<V> {
        if (configuration.maxInitialPlans <= 0) {
            return InitialPlanPool(
                plans = emptyList(),
                statistics = null
            )
        }
        val report = initialGenerator.generateWithReport(
            CuttingPlanGenerationInput(
                products = problem.products,
                materials = problem.materials,
                machines = problem.machines,
                costars = problem.costars,
                demands = problem.demands,
                domainPolicies = domainPolicies,
                candidateFilters = candidateFilters,
                widthFeasibilityCheck = widthFeasibilityCheck
            )
        )
        val generatedPlans = report.plans.distinctBy { it.canonicalKey() }
            .take(configuration.maxInitialPlans)
        // Apply flow policy initial plan filter with context
        val filteredPlans = if (flowPolicies.isNotEmpty()) {
            val flowContext = object : Csp1dFlowContext<V> {
                override val iteration = 0
                override val currentPlans: List<CuttingPlan<V>> = generatedPlans
                override val iterationLimit = configuration.iterationLimit
                override val allowPartialSolution = true
            }
            filterInitialPlansByPolicies(flowPolicies, flowContext, generatedPlans)
        } else {
            generatedPlans
        }
        return InitialPlanPool(
            plans = filteredPlans,
            statistics = report.statistics
        )
    }

    private data class InitialPlanPool<V : RealNumber<V>>(
        val plans: List<CuttingPlan<V>>,
        val statistics: CuttingPlanGenerationStatistics?
    )

    private fun mergeGenerationStatistics(
        left: CuttingPlanGenerationStatistics?,
        right: CuttingPlanGenerationStatistics
    ): CuttingPlanGenerationStatistics {
        if (left == null) {
            return right
        }
        return CuttingPlanGenerationStatistics(
            visitedNodes = left.visitedNodes + right.visitedNodes,
            generatedCandidates = left.generatedCandidates + right.generatedCandidates,
            acceptedPlans = left.acceptedPlans + right.acceptedPlans,
            infeasibleCandidates = left.infeasibleCandidates + right.infeasibleCandidates,
            duplicateCandidates = left.duplicateCandidates + right.duplicateCandidates,
            dominatedCandidates = left.dominatedCandidates + right.dominatedCandidates,
            widthBoundPrunedNodes = left.widthBoundPrunedNodes + right.widthBoundPrunedNodes,
            knifeBoundPrunedNodes = left.knifeBoundPrunedNodes + right.knifeBoundPrunedNodes,
            lengthBoundPrunedEntries = left.lengthBoundPrunedEntries + right.lengthBoundPrunedEntries,
            materialWidthIndexCacheHits = left.materialWidthIndexCacheHits + right.materialWidthIndexCacheHits,
            materialSliceTemplateCacheHits =
                left.materialSliceTemplateCacheHits + right.materialSliceTemplateCacheHits,
            quantityCacheHits = left.quantityCacheHits + right.quantityCacheHits,
            quantityCacheMisses = left.quantityCacheMisses + right.quantityCacheMisses,
            materialSliceTemplateCacheMisses =
                left.materialSliceTemplateCacheMisses + right.materialSliceTemplateCacheMisses,
            crossWorkerDuplicateCandidates =
                left.crossWorkerDuplicateCandidates + right.crossWorkerDuplicateCandidates,
            crossContributionDominated = left.crossContributionDominated + right.crossContributionDominated,
            elapsedMilliseconds = left.elapsedMilliseconds + right.elapsedMilliseconds,
            stopReason = right.stopReason
        )
    }

    private fun deduplicatePlans(
        existing: List<CuttingPlan<V>>,
        candidates: List<CuttingPlan<V>>,
        flowPolicies: List<Csp1dFlowPolicy<V>> = emptyList(),
        flowContext: Csp1dFlowContext<V>? = null
    ): List<CuttingPlan<V>> {
        val existingIds = existing.map { it.id }.toSet()
        val existingKeys = existing.map { it.canonicalKey() }.toSet()
        return candidates.filter { candidate ->
            candidate.id !in existingIds && candidate.canonicalKey() !in existingKeys
                // Apply flow policy equivalence check with context
                && if (flowPolicies.isNotEmpty() && flowContext != null) {
                    !existing.any { existingPlan ->
                        isEquivalentByPolicies(flowPolicies, flowContext, existingPlan, candidate)
                    }
                } else if (flowPolicies.isNotEmpty()) {
                    !existing.any { existingPlan ->
                        flowPolicies.any { it.isEquivalent(existingPlan, candidate) }
                    }
                } else {
                    true
                }
        }
    }

    private fun pricingObjectiveConfig(solveConfig: Csp1dSolveConfig<V>): Csp1dPricingObjectiveConfig<V> {
        return Csp1dPricingObjectiveConfig(
            planUsagePenalty = solveConfig.lengthConfig?.batchMinPenalty,
            trimWidthPenalty = solveConfig.wasteConfig?.trimWidthPenalty,
            restMaterialPenalty = solveConfig.wasteConfig?.restMaterialPenalty,
            materialCostPenalty = solveConfig.wasteConfig?.materialCostPenalty ?: emptyMap()
        )
    }

    private fun resolveSolveConfig(
        problem: Csp1dProblem<V>,
        solveConfig: Csp1dSolveConfig<V>?
    ): Csp1dSolveConfig<V> {
        val baseConfig = solveConfig ?: problem.solveConfig ?: Csp1dSolveConfig(
            columnGeneration = problem.configuration
        )
        return baseConfig.copy(
            yieldConfig = baseConfig.yieldConfig ?: yieldConfig,
            wasteConfig = baseConfig.wasteConfig ?: wasteConfig,
            lengthConfig = baseConfig.lengthConfig ?: lengthConfig
        )
    }

    private suspend fun solveFinalMilp(
        problem: Csp1dProblem<V>,
        cuttingPlans: List<CuttingPlan<V>>,
        solveConfig: Csp1dSolveConfig<V>
    ): FinalMilpSolveResult<V> {
        val result = try {
            Csp1dMilpSolver(solver).solve(
                input = ProduceInput(
                    cuttingPlans = cuttingPlans,
                    demands = problem.demands,
                    materials = problem.materials,
                    machines = problem.machines,
                    warmStartPlanUsages = warmStartPlanUsages
                ),
                yieldConfig = solveConfig.yieldConfig,
                wasteConfig = solveConfig.wasteConfig,
                lengthConfig = solveConfig.lengthConfig,
                extensions = solveConfig.allExtensions,
                objectivePolicies = solveConfig.extensionSet.objectivePolicies,
                isFinalMilp = true
            )
        } catch (error: Exception) {
            return FinalMilpSolveResult(
                status = Csp1dFinalMilpStatus.Failed,
                milpResult = null,
                failureMessage = error.message ?: "Final MILP solve failed"
            )
        }

        if (result != null) {
            return FinalMilpSolveResult(
                status = Csp1dFinalMilpStatus.Solved,
                milpResult = result,
                failureMessage = null
            )
        }

        val failureMessage = "Final MILP returned no solution"
        return FinalMilpSolveResult(
            status = Csp1dFinalMilpStatus.Failed,
            milpResult = null,
            failureMessage = failureMessage
        )
    }

    private fun emptyProduce(problem: Csp1dProblem<V>): Produce<V> {
        return Produce(
            cuttingPlans = emptyList(),
            materialUsages = emptyList(),
            machineUsages = emptyList(),
            unmetDemands = problem.demands
        )
    }

    private fun buildFlowContext(
        iteration: Int,
        currentPlans: List<CuttingPlan<V>>,
        iterationLimit: Int,
        allowPartialSolution: Boolean,
        newPlans: List<CuttingPlan<V>> = emptyList(),
        hasValidLpResult: Boolean = false,
        pricingStatistics: CuttingPlanGenerationStatistics? = null
    ): Csp1dFlowContext<V> {
        return object : Csp1dFlowContext<V> {
            override val iteration = iteration
            override val currentPlans = currentPlans
            override val iterationLimit = iterationLimit
            override val allowPartialSolution = allowPartialSolution
            override val newPlans = newPlans
            override val hasValidLpResult = hasValidLpResult
            override val pricingStatistics = pricingStatistics
        }
    }

    private data class FinalMilpSolveResult<V : RealNumber<V>>(
        val status: Csp1dFinalMilpStatus,
        val milpResult: Csp1dMilpSolver.MilpResult<V>?,
        val failureMessage: String?
    )
}

data class Csp1dColumnGenerationResult<V : RealNumber<V>>(
    val solution: Csp1dSolution<V>,
    val trace: Csp1dColumnGenerationTrace
)
