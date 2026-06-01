package fuookami.ospf.kotlin.framework.csp1d.application.service

import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.framework.csp1d.application.model.Csp1dProblem
import fuookami.ospf.kotlin.framework.csp1d.application.model.Csp1dSolution
import fuookami.ospf.kotlin.framework.csp1d.application.model.Csp1dSolutionAnalyzer
import fuookami.ospf.kotlin.framework.csp1d.application.model.DefaultCsp1dSolutionAnalyzer
import fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.Csp1dInitialCuttingPlanGenerator
import fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.Csp1dPricingGenerator
import fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.Csp1dPricingInput
import fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.CuttingPlanGenerationInput
import fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.SimpleInitialCuttingPlanGenerator
import fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.SimplePricingGenerator
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.CuttingPlan
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.ProductDemandShadowPriceKey
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.ShadowPriceKey
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.ShadowPriceMap
import fuookami.ospf.kotlin.framework.csp1d.domain.produce.ProduceInput
import fuookami.ospf.kotlin.framework.csp1d.domain.produce.ProduceSolver
import fuookami.ospf.kotlin.framework.csp1d.domain.produce.SimpleProduceSolver

/**
 * 影子价格估计器 / Shadow price estimator
 *
 * @param V 数值类型 / Numeric value type
 */
fun interface Csp1dShadowPriceEstimator<V : RealNumber<V>> {
    /**
     * 估计影子价格 / Estimate shadow prices
     *
     * @param problem 问题定义 / Problem definition
     * @param selectedPlans 当前已选方案 / Current selected plans
     * @return 影子价格表 / Shadow price map
     */
    fun estimate(
        problem: Csp1dProblem<V>,
        selectedPlans: List<CuttingPlan<V>>
    ): ShadowPriceMap<V>
}

/**
 * 默认影子价格估计器：未被满足的需求给正价，其余给零价 / Default estimator: unmet demand gets positive price, otherwise zero
 *
 * @param V 数值类型 / Numeric value type
 */
class DefaultCsp1dShadowPriceEstimator<V : RealNumber<V>> : Csp1dShadowPriceEstimator<V> {
    override fun estimate(
        problem: Csp1dProblem<V>,
        selectedPlans: List<CuttingPlan<V>>
    ): ShadowPriceMap<V> {
        val producedProductIds = selectedPlans.flatMap { plan ->
            plan.demandContributions.map { contribution -> contribution.product.id }
        }.toSet()
        val prices = LinkedHashMap<ShadowPriceKey, V>()
        for (demand in problem.demands) {
            val value = if (producedProductIds.contains(demand.product.id)) {
                demand.quantity.value.constants.zero
            } else {
                demand.quantity.value.constants.one
            }
            prices[ProductDemandShadowPriceKey(demand.product.id)] = value
        }
        return ShadowPriceMap(prices)
    }
}

/**
 * CSP1D 列生成入口（最小实现）/ CSP1D column generation entry point (minimal implementation)
 *
 * @param V 数值类型 / Numeric value type
 */
class Csp1dColumnGeneration<V : RealNumber<V>>(
    private val initialGenerator: Csp1dInitialCuttingPlanGenerator<V> = SimpleInitialCuttingPlanGenerator(),
    private val pricingGenerator: Csp1dPricingGenerator<V> = SimplePricingGenerator(),
    private val produceSolver: ProduceSolver<V> = SimpleProduceSolver(),
    private val shadowPriceEstimator: Csp1dShadowPriceEstimator<V> = DefaultCsp1dShadowPriceEstimator(),
    private val analyzer: Csp1dSolutionAnalyzer<V> = DefaultCsp1dSolutionAnalyzer()
) {
    /**
     * 列生成过程调试信息 / Column generation debug info
     *
     * @param V 数值类型 / Numeric value type
     * @property iterations 迭代轮次 / Iteration index
     * @property pricedPlanCount 每轮新增列数量 / Added plan count per iteration
     */
    data class DebugTrace<V : RealNumber<V>>(
        val iterations: List<Int>,
        val pricedPlanCount: List<UInt64>
    )

    /**
     * 求解 CSP1D 列生成 / Solve CSP1D by column generation
     *
     * @param problem 问题定义 / Problem definition
     * @return 求解结果 / Solution
     */
    fun solve(problem: Csp1dProblem<V>): Csp1dSolution<V> {
        return solveWithTrace(problem).first
    }

    /**
     * 求解并返回调试信息 / Solve with debug trace
     *
     * @param problem 问题定义 / Problem definition
     * @return 解与调试信息 / Solution with trace
     */
    fun solveWithTrace(problem: Csp1dProblem<V>): Pair<Csp1dSolution<V>, DebugTrace<V>> {
        val input = CuttingPlanGenerationInput(
            products = problem.products,
            materials = problem.materials,
            machines = problem.machines,
            costars = problem.costars,
            demands = problem.demands
        )
        val planPool = initialGenerator
            .generate(input)
            .take(problem.configuration.maxInitialPlans)
            .toMutableList()
        val iterationMarks = ArrayList<Int>()
        val pricedPlanCount = ArrayList<UInt64>()

        var iteration = 0
        while (iteration < problem.configuration.iterationLimit) {
            val shadowPrices = shadowPriceEstimator.estimate(
                problem = problem,
                selectedPlans = planPool
            )
            val pricedPlans = pricingGenerator.generate(
                Csp1dPricingInput(
                    generationInput = input.copy(existingPlans = planPool.toList()),
                    shadowPrices = shadowPrices,
                    maxGeneratedPlans = UInt64(problem.configuration.maxPricingPlans)
                )
            ).filter { pricedPlan ->
                planPool.none { oldPlan -> oldPlan.id == pricedPlan.id }
            }
            iterationMarks.add(iteration)
            pricedPlanCount.add(UInt64(pricedPlans.size))
            if (pricedPlans.isEmpty()) {
                break
            }
            planPool.addAll(pricedPlans)
            iteration += 1
        }

        val produce = produceSolver.solve(
            ProduceInput(
                candidatePlans = planPool,
                demands = problem.demands,
                machines = problem.machines
            )
        )
        val solution = analyzer.analyze(
            problem = problem,
            produce = produce,
            generatedPlans = planPool
        )
        return Pair(
            solution,
            DebugTrace(
                iterations = iterationMarks,
                pricedPlanCount = pricedPlanCount
            )
        )
    }
}
