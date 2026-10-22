package fuookami.ospf.kotlin.framework.network_scheduling.domain.flow.service

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMetaModel
import fuookami.ospf.kotlin.framework.model.Pipeline

/** 运行费用目标。 / Operating cost objective.
 * @property name 目标名称 / Objective name
 */
class MinCostFlowObjective(
    private val cost: LinearPolynomial<Flt64>,
    override val name: String
) : Pipeline<AbstractLinearMetaModel<Flt64>> {
    /** 注册运行费用目标。 / Register the operating-cost objective.
     * @param model 元模型 / Meta model
     * @return 注册结果 / Registration result
     */
    override fun invoke(model: AbstractLinearMetaModel<Flt64>): Try {
        return model.minimize(cost, name = name)
    }
}
