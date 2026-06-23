package fuookami.ospf.kotlin.example.framework_demo.demo1.bandwidth_context

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.framework.model.invoke
import fuookami.ospf.kotlin.example.framework_demo.demo1.bandwidth_context.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo1.bandwidth_context.service.PipelineListGenerator
import fuookami.ospf.kotlin.example.framework_demo.demo1.bandwidth_context.service.SolutionAnalyzer
import fuookami.ospf.kotlin.example.framework_demo.demo1.infrastructure.*
import fuookami.ospf.kotlin.example.framework_demo.demo1.route_context.RouteContext
import fuookami.ospf.kotlin.example.framework_demo.demo1.route_context.model.Node

/**
 * 管理带宽分配上下文，从路由图构建带宽模型并分析解。Manages bandwidth allocation context, building bandwidth models from the route graph and analyzing solutions.
 *
 * @property routeContext 路由上下文 / Route context
 */
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
        return ok
    }

    fun register(model: LinearMetaModel<Flt64>): Try {
        return aggregation.register(model)
    }

    fun construct(model: LinearMetaModel<Flt64>): Try {
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

            is Fatal -> {
                return Fatal(pipelinesRet.errors)
            }

            is Ok -> {
                val pipelines = pipelinesRet.value
                when (val result = pipelines(model)) {
                    is Failed -> {
                        return Failed(result.error)
                    }

                    is Fatal -> {
                        return Fatal(result.errors)
                    }

                    is Ok -> {}
                }
            }
        }
        return ok
    }

    fun analyze(model: LinearMetaModel<Flt64>, result: List<Flt64>): Ret<List<List<Node>>> {
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
