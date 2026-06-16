package fuookami.ospf.kotlin.example.framework_demo.demo1.route_context.model

import fuookami.ospf.kotlin.utils.concept.*
import fuookami.ospf.kotlin.utils.functional.*

import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.math.algebra.number.*

/** Base class for network nodes, identified by a unique ID and connected to edges. */
sealed class Node(
    val id: UInt64
) : AutoIndexed(Node::class) {
    val edges: MutableList<Edge> = ArrayList()

    fun add(edge: Edge) {
        edges.add(edge)
    }
}

/** A transit node in the network that can carry service traffic. */
class NormalNode(
    id: UInt64
) : Node(id) {
    override fun toString() = "N$id"
}

/** A terminal node that consumes bandwidth from the network with a specific demand. */
class ClientNode(
    id: UInt64,
    val demand: UInt64
) : Node(id) {
    override fun toString() = "C$id"
}

val normal: Predicate<Node> = { it is NormalNode }
val client: Predicate<Node> = { it is ClientNode }

/** A directed edge between two nodes with bandwidth capacity and per-unit cost. */
class Edge(
    val from: Node,
    val to: Node,
    val maxBandwidth: UInt64,
    val costPerBandwidth: UInt64
) : AutoIndexed(Edge::class) {
    override fun toString() = "E($from,$to)"
}

fun from(node: Node): Predicate<Edge> = { node == it.from }
inline fun from(crossinline predicate: Predicate<Node>): Predicate<Edge> = { predicate(it.from) }
fun to(node: Node): Predicate<Edge> = { node == it.to }
inline fun to(crossinline predicate: Predicate<Node>): Predicate<Edge> = { predicate(it.to) }

/** Container for the network graph structure holding all nodes and edges. */
data class Graph(
    val nodes: ArrayList<Node>,
    val edges: ArrayList<Edge>
)
