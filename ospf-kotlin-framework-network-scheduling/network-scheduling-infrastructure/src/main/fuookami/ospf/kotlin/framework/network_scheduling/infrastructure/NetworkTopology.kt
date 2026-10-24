package fuookami.ospf.kotlin.framework.network_scheduling.infrastructure

import java.util.Collections
import java.util.LinkedHashSet
import java.util.PriorityQueue
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.quantities.unit.PhysicalUnit
import fuookami.ospf.kotlin.quantities.quantity.*

/** 最短路正常终态。 / Normal shortest-path terminal states. */
enum class ShortestPathStatus { Found, Unreachable, NegativeCycle }

/** 最短路结果。 / Shortest-path result.
 * @property status 终态 / Terminal state
 * @property arcs 有序业务弧 / Ordered payload arcs
 * @property cost 总成本，无最短路时为空 / Total cost, absent without a shortest path
 */
data class ShortestPathResult<A, V : RealNumber<V>>(
    val status: ShortestPathStatus,
    val arcs: List<A>,
    val cost: Quantity<V>?
)

/** 拓扑算法，不拥有业务或求解状态。 / Topology algorithms without business or solver state.
 * @property graph 有效网络视图 / Effective network view
 */
class NetworkTopology<N, A>(private val graph: NetworkGraph<N, A>) {
    /** 创建仅含允许节点和弧的拓扑视图。 / Create a topology view containing only allowed nodes and arcs.
     * @param allowedNode 节点过滤器 / Node predicate
     * @param allowedArc 弧过滤器 / Arc predicate
     * @return 过滤后的拓扑 / Filtered topology
     */
    fun filtered(
        allowedNode: (NetworkNodeId) -> Boolean = { true },
        allowedArc: (A) -> Boolean = { true }
    ): NetworkTopology<N, A> {
        val filteredNodes = graph.filteredNodes { allowedNode(graph.nodeId(it)) }
        return NetworkTopology(filteredNodes.filtered(allowedArc))
    }

    /** 多源可达节点。 / Multi-source reachability.
     * @param sources 起点集合 / Sources
     * @param allowed 弧过滤器 / Arc predicate
     * @param allowedNode 节点过滤器 / Node predicate
     * @return 可达标识或输入错误 / Reachable identities or invalid input
     */
    fun reachable(
        sources: Set<NetworkNodeId>,
        allowed: (A) -> Boolean = { true },
        allowedNode: (NetworkNodeId) -> Boolean = { true }
    ): Ret<Set<NetworkNodeId>> {
        if (sources.any { graph.indexOf(it) == null }) {
            return networkSchedulingFailure("可达性起点不存在 / Reachability source does not exist")
        }
        val visited = sources.filter(allowedNode).toMutableSet()
        val queue = ArrayDeque(visited)
        val outgoing = if (queue.isEmpty()) emptyMap() else graph.outgoingIndex()
        while (queue.isNotEmpty()) {
            for (arc in outgoing[queue.removeFirst()].orEmpty().filter(allowed)) {
                val next = graph.to(arc)
                if (allowedNode(next) && visited.add(next)) queue.add(next)
            }
        }
        return ok(Collections.unmodifiableSet(LinkedHashSet(visited)))
    }

