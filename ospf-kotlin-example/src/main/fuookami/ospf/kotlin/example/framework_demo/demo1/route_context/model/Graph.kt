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
     * @param edge the edge to add / 要添加的边
    */
    fun add(edge: Edge) {
        edges.add(edge)
    }
}

/**
 * A transit node in the network that can carry service traffic.
 * 网络中可以承载服务流量的传输节点。
 *
 * @property id the unique node identifier / 唯一节点标识符
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
 * @property id the unique node identifier / 唯一节点标识符
 * @property demand the bandwidth demand of this client / 该客户端的带宽需求
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
 * A directed edge between two nodes with bandwidth capacity and per-unit cost.
 * 两个节点之间的有向边（具有带宽容量和单位成本）。
 *
 * @property from the source node / 源节点
 * @property to the target node / 目标节点
 * @property maxBandwidth the maximum bandwidth capacity / 最大带宽容量
 * @property costPerBandwidth the cost per unit of bandwidth / 每单位带宽成本
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
 * @param node the source node / 源节点
 * @return predicate matching edges whose source node is the specified node / 匹配源节点为指定节点的边的谓词
*/
fun from(node: Node): Predicate<Edge> = { node == it.from }

/**
 * Creates a predicate matching edges whose source node satisfies the given predicate.
 * 创建匹配源节点满足给定谓词的边的谓词。
 *
 * @param predicate the predicate to match against the source node / 用于匹配源节点的谓词
 * @return predicate matching edges whose source node satisfies the predicate / 匹配源节点满足谓词的边的谓词
*/
inline fun from(crossinline predicate: Predicate<Node>): Predicate<Edge> = { predicate(it.from) }

/**
 * Creates a predicate matching edges whose target node is the specified node.
 * 创建匹配目标节点为指定节点的边的谓词。
 *
 * @param node the target node / 目标节点
 * @return predicate matching edges whose target node is the specified node / 匹配目标节点为指定节点的边的谓词
*/
fun to(node: Node): Predicate<Edge> = { node == it.to }

/**
 * Creates a predicate matching edges whose target node satisfies the given predicate.
 * 创建匹配目标节点满足给定谓词的边的谓词。
 *
 * @param predicate the predicate to match against the target node / 用于匹配目标节点的谓词
 * @return predicate matching edges whose target node satisfies the predicate / 匹配目标节点满足谓词的边的谓词
*/
inline fun to(crossinline predicate: Predicate<Node>): Predicate<Edge> = { predicate(it.to) }

/**
 * Container for the network graph structure holding all nodes and edges.
 * 持有所有节点和边的网络图结构容器。
 *
 * @property nodes the list of nodes in the graph / 图中的节点列表
 * @property edges the list of edges in the graph / 图中的边列表
*/
data class Graph(
    val nodes: ArrayList<Node>,
    val edges: ArrayList<Edge>
)
