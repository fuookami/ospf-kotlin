package fuookami.ospf.kotlin.framework.csp1d.domain.produce.service.pipeline

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.variable.URealVar
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.*
import fuookami.ospf.kotlin.framework.csp1d.domain.produce.ProduceAggregation
import fuookami.ospf.kotlin.framework.csp1d.domain.produce.model.*
import fuookami.ospf.kotlin.framework.model.*

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
 * 替代原先的约束名映射机制。LP 对偶值提取通过 refresh / extractor
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
 * replacing the previous constraint-name based mechanism. LP dual value extraction uses
 * refresh / extractor to obtain values directly from AbstractCsp1dShadowPriceMap without
 * manual constraint name traversal.
 *
 * @param V 数值类型 / Numeric value type
 * @property produce 产出聚合 / Produce aggregation
 * @property demands 需求列表 / Demand list
*/
class DemandConstraintPipeline<V : RealNumber<V>>(
    private val produce: ProduceAggregation<V>,
    private val demands: List<ProductDemand<V>>,
    private val yieldUnderVars: List<URealVar?> = emptyList(),
    private val yieldOverVars: List<URealVar?> = emptyList()
) : CGPipeline<
        AbstractCsp1dShadowPriceArguments,
        AbstractLinearMetaModel<Flt64>,
        AbstractCsp1dShadowPriceMap<AbstractCsp1dShadowPriceArguments>
        > {

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
     *
     * @param demandIndex 需求索引 / Demand index
     * @return 需求贡献左侧多项式 / Demand contribution left-hand side polynomial
    */
    private fun buildDemandLhs(demandIndex: Int): LinearPolynomial<Flt64> {
        return LinearPolynomial(
            monomials = listOf(LinearMonomial(Flt64.one, produce.demandQuantity[demandIndex])),
            constant = Flt64.zero
        )
    }

    /**
     * 添加需求等式约束 / Add demand equality constraint
     *
     * 当存在 yield slack 变量时，构建 demandQuantity - over + under = demand 等式约束。
     * When yield slack variables exist, build demandQuantity - over + under = demand equality constraint.
     *
     * @param model 线性元模型 / Linear meta model
     * @param demandIndex 需求索引 / Demand index
     * @param demand 产品需求 / Product demand
     * @param priceKey 影子价格键 / Shadow price key
    */
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

    /**
     * 添加需求大于等于约束 / Add demand greater-than-or-equal constraint
     *
     * 构建 demandQuantity >= demand 约束。
     * Build demandQuantity >= demand constraint.
     *
     * @param model 线性元模型 / Linear meta model
     * @param demand 产品需求 / Product demand
     * @param demandIndex 需求索引 / Demand index
     * @param priceKey 影子价格键 / Shadow price key
    */
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
        /**
         * 创建常数多项式 / Create constant polynomial
         *
         * @param value 常数值 / Constant value
         * @return 常数多项式 / Constant polynomial
        */
        internal fun constantPolynomial(value: Flt64): LinearPolynomial<Flt64> {
            return LinearPolynomial(emptyList(), value)
        }

        /**
         * 获取物理单位的符号表示 / Get symbol representation of physical unit
         *
         * @param unit 物理单位 / Physical unit
         * @return 单位符号字符串 / Unit symbol string
        */
        internal fun shadowPriceUnitSymbol(unit: fuookami.ospf.kotlin.quantities.unit.PhysicalUnit): String {
            return unit.symbol ?: unit.toString()
        }
    }
}
