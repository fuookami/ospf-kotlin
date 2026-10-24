package fuookami.ospf.kotlin.example.framework_demo.demo1.bandwidth_context.service

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.example.framework_demo.demo1.bandwidth_context.*
import fuookami.ospf.kotlin.example.framework_demo.demo1.route_context.model.*

/**
 * Mapping from service to its assigned node.
 * 从服务到其分配节点的映射类型别名。
*/
private typealias NodeSolution = HashMap<Service, Node>

/**
 * Mapping from service to its edge bandwidth allocation.
 * 从服务到其边带宽分配的映射类型别名。
*/
private typealias EdgeSolution = HashMap<Service, ArrayList<Pair<Edge, UInt64>>>

/**
 * Extracts service paths from the solved model by tracing assignment and bandwidth variables using DFS.
 * 使用 DFS 通过跟踪分配和带宽变量从求解模型中提取服务路径。
 *
 * @property graph 路由网络图 / the route network graph
 * @property services 服务列表 / the list of services
 * @property assignment 服务到节点的分配模型 / the service-to-node assignment model
 * @property aggregation 带宽聚合模型 / the bandwidth aggregation model
*/
class SolutionAnalyzer(
    private val graph: Graph,
    private val services: List<Service>,
    private val assignment: Assignment,
    private val aggregation: Aggregation
) {
    operator fun invoke(model: LinearMetaModel<Flt64>, result: List<Flt64>): Ret<List<List<Node>>> {
        val nodeSolution = NodeSolution()
        val edgeSolution = EdgeSolution()
        val servicesByIndex = services.groupBy { it.index }.mapValues { it.value.first() }
        val nodesByIndex = graph.nodes.groupBy { it.index }.mapValues { it.value.first() }
        val edgesByIndex = graph.edges.groupBy { it.index }.mapValues { it.value.first() }

        for (token in model.tokens.tokens) {
            if (token.variable.belongsTo(assignment.x)) {
                if (result[token.solverIndex] eq Flt64.one) {
                    val vector = token.variable.vectorView
                    nodeSolution[servicesByIndex[vector[1]]!!] = nodesByIndex[vector[0]]!!
                }
            }
        }
        for (token in model.tokens.tokens) {
            if (token.variable.belongsTo(aggregation.edgeBandwidth.y)) {
                if (result[token.solverIndex] gr Flt64.zero) {
                    val vector = token.variable.vectorView
                    val service = services[vector[1]]
                    if (edgeSolution.containsKey(service)) {
                        edgeSolution[service]!!.add(
                            Pair(
                                edgesByIndex[vector[0]]!!,
                                result[token.solverIndex].toUInt64()
                            )
                        )
                    } else {
                        edgeSolution[service] = arrayListOf(
                            Pair(
                                edgesByIndex[vector[0]]!!,
                                result[token.solverIndex].toUInt64()
                            )
                        )
                    }
                }
            }
        }

        return Ok(dump(nodeSolution, edgeSolution))
    }

    /**
     * Reconstructs service paths from the node and edge solutions by tracing edges from each assigned node.
     * 通过从每个分配节点追踪边，从节点和边解中重建服务路径。
     *
     * @param nodeSolution 服务到其分配节点的映射 / the mapping from service to its assigned node
     * @param edgeSolution 服务到其边分配的映射 / the mapping from service to its edge allocations
     * @return 服务路径列表 / the list of service paths
    */
    private fun dump(nodeSolution: NodeSolution, edgeSolution: EdgeSolution): List<List<Node>> {
        val links = ArrayList<List<Node>>()
        for (pair in nodeSolution) {
            val service = pair.key
            val firstNode = pair.value
            if (edgeSolution.containsKey(service)) {
                appendServicePaths(
                    firstNode = firstNode,
                    edges = edgeSolution[service]!!,
                    links = links
                )
            }
        }
        return links
    }
}
