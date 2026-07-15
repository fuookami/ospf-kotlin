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
 * @property graph the route network graph / 路由网络图
 * @property services the list of services / 服务列表
 * @property assignment the service-to-node assignment model / 服务到节点的分配模型
 * @property aggregation the bandwidth aggregation model / 带宽聚合模型
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

        for (token in model.tokens.tokens) {
            if (token.variable.belongsTo(assignment.x)) {
                if (result[token.solverIndex] eq Flt64.one) {
                    val vector = token.variable.vectorView
                    nodeSolution[services.find { it.index == vector[1] }!!] =
                        graph.nodes.find { it.index == vector[0] }!!
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
                                graph.edges.find { it.index == vector[0] }!!,
                                result[token.solverIndex].toUInt64()
                            )
                        )
                    } else {
                        edgeSolution[service] = arrayListOf(
                            Pair(
                                graph.edges.find { it.index == vector[0] }!!,
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
     * @param nodeSolution the mapping from service to its assigned node / 服务到其分配节点的映射
     * @param edgeSolution the mapping from service to its edge allocations / 服务到其边分配的映射
     * @return the list of service paths / 服务路径列表
    */
    private fun dump(nodeSolution: NodeSolution, edgeSolution: EdgeSolution): List<List<Node>> {
        val links = ArrayList<List<Node>>()
        for (pair in nodeSolution) {
            val service = pair.key
            val firstNode = pair.value
            if (edgeSolution.containsKey(service)) {
                val thisEdges = edgeSolution[service]!!

                for (edge in thisEdges) {
                    val link = ArrayList<Node>()
                    if (edge.first.from == firstNode) {
                        link.add(firstNode)
                        link.add(edge.first.to)
                        findLink(thisEdges, edge.first.to, link, links)
                    }
                }
            }
        }
        return links
    }

    /**
     * Recursively traces edges using DFS to build service paths from the current node to client nodes.
     * 使用 DFS 递归追踪边，从当前节点到客户端节点构建服务路径。
     *
     * @param edges the list of edge-bandwidth pairs for the current service / 当前服务的边-带宽对列表
     * @param currentNode the node being visited / 当前访问的节点
     * @param currentLike the path accumulated so far / 目前累积的路径
     * @param links the mutable list collecting all completed paths / 收集所有完整路径的可变列表
    */
    // DFS
    private fun findLink(
        edges: List<Pair<Edge, UInt64>>,
        currentNode: Node,
        currentLike: List<Node>,
        links: MutableList<List<Node>>
    ) {
        if (currentNode is ClientNode) {
            links.add(currentLike)
        } else {
            for (edge in edges) {
                if (edge.first.from == currentNode
                    && !currentLike.contains(edge.first.to)
                ) {
                    val link = currentLike.toMutableList()
                    link.add(edge.first.to)
                    findLink(edges, edge.first.to, link, links)
                }
            }
        }
    }
}
