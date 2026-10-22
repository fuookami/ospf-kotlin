package fuookami.ospf.kotlin.framework.network_scheduling.domain.path

import java.util.Collections
import java.util.PriorityQueue
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.framework.network_scheduling.infrastructure.*

/** 搜索完成证据；截断、未认证支配剪枝或完整路径复核拒绝都不能证明不存在改进路径。
 * / Search-completion evidence; truncation, uncertified dominance pruning, or full-trace rejection is not an optimality certificate.
 */
enum class PathSearchCompletion { Exhausted, BudgetLimit, Interrupted, UncertifiedDominance, UncertifiedValidation }

/** 搜索预算，与业务路径长度约束分离。 / Search budgets, separate from domain path limits.
 * @property maximumLabels 标签总预算 / Total-label budget
 * @property maximumLabelsPerNode 每节点标签预算 / Per-node label budget
 * @property shouldStop 外部中断检查 / External interruption check
 */
class PathSearchBudget(
    val maximumLabels: Int = Int.MAX_VALUE,
    val maximumLabelsPerNode: Int = Int.MAX_VALUE,
    val shouldStop: () -> Boolean = { false }
)

/** 不可变标签，前驱索引仅在本轮有效。 / Immutable label with request-local predecessor identity.
 * @property index 本轮标签索引 / Request-local index
 * @property node 节点 / Node
 * @property state 业务资源状态 / Business resource state
 * @property predecessor 前驱 / Predecessor
 * @property incoming 到达弧 / Incoming arc
 * @property visited 已访问节点 / Visited nodes
 * @property steps 业务路径长度 / Domain path length
 */
data class PathLabel<S, A>(
    val index: Int,
    val node: NetworkNodeId,
    val state: S,
    val predecessor: Int?,
    val incoming: A?,
    val visited: Set<NetworkNodeId>,
    val steps: Int
)

/** 路径问题；状态和回调不应跨请求共享可变值。 / Path problem with request-local resource state.
 * @property graph 有效图 / Effective graph
 * @property source 起点 / Source
 * @property initial 初始状态 / Initial state
 * @property transition 资源转移，空表示不可行 / Transition, null means infeasible
 * @property terminal 终点判定 / Terminal predicate
 * @property accept 结果筛选 / Output predicate
 * @property elementary 是否禁止重复节点 / Whether repeated nodes are forbidden
 * @property maximumSteps 业务步数上限，不是搜索预算 / Domain step bound, not a search budget
 * @property dominates 支配候选判定 / Dominance predicate
 * @property dominanceIsExact 是否由业务保证支配安全 / Whether dominance is certified by the domain
 * @property order 候选排序 / Candidate ordering
 * @property startContribution 起点资源或成本贡献 / Start resource or cost contribution
 * @property terminalContribution 终点资源或成本贡献 / Terminal resource or cost contribution
 * @property allowedArc 弧过滤器 / Arc predicate
 * @property allowedNode 节点过滤器 / Node predicate
 * @property validate 基于完整路径标签链的独立校验 / Independent validation over the complete label trace
 *
 * Terminal and expandable labels cannot dominate one another. Elementary dominance additionally requires the dominating
 * label to have visited no more nodes; a finite step bound additionally requires it to use no more steps. Callbacks
 * should derive future behavior from `state`, not request-local predecessor indices.
 * / 终止标签与可扩展标签不能相互支配。Elementary 搜索还要求支配标签的已访问节点集合不更多；存在步数上限时还要求其步数不更多。
 * 回调应从 `state` 推导后续行为，不要依赖本轮前驱索引。
 */
class PathProblem<N, A, S>(
    val graph: NetworkGraph<N, A>,
    val source: NetworkNodeId,
    val initial: S,
    val transition: (PathLabel<S, A>, A) -> Ret<S?>,
    val terminal: (NetworkNodeId, S) -> Boolean,
    val accept: (S) -> Boolean = { true },
    val elementary: Boolean = true,
    val maximumSteps: Int? = null,
    val dominates: ((PathLabel<S, A>, PathLabel<S, A>) -> Boolean)? = null,
    val dominanceIsExact: Boolean = false,
    val order: kotlin.Comparator<S>? = null,
    val startContribution: (NetworkNodeId, S) -> Ret<S?> = { _, state -> ok(state) },
    val terminalContribution: (PathLabel<S, A>) -> Ret<S?> = { ok(it.state) },
    val allowedArc: (A) -> Boolean = { true },
    val allowedNode: (NetworkNodeId) -> Boolean = { true },
    val validate: (List<PathLabel<S, A>>) -> Ret<Boolean> = { ok(true) }
)

