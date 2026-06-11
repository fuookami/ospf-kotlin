package fuookami.ospf.kotlin.framework.csp1d.domain.yield.service.pipeline

import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.core.model.mechanism.LinearMetaModel
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.framework.model.Pipeline
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.ProductDemand
import fuookami.ospf.kotlin.framework.csp1d.domain.yield.YieldAggregation
import fuookami.ospf.kotlin.framework.csp1d.domain.yield.model.YieldModelingConfig

/**
 * 产出偏差目标管线 / Yield deviation objective pipeline
 *
 * 在目标函数中添加欠产和超产惩罚项：
 * minimize sum(underPenalty * under_production_i + overPenalty * over_production_i)
 *
 * Add under-production and over-production penalty terms to the objective function:
 * minimize sum(underPenalty * under_production_i + overPenalty * over_production_i)
 *
 * 注意：此管线不直接设置目标函数，而是返回目标项的单项式列表，
 * 由 Csp1dProduceContext 统一组装目标函数。
 *
 * Note: This pipeline does not set the objective function directly, but returns
 * the objective monomials for Csp1dProduceContext to assemble the full objective.
 *
 * @param V 数值类型 / Numeric value type
 * @property yield 产出偏差聚合 / Yield deviation aggregation
 * @property config 产出建模配置 / Yield modeling configuration
 * @property demands 需求列表 / Demand list
 */
class YieldObjectivePipeline<V : RealNumber<V>>(
    private val yield: YieldAggregation<V>,
    private val config: YieldModelingConfig<V>,
    private val demands: List<ProductDemand<V>>
) : Pipeline<LinearMetaModel<Flt64>> {

    override val name: String = "yield_objective"

    override fun invoke(model: LinearMetaModel<Flt64>): Try {
        // yield 目标项由 Csp1dProduceContext 统一组装到目标函数中
        // 此处不做任何操作，仅作为管线注册标记
        return ok
    }

    /**
     * 生成目标项单项式 / Generate objective monomials
     *
     * @return 目标项单项式列表 / Objective monomial list
     */
    fun objectiveMonomials(): List<LinearMonomial<Flt64>> {
        val monomials = ArrayList<LinearMonomial<Flt64>>()

        for ((demandIndex, demand) in demands.withIndex()) {
            val demandKey = YieldAggregation.demandShadowPriceKey(demand)

            val underPenalty = config.underProductionPenalty[demandKey]
            if (underPenalty != null) {
                val underVar = yield.underProduction.getOrNull(demandIndex)
                if (underVar != null) {
                    monomials.add(LinearMonomial(underPenalty.toFlt64(), underVar))
                }
            }

            val overPenalty = config.overProductionPenalty[demandKey]
            if (overPenalty != null) {
                val overVar = yield.overProduction.getOrNull(demandIndex)
                if (overVar != null) {
                    monomials.add(LinearMonomial(overPenalty.toFlt64(), overVar))
                }
            }
        }

        return monomials
    }
}
