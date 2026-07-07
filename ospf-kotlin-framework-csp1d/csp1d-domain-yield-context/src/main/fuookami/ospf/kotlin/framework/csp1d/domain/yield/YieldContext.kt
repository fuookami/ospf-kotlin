package fuookami.ospf.kotlin.framework.csp1d.domain.yield

import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.*
import fuookami.ospf.kotlin.framework.csp1d.domain.produce.model.*
import fuookami.ospf.kotlin.framework.csp1d.domain.yield.model.*
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.quantities.quantity.*
import fuookami.ospf.kotlin.utils.functional.*

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
    ): Ret<YieldAnalysis<V>> {
        val contributionByKey = when (val result = aggregateContributions(produce)) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        val underProductions = ArrayList<UnderProduction<V>>()
        val overProductions = ArrayList<OverProduction<V>>()
        val outputs = ArrayList<ProductOutput<V>>()

        for (demand in demands) {
            val key = DemandAggregationKey<V>(demand.product.id, demand.quantity.unit)
            val contributions = contributionByKey[key] ?: emptyList()
            val totalOutput = when (val result = sumContributions(contributions)) {
                is Ok -> result.value
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
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
                            shortfall = when (val result = arithmetic.subtract(demand.quantity, totalOutput)) {
                                is Ok -> result.value
                                is Failed -> return Failed(result.error)
                                is Fatal -> return Fatal(result.errors)
                            }
                        )
                    )
                }

                is Order.Greater -> {
                    overProductions.add(
                        OverProduction(
                            demand = demand,
                            surplus = when (val result = arithmetic.subtract(totalOutput, demand.quantity)) {
                                is Ok -> result.value
                                is Failed -> return Failed(result.error)
                                is Fatal -> return Fatal(result.errors)
                            }
                        )
                    )
                }

                else -> {
                    // 相等：无偏差 / Equal: no deviation
                }
            }
        }

        return Ok(
            YieldAnalysis(
                underProductions = underProductions,
                overProductions = overProductions,
                outputs = outputs
            )
        )
    }

    /**
     * 按产品+单位聚合贡献 / Aggregate contributions by product+unit
     *
     * @param produce 主问题产出 / Master problem output
     * @return 按聚合键分组的贡献映射 / Contribution map grouped by aggregation key
     */
    private fun aggregateContributions(
        produce: Produce<V>
    ): Ret<Map<DemandAggregationKey<V>, List<CuttingPlanDemandContribution<V>>>> {
        val map = LinkedHashMap<DemandAggregationKey<V>, MutableList<CuttingPlanDemandContribution<V>>>()
        for (usage in produce.cuttingPlans) {
            for (contribution in usage.plan.demandContributions) {
                val multiplied = when (val result = multiplyContribution(contribution, usage.amount)) {
                    is Ok -> result.value
                    is Failed -> return Failed(result.error)
                    is Fatal -> return Fatal(result.errors)
                }
                val key = DemandAggregationKey<V>(contribution.product.id, multiplied.quantity.unit)
                map.getOrPut(key) { ArrayList() }.add(multiplied)
            }
        }
        return Ok(map)
    }

    /**
     * 将贡献量按次数累乘 / Multiply contribution quantity by repeating count
     *
     * @param contribution 切割方案需求贡献 / Cutting plan demand contribution
     * @param times 重复次数 / Repeat count
     * @return 累乘后的贡献 / Multiplied contribution
     */
    private fun multiplyContribution(
        contribution: CuttingPlanDemandContribution<V>,
        times: UInt64
    ): Ret<CuttingPlanDemandContribution<V>> {
        var total = Quantity(contribution.quantity.value.constants.zero, contribution.quantity.unit)
        var count = UInt64.zero
        while (count < times) {
            total = when (val result = arithmetic.add(total, contribution.quantity)) {
                is Ok -> result.value
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
            count += UInt64.one
        }
        return Ok(
            CuttingPlanDemandContribution(
                product = contribution.product,
                quantity = total
            )
        )
    }

    /**
     * 汇总同组贡献量 / Sum contributions within the same group
     *
     * @param contributions 同组贡献列表 / Contribution list within the same group
     * @return 汇总后的量，空列表返回 null / Summed quantity, or null if list is empty
     */
    private fun sumContributions(
        contributions: List<CuttingPlanDemandContribution<V>>
    ): Ret<Quantity<V>?> {
        if (contributions.isEmpty()) return Ok(null)
        var total = Quantity(contributions.first().quantity.value.constants.zero, contributions.first().quantity.unit)
        for (contribution in contributions) {
            total = when (val result = arithmetic.add(total, contribution.quantity)) {
                is Ok -> result.value
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
        }
        return Ok(total)
    }
}
