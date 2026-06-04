package fuookami.ospf.kotlin.framework.csp1d.application.service

import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.framework.solver.ColumnGenerationSolver
import fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.Csp1dInitialCuttingPlanGenerator
import fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.Csp1dPricingInput
import fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.Csp1dPricingGenerator
import fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.CuttingPlanGenerationInput
import fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.ReducedCostPricingGenerator
import fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.SimpleInitialCuttingPlanGenerator
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.CuttingPlan
import fuookami.ospf.kotlin.framework.csp1d.domain.produce.ProduceInput
import fuookami.ospf.kotlin.framework.csp1d.domain.produce.model.Produce
import fuookami.ospf.kotlin.framework.csp1d.domain.length_assignment.model.LengthAssignmentModelingConfig
import fuookami.ospf.kotlin.framework.csp1d.domain.yield.model.YieldModelingConfig
import fuookami.ospf.kotlin.framework.csp1d.application.model.Csp1dProblem
import fuookami.ospf.kotlin.framework.csp1d.application.model.Csp1dSolution
import fuookami.ospf.kotlin.framework.csp1d.application.model.Csp1dSolutionAnalyzer
import fuookami.ospf.kotlin.framework.csp1d.application.model.DefaultCsp1dSolutionAnalyzer

/**
 * 列生成终止原因 / Column generation termination reason
 */
enum class Csp1dTerminationReason {
    /** 达到迭代上限 / Iteration limit reached */
    IterationLimitReached,
    /** LP 求解失败 / LP solve failed */
    LpSolveFailed,
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

data class Csp1dColumnGenerationTrace(
    val initialPlanCount: UInt64,
    val finalPlanCount: UInt64,
    val pricedPlanCount: List<UInt64>,
    val terminationReason: Csp1dTerminationReason = Csp1dTerminationReason.PricingConverged,
    val iterations: List<Csp1dIterationRecord> = emptyList()
)

class Csp1dColumnGeneration<V : RealNumber<V>>(
    private val solver: ColumnGenerationSolver,
    private val initialGenerator: Csp1dInitialCuttingPlanGenerator<V> = SimpleInitialCuttingPlanGenerator(),
    private val pricingGenerator: Csp1dPricingGenerator<V> = ReducedCostPricingGenerator(initialGenerator),
    private val analyzer: Csp1dSolutionAnalyzer<V> = DefaultCsp1dSolutionAnalyzer(),
    private val yieldConfig: YieldModelingConfig<V>? = null,
    private val wasteConfig: WasteMinimizationConfig<V>? = null,
    private val lengthConfig: LengthAssignmentModelingConfig<V>? = null
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

        if (initialPlans.isEmpty()) {
            val emptyProduce = Produce<V>(
                cuttingPlans = emptyList(),
                materialUsages = emptyList(),
                machineUsages = emptyList(),
                unmetDemands = problem.demands
            )
            return Csp1dColumnGenerationResult(
                solution = analyzer.analyze(
                    problem = problem,
                    produce = emptyProduce,
                    generatedPlans = emptyList()
                ),
                trace = Csp1dColumnGenerationTrace(
                    initialPlanCount = UInt64.zero,
                    finalPlanCount = UInt64.zero,
                    pricedPlanCount = emptyList(),
                    terminationReason = Csp1dTerminationReason.NoInitialPlans,
                    iterations = emptyList()
                )
            )
        }

        var currentPlans = initialPlans
        val pricedPlanCounts = ArrayList<UInt64>()
        val iterationRecords = ArrayList<Csp1dIterationRecord>()
        var terminationReason: Csp1dTerminationReason = Csp1dTerminationReason.PricingConverged

        for (iteration in 0 until config.iterationLimit) {
            val planCountBefore = currentPlans.size
            val lpResult = Csp1dMilpSolver(solver).solveLP(
                ProduceInput(
                    cuttingPlans = currentPlans,
                    demands = problem.demands,
                    materials = problem.materials,
                    machines = problem.machines
                )
            )
            if (lpResult == null) {
                terminationReason = Csp1dTerminationReason.LpSolveFailed
                break
            }

            val lpObjective = lpResult.lpOutput.result.obj
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

            if (newPlans.isEmpty()) {
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

            val addedPlans = deduplicatePlans(currentPlans, newPlans)
            if (addedPlans.isEmpty()) {
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

            if (iteration == config.iterationLimit - 1) {
                terminationReason = Csp1dTerminationReason.IterationLimitReached
            }
        }

        val milpResult = Csp1dMilpSolver(solver).solve(
            ProduceInput(
                cuttingPlans = currentPlans,
                demands = problem.demands,
                materials = problem.materials,
                machines = problem.machines
            ),
            yieldConfig = yieldConfig,
            wasteConfig = wasteConfig,
            lengthConfig = lengthConfig
        )
        val produce = milpResult?.produce ?: Produce(
            cuttingPlans = emptyList(),
            materialUsages = emptyList(),
            machineUsages = emptyList(),
            unmetDemands = problem.demands
        )
        val baseSolution = analyzer.analyze(
            problem = problem,
            produce = produce,
            generatedPlans = currentPlans
        )
        val solution = baseSolution.copy(
            yieldResult = milpResult?.yieldResult,
            wasteResult = milpResult?.wasteResult,
            lengthResult = milpResult?.lengthResult
        )
        return Csp1dColumnGenerationResult(
            solution = solution,
            trace = Csp1dColumnGenerationTrace(
                initialPlanCount = UInt64(initialCount),
                finalPlanCount = UInt64(currentPlans.size),
                pricedPlanCount = pricedPlanCounts,
                terminationReason = terminationReason,
                iterations = iterationRecords
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
