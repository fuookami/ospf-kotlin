package fuookami.ospf.kotlin.example.framework_demo.demo1.bandwidth_context.service

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.framework.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo1.bandwidth_context.Aggregation
import fuookami.ospf.kotlin.example.framework_demo.demo1.bandwidth_context.service.limits.*
import fuookami.ospf.kotlin.example.framework_demo.demo1.route_context.model.*

/**
 * 生成用于模型构建的带宽约束和成本目标的管线列表。Generates the pipeline list of bandwidth constraints and cost objective for model construction.
 *
 * @property aggregation 参数。
 * @property graph 参数。
 * @property services 参数。
 * @property assignment 参数。
 */
class PipelineListGenerator(
    private val aggregation: Aggregation,
    private val graph: Graph,
    private val services: List<Service>,
    private val assignment: Assignment,
) {
    operator fun invoke(): Ret<PipelineList<LinearMetaModel<Flt64>>> {
        val list = ArrayList<Pipeline<LinearMetaModel<Flt64>>>()

        list.add(EdgeBandwidthConstraint(graph.edges, services, assignment, aggregation.edgeBandwidth))
        list.add(DemandConstraint(graph.nodes, aggregation.nodeBandwidth))
        list.add(ServiceCapacityConstraint(graph.nodes, services, assignment, aggregation.serviceBandwidth))

        list.add(BandwidthCostObjective(graph.edges, aggregation.edgeBandwidth))

        return Ok(list)
    }
}
