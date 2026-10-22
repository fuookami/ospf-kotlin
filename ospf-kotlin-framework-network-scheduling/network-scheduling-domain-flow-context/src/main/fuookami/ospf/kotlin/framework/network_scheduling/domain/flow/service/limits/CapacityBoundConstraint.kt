package fuookami.ospf.kotlin.framework.network_scheduling.domain.flow.service.limits

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMetaModel
import fuookami.ospf.kotlin.framework.model.Pipeline

/** 资源容量上下界。 / Resource capacity bounds.
 * @property name 约束名称 / Constraint name
 */
class CapacityBoundConstraint(
    private val expression: LinearPolynomial<Flt64>,
    private val lower: Flt64,
    private val upper: Flt64,
    override val name: String
) : Pipeline<AbstractLinearMetaModel<Flt64>> {
    /** 注册资源容量上下界。 / Register resource capacity bounds.
     * @param model 元模型 / Meta model
     * @return 注册结果 / Registration result
     */
    override fun invoke(model: AbstractLinearMetaModel<Flt64>): Try {
        return run(
            { model.addConstraint(expression geq lower, name = "${name}_lower") },
            { model.addConstraint(expression leq upper, name = "${name}_upper") }
        )
    }
}
