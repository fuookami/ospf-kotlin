@file:OptIn(kotlin.time.ExperimentalTime::class)
/** 任务束生成图模型 / Bunch generation graph model */
package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.bunch_generation.model

import kotlin.time.Instant
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.*

/**
 * 节点密封类 / Node sealed class
 *
 * @param index 节点索引 / Node index
 */
sealed class Node(val index: UInt64) {
    companion object {
        internal val root: UInt64 = UInt64.zero
        internal val end: UInt64 = UInt64.maximum
    }

    abstract val time: Instant
}

/** 根节点 / Root node */
object RootNode : Node(root) {
    override val time: Instant = Instant.fromEpochSeconds(Long.MIN_VALUE)

    override fun toString() = "Root"
}

/** 终止节点 / End node */
object EndNode : Node(end) {
    override val time: Instant = Instant.fromEpochSeconds(Long.MAX_VALUE)

    override fun toString() = "End"
}

/**
 * 任务节点 / Task node
 *
 * @param T 任务类型 / Task type
 * @param E 执行器类型 / Executor type
 * @param A 分配策略类型 / Assignment policy type
 * @param Arg 附加参数类型 / Additional argument type
 * @param task 任务 / Task
 * @param arg 附加参数 / Additional argument
 * @param time 时间点 / Time point
 * @param index 节点索引 / Node index
 */
class TaskNode<T : AbstractTask<E, A>, E : Executor, A : AssignmentPolicy<E>, Arg>(
    val task: T,
    val arg: Arg,
    override val time: Instant,
    index: UInt64
) : Node(index) {
    override fun toString() = task.toString()
}

/**
 * 边 / Edge
 *
 * @param from 起始节点 / Source node
 * @param to 目标节点 / Target node
 */
data class Edge(
    val from: Node,
    val to: Node
) {
    override fun toString() = "$from -> $to"
}

/**
 * 图 / Graph
 *
 * @param _nodes 节点映射 / Node map
 * @param _edges 边映射 / Edge map
 */
class Graph(
    private val _nodes: MutableMap<UInt64, Node> = HashMap(),
    private val _edges: MutableMap<Node, MutableSet<Edge>> = HashMap()
) {
    val nodes: Map<UInt64, Node> by ::_nodes
    val edges: Map<Node, Set<Edge>> by ::_edges

    /**
     * 添加节点 / Add node
     *
     * @param node 节点 / Node
     */
    fun put(node: Node) {
        _nodes[node.index] = node
    }

    /**
     * 添加边 / Add edge
     *
     * @param from 起始节点 / Source node
     * @param to 目标节点 / Target node
     */
    fun put(from: Node, to: Node) {
        if (!_edges.containsKey(from)) {
            _edges[from] = HashSet()
        }
        _edges[from]!!.add(Edge(from, to))
    }

    /**
     * 按索引获取节点 / Get node by index
     *
     * @param index 节点索引 / Node index
     * @return 节点或null / Node or null
     */
    operator fun get(index: UInt64): Node? {
        return nodes[index]
    }

    /**
     * 获取节点的边 / Get edges of node
     *
     * @param node 节点 / Node
     * @return 边集合 / Set of edges
     */
    operator fun get(node: Node): Set<Edge> {
        return edges[node] ?: emptySet()
    }

    /**
     * 检查两节点是否连接 / Check if two nodes are connected
     *
     * @param from 起始节点 / Source node
     * @param to 目标节点 / Target node
     * @return 是否连接 / Whether connected
     */
    fun connected(from: Node, to: Node): Boolean {
        return edges[from]?.contains(Edge(from, to)) ?: false
    }

    /**
     * 反转图 / Reverse graph
     *
     * @return 反转后的图 / Reversed graph
     */
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

