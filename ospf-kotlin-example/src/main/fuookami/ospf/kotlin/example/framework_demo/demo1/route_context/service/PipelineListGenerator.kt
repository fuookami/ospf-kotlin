package fuookami.ospf.kotlin.example.framework_demo.demo1.route_context.service

import fuookami.ospf.kotlin.example.framework_demo.demo1.route_context.Aggregation
import fuookami.ospf.kotlin.example.framework_demo.demo1.route_context.service.limits.NodeAssignmentConstraint
import fuookami.ospf.kotlin.example.framework_demo.demo1.route_context.service.limits.ServiceAssignmentConstraint
import fuookami.ospf.kotlin.example.framework_demo.demo1.route_context.service.limits.ServiceCostObjective

import fuookami.ospf.kotlin.utils.functional.*

import fuookami.ospf.kotlin.math.algebra.number.Flt64

import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.token.*

import fuookami.ospf.kotlin.framework.model.*

/** Generates the pipeline list of route constraints and objectives for model construction. */
class PipelineListGenerator(
    private val aggregation: Aggregation
) {
    operator fun invoke(): Ret<PipelineList<LinearMetaModel<Flt64>>> {
        val list = ArrayList<Pipeline<LinearMetaModel<Flt64>>>()

        list.add(NodeAssignmentConstraint(aggregation.graph.nodes, aggregation.assignment))
        list.add(ServiceAssignmentConstraint(aggregation.services, aggregation.assignment))

        list.add(ServiceCostObjective(aggregation.services, aggregation.assignment))

        return Ok(list)
    }
}
