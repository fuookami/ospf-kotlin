package fuookami.ospf.kotlin.example.framework_demo.demo1.domain.bandwidth_context.service.limits

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.*
import fuookami.ospf.kotlin.core.frontend.inequality.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*
import fuookami.ospf.kotlin.framework.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo1.domain.route_context.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo1.domain.bandwidth_context.model.*

fun Node.maxOutDegree(): UInt64 {
    var bandwidth = UInt64.zero
    edges.forEach { bandwidth += it.maxBandwidth }
    return bandwidth
}

class TransferNodeBandwidthConstraint(
    private val nodes: ArrayList<Node>,
    private val assignment: Assignment,
    private val nodeBandwidth: NodeBandwidth,
    override val name: String = "transfer_node_bandwidth_constraint"
) : Pipeline<LinearMetaModel> {
    override fun invoke(model: LinearMetaModel): Try {
        val assignment = assignment.nodeAssignment
        val outFlow = nodeBandwidth.outFlow
        for (node in nodes.filter(normal)) {
            model.addConstraint(
                node.maxOutDegree() * (UInt64.one - assignment[node]) + outFlow[node] leq node.maxOutDegree(),
                "${name}_$node"
            )
        }
        return Ok(success)
    }
}
