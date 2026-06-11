package fuookami.ospf.kotlin.framework.csp1d.domain.produce.service.pipeline

import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.inequality.Comparison
import fuookami.ospf.kotlin.math.symbol.inequality.LinearInequality
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.core.model.mechanism.LinearMetaModel
import fuookami.ospf.kotlin.core.variable.URealVar
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.framework.model.Pipeline
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.ProductDemand
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.ProductDemandShadowPriceKey
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.Csp1dShadowPriceKey
import fuookami.ospf.kotlin.framework.csp1d.domain.produce.ProduceAggregation

/**
 * 需求平衡约束管线 / Demand balance constraint pipeline
 *
 * 为每个产品需求添加平衡约束：
 * - 有 yield slack 时：sum(contribution * x) - over + under = demand
 * - 无 yield slack 时：sum(contribution * x) >= demand
 *
 * Add balance constraint for each product demand:
 * - With yield slack: sum(contribution * x) - over + under = demand
 * - Without yield slack: sum(contribution * x) >= demand
 *
 * @param V 数值类型 / Numeric value type
 * @property produce 产出聚合 / Produce aggregation
 * @property demands 需求列表 / Demand list
 * @property yieldUnderVars 欠产松弛变量 / Under-production slack variables
 * @property yieldOverVars 超产松弛变量 / Over-production slack variables
 * @property shadowPriceKeys 约束名到影子价格键的映射（LP 求解时使用）/ Constraint name to shadow price key mapping (used in LP solving)
 */
class DemandConstraintPipeline<V : RealNumber<V>>(
    private val produce: ProduceAggregation<V>,
    private val demands: List<ProductDemand<V>>,
    private val yieldUnderVars: List<URealVar?> = emptyList(),
    private val yieldOverVars: List<URealVar?> = emptyList(),
    private val shadowPriceKeys: MutableMap<String, Csp1dShadowPriceKey>? = null
) : Pipeline<LinearMetaModel<Flt64>> {

    override val name: String = "demand_constraint"

    override fun invoke(model: LinearMetaModel<Flt64>): Try {
        val hasYieldSlack = yieldUnderVars.any { it != null } || yieldOverVars.any { it != null }

        for ((demandIndex, demand) in demands.withIndex()) {
            val contributionMonomials = buildContributionMonomials(demand)

            val constraintName = "demand_$demandIndex"
            registerShadowPriceKey(constraintName, demand)

            if (hasYieldSlack) {
                addEqualityConstraint(model, demandIndex, demand, contributionMonomials, constraintName)
            } else {
                addGeConstraint(model, demand, contributionMonomials, constraintName)
            }
        }

        return ok
    }

    private fun buildContributionMonomials(
        demand: ProductDemand<V>
    ): List<LinearMonomial<Flt64>> {
        return produce.cuttingPlans.mapIndexedNotNull { index, plan ->
            val contribution = plan.demandContributions.find {
                it.product.id == demand.product.id && it.quantity.unit == demand.quantity.unit
            } ?: return@mapIndexedNotNull null
            LinearMonomial(
                coefficient = contribution.quantity.value.toFlt64(),
                symbol = produce[index]
            )
        }
    }

    private fun registerShadowPriceKey(
        constraintName: String,
        demand: ProductDemand<V>
    ) {
        val unitSymbol = shadowPriceUnitSymbol(demand.quantity.unit)
        shadowPriceKeys?.set(
            constraintName,
            ProductDemandShadowPriceKey(
                productId = demand.product.id,
                unitSymbol = unitSymbol
            )
        )
    }

    private fun addEqualityConstraint(
        model: LinearMetaModel<Flt64>,
        demandIndex: Int,
        demand: ProductDemand<V>,
        contributionMonomials: List<LinearMonomial<Flt64>>,
        constraintName: String
    ) {
        val underVar = yieldUnderVars.getOrNull(demandIndex)
        val overVar = yieldOverVars.getOrNull(demandIndex)

        if (underVar != null || overVar != null) {
            val lhs = LinearPolynomial(
                monomials = buildList {
                    addAll(contributionMonomials)
                    if (underVar != null) {
                        add(LinearMonomial(Flt64.one, underVar))
                    }
                    if (overVar != null) {
                        add(LinearMonomial(Flt64(-1.0), overVar))
                    }
                },
                constant = Flt64.zero
            )
            val rhs = constantPolynomial(demand.quantity.value.toFlt64())
            model.addConstraint(
                relation = LinearInequality(lhs = lhs, rhs = rhs, comparison = Comparison.EQ),
                name = constraintName
            )
        } else {
            addGeConstraint(model, demand, contributionMonomials, constraintName)
        }
    }

    private fun addGeConstraint(
        model: LinearMetaModel<Flt64>,
        demand: ProductDemand<V>,
        contributionMonomials: List<LinearMonomial<Flt64>>,
        constraintName: String
    ) {
        val lhs = LinearPolynomial(
            monomials = contributionMonomials,
            constant = Flt64.zero
        )
        val rhs = constantPolynomial(demand.quantity.value.toFlt64())
        model.addConstraint(
            relation = LinearInequality(lhs = lhs, rhs = rhs, comparison = Comparison.GE),
            name = constraintName
        )
    }

    companion object {
        internal fun constantPolynomial(value: Flt64): LinearPolynomial<Flt64> {
            return LinearPolynomial(emptyList(), value)
        }

        internal fun shadowPriceUnitSymbol(unit: fuookami.ospf.kotlin.quantities.unit.PhysicalUnit): String {
            return unit.symbol ?: unit.toString()
        }
    }
}
