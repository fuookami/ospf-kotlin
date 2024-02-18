package fuookami.ospf.kotlin.example.framework_demo.demo1.domain.bandwidth_context.service.limits

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.inequality.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*
import fuookami.ospf.kotlin.framework.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo1.domain.route_context.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo1.domain.bandwidth_context.model.*

class DemandConstraint(
    private val nodes: List<Node>,
    private val nodeBandwidth: NodeBandwidth,
    override val name: String = "demand_constraint"
) : Pipeline<LinearMetaModel> {
    override fun invoke(model: LinearMetaModel): Try {
        for (node in nodes.filter(client)) {
            model.addConstraint(
                nodeBandwidth.inDegree[node] geq (node as ClientNode).demand,
                "${name}_$node"
            )
        }
        return Ok(success)
    }
}
