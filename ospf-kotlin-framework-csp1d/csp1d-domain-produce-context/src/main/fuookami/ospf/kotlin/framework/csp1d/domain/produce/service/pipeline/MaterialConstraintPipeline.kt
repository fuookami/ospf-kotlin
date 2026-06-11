package fuookami.ospf.kotlin.framework.csp1d.domain.produce.service.pipeline

import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.inequality.Comparison
import fuookami.ospf.kotlin.math.symbol.inequality.LinearInequality
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.core.model.mechanism.LinearMetaModel
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.framework.model.Pipeline
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.Material
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.MaterialUsageShadowPriceKey
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.Csp1dShadowPriceKey
import fuookami.ospf.kotlin.framework.csp1d.domain.produce.ProduceAggregation

/**
 * 物料可用批次约束管线 / Material available batch constraint pipeline
 *
 * 为每个物料添加可用批次上限约束：
 * sum(x_j where plan_j.material == material_i) <= available_batches
 *
 * Add available batch upper bound constraint for each material:
 * sum(x_j where plan_j.material == material_i) <= available_batches
 *
 * @param V 数值类型 / Numeric value type
 * @property produce 产出聚合 / Produce aggregation
 * @property materials 物料列表 / Material list
 * @property shadowPriceKeys 约束名到影子价格键的映射 / Constraint name to shadow price key mapping
 */
class MaterialConstraintPipeline<V : RealNumber<V>>(
    private val produce: ProduceAggregation<V>,
    private val materials: List<Material<V>>,
    private val shadowPriceKeys: MutableMap<String, Csp1dShadowPriceKey>? = null
) : Pipeline<LinearMetaModel<Flt64>> {

    override val name: String = "material_constraint"

    override fun invoke(model: LinearMetaModel<Flt64>): Try {
        for ((materialIndex, material) in materials.withIndex()) {
            if (material.availableBatches == fuookami.ospf.kotlin.math.algebra.number.UInt64.maximum) {
                continue
            }

            val lhs = LinearPolynomial(
                monomials = produce.cuttingPlans.mapIndexedNotNull { index, plan ->
                    if (plan.material.id != material.id) return@mapIndexedNotNull null
                    LinearMonomial(Flt64.one, produce[index])
                },
                constant = Flt64.zero
            )
            if (lhs.monomials.isEmpty()) continue

            val constraintName = "material_$materialIndex"
            shadowPriceKeys?.set(
                constraintName,
                MaterialUsageShadowPriceKey(material.id)
            )
            model.addConstraint(
                relation = LinearInequality(
                    lhs = lhs,
                    rhs = DemandConstraintPipeline.constantPolynomial(material.availableBatches.toFlt64()),
                    comparison = Comparison.LE
                ),
                name = constraintName
            )
        }

        return ok
    }
}
