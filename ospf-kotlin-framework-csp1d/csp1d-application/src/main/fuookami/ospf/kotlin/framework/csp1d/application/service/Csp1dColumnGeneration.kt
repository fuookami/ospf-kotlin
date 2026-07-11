package fuookami.ospf.kotlin.framework.csp1d.application.service

import fuookami.ospf.kotlin.core.model.mechanism.LinearMetaModel
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.framework.csp1d.application.model.*
import fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.*
import fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.model.*
import fuookami.ospf.kotlin.framework.csp1d.domain.length_assignment.model.LengthAssignmentModelingConfig
import fuookami.ospf.kotlin.framework.csp1d.domain.material.error.Csp1dLifecycleError
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.*
import fuookami.ospf.kotlin.framework.csp1d.domain.produce.model.*
import fuookami.ospf.kotlin.framework.csp1d.domain.produce.ProduceInput
import fuookami.ospf.kotlin.framework.csp1d.domain.yield.model.YieldModelingConfig
import fuookami.ospf.kotlin.framework.solver.ColumnGenerationSolver
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*

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
    val iteration: Int64,
    val lpObjective: Flt64,
    val planCountBefore: Int64,
    val pricedPlanCount: UInt64,
    val planCountAfter: Int64
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

/**
 * CSP1D 列生成求解器 / CSP1D column generation solver
 *
 * 实现列生成主循环：初始方案生成 -> LP 松弛求解 -> pricing 定价 -> 加列迭代 -> 最终 MILP 整数求解。
 * 支持 flow policy 自定义终止/去重/早停逻辑、warm start 初始方案注入、以及多种 pricing 生成器。
 *
 * Implements the column generation main loop: initial plan generation -> LP relaxation solve -> pricing -> column addition iteration -> final MILP integer solve.
 * Supports flow policy custom termination/deduplication/early-stop logic, warm start initial plan injection, and multiple pricing generators.
 *
 * @param V 数值类型 / Numeric value type
 * @property solver 列生成求解器 / Column generation solver
 * @property initialGenerator 初始方案生成器 / Initial cutting plan generator
 * @property pricingGenerator 定价方案生成器 / Pricing cutting plan generator
 * @property analyzer 解分析器 / Solution analyzer
 * @property yieldConfig 默认 yield 建模配置 / Default yield modeling config
 * @property wasteConfig 默认 waste 建模配置 / Default waste modeling config
 * @property lengthConfig 默认 length 建模配置 / Default length modeling config
 * @property warmStartPlanUsages warm start 方案使用量 / Warm start plan usages
*/
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

