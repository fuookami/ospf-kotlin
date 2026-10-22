package fuookami.ospf.kotlin.framework.network_scheduling.domain.generalized_flow.service.limits

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMetaModel
import fuookami.ospf.kotlin.framework.model.Pipeline
import fuookami.ospf.kotlin.framework.network_scheduling.infrastructure.NetworkArcId
import fuookami.ospf.kotlin.framework.network_scheduling.infrastructure.networkSchedulingFailure

/** 对弧起点投入量施加硬容量界。 / Enforce hard capacity bounds on origin-side arc input.
 *
 * @property name 管线及约束名称前缀 / Pipeline and constraint-name prefix
 */
class GeneralizedFlowInputCapacityPipeline internal constructor(
    private val inputs: Map<NetworkArcId, LinearPolynomial<Flt64>>,
    private val bounds: Map<NetworkArcId, Pair<Flt64, Flt64>>,
    override val name: String
) : Pipeline<AbstractLinearMetaModel<Flt64>> {
    /** 注册每条弧的投入下界和有限上界。 / Register input lower and finite upper bounds for every arc.
     *
     * @param model 元模型 / Meta model
     * @return 注册结果 / Registration result
     */
    override fun invoke(model: AbstractLinearMetaModel<Flt64>): Try {
        for ((arcId, expression) in inputs) {
            val bound = bounds[arcId]
                ?: return networkSchedulingFailure("广义流弧容量缺失：$arcId / Generalized-flow arc capacity is missing: $arcId")
            when (val lower = model.addConstraint(
                expression geq bound.first,
                name = "${name}_${arcId.value}_input_lower"
            )) {
                is Ok -> {}
                is Failed -> return Failed(lower.error)
                is Fatal -> return Fatal(lower.errors)
            }
            when (val upper = model.addConstraint(
                expression leq bound.second,
                name = "${name}_${arcId.value}_input_upper"
            )) {
                is Ok -> {}
                is Failed -> return Failed(upper.error)
                is Fatal -> return Fatal(upper.errors)
            }
        }
        return ok
    }
}
