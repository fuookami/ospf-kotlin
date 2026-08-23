@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.example.framework_demo.demo4.domain.bunch_generation.model

import kotlin.time.Instant
import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.task.model.*

/**
 * 批次生成路线图中的节点。 / Node in the route graph for bunch generation.
 *
 * @property index 节点索引 / the node index
*/
sealed class Node(val index: UInt64) {
    companion object {
        internal val root: UInt64 = UInt64.zero
        internal val end: UInt64 = UInt64.maximum
    }

    abstract val time: Instant
}

/** 表示飞机路线起点的根节点。Root node representing the start of an aircraft's route. */
object RootNode : Node(root) {
    override val time = Instant.DISTANT_PAST
    override fun toString() = "Root"
}

/** 表示飞机路线结束的终端节点。End node representing the end of an aircraft's route. */
object EndNode : Node(end) {
    override val time = Instant.DISTANT_FUTURE
    override fun toString() = "End"
}

/**
 * 路线图中表示航班任务的任务节点。Task node representing a flight task in the route graph.
 *
 * @property task 此节点表示的航班任务 / The flight task represented by this node
 * @property index 图中的节点索引 / The node index in the graph
*/
class TaskNode(
    val task: FlightTask,
    override val time: Instant,
    index: UInt64
) : Node(index) {
    override fun toString() = task.toString()
}

/**
 * 路线图中的有向边。Directed edge in the route graph.
 *
 * @property from 边的起始节点 / The source node of the edge
 * @property to 边的目标节点 / The target node of the edge
*/
data class Edge(
    val from: Node,
    val to: Node
) {
    override fun toString() = "$from -> $to"
}

/**
 * 批次生成的路线图（包含节点和有向边）。Route graph for bunch generation, containing nodes and directed edges.
 *
 * @property nodes 节点索引到节点的可变映射 / The mutable map of node indices to nodes
*/
class Graph(
    val nodes: MutableMap<UInt64, Node> = HashMap(),
    val edges: MutableMap<Node, MutableSet<Edge>> = HashMap()
) {

    /**
     * 向图中添加节点。Adds a node to the graph.
     *
     * @param node 要添加的节点 / The node to add
    */
    fun put(node: Node) {
        nodes[node.index] = node
    }

    /**
     * 添加从一个节点到另一个节点的有向边。Adds a directed edge from one node to another.
     *
     * @param from 起始节点 / The source node
     * @param to 目标节点 / The target node
    */
    fun put(from: Node, to: Node) {
        if (!edges.containsKey(from)) {
            edges[from] = HashSet()
        }
        edges[from]!!.add(Edge(from, to))
    }

    /**
     * 根据索引获取节点。Gets a node by its index.
     *
     * @param index 要查找的节点索引 / The node index to look up
     * @return 具有给定索引的节点，未找到时为 null / The node with the given index, or null if not found
    */
    operator fun get(index: UInt64): Node? {
        return nodes[index]
    }

    /**
     * 获取节点的所有出边。Gets all outgoing edges from a node.
     *
     * @param node 要获取出边的节点 / The node whose outgoing edges to retrieve
     * @return 该节点的出边集合 / The set of outgoing edges from the node
    */
    operator fun get(node: Node): Set<Edge> {
        return edges[node] ?: emptySet()
    }

    /**
     * 检查两个节点之间是否存在有向边。Checks if there is a directed edge from one node to another.
     *
     * @param from 起始节点 / The source node
     * @param to 目标节点 / The target node
     * @return 从起始节点到目标节点是否存在有向边 / Whether a directed edge exists from the source to the target
    */
    fun connected(from: Node, to: Node): Boolean {
        return edges[from]?.contains(Edge(from, to)) ?: false
    }
}
