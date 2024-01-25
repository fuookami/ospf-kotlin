package fuookami.ospf.kotlin.example.framework_demo.demo1.domain.bandwidth_context

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*
import fuookami.ospf.kotlin.framework.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo1.infrastructure.*
import fuookami.ospf.kotlin.example.framework_demo.demo1.domain.bandwidth_context.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo1.domain.bandwidth_context.service.*
import fuookami.ospf.kotlin.example.framework_demo.demo1.domain.route_context.*
import fuookami.ospf.kotlin.example.framework_demo.demo1.domain.route_context.model.*

class BandwidthContext(
    private val routeContext: RouteContext
) {
    lateinit var aggregation: Aggregation

    fun init(input: Input): Try {
        val routeAggregation = routeContext.aggregation

        val edgeBandwidth = EdgeBandwidth(
            routeAggregation.graph.edges,
            routeAggregation.services,
        )
        val serviceBandwidth = ServiceBandwidth(
            routeAggregation.graph,
            routeAggregation.services,
            edgeBandwidth
        )
        val nodeBandwidth = NodeBandwidth(
            routeAggregation.graph.nodes,
            routeAggregation.services,
            serviceBandwidth
        )
        aggregation = Aggregation(edgeBandwidth, serviceBandwidth, nodeBandwidth)
        return Ok(success)
    }

    fun register(model: LinearMetaModel): Try {
        return aggregation.register(model)
    }

    fun construct(model: LinearMetaModel): Try {
        val routeAggregation = routeContext.aggregation

        val generator = PipelineListGenerator(
            aggregation,
            routeAggregation.graph,
            routeAggregation.services,
            routeAggregation.assignment
        )
        when (val pipelinesRet = generator()) {
            is Failed -> {
                return Failed(pipelinesRet.error)
            }

            is Ok -> {
                val pipelines = pipelinesRet.value
                when (val result = pipelines(model)) {
                    is Failed -> {
                        return Failed(result.error)
                    }

                    is Ok -> {}
                }
            }
        }
        return Ok(success)
    }

    fun analyze(model: LinearMetaModel, result: List<Flt64>): Ret<List<List<Node>>> {
        val routeAggregation = routeContext.aggregation

        val analyzer = SolutionAnalyzer(
            routeAggregation.graph,
            routeAggregation.services,
            routeAggregation.assignment,
            aggregation
        )
        return analyzer(model, result)
    }
}
