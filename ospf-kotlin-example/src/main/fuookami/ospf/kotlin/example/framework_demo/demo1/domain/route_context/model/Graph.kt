package fuookami.ospf.kotlin.example.framework_demo.demo1.domain.route_context.model

import fuookami.ospf.kotlin.utils.math.UInt64
import fuookami.ospf.kotlin.utils.concept.Indexed
import fuookami.ospf.kotlin.utils.functional.Predicate

sealed class Node(
    override val index: Int,
    val id: UInt64
) : Indexed {
    val edges: MutableList<Edge> = ArrayList()

    fun add(edge: Edge) {
        edges.add(edge)
    }
}

class NormalNode(
    index: Int,
    id: UInt64
) : Node(index, id) {
    override fun toString() = "N$id"
}

class ClientNode(
    index: Int,
    id: UInt64,
    val demand: UInt64
) : Node(index, id) {
    override fun toString() = "C$id"
}

val normal: Predicate<Node> = { it is NormalNode }
val client: Predicate<Node> = { it is ClientNode }

class Edge(
    override val index: Int,
    val from: Node,
    val to: Node,
    val maxBandwidth: UInt64,
    val costPerBandwidth: UInt64
) : Indexed {
    override fun toString() = "E($from,$to)"
}

fun from(node: Node): Predicate<Edge> = { node == it.from }
fun from(predicate: Predicate<Node>): Predicate<Edge> = { predicate(it.from) }
fun to(node: Node): Predicate<Edge> = { node == it.to }
fun to(predicate: Predicate<Node>): Predicate<Edge> = { predicate(it.to) }

data class Graph(
    val nodes: ArrayList<Node>,
    val edges: ArrayList<Edge>
) {
}
