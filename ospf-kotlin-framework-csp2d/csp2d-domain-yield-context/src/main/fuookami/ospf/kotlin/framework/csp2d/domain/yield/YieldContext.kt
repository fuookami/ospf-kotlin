/**
 * 良率上下文：超产上界。 / Yield context: over-production upper bounds.
 *
 * 需求覆盖约束是 `≥`（必须满足需求），因此超产是天然存在的。套裁的超产就是「切多了」：多切的部分要么入库、
 * 要么报废，业务上通常要设上限。这里把上限表达为 `Σ 覆盖_i ≤ 需求_i + 上限_i`，不需要引入松弛变量，
 * 因此不会破坏主问题的整数性。
 * / The demand coverage constraint is `≥` (the demand must be met), so over-production always exists. In cutting stock
 * over-production means "cut too much": the excess is either stocked or scrapped, and a business usually caps it. The
 * cap is expressed as `Σ coverage_i ≤ demand_i + bound_i` without an extra slack variable, so the integrality of the
 * master problem is untouched.
 *
 * 与 csp1d 的差异：csp1d 用欠产/超产双侧惩罚并引入松弛变量；csp2d 把「必须满足需求」作为硬约束，
 * 只对超产设上限，因此不需要双侧松弛。
 * / Difference from csp1d: csp1d penalizes both under- and over-production with slack variables, while csp2d treats
 * "demand must be met" as a hard constraint and only caps over-production, so no two-sided slack is needed.
*/
package fuookami.ospf.kotlin.framework.csp2d.domain.yield

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.math.symbol.inequality.Comparison
import fuookami.ospf.kotlin.math.symbol.inequality.LinearInequality
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.core.model.mechanism.LinearMetaModel
import fuookami.ospf.kotlin.framework.csp2d.domain.material.model.CuttingPlan2
import fuookami.ospf.kotlin.framework.csp2d.domain.material.model.DemandMode2
import fuookami.ospf.kotlin.framework.csp2d.domain.material.model.DemandValue2
import fuookami.ospf.kotlin.framework.csp2d.domain.material.model.ProductDemand2
import fuookami.ospf.kotlin.framework.csp2d.domain.produce.Csp2dPatternAssignment
import fuookami.ospf.kotlin.framework.csp2d.domain.produce.safeCsp2dPolynomial

/**
 * 良率配置。 / Yield configuration.
 *
 * @property overProductionUpperBounds 产品编号到超产上限的映射，未配置的产品不设上限 / product identifier to
 * over-production upper bound; unconfigured products are uncapped
*/
data class Csp2dYieldConfig(
    val overProductionUpperBounds: Map<String, FltX> = emptyMap()
)

/**
 * 超产上界约束：`Σ_j 覆盖_ij · y_j ≤ 需求_i + 上限_i`。 / Over-production bound constraint
 * `Σ_j coverage_ij · y_j ≤ demand_i + bound_i`.
 *
 * @property plans 候选方案 / candidate plans
 * @property assignment 图案使用量变量 / pattern usage variables
 * @property demands 产品需求 / product demands
 * @property config 良率配置 / yield configuration
 * @property name 约束名称 / constraint name
*/
class Csp2dOverProductionBoundConstraint(
    private val plans: List<CuttingPlan2<FltX>>,
    private val assignment: Csp2dPatternAssignment,
    private val demands: List<ProductDemand2<FltX>>,
    private val config: Csp2dYieldConfig,
    val name: String = "csp2d_over_production"
) {

    /**
     * 注册超产上界约束。 / Register the over-production bound constraints.
     *
     * @param model 线性元模型 / linear meta model
     * @return 注册结果 / registration result
    */
    fun invoke(model: LinearMetaModel<FltX>): Try {
        if (config.overProductionUpperBounds.isEmpty()) {
            return ok
        }
        for ((index, demand) in demands.withIndex()) {
            val bound = config.overProductionUpperBounds[demand.product.id.value] ?: continue
            val required = requiredValue(demand)
            val monomials = ArrayList<LinearMonomial<FltX>>()
            for ((planIndex, plan) in plans.withIndex()) {
                val coverage = coverageOf(plan, demand)
                if (coverage == FltX.zero) {
                    continue
                }
                monomials.add(LinearMonomial(coverage, assignment.usage[planIndex]))
            }
            if (monomials.isEmpty()) {
                continue
            }
            val relation = LinearInequality(
                safeCsp2dPolynomial(monomials),
                LinearPolynomial(emptyList(), required + bound),
                Comparison.LE
            )
            when (val result = model.addConstraint(relation = relation, name = "${name}_$index")) {
                is Ok -> {}
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
        }
        return ok
    }

    /**
     * 读取需求目标值。 / Read the demand target value.
     *
     * @param demand 需求 / demand
     * @return 目标值 / target value
    */
    private fun requiredValue(demand: ProductDemand2<FltX>): FltX {
        return when (val value = demand.demand) {
            is DemandValue2.Amount -> FltX(value.count.toInt().toDouble())
            is DemandValue2.Area -> value.area.value
            is DemandValue2.Weight -> value.weight.value
        }
    }

    /**
     * 计算覆盖量。 / Compute the coverage.
     *
     * @param plan 方案 / plan
     * @param demand 需求 / demand
     * @return 覆盖量 / coverage
    */
    private fun coverageOf(plan: CuttingPlan2<FltX>, demand: ProductDemand2<FltX>): FltX {
        var amount = UInt64.zero
        for (contribution in plan.contributions) {
            if (contribution.product.id.value != demand.product.id.value) {
                continue
            }
            amount = contribution.amount
            break
        }
        if (amount == UInt64.zero) {
            return FltX.zero
        }
        val count = FltX(amount.toInt().toDouble())
        return when (demand.mode) {
            DemandMode2.Amount -> count
            DemandMode2.Area -> count * demand.product.area.value
            DemandMode2.Weight -> {
                val unitWeight = demand.product.unitWeight
                if (unitWeight == null) FltX.zero else count * unitWeight.value
            }
        }
    }
}
