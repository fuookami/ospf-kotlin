package fuookami.ospf.kotlin.framework.csp1d.domain.produce.service.pipeline

import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.inequality.Comparison
import fuookami.ospf.kotlin.math.symbol.inequality.LinearInequality
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMetaModel
import fuookami.ospf.kotlin.core.model.mechanism.MetaDualSolution
import fuookami.ospf.kotlin.core.variable.URealVar
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.framework.model.CGPipeline
import fuookami.ospf.kotlin.framework.model.ShadowPriceExtractor
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.*
import fuookami.ospf.kotlin.framework.csp1d.domain.produce.ProduceAggregation
import fuookami.ospf.kotlin.framework.csp1d.domain.produce.model.CuttingPlanUsage
import fuookami.ospf.kotlin.framework.csp1d.domain.produce.model.Produce

/**
 * 需求平衡约束管线 / Demand balance constraint pipeline
 *
 * 为每个产品需求添加平衡约束：
 * - 有 yield slack 时：demandQuantity[i] - over + under = demand
 * - 无 yield slack 时：demandQuantity[i] >= demand
 *
 * demandQuantity[i] 是中间符号，由 ProduceAggregation 在 register 和 addColumns 时管理，
 * 约束管线不再直接引用 x 变量，因此 addColumns 时只需 flush 中间符号，无需刷新约束。
 *
 * 实现 CGPipeline 接口，通过 constraint.args = Csp1dShadowPriceKey 关联影子价格，
 * 替代原先的 constraint-name registry 机制。LP 对偶值提取通过 refresh / extractor
 * 直接从 AbstractCsp1dShadowPriceMap 获取，无需手动遍历 constraint name。
 *
 * Add balance constraint for each product demand:
 * - With yield slack: demandQuantity[i] - over + under = demand
 * - Without yield slack: demandQuantity[i] >= demand
 *
 * demandQuantity[i] is an intermediate symbol managed by ProduceAggregation during register
 * and addColumns. Constraint pipelines no longer reference x variables directly, so addColumns
 * only needs to flush intermediate symbols without refreshing constraints.
 *
 * Implements CGPipeline interface, associating shadow prices via constraint.args = Csp1dShadowPriceKey,
 * replacing the previous constraint-name registry mechanism. LP dual value extraction uses
 * refresh / extractor to obtain values directly from AbstractCsp1dShadowPriceMap without
 * manual constraint name traversal.
 *
 * @param V 数值类型 / Numeric value type
 * @property produce 产出聚合 / Produce aggregation
 * @property demands 需求列表 / Demand list
 * @property yieldUnderVars 欠产松弛变量 / Under-production slack variables
 * @property yieldOverVars 超产松弛变量 / Over-production slack variables
 */
class DemandConstraintPipeline<V : RealNumber<V>>(
    private val produce: ProduceAggregation<V>,
    private val demands: List<ProductDemand<V>>,
    private val yieldUnderVars: List<URealVar?> = emptyList(),
    private val yieldOverVars: List<URealVar?> = emptyList()
) : Csp1dCGPipeline {

    override val name: String = "demand_constraint"

    override fun invoke(model: AbstractLinearMetaModel<Flt64>): Try {
        val hasYieldSlack = yieldUnderVars.any { it != null } || yieldOverVars.any { it != null }

        for ((demandIndex, demand) in demands.withIndex()) {
            val priceKey = ProductDemandShadowPriceKey(
                productId = demand.product.id,
                unitSymbol = shadowPriceUnitSymbol(demand.quantity.unit)
            )

            if (hasYieldSlack) {
                addEqualityConstraint(model, demandIndex, demand, priceKey)
            } else {
                addGeConstraint(model, demand, demandIndex, priceKey)
            }
        }

        return ok
    }

    /**
     * 使用中间符号构建需求贡献 LHS / Build demand contribution LHS using intermediate symbol
     *
     * 引用 produce.demandQuantity[demandIndex] 而非直接引用 x 变量，
     * 这样 addColumns 刷新中间符号时约束自动包含新列系数。
     *
     * Reference produce.demandQuantity[demandIndex] instead of x variables directly,
     * so that addColumns flush of intermediate symbols automatically includes new column coefficients.
     */
    private fun buildDemandLhs(demandIndex: Int): LinearPolynomial<Flt64> {
        return LinearPolynomial(
            monomials = listOf(LinearMonomial(Flt64.one, produce.demandQuantity[demandIndex])),
            constant = Flt64.zero
        )
    }

    private fun addEqualityConstraint(
        model: AbstractLinearMetaModel<Flt64>,
        demandIndex: Int,
        demand: ProductDemand<V>,
        priceKey: ProductDemandShadowPriceKey
    ) {
        val underVar = yieldUnderVars.getOrNull(demandIndex)
        val overVar = yieldOverVars.getOrNull(demandIndex)

        if (underVar != null || overVar != null) {
            val lhs = LinearPolynomial(
                monomials = buildList {
                    add(LinearMonomial(Flt64.one, produce.demandQuantity[demandIndex]))
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
                group = this,
                name = "demand_$demandIndex",
                args = priceKey
            )
        } else {
            addGeConstraint(model, demand, demandIndex, priceKey)
        }
    }

    private fun addGeConstraint(
        model: AbstractLinearMetaModel<Flt64>,
        demand: ProductDemand<V>,
        demandIndex: Int,
        priceKey: ProductDemandShadowPriceKey
    ) {
        val lhs = LinearPolynomial(
            monomials = listOf(LinearMonomial(Flt64.one, produce.demandQuantity[demandIndex])),
            constant = Flt64.zero
        )
        val rhs = constantPolynomial(demand.quantity.value.toFlt64())
        model.addConstraint(
            relation = LinearInequality(lhs = lhs, rhs = rhs, comparison = Comparison.GE),
            group = this,
            name = "demand_$demandIndex",
            args = priceKey
        )
    }

    override fun refresh(
        shadowPriceMap: Csp1dShadowPriceMap,
        model: AbstractLinearMetaModel<Flt64>,
        shadowPrices: MetaDualSolution
    ): Try {
        return CGPipeline.refreshByKeyAsArgs(this, shadowPriceMap, model, shadowPrices)
    }

    override fun extractor(): Csp1dShadowPriceExtractor? {
        if (demands.isEmpty()) return null
        return { map, args ->
            if (args is Csp1dCuttingPlanShadowPriceArguments<*>) {
                var price = Flt64.zero
                for (demand in demands) {
                    val key = ProductDemandShadowPriceKey(
                        productId = demand.product.id,
                        unitSymbol = shadowPriceUnitSymbol(demand.quantity.unit)
                    )
                    val shadowPrice = map[key]?.price ?: continue
                    val contribution = args.plan.demandContributions.find {
                        it.product.id == demand.product.id && it.quantity.unit == demand.quantity.unit
                    } ?: continue
                    price += shadowPrice * contribution.quantity.value.toFlt64()
                }
                price
            } else {
                Flt64.zero
            }
        }
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
