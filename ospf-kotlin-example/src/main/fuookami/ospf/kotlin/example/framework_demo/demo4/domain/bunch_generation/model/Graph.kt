@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.example.framework_demo.demo4.domain.bunch_generation.model

import kotlin.time.Instant
import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.task.model.*

/** Node in the route graph for bunch generation. */
sealed class Node(val index: UInt64) {
    companion object {
        internal val root: UInt64 = UInt64.zero
        internal val end: UInt64 = UInt64.maximum
    }

    abstract val time: Instant
}

/** Root node representing the start of an aircraft's route. */
object RootNode : Node(root) {
    override val time = Instant.DISTANT_PAST
    override fun toString() = "Root"
}

/** End node representing the end of an aircraft's route. */
object EndNode : Node(end) {
    override val time = Instant.DISTANT_FUTURE
    override fun toString() = "End"
}

/** Task node representing a flight task in the route graph. */
class TaskNode(
    val task: FlightTask,
    override val time: Instant,
    index: UInt64
) : Node(index) {
    override fun toString() = task.toString()
}

/** Directed edge in the route graph. */
data class Edge(
    val from: Node,
    val to: Node
) {
    override fun toString() = "$from -> $to"
}

/** Route graph for bunch generation, containing nodes and directed edges. */
class Graph(
    val nodes: MutableMap<UInt64, Node> = HashMap(),
    val edges: MutableMap<Node, MutableSet<Edge>> = HashMap()
) {
    /** Adds a node to the graph. */
    fun put(node: Node) {
        nodes[node.index] = node
    }

    /** Adds a directed edge from one node to another. */
    fun put(from: Node, to: Node) {
        if (!edges.containsKey(from)) {
            edges[from] = HashSet()
        }
        edges[from]!!.add(Edge(from, to))
    }

    /** Gets a node by its index. */
    operator fun get(index: UInt64): Node? {
        return nodes[index]
    }

    /** Gets all outgoing edges from a node. */
    operator fun get(node: Node): Set<Edge> {
        return edges[node] ?: emptySet()
    }

    /** Checks if there is a directed edge from one node to another. */
    fun connected(from: Node, to: Node): Boolean {
        return edges[from]?.contains(Edge(from, to)) ?: false
    }
}
