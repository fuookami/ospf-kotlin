package fuookami.ospf.kotlin.example.framework_demo.demo1.route_context.service.limits

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.framework.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo1.route_context.model.Assignment
import fuookami.ospf.kotlin.example.framework_demo.demo1.route_context.model.Node
import fuookami.ospf.kotlin.example.framework_demo.demo1.route_context.model.normal

class NodeAssignmentConstraint(
    private val nodes: List<Node>,
    private val assignment: Assignment,
    override val name: String = "node_assignment"
) : Pipeline<LinearMetaModelFlt64> {
    override fun invoke(model: LinearMetaModelFlt64): Try {
        for (node in nodes.filter(normal)) {
            model.addConstraint(
                assignment.nodeAssignment[node] leq 1,
                name = "${name}_$node"
            )
        }
        return ok
    }
}

