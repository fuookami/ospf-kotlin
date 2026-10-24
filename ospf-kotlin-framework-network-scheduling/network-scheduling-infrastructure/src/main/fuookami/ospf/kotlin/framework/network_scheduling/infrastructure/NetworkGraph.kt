package fuookami.ospf.kotlin.framework.network_scheduling.infrastructure

import java.util.Collections
import java.util.IdentityHashMap
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber

/** 稳定弧标识；平行服务必须显式区分。 / Stable arc identity; parallel services need distinct identifiers. */
@JvmInline
value class NetworkArcId(val value: String)

/** 与数值和业务无关的拓扑访问。 / Numeric- and business-independent topology access.
 * @property nodeId 节点标识提取器 / Node identity selector
 * @property arcId 弧标识提取器 / Arc identity selector
 * @property from 起点提取器 / Origin selector
 * @property to 终点提取器 / Destination selector
 */
class NetworkGraphAccess<N, A>(
    val nodeId: (N) -> NetworkNodeId,
    val arcId: (A) -> NetworkArcId,
    val from: (A) -> NetworkNodeId,
    val to: (A) -> NetworkNodeId
)

/** 不可变有向多重图；拓扑标识在创建时快照化。 / Immutable directed multigraph with snapshotted topology identities.
 * @property nodes 节点快照 / Node snapshot
 * @property arcs 弧快照 / Arc snapshot
 * @property nodeIds 稳定节点标识 / Stable node identities
 * @property arcIds 稳定弧标识 / Stable arc identities
 * @property access 拓扑约定 / Topology access
 */
