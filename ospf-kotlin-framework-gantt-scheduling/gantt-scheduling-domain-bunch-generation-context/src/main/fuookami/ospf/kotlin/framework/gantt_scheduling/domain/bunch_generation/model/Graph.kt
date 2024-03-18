package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.bunch_generation.model

import kotlinx.datetime.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.*

sealed class Node(val index: UInt64) {
    companion object {
        internal val root: UInt64 = UInt64.zero
        internal val end: UInt64 = UInt64.maximum
    }

    abstract val time: Instant
}

object RootNode : Node(root) {
    override val time = Instant.DISTANT_PAST

    override fun toString() = "Root"
}

object EndNode : Node(end) {
    override val time = Instant.DISTANT_FUTURE

    override fun toString() = "End"
}

class TaskNode<E : Executor, Arg>(
    val task: Task<E>,
    val arg: Arg,
    override val time: Instant,
    index: UInt64
) : Node(index) {
    override fun toString() = task.toString()
}

data class Edge(
    val from: Node,
    val to: Node
) {
    override fun toString() = "$from -> $to"
}

class Graph(
    private val _nodes: MutableMap<UInt64, Node> = HashMap(),
    private val _edges: MutableMap<Node, MutableSet<Edge>> = HashMap()
) {
    val nodes: Map<UInt64, Node> by ::_nodes
    val edges: Map<Node, Set<Edge>> by ::_edges

    fun put(node: Node) {
        _nodes[node.index] = node
    }

    fun put(from: Node, to: Node) {
        if (!_edges.containsKey(from)) {
            _edges[from] = HashSet()
        }
        _edges[from]!!.add(Edge(from, to))
    }

    operator fun get(index: UInt64): Node? {
        return nodes[index]
    }

    operator fun get(node: Node): Set<Edge> {
        return edges[node] ?: emptySet()
    }

    fun connected(from: Node, to: Node): Boolean {
        return edges[from]?.contains(Edge(from, to)) ?: false
    }

    fun reverse(): Graph {
        val nodes = this._nodes.toMutableMap()
        val edges = HashMap<Node, MutableSet<Edge>>()
        for ((from, thisEdges) in this.edges) {
            for (edge in thisEdges) {
                val to = edge.to

                val rfrom = if (to is EndNode) {
                    RootNode
                } else {
                    to
                }
                val rto = if (from is RootNode) {
                    EndNode
                } else {
                    from
                }

                if (!edges.containsKey(rto)) {
                    edges[rto] = HashSet()
                }
                edges[rto]!!.add(Edge(rfrom, rto))
            }
        }
        return Graph(nodes, edges)
    }
}
