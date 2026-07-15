package fuookami.ospf.kotlin.framework.csp1d.domain.yield.service.pipeline

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.*
import fuookami.ospf.kotlin.framework.csp1d.domain.yield.YieldAggregation
import fuookami.ospf.kotlin.framework.csp1d.domain.yield.model.YieldModelingConfig
import fuookami.ospf.kotlin.framework.model.*

/**
 * Over-production upper bound constraint pipeline.
 * 超产上限约束管线
 *
 * Add constraint for each demand with over-production upper bound: over_production_i <= upperBound
 * 为每个配置了超产上限的需求添加约束：over_production_i <= upperBound
 *
 * @param V Numeric value type / 数值类型
 * @property yield Yield deviation aggregation / 产出偏差聚合
 * @property config Yield modeling configuration / 产出建模配置
 * @property demands Demand list / 需求列表
*/
class YieldConstraintPipeline<V : RealNumber<V>>(
    private val yield: YieldAggregation<V>,
    private val config: YieldModelingConfig<V>,
    private val demands: List<ProductDemand<V>>
) : CGPipeline<
        AbstractCsp1dShadowPriceArguments,
        AbstractLinearMetaModel<Flt64>,
        AbstractCsp1dShadowPriceMap<AbstractCsp1dShadowPriceArguments>
        > {

    override val name: String = "yield_constraint"

    override fun invoke(model: AbstractLinearMetaModel<Flt64>): Try {
        if (config.overProductionUpperBound.isEmpty()) return ok

        for ((demandIndex, demand) in demands.withIndex()) {
            val demandKey = YieldAggregation.demandShadowPriceKey(demand)
            val upperBound = config.overProductionUpperBound[demandKey] ?: continue
            val overVar = yield.overProduction.getOrNull(demandIndex) ?: continue
            val priceKey = YieldOverProductionBoundShadowPriceKey(
                productId = demandKey.productId,
                unitSymbol = demandKey.unitSymbol
            )

            model.addConstraint(
                relation = LinearInequality(
                    lhs = LinearPolynomial(
                        monomials = listOf(LinearMonomial(Flt64.one, overVar)),
                        constant = Flt64.zero
                    ),
                    rhs = LinearPolynomial(emptyList(), upperBound.toFlt64()),
                    comparison = Comparison.LE
                ),
                group = this,
                args = priceKey,
                name = "over_production_bound_$demandIndex"
            )
        }

        return ok
    }

    override fun refresh(
        shadowPriceMap: AbstractCsp1dShadowPriceMap<AbstractCsp1dShadowPriceArguments>,
        model: AbstractLinearMetaModel<Flt64>,
        shadowPrices: MetaDualSolution
    ): Try {
        return CGPipeline.refreshByKeyAsArgs(this, shadowPriceMap, model, shadowPrices)
    }

    override fun extractor(): ShadowPriceExtractor<
            AbstractCsp1dShadowPriceArguments,
            AbstractCsp1dShadowPriceMap<AbstractCsp1dShadowPriceArguments>
            >? {
        return { _, _ -> Flt64.zero }
    }
}
