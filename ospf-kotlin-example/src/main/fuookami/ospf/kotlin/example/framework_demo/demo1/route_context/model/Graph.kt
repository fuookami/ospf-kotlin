package fuookami.ospf.kotlin.example.framework_demo.demo1.route_context.model

import fuookami.ospf.kotlin.utils.concept.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.math.algebra.number.*

/**
 * Base class for network nodes, identified by a unique ID and connected to edges.
 * 网络节点的基类（通过唯一 ID 标识并连接到边）。
*/
sealed class Node(
    val id: UInt64
) : AutoIndexed(Node::class) {
    val edges: MutableList<Edge> = ArrayList()

    /**
     * Adds an edge to this node.
     * 向该节点添加一条边。
     *
     * @param edge 要添加的边 / the edge to add
    */
    fun add(edge: Edge) {
        edges.add(edge)
    }
}

/**
 * A transit node in the network that can carry service traffic.
 * 网络中可以承载服务流量的传输节点。
 *
 * @property id 唯一节点标识符 / the unique node identifier
*/
class NormalNode(
    id: UInt64
) : Node(id) {
    override fun toString() = "N$id"
}

/**
 * A terminal node that consumes bandwidth from the network with a specific demand.
 * 从网络消耗带宽的具有特定需求的终端节点。
 *
 * @property id 唯一节点标识符 / the unique node identifier
 * @property demand 该客户端的带宽需求 / the bandwidth demand of this client
*/
class ClientNode(
    id: UInt64,
    val demand: UInt64
) : Node(id) {
    override fun toString() = "C$id"
}

val normal: Predicate<Node> = Predicate { it is NormalNode }
val client: Predicate<Node> = Predicate { it is ClientNode }

/**
 * A directed edge between two nodes with bandwidth capacity and per-unit cost.
 * 两个节点之间的有向边（具有带宽容量和单位成本）。
 *
 * @property from 源节点 / the source node
 * @property to 目标节点 / the target node
 * @property maxBandwidth 最大带宽容量 / the maximum bandwidth capacity
 * @property costPerBandwidth 每单位带宽成本 / the cost per unit of bandwidth
*/
class Edge(
    val from: Node,
    val to: Node,
    val maxBandwidth: UInt64,
    val costPerBandwidth: UInt64
) : AutoIndexed(Edge::class) {
    override fun toString() = "E($from,$to)"
}

/**
 * Creates a predicate matching edges whose source node is the specified node.
 * 创建匹配源节点为指定节点的边的谓词。
 *
 * @param node 源节点 / the source node
 * @return 匹配源节点为指定节点的边的谓词 / predicate matching edges whose source node is the specified node
*/
fun from(node: Node): Predicate<Edge> = Predicate { node == it.from }

/**
 * Creates a predicate matching edges whose source node satisfies the given predicate.
 * 创建匹配源节点满足给定谓词的边的谓词。
 *
 * @param predicate 用于匹配源节点的谓词 / the predicate to match against the source node
 * @return 匹配源节点满足谓词的边的谓词 / predicate matching edges whose source node satisfies the predicate
*/
fun from(predicate: Predicate<Node>): Predicate<Edge> = Predicate { predicate(it.from) }

/**
 * Creates a predicate matching edges whose target node is the specified node.
 * 创建匹配目标节点为指定节点的边的谓词。
 *
 * @param node 目标节点 / the target node
 * @return 匹配目标节点为指定节点的边的谓词 / predicate matching edges whose target node is the specified node
*/
fun to(node: Node): Predicate<Edge> = Predicate { node == it.to }

/**
 * Creates a predicate matching edges whose target node satisfies the given predicate.
 * 创建匹配目标节点满足给定谓词的边的谓词。
 *
 * @param predicate 用于匹配目标节点的谓词 / the predicate to match against the target node
 * @return 匹配目标节点满足谓词的边的谓词 / predicate matching edges whose target node satisfies the predicate
*/
fun to(predicate: Predicate<Node>): Predicate<Edge> = Predicate { predicate(it.to) }

/**
 * Container for the network graph structure holding all nodes and edges.
 * 持有所有节点和边的网络图结构容器。
 *
 * @property nodes 图中的节点列表 / the list of nodes in the graph
 * @property edges 图中的边列表 / the list of edges in the graph
*/
data class Graph(
    val nodes: ArrayList<Node>,
    val edges: ArrayList<Edge>
)
