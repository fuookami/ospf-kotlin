package fuookami.ospf.kotlin.example.framework_demo.demo1.domain.bandwidth_context.service

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*
import fuookami.ospf.kotlin.example.framework_demo.demo1.domain.route_context.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo1.domain.bandwidth_context.*

private typealias NodeSolution = HashMap<Service, Node>
private typealias EdgeSolution = HashMap<Service, ArrayList<Pair<Edge, UInt64>>>

class SolutionAnalyzer(
    private val graph: Graph,
    private val services: List<Service>,
    private val assignment: Assignment,
    private val aggregation: Aggregation
) {
    operator fun invoke(model: LinearMetaModel, result: List<Flt64>): Ret<List<List<Node>>> {
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
