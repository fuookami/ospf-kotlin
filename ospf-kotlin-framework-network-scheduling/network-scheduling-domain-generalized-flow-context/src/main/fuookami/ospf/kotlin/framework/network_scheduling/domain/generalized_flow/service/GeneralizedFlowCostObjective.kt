package fuookami.ospf.kotlin.framework.network_scheduling.domain.generalized_flow.service

import fuookami.ospf.kotlin.utils.functional.Try
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMetaModel
import fuookami.ospf.kotlin.framework.model.Pipeline

/** 按弧投入量资费最小化总费用。 / Minimize total tariff charged on arc input.
 *
 * @property name 目标名称 / Objective name
 */
class GeneralizedFlowCostObjective(
    private val cost: LinearPolynomial<Flt64>,
    override val name: String
) : Pipeline<AbstractLinearMetaModel<Flt64>> {
    /** 注册总费用目标。 / Register total-cost objective.
     *
     * @param model 元模型 / Meta model
     * @return 注册结果 / Registration result
     */
    override fun invoke(model: AbstractLinearMetaModel<Flt64>): Try {
        return model.minimize(cost, name = name)
    }
}
