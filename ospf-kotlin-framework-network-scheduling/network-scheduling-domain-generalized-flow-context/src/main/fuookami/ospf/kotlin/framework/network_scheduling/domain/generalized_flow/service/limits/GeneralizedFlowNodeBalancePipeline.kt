package fuookami.ospf.kotlin.framework.network_scheduling.domain.generalized_flow.service.limits

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMetaModel
import fuookami.ospf.kotlin.framework.model.Pipeline
import fuookami.ospf.kotlin.framework.network_scheduling.infrastructure.NetworkNodeId
import fuookami.ospf.kotlin.framework.network_scheduling.infrastructure.networkSchedulingFailure

/** 应用“流出投入 - 流入产出 = 节点净供给”的广义平衡约束。 / Apply generalized balance: outgoing input minus incoming output equals node net supply.
 *
 * @property name 管线及约束名称前缀 / Pipeline and constraint-name prefix
 */
class GeneralizedFlowNodeBalancePipeline internal constructor(
    private val balances: Map<NetworkNodeId, LinearPolynomial<Flt64>>,
    private val netSupplies: Map<NetworkNodeId, Flt64>,
    override val name: String
) : Pipeline<AbstractLinearMetaModel<Flt64>> {
    /** 注册节点广义平衡约束。 / Register generalized node-balance constraints.
     *
     * @param model 元模型 / Meta model
     * @return 注册结果 / Registration result
     */
    override fun invoke(model: AbstractLinearMetaModel<Flt64>): Try {
        for ((nodeId, expression) in balances) {
            val netSupply = netSupplies[nodeId]
                ?: return networkSchedulingFailure("广义流节点供需缺失：$nodeId / Generalized-flow node balance is missing: $nodeId")
            when (val result = model.addConstraint(
                expression eq netSupply,
                name = "${name}_balance_${nodeId.value}"
            )) {
                is Ok -> {}
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
        }
        return ok
    }
}
