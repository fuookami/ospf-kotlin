package fuookami.ospf.kotlin.framework.csp1d.application.service

import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.framework.solver.ColumnGenerationSolver
import fuookami.ospf.kotlin.framework.csp1d.application.model.Csp1dProblem
import fuookami.ospf.kotlin.framework.csp1d.application.model.Csp1dSolution
import fuookami.ospf.kotlin.framework.csp1d.application.model.Csp1dSolutionAnalyzer
import fuookami.ospf.kotlin.framework.csp1d.application.model.DefaultCsp1dSolutionAnalyzer
import fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.Csp1dInitialCuttingPlanGenerator
import fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.Csp1dPricingGenerator
import fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.CuttingPlanGenerationInput
import fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.Csp1dPricingInput
import fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.SimpleInitialCuttingPlanGenerator
import fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.SimplePricingGenerator
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.CuttingPlan
import fuookami.ospf.kotlin.framework.csp1d.domain.produce.ProduceInput
import fuookami.ospf.kotlin.framework.csp1d.domain.produce.model.Produce

data class Csp1dColumnGenerationTrace(
    val initialPlanCount: UInt64,
    val finalPlanCount: UInt64,
    val pricedPlanCount: List<UInt64>
)

class Csp1dColumnGeneration<V : RealNumber<V>>(
    private val solver: ColumnGenerationSolver,
    private val initialGenerator: Csp1dInitialCuttingPlanGenerator<V> = SimpleInitialCuttingPlanGenerator(),
    private val pricingGenerator: Csp1dPricingGenerator<V> = SimplePricingGenerator(),
    private val analyzer: Csp1dSolutionAnalyzer<V> = DefaultCsp1dSolutionAnalyzer()
) {
    suspend fun solve(
        problem: Csp1dProblem<V>
    ): Csp1dSolution<V> {
        return solveWithTrace(problem).solution
    }

    suspend fun solveWithTrace(
        problem: Csp1dProblem<V>
    ): Csp1dColumnGenerationResult<V> {
        val initialPlans = initialPlans(problem)
        val initialCount = initialPlans.size
        val config = problem.configuration

        var currentPlans = initialPlans
        val pricedPlanCounts = ArrayList<UInt64>()

        for (iteration in 0 until config.iterationLimit) {
            val lpResult = Csp1dMilpSolver(solver).solveLP(
                ProduceInput(
                    cuttingPlans = currentPlans,
                    demands = problem.demands,
                    materials = problem.materials,
                    machines = problem.machines
                )
            )
            if (lpResult == null) break

            val shadowPrices = lpResult.shadowPrices

            val pricingInput = Csp1dPricingInput(
                generationInput = CuttingPlanGenerationInput(
                    products = problem.products,
                    materials = problem.materials,
                    machines = problem.machines,
                    costars = problem.costars,
                    demands = problem.demands,
                    existingPlans = currentPlans
                ),
                shadowPrices = shadowPrices,
                maxGeneratedPlans = UInt64(config.maxPricingPlans)
            )
            val newPlans = pricingGenerator.generate(pricingInput)

            if (newPlans.isEmpty()) break

            val addedPlans = deduplicatePlans(currentPlans, newPlans)
            if (addedPlans.isEmpty()) break

            currentPlans = currentPlans + addedPlans
            pricedPlanCounts.add(UInt64(addedPlans.size))
        }

        val milpResult = Csp1dMilpSolver(solver).solve(
            ProduceInput(
                cuttingPlans = currentPlans,
                demands = problem.demands,
                materials = problem.materials,
                machines = problem.machines
            )
        )
        val produce = milpResult?.produce ?: Produce(
            cuttingPlans = emptyList(),
            materialUsages = emptyList(),
            machineUsages = emptyList(),
            unmetDemands = problem.demands
        )
        return Csp1dColumnGenerationResult(
            solution = analyzer.analyze(
                problem = problem,
                produce = produce,
                generatedPlans = currentPlans
            ),
            trace = Csp1dColumnGenerationTrace(
                initialPlanCount = UInt64(initialCount),
                finalPlanCount = UInt64(currentPlans.size),
                pricedPlanCount = pricedPlanCounts
            )
        )
    }

    private fun initialPlans(problem: Csp1dProblem<V>): List<CuttingPlan<V>> {
        return initialGenerator.generate(
            CuttingPlanGenerationInput(
                products = problem.products,
                materials = problem.materials,
                machines = problem.machines,
                costars = problem.costars,
                demands = problem.demands
            )
        )
    }

    private fun deduplicatePlans(
        existing: List<CuttingPlan<V>>,
        candidates: List<CuttingPlan<V>>
    ): List<CuttingPlan<V>> {
        val existingIds = existing.map { it.id }.toSet()
        return candidates.filter { it.id !in existingIds }
    }
}

data class Csp1dColumnGenerationResult<V : RealNumber<V>>(
    val solution: Csp1dSolution<V>,
    val trace: Csp1dColumnGenerationTrace
)
