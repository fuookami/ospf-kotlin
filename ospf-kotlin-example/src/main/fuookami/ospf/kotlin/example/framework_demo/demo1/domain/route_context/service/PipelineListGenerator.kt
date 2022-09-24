package fuookami.ospf.kotlin.example.framework_demo.demo1.domain.route_context.service

import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*
import fuookami.ospf.kotlin.framework.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo1.domain.route_context.Aggregation
import fuookami.ospf.kotlin.example.framework_demo.demo1.domain.route_context.service.limits.*
import fuookami.ospf.kotlin.framework.model.Pipeline
import fuookami.ospf.kotlin.framework.model.PipelineList

class PipelineListGenerator(
    private val aggregation: Aggregation
) {
    operator fun invoke(): Result<PipelineList<LinearMetaModel>, Error> {
        val list = ArrayList<Pipeline<LinearMetaModel>>()

        list.add(NodeAssignmentConstraint(aggregation.graph.nodes, aggregation.assignment))
        list.add(ServiceAssignmentConstraint(aggregation.services, aggregation.assignment))

        list.add(ServiceCostObjective(aggregation.services, aggregation.assignment))

        return Ok(list)
    }
}
