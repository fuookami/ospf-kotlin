package fuookami.ospf.kotlin.example.framework_demo.demo1.bandwidth_context.service.limits

import fuookami.ospf.kotlin.example.framework_demo.demo1.bandwidth_context.model.NodeBandwidth
import fuookami.ospf.kotlin.example.framework_demo.demo1.route_context.model.Assignment
import fuookami.ospf.kotlin.example.framework_demo.demo1.route_context.model.Node
import fuookami.ospf.kotlin.example.framework_demo.demo1.route_context.model.normal

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

/** Computes the maximum outgoing bandwidth capacity of a node by summing its edge capacities. */
fun Node.maxOutDegree(): UInt64 {
    var bandwidth = UInt64.zero
    edges.forEach { bandwidth += it.maxBandwidth }
    return bandwidth
}

/** Constrains total node out-flow to the node's capacity when the node is assigned. */
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