/** 路径搜索结果。 / Path-search result.
 * @property paths 筛选后的终止标签 / Selected terminal labels
 * @property labels 全部前驱标签 / All predecessor labels
 * @property completion 搜索终态 / Completion state
 * @property expandedLabels 已扩展标签数 / Expanded-label count
 * @property dominatedLabels 支配剪枝数 / Dominance-pruning count
 * @property terminalCount 终止贡献成功的标签数，路径校验和 accept 前 / Labels with successful terminal contribution, before trace validation and accept
 * @property heuristicDominanceUsed 是否使用过未认证支配剪枝 / Whether uncertified dominance pruning occurred
 */
data class PathSearchResult<S, A>(
    val paths: List<PathLabel<S, A>>,
    val labels: List<PathLabel<S, A>>,
    val completion: PathSearchCompletion,
    val expandedLabels: Int,
    val dominatedLabels: Int,
    val terminalCount: Int,
    val heuristicDominanceUsed: Boolean = false
) {
    /** 搜索是否穷尽且未使用不确定剪枝或路径校验过滤。 / Whether search is exhaustive without uncertified pruning or trace rejection. */
    val complete: Boolean get() = completion == PathSearchCompletion.Exhausted

    /** 回溯完整标签链。 / Backtrack a complete label chain.
     * @param path 终止标签 / Terminal label
     * @return 有序标签 / Ordered labels
     */
    fun trace(path: PathLabel<S, A>): List<PathLabel<S, A>> {
        val result = mutableListOf<PathLabel<S, A>>()
        var current: PathLabel<S, A>? = path
        var traversed = 0
        while (current != null) {
            if (traversed >= labels.size || labels.getOrNull(current.index) != current) return emptyList()
            result.add(current)
            val predecessor = current.predecessor
            if (predecessor != null && labels.getOrNull(predecessor) == null) return emptyList()
            current = predecessor?.let(labels::get)
            traversed += 1
        }
        return result.asReversed()
    }
}

