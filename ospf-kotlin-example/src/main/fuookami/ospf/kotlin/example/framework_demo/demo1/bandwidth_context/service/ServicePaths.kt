package fuookami.ospf.kotlin.example.framework_demo.demo1.bandwidth_context.service

import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.example.framework_demo.demo1.route_context.model.*

internal fun appendServicePaths(
    firstNode: Node,
    edges: List<Pair<Edge, UInt64>>,
    links: MutableList<List<Node>>
) {
    val outgoing = edges.groupBy { it.first.from }
    val path = ArrayList<Node>()
    val visited = HashSet<Node>()

    fun visit(node: Node) {
        if (node is ClientNode) {
            links.add(path.toList())
            return
        }
        for ((edge, _) in outgoing[node] ?: emptyList()) {
            if (visited.add(edge.to)) {
                path.add(edge.to)
                visit(edge.to)
                path.removeAt(path.lastIndex)
                visited.remove(edge.to)
            }
        }
    }

    for ((edge, _) in outgoing[firstNode] ?: emptyList()) {
        path.add(firstNode)
        path.add(edge.to)
        visited.add(firstNode)
        visited.add(edge.to)
        visit(edge.to)
        path.clear()
        visited.clear()
    }
}
