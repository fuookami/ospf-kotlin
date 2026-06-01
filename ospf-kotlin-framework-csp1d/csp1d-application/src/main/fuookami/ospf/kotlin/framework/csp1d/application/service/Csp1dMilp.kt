package fuookami.ospf.kotlin.framework.csp1d.application.service

import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.framework.csp1d.application.model.Csp1dProblem
import fuookami.ospf.kotlin.framework.csp1d.application.model.Csp1dSolution
import fuookami.ospf.kotlin.framework.csp1d.application.model.Csp1dSolutionAnalyzer
import fuookami.ospf.kotlin.framework.csp1d.application.model.DefaultCsp1dSolutionAnalyzer
import fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.Csp1dInitialCuttingPlanGenerator
import fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.CuttingPlanGenerationInput
import fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.SimpleInitialCuttingPlanGenerator
import fuookami.ospf.kotlin.framework.csp1d.domain.produce.ProduceInput
import fuookami.ospf.kotlin.framework.csp1d.domain.produce.ProduceSolver
import fuookami.ospf.kotlin.framework.csp1d.domain.produce.SimpleProduceSolver

/**
 * CSP1D 静态 MILP 入口（最小实现）/ CSP1D static MILP entry point (minimal implementation)
 *
 * @param V 数值类型 / Numeric value type
 */
class Csp1dMilp<V : RealNumber<V>>(
    private val initialGenerator: Csp1dInitialCuttingPlanGenerator<V> = SimpleInitialCuttingPlanGenerator(),
    private val produceSolver: ProduceSolver<V> = SimpleProduceSolver(),
    private val analyzer: Csp1dSolutionAnalyzer<V> = DefaultCsp1dSolutionAnalyzer()
) {
    /**
     * 求解 CSP1D / Solve CSP1D
     *
     * @param problem 问题定义 / Problem definition
     * @return 求解结果 / Solution
     */
    fun solve(problem: Csp1dProblem<V>): Csp1dSolution<V> {
        val generationInput = CuttingPlanGenerationInput(
            products = problem.products,
            materials = problem.materials,
            machines = problem.machines,
            costars = problem.costars,
            demands = problem.demands
        )
        val plans = initialGenerator
            .generate(generationInput)
            .take(problem.configuration.maxInitialPlans)
        val produce = produceSolver.solve(
            ProduceInput(
                candidatePlans = plans,
                demands = problem.demands,
                machines = problem.machines
            )
        )
        return analyzer.analyze(
            problem = problem,
            produce = produce,
            generatedPlans = plans
        )
    }
}