/** 无求解器依赖的资源标签搜索。 / Solver-independent resource-label search. */
class PathSearchContext {
    /** 执行有限域搜索；输出列限制不截断搜索。 / Search a finite domain; output limits do not truncate search.
     * @param problem 路径问题 / Path problem
     * @param budget 搜索预算 / Search budget
     * @param maximumResults 返回路径数上限 / Output path limit
     * @return 路径和完成状态 / Paths and completion state
     */
    fun <N, A, S> search(
        problem: PathProblem<N, A, S>,
        budget: PathSearchBudget = PathSearchBudget(),
        maximumResults: Int = Int.MAX_VALUE
    ): Ret<PathSearchResult<S, A>> {
        val graph = problem.graph
        if (graph.indexOf(problem.source) == null || maximumResults < 0 ||
            budget.maximumLabels < 1 || budget.maximumLabelsPerNode < 1 ||
            (problem.maximumSteps != null && problem.maximumSteps < 0)) {
            return networkSchedulingFailure("路径请求或搜索预算无效 / Invalid path request or search budget")
        }
        if (!problem.allowedNode(problem.source)) {
            return ok(
                PathSearchResult(
                    paths = emptyList(),
                    labels = emptyList(),
                    completion = PathSearchCompletion.Exhausted,
                    expandedLabels = 0,
                    dominatedLabels = 0,
                    terminalCount = 0
                )
            )
        }
        val searchGraph = graph.filteredNodes { problem.allowedNode(graph.nodeId(it)) }.filtered(problem.allowedArc)
        val topology = NetworkTopology(searchGraph).topologicalOrder()
        val topologicalPosition = topology?.withIndex()?.associate { it.value to it.index }
        if (!problem.elementary && problem.maximumSteps == null && topology == null) {
            return networkSchedulingFailure("循环路径必须声明有限业务访问域 / Cyclic walks require a finite domain bound")
        }
        val initialState = when (val result = problem.startContribution(problem.source, problem.initial)) {
            is Ok -> result.value ?: return ok(
                PathSearchResult(
                    paths = emptyList(),
                    labels = emptyList(),
                    completion = PathSearchCompletion.Exhausted,
                    expandedLabels = 0,
                    dominatedLabels = 0,
                    terminalCount = 0
                )
            )
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        val labels = mutableListOf<PathLabel<S, A>>(
            PathLabel(
                index = 0,
                node = problem.source,
                state = initialState,
                predecessor = null,
                incoming = null,
                visited = immutableSet(setOf(problem.source)),
                steps = 0
            )
        )
        val active = mutableMapOf(problem.source to mutableListOf(0))
        val removed = mutableSetOf<Int>()
        val queue = PriorityQueue<Int>(
            compareBy<Int> { topologicalPosition?.get(labels[it].node) ?: it }.thenBy { it }
        )
        queue.add(0)
        val terminals = mutableListOf<Int>()
        var expanded = 0
        var dominated = 0
        var terminalCount = 0
        var heuristicDominanceUsed = false
        var completion = PathSearchCompletion.Exhausted
        var totalBudgetReached = false
        fun dominates(first: PathLabel<S, A>, second: PathLabel<S, A>): Boolean {
            if (problem.terminal(first.node, first.state) != problem.terminal(second.node, second.state)) return false
            if (problem.elementary && !second.visited.containsAll(first.visited)) return false
            if (problem.maximumSteps != null && first.steps > second.steps) return false
            return problem.dominates?.invoke(first, second) == true
        }
        fun trace(index: Int): List<PathLabel<S, A>> {
            val result = mutableListOf<PathLabel<S, A>>()
            var current: Int? = index
            while (current != null) {
                val label = labels[current]
                result.add(label)
                current = label.predecessor
            }
            return immutableList(result.asReversed())
        }
        while (queue.isNotEmpty()) {
            if (budget.shouldStop()) {
                completion = PathSearchCompletion.Interrupted
                break
            }
            val index = queue.remove()
            if (index in removed) continue
            val label = labels[index]
            if (problem.terminal(label.node, label.state)) {
                active[label.node]?.remove(index)
                when (val result = problem.terminalContribution(label)) {
                    is Ok -> {
                        val state = result.value
                        if (state != null) {
                            labels[index] = label.copy(state = state)
                            terminalCount += 1
                            when (val validation = problem.validate(trace(index))) {
                                is Ok -> {
                                    if (validation.value) {
                                        terminals.add(index)
                                    } else if (completion == PathSearchCompletion.Exhausted) {
                                        completion = PathSearchCompletion.UncertifiedValidation
                                    }
                                }
                                is Failed -> return Failed(validation.error)
                                is Fatal -> return Fatal(validation.errors)
                            }
                        }
                    }
                    is Failed -> return Failed(result.error)
                    is Fatal -> return Fatal(result.errors)
                }
                continue
            }
            if (problem.maximumSteps != null && label.steps >= problem.maximumSteps) continue
            expanded += 1
            for (arc in searchGraph.outgoing(label.node)) {
                if (budget.shouldStop()) {
                    completion = PathSearchCompletion.Interrupted
                    break
                }
                val next = searchGraph.to(arc)
                if (problem.elementary && next in label.visited) continue
                val state = when (val result = problem.transition(label, arc)) {
                    is Ok -> result.value ?: continue
                    is Failed -> return Failed(result.error)
                    is Fatal -> return Fatal(result.errors)
                }
                val candidate = PathLabel<S, A>(
                    index = labels.size,
                    node = next,
                    state = state,
                    predecessor = index,
                    incoming = arc,
                    visited = immutableSet(label.visited + next),
                    steps = label.steps + 1
                )
                val atNode = active.getOrPut(next) { mutableListOf() }
                val dominance = problem.dominates
                if (dominance != null && atNode.any { dominates(labels[it], candidate) }) {
                    dominated += 1
                    if (!problem.dominanceIsExact) {
                        heuristicDominanceUsed = true
                        if (completion == PathSearchCompletion.Exhausted) completion = PathSearchCompletion.UncertifiedDominance
                    }
                    continue
                }
                if (labels.size >= budget.maximumLabels) {
                    completion = PathSearchCompletion.BudgetLimit
                    totalBudgetReached = true
                    break
                }
                if (dominance != null) {
                    val obsolete = atNode.filter { dominates(candidate, labels[it]) }
                    removed.addAll(obsolete)
                    atNode.removeAll(obsolete.toSet())
                    dominated += obsolete.size
                    if (obsolete.isNotEmpty() && !problem.dominanceIsExact) {
                        heuristicDominanceUsed = true
                        if (completion == PathSearchCompletion.Exhausted) completion = PathSearchCompletion.UncertifiedDominance
                    }
                }
                labels.add(candidate)
                atNode.add(candidate.index)
                if (atNode.size > budget.maximumLabelsPerNode) {
                    completion = PathSearchCompletion.BudgetLimit
                    val worst = problem.order?.let { order -> atNode.maxWithOrNull { a, b -> order.compare(labels[a].state, labels[b].state) } }
                        ?: candidate.index
                    atNode.remove(worst)
                    removed.add(worst)
                }
                if (candidate.index !in removed) queue.add(candidate.index)
            }
            if (completion == PathSearchCompletion.Interrupted || totalBudgetReached) break
        }
        var paths = terminals.filter { it !in removed }.map(labels::get).filter { problem.accept(it.state) }
        problem.order?.let { order -> paths = paths.sortedWith { a, b -> order.compare(a.state, b.state) } }
        return ok(
            PathSearchResult(
                paths = immutableList(paths.take(maximumResults)),
                labels = immutableList(labels),
                completion = completion,
                expandedLabels = expanded,
                dominatedLabels = dominated,
                terminalCount = terminalCount,
                heuristicDominanceUsed = heuristicDominanceUsed
            )
        )
    }
}

private fun <T> immutableList(values: Collection<T>): List<T> = Collections.unmodifiableList(ArrayList(values))

private fun <T> immutableSet(values: Collection<T>): Set<T> = Collections.unmodifiableSet(LinkedHashSet(values))
