package fuookami.ospf.kotlin.framework.csp1d.domain.length_assignment.service.pipeline

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.core.model.mechanism.LinearMetaModel
import fuookami.ospf.kotlin.framework.csp1d.domain.length_assignment.LengthAggregation
import fuookami.ospf.kotlin.framework.csp1d.domain.length_assignment.model.LengthAssignmentModelingConfig
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.ProductDemand
import fuookami.ospf.kotlin.framework.model.Pipeline

/**
 * Length assignment constraint pipeline.
 * 长度分配约束管线
 *
 * Add constraints for dynamic-length products:
 * - Lower bound: assigned_length_i >= lowerBound
 * - Upper bound: assigned_length_i <= upperBound
 * - Over-length bound: over_length_i <= overLengthUpperBound
 * - Assigned-over-length link: assigned_length_i - over_length_i <= maxOverProduceLength
 *
 * 为动态长度产品添加约束：
 * - 下界约束: assigned_length_i >= lowerBound
 * - 上界约束: assigned_length_i <= upperBound
 * - 超长上限约束: over_length_i <= overLengthUpperBound
 * - 卷长-超长关联约束: assigned_length_i - over_length_i <= maxOverProduceLength
 *
 * @param V Numeric value type / 数值类型
 * @property length Length assignment aggregation / 长度分配聚合
 * @property config Length assignment modeling configuration / 长度分配建模配置
 * @property demands Demand list / 需求列表
*/
class LengthConstraintPipeline<V : RealNumber<V>>(
    private val length: LengthAggregation<V>,
    private val config: LengthAssignmentModelingConfig<V>,
    private val demands: List<ProductDemand<V>>
) : Pipeline<LinearMetaModel<Flt64>> {

    override val name: String = "length_constraint"

    override fun invoke(model: LinearMetaModel<Flt64>): Try {
        for ((demandIndex, demand) in demands.withIndex()) {
            val productId = demand.product.id
            if (!config.dynamicProductIds.contains(productId)) continue

            val assignedVar = length.assignedLength.getOrNull(demandIndex)
            val overVar = length.overLength.getOrNull(demandIndex)

            // 下界约束: assigned_length_i >= lowerBound
            val lowerBound = config.assignedLengthLowerBound[productId]
            if (lowerBound != null && assignedVar != null) {
                model.addConstraint(
                    relation = LinearInequality(
                        lhs = LinearPolynomial(listOf(LinearMonomial(Flt64.one, assignedVar)), Flt64.zero),
                        rhs = LinearPolynomial(emptyList(), lowerBound.toFlt64()),
                        comparison = Comparison.GE
                    ),
                    name = "assigned_length_lower_bound_$demandIndex"
                )
            }

            // 上界约束: assigned_length_i <= upperBound
            val upperBound = config.assignedLengthUpperBound[productId]
            if (upperBound != null && assignedVar != null) {
                model.addConstraint(
                    relation = LinearInequality(
                        lhs = LinearPolynomial(listOf(LinearMonomial(Flt64.one, assignedVar)), Flt64.zero),
                        rhs = LinearPolynomial(emptyList(), upperBound.toFlt64()),
                        comparison = Comparison.LE
                    ),
                    name = "assigned_length_upper_bound_$demandIndex"
                )
            }

            // 超长上限约束: over_length_i <= overLengthUpperBound
            val overLengthUpperBound = config.overLengthUpperBound[productId]
            if (overLengthUpperBound != null && overVar != null) {
                model.addConstraint(
                    relation = LinearInequality(
                        lhs = LinearPolynomial(listOf(LinearMonomial(Flt64.one, overVar)), Flt64.zero),
                        rhs = LinearPolynomial(emptyList(), overLengthUpperBound.toFlt64()),
                        comparison = Comparison.LE
                    ),
                    name = "over_length_bound_$demandIndex"
                )
            }

            // 卷长-超长关联约束: assigned_length_i - over_length_i <= maxOverProduceLength
            val maxOverProduceLength = demand.product.maxOverProduceLength
            if (maxOverProduceLength != null && assignedVar != null && overVar != null) {
                model.addConstraint(
                    relation = LinearInequality(
                        lhs = LinearPolynomial(
                            monomials = listOf(
                                LinearMonomial(Flt64.one, assignedVar),
                                LinearMonomial(Flt64(-1.0), overVar)
                            ),
                            constant = Flt64.zero
                        ),
                        rhs = LinearPolynomial(emptyList(), maxOverProduceLength.value.toFlt64()),
                        comparison = Comparison.LE
                    ),
                    name = "assigned_over_length_link_$demandIndex"
                )
            }
        }

        return ok
    }
}
