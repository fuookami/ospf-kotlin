package fuookami.ospf.kotlin.framework.network_scheduling.domain.flow.service.limits

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMetaModel
import fuookami.ospf.kotlin.framework.model.Pipeline

/** 节点净流量守恒。 / Node net-flow conservation.
 * @property name 约束名称 / Constraint name
 */
class FlowConservationConstraint(
    private val balance: LinearPolynomial<Flt64>,
    private val net: Flt64,
    override val name: String
) : Pipeline<AbstractLinearMetaModel<Flt64>> {
    /** 注册节点流量守恒约束。 / Register the node flow-conservation constraint.
     * @param model 元模型 / Meta model
     * @return 注册结果 / Registration result
     */
    override fun invoke(model: AbstractLinearMetaModel<Flt64>): Try {
        return model.addConstraint(balance eq net, name = name)
    }
}
