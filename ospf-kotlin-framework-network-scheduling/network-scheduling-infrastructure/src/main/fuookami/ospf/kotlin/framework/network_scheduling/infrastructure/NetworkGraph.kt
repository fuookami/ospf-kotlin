package fuookami.ospf.kotlin.framework.network_scheduling.infrastructure

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber

/**
 * 具有稳定节点索引的不可变网络图。 / Immutable network graph with stable node indices.
 *
 * @property nodes 节点快照 / Node snapshot
 * @property arcs 弧快照 / Arc snapshot
 */
class NetworkGraph<V : RealNumber<V>> private constructor(
    nodes: List<NetworkNode<V>>,
    arcs: List<NetworkArc<V>>
) {
    val nodes: List<NetworkNode<V>> = nodes.toList()
    val arcs: List<NetworkArc<V>> = arcs.toList()
    private val nodeIndex: Map<NetworkNodeId, Int> = this.nodes.mapIndexed { index, node -> node.id to index }.toMap()
    private val outgoing: Map<NetworkNodeId, List<NetworkArc<V>>> = this.arcs.groupBy { it.from }
    private val incoming: Map<NetworkNodeId, List<NetworkArc<V>>> = this.arcs.groupBy { it.to }

    /** 获取图内稳定索引。 / Get the stable graph-local index. */
    fun indexOf(id: NetworkNodeId): Int? = nodeIndex[id]

    /** 获取节点。 / Get a node. */
    fun node(id: NetworkNodeId): NetworkNode<V>? = nodeIndex[id]?.let(nodes::get)

    /** 获取出弧快照。 / Get the outgoing-arc snapshot. */
    fun outgoing(id: NetworkNodeId): List<NetworkArc<V>> = outgoing[id].orEmpty()

    /** 获取入弧快照。 / Get the incoming-arc snapshot. */
    fun incoming(id: NetworkNodeId): List<NetworkArc<V>> = incoming[id].orEmpty()

    /** 以谓词创建过滤后的新图。 / Create a filtered graph with a predicate. */
    fun filtered(predicate: (NetworkArc<V>) -> Boolean): NetworkGraph<V> {
        return NetworkGraph(nodes, arcs.filter(predicate))
    }

    companion object {
        /**
         * 创建并校验网络图。 / Create and validate a network graph.
         *
         * @param nodes 节点 / Nodes
         * @param arcs 弧 / Arcs
         * @return 网络图或校验失败 / Network graph or validation failure
         */
        operator fun <V : RealNumber<V>> invoke(
            nodes: List<NetworkNode<V>>,
            arcs: List<NetworkArc<V>>
        ): Ret<NetworkGraph<V>> {
            val ids = nodes.map { it.id }
            if (ids.toSet().size != ids.size) {
                return networkSchedulingFailure(
                    "创建网络图失败：节点 ID 重复 / Failed to create network graph: duplicate node ID"
                )
            }
            val idSet = ids.toSet()
            val missing = arcs.firstOrNull { it.from !in idSet || it.to !in idSet }
            if (missing != null) {
                return networkSchedulingFailure(
                    "创建网络图失败：弧 ${missing.from.value}->${missing.to.value} 引用了缺失节点 / " +
                            "Failed to create network graph: arc ${missing.from.value}->${missing.to.value} references a missing node"
                )
            }
            val arcKeys = arcs.map { it.from to it.to }
            if (arcKeys.toSet().size != arcKeys.size) {
                return networkSchedulingFailure(
                    "创建网络图失败：存在重复弧 / Failed to create network graph: duplicate arc detected"
                )
            }
            return ok(NetworkGraph(nodes, arcs))
        }
    }
}
