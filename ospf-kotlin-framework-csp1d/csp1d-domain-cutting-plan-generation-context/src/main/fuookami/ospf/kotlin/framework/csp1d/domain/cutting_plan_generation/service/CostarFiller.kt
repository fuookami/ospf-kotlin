package fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.service

import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.Costar
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.CuttingPlan
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.CuttingPlanSlice
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.QuantityArithmetic
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.quantities.quantity.partialOrd
import fuookami.ospf.kotlin.utils.functional.Order

/**
 * 配规填充器，为切割方案的剩余宽度填充配规切片 / Costar filler: fills remaining width with costar slices
 *
 * 配规只作为切片加入，不进入 demandContributions / Costars are added as slices only, not in demandContributions
 *
 * @param V 数值类型 / Numeric value type
 * @property arithmetic 物理量算术策略 / Quantity arithmetic strategy
 */
class CostarFiller<V : RealNumber<V>>(
    private val arithmetic: QuantityArithmetic<V>
) {
    /**
     * 尝试为切割方案填充配规 / Try to fill a cutting plan with costars
     *
     * @param plan 原始方案 / Original plan
     * @param costars 可用配规列表 / Available costars
     * @return 填充后的方案列表（可能多个方案因为不同配规组合）/ Filled plan list
     */
    fun fill(plan: CuttingPlan<V>, costars: List<Costar<V>>): List<CuttingPlan<V>> {
        if (costars.isEmpty()) return listOf(plan)
        val restWidth = plan.restWidth ?: return listOf(plan)
        if (arithmetic.isZero(restWidth) || !arithmetic.isPositive(restWidth)) return listOf(plan)

        val results = ArrayList<CuttingPlan<V>>()
        fillDFS(
            currentSlices = plan.slices.toMutableList(),
            remainingWidth = restWidth,
            costars = costars,
            costarIndex = 0,
            plan = plan,
            results = results
        )

        return if (results.isEmpty()) listOf(plan) else results
    }

    private fun fillDFS(
        currentSlices: MutableList<CuttingPlanSlice<V>>,
        remainingWidth: Quantity<V>,
        costars: List<Costar<V>>,
        costarIndex: Int,
        plan: CuttingPlan<V>,
        results: MutableList<CuttingPlan<V>>
    ) {
        if (arithmetic.isZero(remainingWidth) || !arithmetic.isPositive(remainingWidth)) {
            results.add(buildPlan(plan, currentSlices))
            return
        }

        if (costarIndex >= costars.size) {
            results.add(buildPlan(plan, currentSlices))
            return
        }

        val costar = costars[costarIndex]
        for (costarWidth in costar.width) {
            if (remainingWidth.value partialOrd costarWidth.value is Order.Less) continue

            val maxAmount = computeMaxCostarAmount(costarWidth, remainingWidth)
            val amountLimit = maxAmount.coerceAtMost(UInt64(2UL))
            for (amount in UInt64.one..amountLimit) {
                val totalCostarWidth = repeatWidth(costarWidth, amount)
                val newRemaining = arithmetic.subtract(remainingWidth, totalCostarWidth)

                currentSlices.add(
                    CuttingPlanSlice(
                        production = costar,
                        width = costarWidth,
                        amount = amount
                    )
                )

                fillDFS(
                    currentSlices = currentSlices,
                    remainingWidth = newRemaining,
                    costars = costars,
                    costarIndex = costarIndex + 1,
                    plan = plan,
                    results = results
                )

                currentSlices.removeAt(currentSlices.lastIndex)
            }
        }

        // Try skipping this costar
        fillDFS(
            currentSlices = currentSlices,
            remainingWidth = remainingWidth,
            costars = costars,
            costarIndex = costarIndex + 1,
            plan = plan,
            results = results
        )
    }

    private fun buildPlan(
        original: CuttingPlan<V>,
        slices: List<CuttingPlanSlice<V>>
    ): CuttingPlan<V> {
        return CuttingPlan(
            id = original.id,
            material = original.material,
            slices = slices.toList(),
            demandContributions = original.demandContributions,
            arithmetic = arithmetic
        )
    }

    private fun computeMaxCostarAmount(costarWidth: Quantity<V>, remainingWidth: Quantity<V>): UInt64 {
        if (costarWidth.value partialOrd remainingWidth.value is Order.Greater) return UInt64.zero
        var count = UInt64.zero
        var w = remainingWidth
        while (w.value partialOrd costarWidth.value !is Order.Less) {
            w = arithmetic.subtract(w, costarWidth)
            count = count + UInt64.one
        }
        return count
    }

    private fun repeatWidth(width: Quantity<V>, times: UInt64): Quantity<V> {
        var result = arithmetic.zero(width.unit)
        repeat(times.toInt()) {
            result = arithmetic.add(result, width)
        }
        return result
    }
}
