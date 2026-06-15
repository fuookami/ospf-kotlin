package fuookami.ospf.kotlin.framework.csp1d.domain.yield

import fuookami.ospf.kotlin.utils.functional.Order
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.quantities.quantity.*
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.*
import fuookami.ospf.kotlin.framework.csp1d.domain.produce.model.*
import fuookami.ospf.kotlin.framework.csp1d.domain.yield.model.*

/**
 * 产出上下文，按产品+单位聚合贡献并与需求对比 / Yield context: aggregates contributions by product+unit and compares with demands
 *
 * @param V 数值类型 / Numeric value type
 */
class YieldContext<V : RealNumber<V>>(
    private val arithmetic: QuantityArithmetic<V>
) {
    /**
     * 分析产出偏差：按产品+单位聚合贡献，只在同单位下比较 / Analyze yield deviation: aggregate by product+unit, compare only under same unit
     *
     * @param produce 主问题产出 / Master problem output
     * @param demands 需求列表 / Demand list
     * @return 产出偏差分析 / Yield deviation analysis
     */
    fun analyze(
        produce: Produce<V>,
        demands: List<ProductDemand<V>>
    ): YieldAnalysis<V> {
        val contributionByKey = aggregateContributions(produce)
        val underProductions = ArrayList<UnderProduction<V>>()
        val overProductions = ArrayList<OverProduction<V>>()
        val outputs = ArrayList<ProductOutput<V>>()

        for (demand in demands) {
            val key = DemandAggregationKey<V>(demand.product.id, demand.quantity.unit)
            val contributions = contributionByKey[key] ?: emptyList()
            val totalOutput = sumContributions(contributions)
            if (totalOutput == null) {
                underProductions.add(
                    UnderProduction(
                        demand = demand,
                        shortfall = demand.quantity
                    )
                )
                continue
            }

            outputs.add(
                ProductOutput(
                    product = demand.product,
                    totalQuantity = totalOutput,
                    mode = demand.mode
                )
            )

            val comparison = totalOutput.value partialOrd demand.quantity.value
            when (comparison) {
                is Order.Less -> {
                    underProductions.add(
                        UnderProduction(
                            demand = demand,
                            shortfall = arithmetic.subtract(demand.quantity, totalOutput)
                        )
                    )
                }

                is Order.Greater -> {
                    overProductions.add(
                        OverProduction(
                            demand = demand,
                            surplus = arithmetic.subtract(totalOutput, demand.quantity)
                        )
                    )
                }

                else -> {
                    // Equal: no deviation
                }
            }
        }

        return YieldAnalysis(
            underProductions = underProductions,
            overProductions = overProductions,
            outputs = outputs
        )
    }

    private fun aggregateContributions(
        produce: Produce<V>
    ): Map<DemandAggregationKey<V>, List<CuttingPlanDemandContribution<V>>> {
        val map = LinkedHashMap<DemandAggregationKey<V>, MutableList<CuttingPlanDemandContribution<V>>>()
        for (usage in produce.cuttingPlans) {
            for (contribution in usage.plan.demandContributions) {
                val multiplied = multiplyContribution(contribution, usage.amount)
                val key = DemandAggregationKey<V>(contribution.product.id, multiplied.quantity.unit)
                map.getOrPut(key) { ArrayList() }.add(multiplied)
            }
        }
        return map
    }

    private fun multiplyContribution(
        contribution: CuttingPlanDemandContribution<V>,
        times: UInt64
    ): CuttingPlanDemandContribution<V> {
        var total = Quantity(contribution.quantity.value.constants.zero, contribution.quantity.unit)
        var count = UInt64.zero
        while (count < times) {
            total = arithmetic.add(total, contribution.quantity)
            count += UInt64.one
        }
        return CuttingPlanDemandContribution(
            product = contribution.product,
            quantity = total
        )
    }

    private fun sumContributions(
        contributions: List<CuttingPlanDemandContribution<V>>
    ): Quantity<V>? {
        if (contributions.isEmpty()) return null
        return contributions.fold(Quantity(contributions.first().quantity.value.constants.zero, contributions.first().quantity.unit)) { acc, c ->
            arithmetic.add(acc, c.quantity)
        }
    }
}
