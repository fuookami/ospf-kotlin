@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.example.framework_demo.demo4.domain.bunch_generation.model

import kotlin.time.Instant
import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.task.model.*

/** 批次生成路线图中的节点。Node in the route graph for bunch generation. */
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
 * @property task 参数。
 * @property override val time 参数。
 * @property index 参数。
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
 * @property from 参数。
 * @property to 参数。
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
 * @property nodes 参数。
 */
class Graph(
    val nodes: MutableMap<UInt64, Node> = HashMap(),
    val edges: MutableMap<Node, MutableSet<Edge>> = HashMap()
) {
    /**
     * Adds a node to the graph.
 *
     * @param node 参数。
     */
    fun put(node: Node) {
        nodes[node.index] = node
    }

    /**
     * Adds a directed edge from one node to another.
 *
     * @param from 参数。
     * @param to 参数。
     */
    fun put(from: Node, to: Node) {
        if (!edges.containsKey(from)) {
            edges[from] = HashSet()
        }
        edges[from]!!.add(Edge(from, to))
    }

    /**
     * Gets a node by its index.
 *
     * @param index 参数。
     * @return 返回结果。
     */
    operator fun get(index: UInt64): Node? {
        return nodes[index]
    }

    /**
     * Gets all outgoing edges from a node.
 *
     * @param node 参数。
     * @return 返回结果。
     */
    operator fun get(node: Node): Set<Edge> {
        return edges[node] ?: emptySet()
    }

    /**
     * Checks if there is a directed edge from one node to another.
 *
     * @param from 参数。
     * @param to 参数。
     * @return 返回结果。
     */
    fun connected(from: Node, to: Node): Boolean {
        return edges[from]?.contains(Edge(from, to)) ?: false
    }
}
