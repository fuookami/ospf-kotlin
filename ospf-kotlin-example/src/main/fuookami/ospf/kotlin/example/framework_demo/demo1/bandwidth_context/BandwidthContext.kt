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
 * Manages bandwidth allocation context, building bandwidth models from the route graph and analyzing solutions.
 * 表示管理带宽分配上下文，从路由图构建带宽模型并分析解。
 *
 * @property routeContext the route context / 路由上下文
 */
class BandwidthContext(
    private val routeContext: RouteContext
) {
    lateinit var aggregation: Aggregation

    /**
     * Initializes the bandwidth context by creating edge, service, and node bandwidth models from the route graph.
     * 表示通过从路由图创建边、服务和节点带宽模型来初始化带宽上下文。
     *
     * @param input the aggregated input data / 聚合输入数据
     * @return the initialization result / 初始化结果
     */
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

    /**
     * Registers the bandwidth aggregation models with the optimization model.
     * 表示将带宽聚合模型注册到优化模型。
     *
     * @param model the linear meta model / 线性元模型
     * @return the registration result / 注册结果
     */
    fun register(model: LinearMetaModel<Flt64>): Try {
        return aggregation.register(model)
    }

    /**
     * Constructs the bandwidth model constraints and objectives using the pipeline list generator.
     * 表示使用管线列表生成器构建带宽模型约束和目标。
     *
     * @param model the linear meta model / 线性元模型
     * @return the construction result / 构建结果
     */
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

    /**
     * Analyzes the solved model to extract service paths as lists of nodes.
     * 表示分析求解模型以提取服务路径（节点列表）。
     *
     * @param model the solved linear meta model / 已求解的线性元模型
     * @param result the solution values / 求解值列表
     * @return the list of service paths / 服务路径列表
     */
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
