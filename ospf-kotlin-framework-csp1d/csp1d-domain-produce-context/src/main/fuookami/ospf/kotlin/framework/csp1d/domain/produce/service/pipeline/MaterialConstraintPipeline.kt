package fuookami.ospf.kotlin.framework.csp1d.domain.produce.service.pipeline

import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.inequality.Comparison
import fuookami.ospf.kotlin.math.symbol.inequality.LinearInequality
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMetaModel
import fuookami.ospf.kotlin.core.model.mechanism.MetaDualSolution
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.framework.model.CGPipeline
import fuookami.ospf.kotlin.framework.model.ShadowPriceExtractor
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.*
import fuookami.ospf.kotlin.framework.csp1d.domain.produce.ProduceAggregation

/**
 * 物料可用批次约束管线 / Material available batch constraint pipeline
 *
 * 为每个物料添加可用批次上限约束：
 * materialQuantity[i] <= available_batches
 *
 * materialQuantity[i] 是中间符号，由 ProduceAggregation 管理，
 * 约束管线不再直接引用 x 变量。
 *
 * 实现 CGPipeline 接口，通过 constraint.args = MaterialUsageShadowPriceKey
 * 关联影子价格，替代约束名映射。
 *
 * Add available batch upper bound constraint for each material:
 * materialQuantity[i] <= available_batches
 *
 * materialQuantity[i] is an intermediate symbol managed by ProduceAggregation.
 * Constraint pipelines no longer reference x variables directly.
 *
 * Implements CGPipeline interface, associating shadow prices via
 * constraint.args = MaterialUsageShadowPriceKey, replacing the constraint-name based mechanism.
 *
 * @param V 数值类型 / Numeric value type
 * @property produce 产出聚合 / Produce aggregation
 * @property materials 物料列表 / Material list
 */
class MaterialConstraintPipeline<V : RealNumber<V>>(
    private val produce: ProduceAggregation<V>,
    private val materials: List<Material<V>>
) : CGPipeline<
        AbstractCsp1dShadowPriceArguments,
        AbstractLinearMetaModel<Flt64>,
        AbstractCsp1dShadowPriceMap<AbstractCsp1dShadowPriceArguments>
        > {

    override val name: String = "material_constraint"

    override fun invoke(model: AbstractLinearMetaModel<Flt64>): Try {
        for ((materialIndex, material) in materials.withIndex()) {
            if (material.availableBatches == fuookami.ospf.kotlin.math.algebra.number.UInt64.maximum) {
                continue
            }

            val constraintName = "material_$materialIndex"
            val priceKey = MaterialUsageShadowPriceKey(material.id)

            val lhs = LinearPolynomial(
                monomials = listOf(LinearMonomial(Flt64.one, produce.materialQuantity[materialIndex])),
                constant = Flt64.zero
            )
            model.addConstraint(
                relation = LinearInequality(
                    lhs = lhs,
                    rhs = DemandConstraintPipeline.constantPolynomial(material.availableBatches.toFlt64()),
                    comparison = Comparison.LE
                ),
                group = this,
                name = constraintName,
                args = priceKey
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
        if (materials.isEmpty()) return null
        return { map, args ->
            if (args is Csp1dCuttingPlanShadowPriceArguments<*>) {
                val key = MaterialUsageShadowPriceKey(args.plan.material.id)
                map[key]?.price ?: Flt64.zero
            } else {
                Flt64.zero
            }
        }
    }
}
