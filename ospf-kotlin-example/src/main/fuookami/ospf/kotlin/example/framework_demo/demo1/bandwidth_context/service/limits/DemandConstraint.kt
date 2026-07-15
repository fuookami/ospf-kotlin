package fuookami.ospf.kotlin.example.framework_demo.demo1.bandwidth_context.service.limits

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.framework.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo1.bandwidth_context.model.NodeBandwidth
import fuookami.ospf.kotlin.example.framework_demo.demo1.route_context.model.*

/**
 * Ensures each client node receives at least its required bandwidth demand.
 * 确保每个客户端节点至少接收其所需的带宽需求。
 *
 * @property nodes the list of network nodes / 网络节点列表
 * @property nodeBandwidth the node bandwidth model / 节点带宽模型
*/
class DemandConstraint(
    private val nodes: List<Node>,
    private val nodeBandwidth: NodeBandwidth,
    override val name: String = "demand_constraint"
) : Pipeline<LinearMetaModel<Flt64>> {
    override fun invoke(model: LinearMetaModel<Flt64>): Try {
        for (node in nodes.filter(client)) {
            model.addConstraint(
                nodeBandwidth.inDegree[node] geq (node as ClientNode).demand,
                name = "${name}_$node"
            )
        }
        return ok
    }
}
