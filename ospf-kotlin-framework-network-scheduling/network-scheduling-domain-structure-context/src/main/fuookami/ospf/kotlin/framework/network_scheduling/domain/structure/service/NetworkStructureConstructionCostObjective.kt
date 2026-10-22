package fuookami.ospf.kotlin.framework.network_scheduling.domain.structure.service

import fuookami.ospf.kotlin.utils.functional.Try
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMetaModel
import fuookami.ospf.kotlin.framework.model.Pipeline

/** 注册建设费用最小化目标。 / Register the construction-cost minimization objective.
 * @property name 目标名称 / Objective name
 */
class NetworkStructureConstructionCostObjective(
    private val cost: () -> LinearPolynomial<Flt64>,
    override val name: String
) : Pipeline<AbstractLinearMetaModel<Flt64>> {
    /** 将建设费用设为元模型目标。 / Set construction cost as a meta-model objective.
     * @param model 目标元模型 / Target meta model
     * @return 注册结果 / Registration result
     */
    override fun invoke(model: AbstractLinearMetaModel<Flt64>): Try {
        return model.minimize(cost(), name = name)
    }
}