    /** 无权路径见证。 / Unweighted path witness.
     * @param source 起点 / Source
     * @param target 终点 / Target
     * @param allowedArc 弧过滤器 / Arc predicate
     * @param allowedNode 节点过滤器 / Node predicate
     * @return 有序弧，不可达时为空 / Ordered arcs, null when unreachable
     */
    fun witness(
        source: NetworkNodeId,
        target: NetworkNodeId,
        allowedArc: (A) -> Boolean = { true },
        allowedNode: (NetworkNodeId) -> Boolean = { true }
    ): Ret<List<A>?> {
        if (graph.indexOf(source) == null || graph.indexOf(target) == null) {
            return networkSchedulingFailure("路径端点不存在 / Path endpoint does not exist")
        }
        if (!allowedNode(source) || !allowedNode(target)) return ok(null)
        if (source == target) return ok(emptyList())
        val previous = mutableMapOf<NetworkNodeId, A>()
        val visited = mutableSetOf(source)
        val queue = ArrayDeque(listOf(source))
        val outgoing = graph.outgoingIndex()
        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()
            if (node == target) {
                return ok(backtrack(source = source, target = target, previous = previous))
            }
            for (arc in outgoing[node].orEmpty().filter(allowedArc)) {
                val next = graph.to(arc)
                if (allowedNode(next) && visited.add(next)) {
                    previous[next] = arc
                    queue.add(next)
                }
            }
        }
        return ok(null)
    }

    /** 拓扑排序；有环时返回空。 / Topological order, null for cyclic graphs.
     * @return 节点顺序 / Node order
     */
    fun topologicalOrder(): List<NetworkNodeId>? {
        val incoming = if (graph.nodes.isEmpty()) emptyMap() else graph.incomingIndex()
        val degree = graph.nodeIds.associateWith { incoming[it].orEmpty().size }.toMutableMap()
        val queue = ArrayDeque(degree.filterValues { it == 0 }.keys)
        val result = mutableListOf<NetworkNodeId>()
        val outgoing = if (queue.isEmpty()) emptyMap() else graph.outgoingIndex()
        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()
            result.add(node)
            for (arc in outgoing[node].orEmpty()) {
                val next = graph.to(arc)
                val count = degree.getValue(next) - 1
                degree[next] = count
                if (count == 0) queue.add(next)
            }
        }
        return result.takeIf { it.size == graph.nodes.size }?.let(::immutableList)
    }

    /** 检查有向环。 / Detect a directed cycle.
     * @return 是否有环 / Whether a cycle exists
     */
    fun hasCycle(): Boolean = topologicalOrder() == null

    /** 迭代式 Kosaraju 强连通分量。 / Iterative Kosaraju strongly connected components.
     * @return 分量列表 / Components
     */
    fun stronglyConnectedComponents(): List<Set<NetworkNodeId>> {
        val seen = mutableSetOf<NetworkNodeId>()
        val order = mutableListOf<NetworkNodeId>()
        val outgoing = if (graph.nodes.isEmpty()) emptyMap() else graph.outgoingIndex()
        for (root in graph.nodeIds) {
            val stack = ArrayDeque(listOf(root to false))
            while (stack.isNotEmpty()) {
                val (node, closing) = stack.removeLast()
                if (closing) {
                    order.add(node)
                } else if (seen.add(node)) {
                    stack.add(node to true)
                    outgoing[node].orEmpty().asReversed().forEach { stack.add(graph.to(it) to false) }
                }
            }
        }
        seen.clear()
        val components = mutableListOf<Set<NetworkNodeId>>()
        val incoming = if (order.isEmpty()) emptyMap() else graph.incomingIndex()
        for (root in order.asReversed()) {
            if (root in seen) continue
            val component = mutableSetOf<NetworkNodeId>()
            val stack = ArrayDeque(listOf(root))
            while (stack.isNotEmpty()) {
                val node = stack.removeLast()
                if (!seen.add(node)) continue
                component.add(node)
                incoming[node].orEmpty().forEach { stack.add(graph.from(it)) }
            }
            components.add(Collections.unmodifiableSet(LinkedHashSet(component)))
        }
        return immutableList(components)
    }

    /** 最短路径；只将能影响目标的可达负环报告为无最短路。 / Shortest path, reporting only reachable negative cycles that can affect the target.
     * @param source 起点 / Source
     * @param target 终点 / Target
     * @param zero 数值零 / Numeric zero
     * @param unit 成本单位 / Cost unit
     * @param weight 弧成本 / Arc cost
     * @param allowedArc 弧过滤器 / Arc predicate
     * @param allowedNode 节点过滤器 / Node predicate
     * @return 最短路结果 / Shortest-path result
     */
    fun <V : RealNumber<V>> shortestPath(
        source: NetworkNodeId,
        target: NetworkNodeId,
        zero: V,
        unit: PhysicalUnit,
        weight: (A) -> Ret<Quantity<V>>,
        allowedArc: (A) -> Boolean = { true },
        allowedNode: (NetworkNodeId) -> Boolean = { true }
    ): Ret<ShortestPathResult<A, V>> {
        if (graph.indexOf(source) == null || graph.indexOf(target) == null) {
            return networkSchedulingFailure("最短路端点不存在 / Shortest-path endpoint does not exist")
        }
        if (!allowedNode(source) || !allowedNode(target)) {
            return ok(
                ShortestPathResult(
                    status = ShortestPathStatus.Unreachable,
                    arcs = emptyList(),
                    cost = null
                )
            )
        }
        val filteredNodes = graph.filteredNodes { allowedNode(graph.nodeId(it)) }
        val effectiveGraph = filteredNodes.filtered(allowedArc)
        return NetworkTopology(effectiveGraph).shortestPathOnView(
            source = source,
            target = target,
            zero = zero,
            unit = unit,
            weight = weight
        )
    }

    private fun <V : RealNumber<V>> shortestPathOnView(
        source: NetworkNodeId,
        target: NetworkNodeId,
        zero: V,
        unit: PhysicalUnit,
        weight: (A) -> Ret<Quantity<V>>
    ): Ret<ShortestPathResult<A, V>> {
        val weights = mutableMapOf<NetworkArcId, V>()
        for (arc in graph.arcs) {
            val quantity = when (val result = weight(arc)) {
                is Ok -> result.value
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
            weights[graph.arcId(arc)] = quantity.convertTo(unit)?.value
                ?: return networkSchedulingFailure("最短路成本单位不兼容 / Incompatible shortest-path cost unit")
        }
        val distance = mutableMapOf(source to zero)
        val previous = mutableMapOf<NetworkNodeId, A>()
        fun relax(arc: A): Boolean {
            val origin = distance[graph.from(arc)] ?: return false
            val candidate = origin + weights.getValue(graph.arcId(arc))
            val old = distance[graph.to(arc)]
            if (old != null && !(candidate ls old)) return false
            distance[graph.to(arc)] = candidate
            previous[graph.to(arc)] = arc
            return true
        }
        if (weights.values.none { it ls zero }) {
            val outgoing = graph.outgoingIndex()
            val queue = PriorityQueue<Pair<NetworkNodeId, V>>(compareBy { it.second })
            queue.add(source to zero)
            while (queue.isNotEmpty()) {
                val (node, value) = queue.remove()
                if (value != distance[node]) continue
                for (arc in outgoing[node].orEmpty()) {
                    if (relax(arc)) queue.add(graph.to(arc) to distance.getValue(graph.to(arc)))
                }
            }
        } else {
            for (iteration in 1 until graph.nodes.size) {
                var changed = false
                for (arc in graph.arcs) if (relax(arc)) changed = true
                if (!changed) break
            }
            val affected = graph.arcs.filter { arc ->
                val origin = distance[graph.from(arc)]
                val dest = distance[graph.to(arc)]
                origin != null && (dest == null || (origin + weights.getValue(graph.arcId(arc))) ls dest)
            }.map(graph::to).toSet()
            val relevant = NetworkTopology(graph.reverse()).reachable(setOf(target)).value.orEmpty()
            if (affected.any { it in relevant }) {
                return ok(
                    ShortestPathResult(
                        status = ShortestPathStatus.NegativeCycle,
                        arcs = emptyList(),
                        cost = null
                    )
                )
            }
        }
        val cost = distance[target]
            ?: return ok(
                ShortestPathResult(
                    status = ShortestPathStatus.Unreachable,
                    arcs = emptyList(),
                    cost = null
                )
            )
        return ok(
            ShortestPathResult(
                status = ShortestPathStatus.Found,
                arcs = backtrack(source = source, target = target, previous = previous),
                cost = Quantity(cost, unit)
            )
        )
    }

    private fun backtrack(source: NetworkNodeId, target: NetworkNodeId, previous: Map<NetworkNodeId, A>): List<A> {
        val result = mutableListOf<A>()
        var current = target
        while (current != source && result.size < graph.nodes.size) {
            val arc = previous[current] ?: return emptyList()
            result.add(arc)
            current = graph.from(arc)
        }
        return if (current == source) immutableList(result.asReversed()) else emptyList()
    }
}

private fun <T> immutableList(values: Collection<T>): List<T> = Collections.unmodifiableList(ArrayList(values))
