package fuookami.ospf.kotlin.framework.network_scheduling.domain.generalized_flow.model

import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.quantities.unit.NoneUnit
import fuookami.ospf.kotlin.quantities.unit.PhysicalUnit
import fuookami.ospf.kotlin.framework.network_scheduling.infrastructure.*

/** 不含 solver 注册态的单商品广义流问题。 / Single-commodity generalized-flow problem without solver registration state.
 *
 * @property graph 通用有向网络图 / Generic directed network graph
 * @property nodes 节点单位与硬供需 / Node units and hard balances
 * @property arcs 单输入服务弧 / Single-input service arcs
 * @property zero 领域数值零 / Domain numeric zero
 * @property costUnit 总费用单位 / Total-cost unit
 */
class GeneralizedFlowProblem<V : RealNumber<V>>(
    val graph: NetworkGraph<NetworkNode<V>, NetworkArc<V>>,
    nodes: List<GeneralizedFlowNode<V>>,
    arcs: List<GeneralizedFlowArc<V>>,
    val zero: V,
    val costUnit: PhysicalUnit = NoneUnit
) {
    val nodes: List<GeneralizedFlowNode<V>> = nodes.toList()
    val arcs: List<GeneralizedFlowArc<V>> = arcs.toList()
}
