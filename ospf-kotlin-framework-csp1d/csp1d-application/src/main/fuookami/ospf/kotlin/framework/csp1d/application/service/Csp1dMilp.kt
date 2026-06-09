package fuookami.ospf.kotlin.framework.csp1d.application.service

import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.framework.solver.ColumnGenerationSolver
import fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.Csp1dInitialCuttingPlanGenerator
import fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.CuttingPlanGenerationInput
import fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.SimpleInitialCuttingPlanGenerator
import fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.model.canonicalKey
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.CuttingPlan
import fuookami.ospf.kotlin.framework.csp1d.domain.length_assignment.model.LengthAssignmentModelingConfig
import fuookami.ospf.kotlin.framework.csp1d.domain.yield.model.YieldModelingConfig
import fuookami.ospf.kotlin.framework.csp1d.domain.produce.ProduceInput
import fuookami.ospf.kotlin.framework.csp1d.domain.produce.model.CuttingPlanUsage
import fuookami.ospf.kotlin.framework.csp1d.domain.produce.model.Produce
import fuookami.ospf.kotlin.framework.csp1d.application.model.Csp1dConfiguration
import fuookami.ospf.kotlin.framework.csp1d.application.model.Csp1dProblem
import fuookami.ospf.kotlin.framework.csp1d.application.model.Csp1dSolveConfig
import fuookami.ospf.kotlin.framework.csp1d.application.model.Csp1dSolution
import fuookami.ospf.kotlin.framework.csp1d.application.model.Csp1dSolutionAnalyzer
import fuookami.ospf.kotlin.framework.csp1d.application.model.Csp1dSolutionStatus
import fuookami.ospf.kotlin.framework.csp1d.application.model.DefaultCsp1dSolutionAnalyzer

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
        val generatedPlans = initialPlans(
            problem = problem,
            configuration = resolvedConfig.columnGeneration
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
                partialSolutionAvailable = false
            )
        }

        val milpResult = solveMilp(
            problem = problem,
            cuttingPlans = generatedPlans,
            solveConfig = resolvedConfig
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
            partialSolutionAvailable = milpResult.status == Csp1dFinalMilpStatus.Failed
        )
    }

    private fun initialPlans(
        problem: Csp1dProblem<V>,
        configuration: Csp1dConfiguration<V>
    ): List<CuttingPlan<V>> {
        if (configuration.maxInitialPlans <= 0) {
            return emptyList()
        }
        val report = initialGenerator.generateWithReport(
            CuttingPlanGenerationInput(
                products = problem.products,
                materials = problem.materials,
                machines = problem.machines,
                costars = problem.costars,
                demands = problem.demands
            )
        )
        return report.plans.distinctBy { it.canonicalKey() }
            .take(configuration.maxInitialPlans)
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

    private suspend fun solveMilp(
        problem: Csp1dProblem<V>,
        cuttingPlans: List<CuttingPlan<V>>,
        solveConfig: Csp1dSolveConfig<V>
    ): MilpSolveResult<V> {
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
                lengthConfig = solveConfig.lengthConfig
            )
        } catch (error: Exception) {
            return MilpSolveResult(
                status = Csp1dFinalMilpStatus.Failed,
                result = null,
                failureMessage = error.message ?: "MILP solve failed"
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

    private fun emptyProduce(problem: Csp1dProblem<V>): Produce<V> {
        return Produce(
            cuttingPlans = emptyList(),
            materialUsages = emptyList(),
            machineUsages = emptyList(),
            unmetDemands = problem.demands
        )
    }

    private data class MilpSolveResult<V : RealNumber<V>>(
        val status: Csp1dFinalMilpStatus,
        val result: Csp1dMilpSolver.MilpResult<V>?,
        val failureMessage: String?
    )
}
