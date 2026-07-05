package fuookami.ospf.kotlin.example.framework_demo.demo1.route_context

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.framework.model.invoke
import fuookami.ospf.kotlin.example.framework_demo.demo1.infrastructure.*
import fuookami.ospf.kotlin.example.framework_demo.demo1.route_context.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo1.route_context.service.PipelineListGenerator

/** 从输入数据构建路由网络图并管理路由相关模型构建。Builds the route network graph from input data and manages route-related model construction. */
class RouteContext {
    lateinit var aggregation: Aggregation

    /**
     * 
     * @param input 输入数据。
     * @return 执行结果。
     */
    fun init(input: Input): Try {
        var totalDemand = UInt64(0U)
        for (node in input.clientNodes) {
            totalDemand += node.demand
        }

        val services: ArrayList<Service> = arrayListOf()
        for (i in 0 until input.normalNodeAmount.toString().toInt() / 2) {
            services.add(Service(UInt64(i), totalDemand, input.serviceCost))
        }

        val nodes: ArrayList<Node> = ArrayList()
        for (i in 0 until input.normalNodeAmount.toString().toInt()) {
            nodes.add(NormalNode(UInt64(i)))
        }
        for (i in 0 until input.clientNodes.size) {
            val node = input.clientNodes[i]
            nodes.add(ClientNode(node.id, node.demand))
        }

        val edges: ArrayList<Edge> = ArrayList()
        for (i in 0 until input.edges.size) {
            val edge = input.edges[i]
            edges.add(
                Edge(
                    nodes[edge.fromNodeId.toString().toInt()],
                    nodes[edge.toNodeId.toString().toInt()],
                    edge.maxBandwidth,
                    edge.costPerBandwidth
                )
            )
            edges.add(
                Edge(
                    nodes[edge.toNodeId.toString().toInt()],
                    nodes[edge.fromNodeId.toString().toInt()],
                    edge.maxBandwidth,
                    edge.costPerBandwidth
                )
            )
            nodes[edge.fromNodeId.toString().toInt()].add(edges[i * 2])
            nodes[edge.toNodeId.toString().toInt()].add(edges[i * 2 + 1])
        }

        for (i in 0 until input.clientNodes.size) {
            val node = input.clientNodes[i]
            edges.add(
                Edge(
                    nodes[node.normalNodeId.toString().toInt()],
                    nodes[(input.normalNodeAmount + node.id).toString().toInt()],
                    node.demand,
                    UInt64(0U)
                )
            )
            nodes[node.normalNodeId.toString().toInt()].add(edges[input.edges.size * 2 + i])
        }

        val graph = Graph(nodes, edges)
        aggregation = Aggregation(graph, services, Assignment(graph.nodes, services))
        return ok
    }

    /**
     * 将路由相关决策变量与约束注册到模型中。
     * Registers route-related decision variables and constraints into the model.
     *
     * @param model 线性元模型。
     * @return 执行结果。
     */
    fun register(model: LinearMetaModel<Flt64>): Try {
        return aggregation.register(model)
    }

    /**
     * 构建路由模型约束与流水线。
     * Constructs route model constraints and pipelines.
     *
     * @param model 线性元模型。
     * @return 执行结果。
     */
    fun construct(model: LinearMetaModel<Flt64>): Try {
        val generator = PipelineListGenerator(aggregation)
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
}
