package fuookami.ospf.kotlin.example.framework_demo.demo1.route_context.service.limits

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.framework.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo1.route_context.model.*

/**
 * Constrains each normal node to be assigned to at most one service.
 * 约束每个普通节点最多分配一个服务。
 *
 * @property nodes the list of network nodes / 网络节点列表
 * @property assignment the service-to-node assignment model / 服务到节点的分配模型
*/
class NodeAssignmentConstraint(
    private val nodes: List<Node>,
    private val assignment: Assignment,
    override val name: String = "node_assignment"
) : Pipeline<LinearMetaModel<Flt64>> {
    override fun invoke(model: LinearMetaModel<Flt64>): Try {
        for (node in nodes.filter(normal)) {
            model.addConstraint(
                assignment.nodeAssignment[node] leq 1,
                name = "${name}_$node"
            )
        }
        return ok
    }
}