class NetworkGraph<N, A> private constructor(
    private val topology: NetworkGraphSnapshot<N, A>,
    val access: NetworkGraphAccess<N, A>,
    private val reversed: Boolean = false
) {
    private constructor(
        nodes: List<N>,
        arcs: List<A>,
        access: NetworkGraphAccess<N, A>,
        nodeIds: List<NetworkNodeId>,
        arcIds: List<NetworkArcId>,
        arcFrom: List<NetworkNodeId>,
        arcTo: List<NetworkNodeId>,
        reversed: Boolean = false
    ) : this(
        topology = NetworkGraphSnapshot(
            nodes = nodes,
            arcs = arcs,
            nodeIds = nodeIds,
            arcIds = arcIds,
            arcFrom = arcFrom,
            arcTo = arcTo
        ),
        access = access,
        reversed = reversed
    )

    val nodes: List<N> = topology.nodes
    val arcs: List<A> = topology.arcs
    val nodeIds: List<NetworkNodeId> = topology.nodeIds
    val arcIds: List<NetworkArcId> = topology.arcIds
    private val arcFrom: List<NetworkNodeId> = topology.arcFrom
    private val arcTo: List<NetworkNodeId> = topology.arcTo

    /** 已初始化索引的观测快照，不在读取热路径计数。 / Observe initialized indexes without counting on the read hot path.
     * @return 同源视图共享的已建索引名称 / Built index names shared by sibling views
     */
    internal fun initializedIndexes(): Set<String> = topology.initializedIndexes()

    // 算法在循环外固定方向表，避免每个节点重复读取lazy状态。 / Algorithms capture a directional table outside loops to avoid per-node lazy reads.
    internal fun outgoingIndex(): Map<NetworkNodeId, List<A>> = (if (reversed) topology.incoming else topology.outgoing).value

    internal fun incomingIndex(): Map<NetworkNodeId, List<A>> = (if (reversed) topology.outgoing else topology.incoming).value

    /** 查询节点索引。 / Look up the node index.
     * @param id 节点标识 / Node identity
     * @return 索引或空 / Index or null
     */
    fun indexOf(id: NetworkNodeId): Int? = topology.nodeIndex.value[id]

    /** 获取节点的快照标识。 / Get a node's snapshotted identity.
     * @param node 节点载荷 / Node payload
     * @return 稳定标识 / Stable identity
     */
    fun nodeId(node: N): NetworkNodeId = topology.nodePositionsByIdentity.value[node]?.let(nodeIds::get) ?: access.nodeId(node)

    /** 获取弧的快照标识。 / Get an arc's snapshotted identity.
     * @param arc 弧载荷 / Arc payload
     * @return 稳定标识 / Stable identity
     */
    fun arcId(arc: A): NetworkArcId = topology.arcPositionsByIdentity[arc]?.let(arcIds::get) ?: access.arcId(arc)

    /** 查询节点。 / Look up a node.
     * @param id 节点标识 / Node identity
     * @return 节点或空 / Node or null
     */
    fun node(id: NetworkNodeId): N? = topology.nodeIndex.value[id]?.let(nodes::get)

    /** 查询弧。 / Look up an arc.
     * @param id 弧标识 / Arc identity
     * @return 弧或空 / Arc or null
     */
    fun arc(id: NetworkArcId): A? = topology.arcPosition.value[id]?.let(arcs::get)

    /** 获取视图中的起点。 / Get the origin in this view.
     * @param arc 弧 / Arc
     * @return 起点标识 / Origin identity
     */
    fun from(arc: A): NetworkNodeId {
        val index = topology.arcPositionsByIdentity[arc] ?: topology.arcPosition.value[access.arcId(arc)]
        return index?.let { if (reversed) arcTo[it] else arcFrom[it] } ?: access.from(arc)
    }

    /** 获取视图中的终点。 / Get the destination in this view.
     * @param arc 弧 / Arc
     * @return 终点标识 / Destination identity
     */
    fun to(arc: A): NetworkNodeId {
        val index = topology.arcPositionsByIdentity[arc] ?: topology.arcPosition.value[access.arcId(arc)]
        return index?.let { if (reversed) arcFrom[it] else arcTo[it] } ?: access.to(arc)
    }

    /** 获取出弧。 / Get outgoing arcs.
     * @param id 节点标识 / Node identity
     * @return 弧快照 / Arc snapshot
     */
    fun outgoing(id: NetworkNodeId): List<A> = outgoingIndex()[id].orEmpty()

    /** 获取入弧。 / Get incoming arcs.
     * @param id 节点标识 / Node identity
     * @return 弧快照 / Arc snapshot
     */
    fun incoming(id: NetworkNodeId): List<A> = incomingIndex()[id].orEmpty()

    /** 过滤弧，保留原始标识。 / Filter arcs, preserving their original identities.
     * @param predicate 弧过滤器 / Arc predicate
     * @return 过滤图 / Filtered graph
     */
    fun filtered(predicate: (A) -> Boolean): NetworkGraph<N, A> {
        val retained = arcs.indices.filter { predicate(arcs[it]) }
        return NetworkGraph(
            nodes = nodes,
            arcs = retained.map(arcs::get),
            access = access,
            nodeIds = nodeIds,
            arcIds = retained.map(arcIds::get),
            arcFrom = retained.map(arcFrom::get),
            arcTo = retained.map(arcTo::get),
            reversed = reversed
        )
    }

    /** 过滤节点并移除所有关联弧。 / Filter nodes and remove every incident arc.
     * @param predicate 节点过滤器 / Node predicate
     * @return 过滤图 / Filtered graph
     */
    fun filteredNodes(predicate: (N) -> Boolean): NetworkGraph<N, A> {
        val retainedNodes = nodes.indices.filter { predicate(nodes[it]) }.toSet()
        val retainedIds = retainedNodes.map(nodeIds::get).toSet()
        val retainedArcs = arcs.indices.filter { arcFrom[it] in retainedIds && arcTo[it] in retainedIds }
        return NetworkGraph(
            nodes = retainedNodes.map(nodes::get),
            arcs = retainedArcs.map(arcs::get),
            access = access,
            nodeIds = retainedNodes.map(nodeIds::get),
            arcIds = retainedArcs.map(arcIds::get),
            arcFrom = retainedArcs.map(arcFrom::get),
            arcTo = retainedArcs.map(arcTo::get),
            reversed = reversed
        )
    }

    /** 反向视图不改写业务载荷或稳定标识。 / Reverse view without changing payloads or stable identities.
     * @return 反向图 / Reversed graph
     */
    fun reverse(): NetworkGraph<N, A> = NetworkGraph(
        topology = topology,
        access = access,
        reversed = !reversed
    )

    companion object {
        /** 创建并校验通用图。 / Create and validate a generic graph.
         * @param nodes 节点 / Nodes
         * @param arcs 弧 / Arcs
         * @param access 拓扑访问 / Topology access
         * @return 图或错误 / Graph or error
         */
        operator fun <N, A> invoke(
            nodes: List<N>,
            arcs: List<A>,
            access: NetworkGraphAccess<N, A>
        ): Ret<NetworkGraph<N, A>> {
            val nodeIds = nodes.map(access.nodeId)
            if (nodeIds.any { it.value.isBlank() } || nodeIds.toSet().size != nodeIds.size) {
                return networkSchedulingFailure("节点标识为空或重复 / Blank or duplicate node identity")
            }
            val arcIds = arcs.map(access.arcId)
            if (arcIds.any { it.value.isBlank() } || arcIds.toSet().size != arcIds.size) {
                return networkSchedulingFailure("弧标识为空或重复 / Blank or duplicate arc identity")
            }
            val arcFrom = arcs.map(access.from)
            val arcTo = arcs.map(access.to)
            val nodeIdSet = nodeIds.toSet()
            if (arcFrom.indices.any { arcFrom[it] !in nodeIdSet || arcTo[it] !in nodeIdSet }) {
                return networkSchedulingFailure("弧引用缺失节点 / Arc references a missing node")
            }
            return ok(
                NetworkGraph(
                    nodes = nodes,
                    arcs = arcs,
                    access = access,
                    nodeIds = nodeIds,
                    arcIds = arcIds,
                    arcFrom = arcFrom,
                    arcTo = arcTo
                )
            )
        }

        /** 创建内置物理量图。 / Create a physical-quantity graph.
         * @param nodes 节点 / Nodes
         * @param arcs 弧 / Arcs
         * @return 图或错误 / Graph or error
         */
        operator fun <V : RealNumber<V>> invoke(
            nodes: List<NetworkNode<V>>,
            arcs: List<NetworkArc<V>>
        ): Ret<NetworkGraph<NetworkNode<V>, NetworkArc<V>>> {
            return invoke(
                nodes = nodes,
                arcs = arcs,
                access = NetworkGraphAccess(
                    nodeId = { it.id },
                    arcId = { it.id },
                    from = { it.from },
                    to = { it.to }
                )
            )
        }
    }
}