/**
 * LpMaster data class.
 * LpMaster数据类。
 *
 * @param model 线性元模型 / Linear meta model
 * @param context CSP1D 生产上下文 / CSP1D produce context
 * @param domainValueSample 领域值样本 / Domain value sample
*/
    private data class LpMaster<V : RealNumber<V>>(
        val model: LinearMetaModel<Flt64>,
        val context: Csp1dProduceContext<V>,
        val domainValueSample: V
    )

    /**
     * 列生成求解（仅返回解）/ Column generation solve (returns solution only)
     *
     * @param problem 问题定义 / Problem definition
     * @param solveConfig 显式求解配置，优先级高于 problem.solveConfig / Explicit solve config, higher priority than problem.solveConfig
     * @return CSP1D 解 / CSP1D solution
    */
    suspend fun solve(
        problem: Csp1dProblem<V>,
        solveConfig: Csp1dSolveConfig<V>? = null
    ): Csp1dSolution<V> {
        return solveWithTrace(
            problem = problem,
            solveConfig = solveConfig
        ).solution
    }

    /**
     * 带追踪信息的列生成求解 / Column generation solve with trace
     *
     * 返回完整列生成结果，包含迭代记录、终止原因、pricing 统计等追踪信息。
     * Returns complete column generation result including iteration records, termination reason, pricing statistics and other trace information.
     *
     * @param problem 问题定义 / Problem definition
     * @param solveConfig 显式求解配置，优先级高于 problem.solveConfig / Explicit solve config, higher priority than problem.solveConfig
     * @return 列生成结果（含追踪信息）/ Column generation result with trace
    */
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
        val canonicalKeyOverrides = resolvedConfig.extensionSet.generationStrategies.map { strategy ->
            { candidate: CuttingPlan<V> -> strategy.canonicalKeyFor(candidate) }
        }
        val dominanceAcceptOverrides = resolvedConfig.extensionSet.generationStrategies.map { strategy ->
            { candidate: CuttingPlan<V>, existing: List<CuttingPlan<V>> -> strategy.acceptDominance(candidate, existing) }
        }
        val domainPolicies = resolvedConfig.extensionSet.domainPolicies
        val domainValueSample = problem.demands.firstOrNull()?.quantity?.value
            ?: problem.materials.firstOrNull()?.widthRange?.upperBound?.value
        val widthCheck = if (domainValueSample != null) widthFeasibilityCheckFromPolicies(domainPolicies, domainValueSample) else null
        val initialPlanPool = initialPlanPool(
            problem = problem,
            configuration = columnConfig,
            domainPolicies = domainPolicies,
            candidateFilters = candidateFilters,
            canonicalKeyOverrides = canonicalKeyOverrides,
            dominanceAcceptOverrides = dominanceAcceptOverrides,
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
        val iterationLimit = columnConfig.iterationLimit
        val iterationLimitIndexBound = iterationLimit.toInt()
        val lpDomainValueSample = domainValueSample
            ?: currentPlans.firstOrNull()?.restWidth?.value
            ?: currentPlans.firstOrNull()?.demandContributions?.firstOrNull()?.quantity?.value
            ?: run {
                val failureMessage = "Cannot derive domain value sample for CSP1D LP master"
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
        val lpMaster = if (iterationLimitIndexBound > 0) {
            when (val result = buildLpMaster(
                problem = problem,
                cuttingPlans = currentPlans,
                extensions = resolvedConfig.allExtensions,
                domainValueSample = lpDomainValueSample
            )) {
                is Ok -> result.value
                is Failed, is Fatal -> {
                    val errorMessage = when (result) {
                        is Failed -> result.error.message
                        is Fatal -> result.errors.joinToString(", ") { it.message }
                        else -> ""
                    }
                    pricedPlanCounts.add(UInt64.zero)
                    iterationRecords.add(
                        Csp1dIterationRecord(
                            iteration = Int64.zero,
                            lpObjective = Flt64.zero,
                            planCountBefore = Int64(currentPlans.size.toLong()),
                            pricedPlanCount = UInt64.zero,
                            planCountAfter = Int64(currentPlans.size.toLong())
                        )
                    )
                    terminationReason = Csp1dTerminationReason.LpInfeasible
                    lpFailureMessage = errorMessage ?: "LP master build failed"
                    if (flowPolicies.isNotEmpty()) {
                        val flowCtx = buildFlowContext(
                            iteration = Int64.zero,
                            currentPlans = currentPlans,
                            iterationLimit = iterationLimit,
                            allowPartialSolution = resolvedConfig.allowPartialSolution,
                            hasValidLpResult = hasValidLpResult,
                            pricingStatistics = pricingGenerationStatistics
                        )
                        val (customReason, customMessage) = selectTerminationByPolicies(
                            flowPolicies, flowCtx, terminationReason.name, lpFailureMessage
                        )
                        terminationReason = resolveTerminationReason(customReason, terminationReason)
                        lpFailureMessage = customMessage ?: lpFailureMessage
                    }
                    null
                }
            }
        } else {
            null
        }

        if (lpMaster != null) {
            for (iteration in 0 until iterationLimitIndexBound) {
                val iterationNumber = Int64(iteration.toLong())
                val planCountBefore = currentPlans.size
                val lpResult = solveLpMaster(
                    master = lpMaster,
                    iteration = iterationNumber
                )
                if (lpResult == null) {
                    pricedPlanCounts.add(UInt64.zero)
                    iterationRecords.add(
                        Csp1dIterationRecord(
                            iteration = iterationNumber,
                            lpObjective = Flt64.zero,
                            planCountBefore = Int64(planCountBefore.toLong()),
                            pricedPlanCount = UInt64.zero,
                            planCountAfter = Int64(planCountBefore.toLong())
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
                            iteration = iterationNumber,
                            currentPlans = currentPlans,
                            iterationLimit = iterationLimit,
                            allowPartialSolution = resolvedConfig.allowPartialSolution,
                            hasValidLpResult = hasValidLpResult,
                            pricingStatistics = pricingGenerationStatistics
                        )
                        val (customReason, customMessage) = selectTerminationByPolicies(
                            flowPolicies, flowCtx, terminationReason.name, lpFailureMessage
                        )
                        terminationReason = resolveTerminationReason(customReason, terminationReason)
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
                        widthFeasibilityCheck = widthCheck,
                        canonicalKeyOverrides = canonicalKeyOverrides,
                        dominanceAcceptOverrides = dominanceAcceptOverrides
                    ),
                    shadowPrices = shadowPrices,
                    maxGeneratedPlans = UInt64(columnConfig.maxPricingPlans.toLong().coerceAtLeast(0L).toULong()),
                    objectiveConfig = pricingObjectiveConfig(resolvedConfig),
                    pricingCostModifiers = pricingCostModifiers,
                    pricingBenefitModifiers = pricingBenefitModifiers,
                    isImprovingJudges = isImprovingJudges,
                    canonicalKeyOverrides = canonicalKeyOverrides
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
                            iteration = iterationNumber,
                            lpObjective = lpObjective,
                            planCountBefore = Int64(planCountBefore.toLong()),
                            pricedPlanCount = UInt64.zero,
                            planCountAfter = Int64(planCountBefore.toLong())
                        )
                    )
                    terminationReason = Csp1dTerminationReason.PricingConverged
                    // Apply flow policy selectTermination
                    if (flowPolicies.isNotEmpty()) {
                        val flowCtx = buildFlowContext(
                            iteration = iterationNumber,
                            currentPlans = currentPlans,
                            iterationLimit = iterationLimit,
                            allowPartialSolution = resolvedConfig.allowPartialSolution,
                            hasValidLpResult = hasValidLpResult,
                            pricingStatistics = pricingGenerationStatistics
                        )
                        val (customReason, customMessage) = selectTerminationByPolicies(
                            flowPolicies, flowCtx, terminationReason.name, null
                        )
                        terminationReason = resolveTerminationReason(customReason, terminationReason)
                        if (customMessage != null) {
                            lpFailureMessage = customMessage
                        }
                    }
                    break
                }

                // Build flow context for deduplication and iteration control
                val flowContext = buildFlowContext(
                    iteration = iterationNumber,
                    currentPlans = currentPlans,
                    iterationLimit = iterationLimit,
                    allowPartialSolution = resolvedConfig.allowPartialSolution,
                    newPlans = newPlans,
                    hasValidLpResult = hasValidLpResult,
                    pricingStatistics = pricingGenerationStatistics
                )

                val addedPlans = deduplicatePlans(currentPlans, newPlans, flowPolicies, flowContext, canonicalKeyOverrides)
                if (addedPlans.isEmpty()) {
                    pricedPlanCounts.add(UInt64.zero)
                    iterationRecords.add(
                        Csp1dIterationRecord(
                            iteration = iterationNumber,
                            lpObjective = lpObjective,
                            planCountBefore = Int64(planCountBefore.toLong()),
                            pricedPlanCount = UInt64.zero,
                            planCountAfter = Int64(planCountBefore.toLong())
                        )
                    )
                    terminationReason = Csp1dTerminationReason.AllDuplicates
                    // Apply flow policy selectTermination
                    if (flowPolicies.isNotEmpty()) {
                        val (customReason, customMessage) = selectTerminationByPolicies(
                            flowPolicies, flowContext, terminationReason.name, null
                        )
                        terminationReason = resolveTerminationReason(customReason, terminationReason)
                        if (customMessage != null) {
                            lpFailureMessage = customMessage
                        }
                    }
                    break
                }

                val addColumnsResult = lpMaster.context.addColumns(
                    iteration = UInt64((iteration + 1).toULong()),
                    newPlans = addedPlans,
                    model = lpMaster.model
                )
                val modelAddedPlans = when (addColumnsResult) {
                    is Ok -> addColumnsResult.value
                    is Failed -> null
                    is Fatal -> null
                }
                if (modelAddedPlans == null) {
                    val message = when (addColumnsResult) {
                        is Failed -> "addColumns failed at iteration $iteration: ${addColumnsResult.error}"
                        is Fatal -> "addColumns fatal at iteration $iteration: ${addColumnsResult.errors}"
                        is Ok -> "addColumns returned no result at iteration $iteration"
                    }
                    pricedPlanCounts.add(UInt64.zero)
                    iterationRecords.add(
                        Csp1dIterationRecord(
                            iteration = iterationNumber,
                            lpObjective = lpObjective,
                            planCountBefore = Int64(planCountBefore.toLong()),
                            pricedPlanCount = UInt64.zero,
                            planCountAfter = Int64(planCountBefore.toLong())
                        )
                    )
                    terminationReason = Csp1dTerminationReason.LpSolveFailed
                    lpFailureMessage = message
                    if (flowPolicies.isNotEmpty()) {
                        val flowCtx = buildFlowContext(
                            iteration = iterationNumber,
                            currentPlans = currentPlans,
                            iterationLimit = iterationLimit,
                            allowPartialSolution = resolvedConfig.allowPartialSolution,
                            hasValidLpResult = hasValidLpResult,
                            pricingStatistics = pricingGenerationStatistics
                        )
                        val (customReason, customMessage) = selectTerminationByPolicies(
                            flowPolicies, flowCtx, terminationReason.name, lpFailureMessage
                        )
                        terminationReason = resolveTerminationReason(customReason, terminationReason)
                        lpFailureMessage = customMessage ?: lpFailureMessage
                    }
                    break
                }
                if (modelAddedPlans.isEmpty()) {
                    pricedPlanCounts.add(UInt64.zero)
                    iterationRecords.add(
                        Csp1dIterationRecord(
                            iteration = iterationNumber,
                            lpObjective = lpObjective,
                            planCountBefore = Int64(planCountBefore.toLong()),
                            pricedPlanCount = UInt64.zero,
                            planCountAfter = Int64(planCountBefore.toLong())
                        )
                    )
                    terminationReason = Csp1dTerminationReason.AllDuplicates
                    if (flowPolicies.isNotEmpty()) {
                        val (customReason, customMessage) = selectTerminationByPolicies(
                            flowPolicies, flowContext, terminationReason.name, null
                        )
                        terminationReason = resolveTerminationReason(customReason, terminationReason)
                        if (customMessage != null) {
                            lpFailureMessage = customMessage
                        }
                    }
                    break
                }

                currentPlans = currentPlans + modelAddedPlans
                pricedPlanCounts.add(UInt64(modelAddedPlans.size))
                iterationRecords.add(
                    Csp1dIterationRecord(
                        iteration = iterationNumber,
                        lpObjective = lpObjective,
                        planCountBefore = Int64(planCountBefore.toLong()),
                        pricedPlanCount = UInt64(modelAddedPlans.size),
                        planCountAfter = Int64(currentPlans.size.toLong())
                    )
                )

                if (iteration == iterationLimitIndexBound - 1) {
                    terminationReason = Csp1dTerminationReason.IterationLimitReached
                    // Apply flow policy selectTermination
                    if (flowPolicies.isNotEmpty()) {
                        val limitContext = buildFlowContext(
                            iteration = iterationNumber,
                            currentPlans = currentPlans,
                            iterationLimit = iterationLimit,
                            allowPartialSolution = resolvedConfig.allowPartialSolution,
                            hasValidLpResult = hasValidLpResult,
                            pricingStatistics = pricingGenerationStatistics
                        )
                        val (customReason, customMessage) = selectTerminationByPolicies(
                            flowPolicies, limitContext, terminationReason.name, null
                        )
                        terminationReason = resolveTerminationReason(customReason, terminationReason)
                        if (customMessage != null) {
                            lpFailureMessage = customMessage
                        }
                    }
                }

                // Check flow policy early stop condition
                if (iteration < iterationLimitIndexBound - 1 && flowPolicies.isNotEmpty()) {
                    val stopContext = buildFlowContext(
                        iteration = iterationNumber,
                        currentPlans = currentPlans,
                        iterationLimit = iterationLimit,
                        allowPartialSolution = resolvedConfig.allowPartialSolution,
                        hasValidLpResult = hasValidLpResult,
                        pricingStatistics = pricingGenerationStatistics
                    )
                    if (shouldStopByPolicies(flowPolicies, stopContext)) {
                        terminationReason = Csp1dTerminationReason.PricingConverged
                        // Apply custom termination reason/message from flow policy
                        val (customReason, customMessage) = selectTerminationByPolicies(
                            flowPolicies, stopContext, terminationReason.name, null
                        )
                        terminationReason = resolveTerminationReason(customReason, terminationReason)
                        if (customMessage != null) {
                            lpFailureMessage = customMessage
                        }
                        break
                    }
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
                iteration = Int64(iterationRecords.size.toLong()),
                currentPlans = currentPlans,
                iterationLimit = iterationLimit,
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

/**
 * initialPlanPool.
 * initialPlanPool。
 *
 * @param problem 问题定义 / Problem definition
 * @param configuration 列生成配置 / Column generation configuration
 * @param domainPolicies 领域策略列表 / Domain policy list
 * @param candidateFilters 候选过滤函数列表 / Candidate filter function list
 * @param canonicalKeyOverrides 规范键覆盖函数列表 / Canonical key override function list
 * @param dominanceAcceptOverrides 支配接受覆盖函数列表 / Dominance accept override function list
 * @param flowPolicies 流策略列表 / Flow policy list
 * @param widthFeasibilityCheck 宽度可行性检查函数 / Width feasibility check function
 * @return 初始方案池 / Initial plan pool
*/
    private fun initialPlanPool(
        problem: Csp1dProblem<V>,
        configuration: Csp1dConfiguration<V>,
        domainPolicies: List<Csp1dDomainPolicy<V>> = emptyList(),
        candidateFilters: List<(CuttingPlan<V>, List<CuttingPlan<V>>) -> Boolean> = emptyList(),
        canonicalKeyOverrides: List<(CuttingPlan<V>) -> String?> = emptyList(),
        dominanceAcceptOverrides: List<(CuttingPlan<V>, List<CuttingPlan<V>>) -> Boolean> = emptyList(),
        flowPolicies: List<Csp1dFlowPolicy<V>> = emptyList(),
        widthFeasibilityCheck: ((Material<V>, Product<V>, Quantity<V>) -> Boolean)? = null
    ): InitialPlanPool<V> {
        if (configuration.maxInitialPlans.toLong() <= 0L) {
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
                widthFeasibilityCheck = widthFeasibilityCheck,
                canonicalKeyOverrides = canonicalKeyOverrides,
                dominanceAcceptOverrides = dominanceAcceptOverrides
            )
        )
        // Resolve canonical key with strategy overrides
        val resolveCanonicalKey: (CuttingPlan<V>) -> CuttingPlanCanonicalKey = { plan ->
            val customKey = canonicalKeyOverrides.firstNotNullOfOrNull { it(plan) }
            if (customKey != null) CuttingPlanCanonicalKey(customKey) else plan.canonicalKey()
        }
        val generatedPlans = report.plans.distinctBy { resolveCanonicalKey(it) }
            .take(configuration.maxInitialPlans.toInt())
        // Apply flow policy initial plan filter with context
        val filteredPlans = if (flowPolicies.isNotEmpty()) {
            val flowContext = object : Csp1dFlowContext<V> {
                override val iteration = Int64.zero
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

/**
 * InitialPlanPool data class.
 * InitialPlanPool数据类。
 *
 * @param plans 切割方案列表 / Cutting plan list
 * @param statistics 生成统计 / Generation statistics
*/
    private data class InitialPlanPool<V : RealNumber<V>>(
        val plans: List<CuttingPlan<V>>,
        val statistics: CuttingPlanGenerationStatistics?
    )

/**
 * mergeGenerationStatistics.
 * mergeGenerationStatistics。
 *
 * @param left 左侧统计 / Left statistics
 * @param right 右侧统计 / Right statistics
 * @return 合并后的统计 / Merged statistics
*/
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

/**
 * deduplicatePlans.
 * deduplicatePlans。
 *
 * @param existing 已有方案列表 / Existing plan list
 * @param candidates 候选方案列表 / Candidate plan list
 * @param flowPolicies 流策略列表 / Flow policy list
 * @param flowContext 流上下文 / Flow context
 * @param canonicalKeyOverrides 规范键覆盖函数列表 / Canonical key override function list
 * @return 去重后的新方案列表 / Deduplicated new plan list
*/
    private fun deduplicatePlans(
        existing: List<CuttingPlan<V>>,
        candidates: List<CuttingPlan<V>>,
        flowPolicies: List<Csp1dFlowPolicy<V>> = emptyList(),
        flowContext: Csp1dFlowContext<V>? = null,
        canonicalKeyOverrides: List<(CuttingPlan<V>) -> String?> = emptyList()
    ): List<CuttingPlan<V>> {
        // Resolve canonical key with strategy overrides
        val resolveCanonicalKey: (CuttingPlan<V>) -> CuttingPlanCanonicalKey = { plan ->
            val customKey = canonicalKeyOverrides.firstNotNullOfOrNull { it(plan) }
            if (customKey != null) CuttingPlanCanonicalKey(customKey) else plan.canonicalKey()
        }
        val existingIds = existing.map { it.id }.toSet()
        val existingKeys = existing.map { resolveCanonicalKey(it) }.toSet()
        return candidates.filter { candidate ->
            val candidateKey = resolveCanonicalKey(candidate)
            candidate.id !in existingIds && candidateKey !in existingKeys
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

    /**
     * 定价目标配置 / Pricing objective configuration
     *
     * @param solveConfig 求解配置 / solve configuration
     * @return 定价目标配置 / pricing objective configuration
    */
    private fun pricingObjectiveConfig(solveConfig: Csp1dSolveConfig<V>): Csp1dPricingObjectiveConfig<V> {
        return Csp1dPricingObjectiveConfig(
            planUsagePenalty = solveConfig.lengthConfig?.batchMinPenalty,
            trimWidthPenalty = solveConfig.wasteConfig?.trimWidthPenalty,
            restMaterialPenalty = solveConfig.wasteConfig?.restMaterialPenalty,
            materialCostPenalty = solveConfig.wasteConfig?.materialCostPenalty ?: emptyMap()
        )
    }

/**
 * Builds lpMaster.
 * 构建LpMaster。
 *
 * @param problem 问题定义 / Problem definition
 * @param cuttingPlans 切割方案列表 / Cutting plan list
 * @param extensions 建模扩展列表 / Modeling extension list
 * @param domainValueSample 领域值样本 / Domain value sample
 * @return LP 主问题 / LP master
*/
    private fun buildLpMaster(
        problem: Csp1dProblem<V>,
        cuttingPlans: List<CuttingPlan<V>>,
        extensions: List<Csp1dModelingExtension<V>>,
        domainValueSample: V
    ): Ret<LpMaster<V>> {
        val model = LinearMetaModel(
            name = "csp1d_produce_lp",
            converter = IntoValue.Identity
        )
        val input = ProduceInput(
            cuttingPlans = cuttingPlans,
            demands = problem.demands,
            materials = problem.materials,
            machines = problem.machines
        )
        val context = Csp1dProduceContextBuilder(input)
            .mode(Csp1dModelingMode.LP)
            .apply {
                for (ext in extensions) {
                    extension(ext)
                }
            }
            .build()
        when (val result = context.register(model)) {
            is Ok -> {}
            is Failed -> return Failed(Csp1dLifecycleError("register LP context failed: ${result.error}"))
            is Fatal -> return Failed(Csp1dLifecycleError("register LP context fatal: ${result.errors}"))
        }
        return Ok(LpMaster(
            model = model,
            context = context,
            domainValueSample = domainValueSample
        ))
    }

/**
 * Solves the problem lpMaster.
 * 求解问题LpMaster。
 *
 * @param master LP 主问题 / LP master
 * @param iteration 迭代号 / Iteration number
 * @return LP 求解结果 / LP solve result
*/
    private suspend fun solveLpMaster(
        master: LpMaster<V>,
        iteration: Int64
    ): Csp1dMilpSolver.LpResult<V>? {
        val lpResult = when (val result = solver.solveLP(
            name = "csp1d-produce-lp-${iteration}",
            metaModel = master.model
        )) {
            is Ok -> result.value
            is Failed -> return null
            is Fatal -> return null
        }
        val lifecycle = try {
            Csp1dShadowPriceLifecycle<V>(master.domainValueSample, master.context.cgPipelines)
        } catch (_: Exception) {
            return null
        }
        val shadowPrices = when (val result = lifecycle.extractFromDualSolution(master.model, lpResult.dualSolution)) {
            is Ok -> result.value
            is Failed -> return null
            is Fatal -> return null
        }
        return Csp1dMilpSolver.LpResult(
            shadowPrices = shadowPrices,
            model = master.model,
            lpOutput = lpResult,
            frameworkShadowPriceMap = lifecycle.frameworkShadowPriceMap
        )
    }

    /**
     * 解析求解配置 / Resolve solve configuration
     *
     * @param problem 问题定义 / problem definition
     * @param solveConfig 显式求解配置 / explicit solve config
     * @return 解析后的求解配置 / resolved solve configuration
    */
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

/**
 * Solves the problem finalMilp.
 * 求解问题FinalMilp。
 *
 * @param problem 问题定义 / Problem definition
 * @param cuttingPlans 切割方案列表 / Cutting plan list
 * @param solveConfig 求解配置 / Solve configuration
 * @return 最终 MILP 求解结果 / Final MILP solve result
*/
    private suspend fun solveFinalMilp(
        problem: Csp1dProblem<V>,
        cuttingPlans: List<CuttingPlan<V>>,
        solveConfig: Csp1dSolveConfig<V>
    ): FinalMilpSolveResult<V> {
        val solveResult = Csp1dMilpSolver(solver).solve(
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
        val result = when (solveResult) {
            is Ok -> solveResult.value
            is Failed -> return FinalMilpSolveResult(
                status = Csp1dFinalMilpStatus.Failed,
                milpResult = null,
                failureMessage = solveResult.error.message
            )
            is Fatal -> return FinalMilpSolveResult(
                status = Csp1dFinalMilpStatus.Failed,
                milpResult = null,
                failureMessage = solveResult.errors.joinToString { it.message }
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

/**
 * emptyProduce.
 * emptyProduce。
 *
 * @param problem 问题定义 / Problem definition
 * @return 空生产结果 / Empty produce
*/
    private fun emptyProduce(problem: Csp1dProblem<V>): Produce<V> {
        return Produce(
            cuttingPlans = emptyList(),
            materialUsages = emptyList(),
            machineUsages = emptyList(),
            unmetDemands = problem.demands
        )
    }

/**
 * Builds flowContext.
 * 构建FlowContext。
 *
 * @param iteration 迭代号 / Iteration number
 * @param currentPlans 当前方案列表 / Current plan list
 * @param iterationLimit 迭代上限 / Iteration limit
 * @param allowPartialSolution 是否允许部分解 / Whether to allow partial solution
 * @param newPlans 新方案列表 / New plan list
 * @param hasValidLpResult 是否有有效 LP 结果 / Whether there is a valid LP result
 * @param pricingStatistics 定价生成统计 / Pricing generation statistics
 * @return 流上下文 / Flow context
*/
    private fun buildFlowContext(
        iteration: Int64,
        currentPlans: List<CuttingPlan<V>>,
        iterationLimit: Int64,
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

/**
 * FinalMilpSolveResult data class.
 * FinalMilpSolveResult数据类。
 *
 * @param status 最终 MILP 状态 / Final MILP status
 * @param milpResult MILP 求解结果 / MILP solve result
 * @param failureMessage 失败信息 / Failure message
*/
    private data class FinalMilpSolveResult<V : RealNumber<V>>(
        val status: Csp1dFinalMilpStatus,
        val milpResult: Csp1dMilpSolver.MilpResult<V>?,
        val failureMessage: String?
    )

    /**
     * 将 selectTerminationByPolicies 返回的 customReason 映射回 Csp1dTerminationReason。
     * 若 customReason 与某个枚举名匹配则使用该值，否则保留默认。
     *
     * Map customReason from selectTerminationByPolicies back to Csp1dTerminationReason.
     * If customReason matches an enum name, use that value; otherwise keep the default.
     *
     * @param customReason 自定义终止原因 / Custom termination reason
     * @param defaultReason 默认终止原因 / Default termination reason
     * @return 解析后的终止原因 / Resolved termination reason
    */
    private fun resolveTerminationReason(
        customReason: String,
        defaultReason: Csp1dTerminationReason
    ): Csp1dTerminationReason {
        return try {
            Csp1dTerminationReason.valueOf(customReason)
        } catch (_: IllegalArgumentException) {
            defaultReason
        }
    }
}

/**
 * CSP1D 列生成结果 / CSP1D column generation result
 *
 * @param V 数值类型 / Numeric value type
 * @property solution CSP1D 解 / CSP1D solution
 * @property trace 列生成追踪信息 / Column generation trace
*/
data class Csp1dColumnGenerationResult<V : RealNumber<V>>(
    val solution: Csp1dSolution<V>,
    val trace: Csp1dColumnGenerationTrace
)
