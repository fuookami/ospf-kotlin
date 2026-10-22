package fuookami.ospf.kotlin.framework.network_scheduling.domain.design.service.limits

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMetaModel
import fuookami.ospf.kotlin.framework.model.Pipeline

/** 由结构决策启用的处理/服务容量约束族。 / Processing/service capacity gated by structural decisions.
 * @property load 已归一化资源占用 / Normalized resource load
 * @property lower 结构相关下界 / Structure-dependent lower bound
 * @property upper 有限的结构相关上界 / Finite structure-dependent upper bound
 * @property name 约束命名空间 / Constraint namespace
 */
class NetworkCapacityConstraint(
    private val load: LinearPolynomial<Flt64>,
    private val lower: LinearPolynomial<Flt64>,
    private val upper: LinearPolynomial<Flt64>,
    override val name: String
) : Pipeline<AbstractLinearMetaModel<Flt64>> {
    /** 注册结构相关容量上下界。 / Register structure-dependent capacity bounds.
     * @param model 元模型 / Meta model
     * @return 注册结果 / Registration result
     */
    override fun invoke(model: AbstractLinearMetaModel<Flt64>): Try {
        return run(
            { model.addConstraint(load geq lower, name = "${name}_lower") },
            { model.addConstraint(load leq upper, name = "${name}_upper") }
        )
    }
}