// 只读取创建时捕获的拓扑，默认同步lazy保证每张表仅初始化一次。 / Read only captured topology; synchronized lazy builds each table once.
private class NetworkGraphSnapshot<N, A>(
    nodes: List<N>,
    arcs: List<A>,
    nodeIds: List<NetworkNodeId>,
    arcIds: List<NetworkArcId>,
    arcFrom: List<NetworkNodeId>,
    arcTo: List<NetworkNodeId>
) {
    val nodes: List<N> = immutableList(nodes)
    val arcs: List<A> = immutableList(arcs)
    val nodeIds: List<NetworkNodeId> = immutableList(nodeIds)
    val arcIds: List<NetworkArcId> = immutableList(arcIds)
    val arcFrom: List<NetworkNodeId> = immutableList(arcFrom)
    val arcTo: List<NetworkNodeId> = immutableList(arcTo)
    val nodeIndex = lazy { this.nodeIds.withIndex().associate { (index, id) -> id to index } }
    val arcPosition = lazy { this.arcIds.withIndex().associate { (index, id) -> id to index } }
    val nodePositionsByIdentity = lazy {
        IdentityHashMap<N, Int>().apply {
            this@NetworkGraphSnapshot.nodes.forEachIndexed { index, node -> put(node, index) }
        }
    }
    // 每条弧的端点读取都依赖身份表，保持同步构建以免热循环增加volatile读取。 / Every arc-endpoint read needs identity lookup; build eagerly to avoid volatile reads in hot loops.
    val arcPositionsByIdentity = IdentityHashMap<A, Int>().apply {
        this@NetworkGraphSnapshot.arcs.forEachIndexed { index, arc -> put(arc, index) }
    }
    val outgoing = lazy { adjacency(this.arcFrom) }
    val incoming = lazy { adjacency(this.arcTo) }

    fun initializedIndexes(): Set<String> = listOf(
        "nodeIndex" to nodeIndex,
        "arcPosition" to arcPosition,
        "nodeIdentity" to nodePositionsByIdentity,
        "outgoing" to outgoing,
        "incoming" to incoming
    ).filter { it.second.isInitialized() }.map { it.first }.toSet() + "arcIdentity"

    private fun adjacency(endpoints: List<NetworkNodeId>): Map<NetworkNodeId, List<A>> {
        val groups = linkedMapOf<NetworkNodeId, MutableList<A>>()
        for (index in arcs.indices) {
            groups.getOrPut(endpoints[index]) { ArrayList() }.add(arcs[index])
        }
        return Collections.unmodifiableMap(groups.mapValues { (_, values) -> immutableList(values) })
    }
}

/** 可重用图构建器。 / Reusable graph builder.
 * @property access 拓扑约定 / Topology access
 */
class NetworkGraphBuilder<N, A>(private val access: NetworkGraphAccess<N, A>) {
    private val nodes = mutableListOf<N>()
    private val arcs = mutableListOf<A>()

    /** 添加节点。 / Add a node.
     * @param node 节点 / Node
     */
    fun addNode(node: N) {
        nodes.add(node)
    }

    /** 添加弧。 / Add an arc.
     * @param arc 弧 / Arc
     */
    fun addArc(arc: A) {
        arcs.add(arc)
    }

    /** 校验并构建快照。 / Validate and build a snapshot.
     * @return 图或错误 / Graph or error
     */
    fun build(): Ret<NetworkGraph<N, A>> = NetworkGraph(nodes = nodes, arcs = arcs, access = access)
}

private fun <T> immutableList(values: Collection<T>): List<T> = Collections.unmodifiableList(ArrayList(values))
