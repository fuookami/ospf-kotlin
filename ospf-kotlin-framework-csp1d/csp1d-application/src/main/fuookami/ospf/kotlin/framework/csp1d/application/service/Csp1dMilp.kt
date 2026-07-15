package fuookami.ospf.kotlin.framework.csp1d.application.service

import fuookami.ospf.kotlin.framework.csp1d.application.model.*
import fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.*
import fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.model.*
import fuookami.ospf.kotlin.framework.csp1d.domain.length_assignment.model.LengthAssignmentModelingConfig
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.*
import fuookami.ospf.kotlin.framework.csp1d.domain.produce.model.*
import fuookami.ospf.kotlin.framework.csp1d.domain.produce.ProduceInput
import fuookami.ospf.kotlin.framework.csp1d.domain.yield.model.YieldModelingConfig
import fuookami.ospf.kotlin.framework.solver.ColumnGenerationSolver
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.Int64
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.utils.functional.*

/**
 * CSP1D 普通 MILP 求解入口 / CSP1D plain MILP solve entry
 *
 * @param V 数值类型 / Numeric value type
*/
class Csp1dMilp<V : RealNumber<V>>(
    private val solver: ColumnGenerationSolver,
    private val initialGenerator: Csp1dInitialCuttingPlanGenerator<V> = SimpleInitialCuttingPlanGenerator(),
    private val analyzer: Csp1dSolutionAnalyzer<V> = DefaultCsp1dSolutionAnalyzer(),
    private val yieldConfig: YieldModelingConfig<V>? = null,
    private val wasteConfig: WasteMinimizationConfig<V>? = null,
    private val lengthConfig: LengthAssignmentModelingConfig<V>? = null,
    private val warmStartPlanUsages: List<CuttingPlanUsage<V>> = emptyList()
) {

    /**
     * 生成初始方案并求解最终 MILP / Generate initial plans and solve final MILP
     *
     * @param problem 问题定义 / Problem definition
     * @param solveConfig 显式求解配置，优先级高于 problem.solveConfig / Explicit solve config, higher priority than problem.solveConfig
     * @return CSP1D 解 / CSP1D solution
    */
    suspend fun solve(
        problem: Csp1dProblem<V>,
        solveConfig: Csp1dSolveConfig<V>? = null
    ): Csp1dSolution<V> {
        val resolvedConfig = resolveSolveConfig(
            problem = problem,
            solveConfig = solveConfig
        )
        val domainPolicies = resolvedConfig.extensionSet.domainPolicies
        val domainValueSample = problem.demands.firstOrNull()?.quantity?.value
            ?: problem.materials.firstOrNull()?.widthRange?.upperBound?.value
        val widthCheck = if (domainValueSample != null) widthFeasibilityCheckFromPolicies(domainPolicies, domainValueSample) else null
        val generatedPlans = initialPlans(
            problem = problem,
            configuration = resolvedConfig.columnGeneration,
            domainPolicies = domainPolicies,
            candidateFilters = resolvedConfig.extensionSet.generationStrategies.map { strategy ->
                { candidate: CuttingPlan<V>, existing: List<CuttingPlan<V>> -> strategy.acceptCandidate(candidate, existing) }
            },
            canonicalKeyOverrides = resolvedConfig.extensionSet.generationStrategies.map { strategy ->
                { candidate: CuttingPlan<V> -> strategy.canonicalKeyFor(candidate) }
            },
            dominanceAcceptOverrides = resolvedConfig.extensionSet.generationStrategies.map { strategy ->
                { candidate: CuttingPlan<V>, existing: List<CuttingPlan<V>> -> strategy.acceptDominance(candidate, existing) }
            },
            flowPolicies = resolvedConfig.extensionSet.flowPolicies,
            widthFeasibilityCheck = widthCheck
        )
        if (generatedPlans.isEmpty()) {
            val failureMessage = "No initial cutting plans generated"
            val baseSolution = analyzer.analyze(
                problem = problem,
                produce = emptyProduce(problem),
                generatedPlans = emptyList()
            )
            return enrichSolution(
                solution = baseSolution,
                topPlans = emptyList(),
                status = Csp1dSolutionStatus.NoInitialPlans,
                failureMessage = failureMessage,
                finalMilpStatus = Csp1dFinalMilpStatus.NotAttempted,
                partialSolutionAvailable = false,
                extractionPolicies = resolvedConfig.extensionSet.extractionPolicies,
                demands = problem.demands,
                materials = problem.materials,
                machines = problem.machines
            )
        }

        val milpResult = solveMilp(
            problem = problem,
            cuttingPlans = generatedPlans,
            solveConfig = resolvedConfig,
            isFinalMilp = false
        )
        val produce = milpResult.result?.produce ?: emptyProduce(problem)
        val topPlans = topCuttingPlans(
            plans = generatedPlans,
            limit = resolvedConfig.topKPlanLimit
        )
        val solutionStatus = when (milpResult.status) {
            Csp1dFinalMilpStatus.Solved -> Csp1dSolutionStatus.Feasible
            Csp1dFinalMilpStatus.Failed -> if (resolvedConfig.allowPartialSolution) Csp1dSolutionStatus.Partial else Csp1dSolutionStatus.Failed
            Csp1dFinalMilpStatus.NotAttempted -> Csp1dSolutionStatus.Partial
        }
        val baseSolution = analyzer.analyze(
            problem = problem,
            produce = produce,
            generatedPlans = generatedPlans
        ).copy(
            yieldResult = milpResult.result?.yieldResult,
            wasteResult = milpResult.result?.wasteResult,
            lengthResult = milpResult.result?.lengthResult
        )
        return enrichSolution(
            solution = baseSolution,
            topPlans = topPlans,
            status = solutionStatus,
            failureMessage = milpResult.failureMessage,
            finalMilpStatus = milpResult.status,
            partialSolutionAvailable = milpResult.status == Csp1dFinalMilpStatus.Failed,
            extractionPolicies = resolvedConfig.extensionSet.extractionPolicies,
            demands = problem.demands,
            materials = problem.materials,
            machines = problem.machines
        )
    }

/**
 * initialPlans.
 * initialPlans。
 * @param problem CSP1D problem definition containing products, materials, machines, and demands / 包含产品、原料、机器和需求的CSP1D问题定义
 * @param configuration Column generation configuration controlling plan generation limits / 控制方案生成数量限制的列生成配置
 * @param domainPolicies Domain-specific policies constraining cutting plan generation / 约束切割方案生成的领域策略
 * @return Generated initial cutting plans after deduplication and flow policy filtering / 经过去重和流量策略过滤后生成的初始切割方案
*/
    private fun initialPlans(
        problem: Csp1dProblem<V>,
        configuration: Csp1dConfiguration<V>,
        domainPolicies: List<Csp1dDomainPolicy<V>> = emptyList(),
        candidateFilters: List<(CuttingPlan<V>, List<CuttingPlan<V>>) -> Boolean> = emptyList(),
        canonicalKeyOverrides: List<(CuttingPlan<V>) -> String?> = emptyList(),
        dominanceAcceptOverrides: List<(CuttingPlan<V>, List<CuttingPlan<V>>) -> Boolean> = emptyList(),
        flowPolicies: List<fuookami.ospf.kotlin.framework.csp1d.domain.produce.model.Csp1dFlowPolicy<V>> = emptyList(),
        widthFeasibilityCheck: ((Material<V>, Product<V>, Quantity<V>) -> Boolean)? = null
    ): List<CuttingPlan<V>> {
        if (configuration.maxInitialPlans.toLong() <= 0L) {
            return emptyList()
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
        val resolveCanonicalKey: (CuttingPlan<V>) -> fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.model.CuttingPlanCanonicalKey = { plan ->
            val customKey = canonicalKeyOverrides.firstNotNullOfOrNull { it(plan) }
            if (customKey != null) fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.model.CuttingPlanCanonicalKey(customKey) else plan.canonicalKey()
        }
        val generatedPlans = report.plans.distinctBy { resolveCanonicalKey(it) }
            .take(configuration.maxInitialPlans.toInt())
        // Apply flow policy initial plan filter with context
        return if (flowPolicies.isNotEmpty()) {
            val flowContext = object : fuookami.ospf.kotlin.framework.csp1d.domain.produce.model.Csp1dFlowContext<V> {
                override val iteration = Int64.zero
                override val currentPlans: List<CuttingPlan<V>> = generatedPlans
                override val iterationLimit = configuration.iterationLimit
                override val allowPartialSolution = true
            }
            fuookami.ospf.kotlin.framework.csp1d.domain.produce.model.filterInitialPlansByPolicies(
                flowPolicies, flowContext, generatedPlans
            )
        } else {
            generatedPlans
        }
    }

/**
 * resolveSolveConfig.
 * resolveSolveConfig。
 * @param problem CSP1D problem definition providing the default solve config / 提供默认求解配置的CSP1D问题定义
 * @param solveConfig Explicit solve config that overrides the problem's default / 显式求解配置，覆盖问题的默认配置
 * @return Resolved solve config with fallbacks from constructor-level configs applied / 应用了构造函数级别配置回退的最终求解配置
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
 * Solves the problem milp.
 * 求解问题Milp。
 * @param problem CSP1D problem definition containing demands, materials, and machines / 包含需求、原料和机器的CSP1D问题定义
 * @param cuttingPlans Cutting plans to include as decision variables in the MILP model / 作为MILP模型决策变量的切割方案
 * @param solveConfig Solve configuration including yield, waste, and length modeling configs / 包含成品率、废料和长度建模配置的求解配置
 * @param isFinalMilp Whether this is the final MILP solve (as opposed to a relaxation) / 是否为最终MILP求解（相对于松弛求解）
 * @return MILP solve result containing status, produce result, and optional failure message / 包含求解状态、生产结果和可选失败信息的MILP求解结果
*/
    private suspend fun solveMilp(
        problem: Csp1dProblem<V>,
        cuttingPlans: List<CuttingPlan<V>>,
        solveConfig: Csp1dSolveConfig<V>,
        isFinalMilp: Boolean = false
    ): MilpSolveResult<V> {
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
            isFinalMilp = isFinalMilp
        )
        val result = when (solveResult) {
            is Ok -> solveResult.value
            is Failed -> return MilpSolveResult(
                status = Csp1dFinalMilpStatus.Failed,
                result = null,
                failureMessage = solveResult.error.message
            )
            is Fatal -> return MilpSolveResult(
                status = Csp1dFinalMilpStatus.Failed,
                result = null,
                failureMessage = solveResult.errors.joinToString { it.message }
            )
        }

        if (result != null) {
            return MilpSolveResult(
                status = Csp1dFinalMilpStatus.Solved,
                result = result,
                failureMessage = null
            )
        }

        val failureMessage = "MILP returned no solution"
        return MilpSolveResult(
            status = Csp1dFinalMilpStatus.Failed,
            result = null,
            failureMessage = failureMessage
        )
    }

/**
 * emptyProduce.
 * emptyProduce。
 * @param problem CSP1D problem definition whose demands become unmet / 其需求变为未满足的CSP1D问题定义
 * @return An empty produce with all demands marked as unmet / 所有需求标记为未满足的空生产结果
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
 * MilpSolveResult data class.
 * MilpSolveResult数据类。
*/
    private data class MilpSolveResult<V : RealNumber<V>>(
        val status: Csp1dFinalMilpStatus,
        val result: Csp1dMilpSolver.MilpResult<V>?,
        val failureMessage: String?
    )
}
