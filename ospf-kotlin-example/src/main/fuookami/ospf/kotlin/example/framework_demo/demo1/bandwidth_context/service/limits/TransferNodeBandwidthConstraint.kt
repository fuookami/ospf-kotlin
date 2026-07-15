package fuookami.ospf.kotlin.example.framework_demo.demo1.bandwidth_context.service.limits

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.math.symbol.operation.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.framework.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo1.bandwidth_context.model.NodeBandwidth
import fuookami.ospf.kotlin.example.framework_demo.demo1.route_context.model.*

/**
 * Computes the maximum outgoing bandwidth capacity of a node by summing its edge capacities.
 * 通过求和边容量计算节点的最大出带宽容量。
 *
 * @receiver Node the node to compute capacity for / 要计算容量的节点
 * @return the maximum outgoing bandwidth capacity / 最大出带宽容量
*/
fun Node.maxOutDegree(): UInt64 {
    var bandwidth = UInt64.zero
    edges.forEach { bandwidth += it.maxBandwidth }
    return bandwidth
}

/**
 * Constrains total node out-flow to the node's capacity when the node is assigned.
 * 约束总节点流出到节点容量（当节点被分配时）。
 *
 * @property nodes the list of network nodes / 网络节点列表
 * @property assignment the service-to-node assignment model / 服务到节点的分配模型
 * @property nodeBandwidth the node bandwidth model / 节点带宽模型
*/
class TransferNodeBandwidthConstraint(
    private val nodes: ArrayList<Node>,
    private val assignment: Assignment,
    private val nodeBandwidth: NodeBandwidth,
    override val name: String = "transfer_node_bandwidth_constraint"
) : Pipeline<LinearMetaModel<Flt64>> {
    override fun invoke(model: LinearMetaModel<Flt64>): Try {
        val assignment = assignment.nodeAssignment
        val outFlow = nodeBandwidth.outFlow
        for (node in nodes.filter(normal)) {
            model.addConstraint(
                node.maxOutDegree().toFlt64() * (UInt64.one - assignment[node]) + LinearPolynomial(outFlow[node]) leq node.maxOutDegree().toFlt64(),
                name = "${name}_$node"
            )
        }
        return ok
    }
}
