package fuookami.ospf.kotlin.example.framework_demo.demo1.route_context.model

import fuookami.ospf.kotlin.utils.concept.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.math.algebra.number.*

/** 网络节点的基类（通过唯一 ID 标识并连接到边）。Base class for network nodes, identified by a unique ID and connected to edges. */
sealed class Node(
    val id: UInt64
) : AutoIndexed(Node::class) {
    val edges: MutableList<Edge> = ArrayList()

    fun add(edge: Edge) {
        edges.add(edge)
    }
}

/**
 * 网络中可以承载服务流量的传输节点。A transit node in the network that can carry service traffic.
 *
 * @property id 参数。
 */
class NormalNode(
    id: UInt64
) : Node(id) {
    override fun toString() = "N$id"
}

/**
 * 从网络消耗带宽的具有特定需求的终端节点。A terminal node that consumes bandwidth from the network with a specific demand.
 *
 * @property id 参数。
 * @property demand 参数。
 */
class ClientNode(
    id: UInt64,
    val demand: UInt64
) : Node(id) {
    override fun toString() = "C$id"
}

val normal: Predicate<Node> = { it is NormalNode }
val client: Predicate<Node> = { it is ClientNode }

/**
 * 两个节点之间的有向边（具有带宽容量和单位成本）。A directed edge between two nodes with bandwidth capacity and per-unit cost.
 *
 * @property from 参数。
 * @property to 参数。
 * @property maxBandwidth 参数。
 * @property costPerBandwidth 参数。
 */
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

/**
 * 持有所有节点和边的网络图结构容器。Container for the network graph structure holding all nodes and edges.
 *
 * @property nodes 参数。
 * @property edges 参数。
 */
data class Graph(
    val nodes: ArrayList<Node>,
    val edges: ArrayList<Edge>
)
