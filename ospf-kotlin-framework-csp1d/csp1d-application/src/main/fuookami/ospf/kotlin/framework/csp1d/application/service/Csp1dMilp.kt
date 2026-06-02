package fuookami.ospf.kotlin.framework.csp1d.application.service

import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.framework.solver.ColumnGenerationSolver
import fuookami.ospf.kotlin.framework.csp1d.application.model.Csp1dProblem
import fuookami.ospf.kotlin.framework.csp1d.application.model.Csp1dSolution
import fuookami.ospf.kotlin.framework.csp1d.application.model.Csp1dSolutionAnalyzer
import fuookami.ospf.kotlin.framework.csp1d.application.model.DefaultCsp1dSolutionAnalyzer
import fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.Csp1dInitialCuttingPlanGenerator
import fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.CuttingPlanGenerationInput
import fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.SimpleInitialCuttingPlanGenerator
import fuookami.ospf.kotlin.framework.csp1d.domain.produce.ProduceInput
import fuookami.ospf.kotlin.framework.csp1d.domain.produce.model.Produce

class Csp1dMilp<V : RealNumber<V>>(
    private val solver: ColumnGenerationSolver,
    private val initialGenerator: Csp1dInitialCuttingPlanGenerator<V> = SimpleInitialCuttingPlanGenerator(),
    private val analyzer: Csp1dSolutionAnalyzer<V> = DefaultCsp1dSolutionAnalyzer()
) {
    suspend fun solve(
        problem: Csp1dProblem<V>
    ): Csp1dSolution<V> {
        val generatedPlans = initialGenerator.generate(
            CuttingPlanGenerationInput(
                products = problem.products,
                materials = problem.materials,
                machines = problem.machines,
                costars = problem.costars,
                demands = problem.demands
            )
        )
        val result = Csp1dMilpSolver(solver).solve(
            ProduceInput(
                cuttingPlans = generatedPlans,
                demands = problem.demands,
                materials = problem.materials,
                machines = problem.machines
            )
        )
        val produce = result?.produce ?: Produce(
            cuttingPlans = emptyList(),
            materialUsages = emptyList(),
            machineUsages = emptyList(),
            unmetDemands = problem.demands
        )
        return analyzer.analyze(
            problem = problem,
            produce = produce,
            generatedPlans = generatedPlans
        )
    }
}
