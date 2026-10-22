package fuookami.ospf.kotlin.core.solver.hyper_heuristic.scheduling

import kotlinx.serialization.*
import kotlinx.serialization.json.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.core.model.basic.Solution
import fuookami.ospf.kotlin.core.solver.heuristic.*
import fuookami.ospf.kotlin.core.solver.hyper_heuristic.memory.*
import fuookami.ospf.kotlin.core.solver.hyper_heuristic.selection.*

@Serializable
private data class CompositeSnapshot(
    val children: Map<String, SearchSessionSnapshot>,
    val random: Map<String, Long>,
    val cursor: Int,
    val steps: Long,
    val initialized: List<Boolean>,
    val archive: JsonElement,
    val working: JsonElement
)

/** 组合会话，共享父级预算但保留各子会话状态 / Composite session sharing parent budgets while retaining child states.
 * @property children 独立子会话 / Independent child sessions
 * @property streams 子会话的独立随机流 / Independent child random streams
 * @property sequential 是否为全局到局部的单向阶段 / Whether phases advance globally to locally
 * @property phaseSteps 每阶段完整步数 / Complete steps per phase
 * @property problem 共用的串行问题边界 / Shared sequential problem boundary
 */
class CompositeSearchSession<O, V>(
    private val children: Map<String, SearchSession<O, V>>,
    private val streams: Map<String, SearchRandom>,
    private val sequential: Boolean,
    private val phaseSteps: Int,
    private val problem: SearchProblem<O, V>
) : SearchSession<O, V>, PolicyCarrier where V : RealNumber<V>, V : NumberField<V> {
    private val ids = children.keys.toList()
    private var cursor = 0
    private var completed = 0L
    private val initialized = MutableList(children.size) { false }
    private val memory = SearchMemory(
        problem = problem,
        capacity = 100,
        workingCapacity = 20,
        acceptance = NotWorseAcceptancePolicy()
    )

    override val supportsImport: Boolean get() = children.values.all { it.supportsImport }

    override suspend fun step(budget: SearchSlice): Ret<SearchStepResult<O, V>> {
        if (children.isEmpty() || phaseSteps <= 0) return searchInvalid("invalid composite")
        val candidates = ArrayList<Individual<O, V>>()
        var steps = 0L
        val begin = budget.used
        var reason = SearchTermination.SliceComplete
        val diagnostics = ArrayList<String>()
        repeat(budget.steps.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()) {
            val child = children.getValue(ids[cursor])
            if (!sequential && initialized[cursor] && completed > 0 && completed % (phaseSteps.toLong() * children.size) == 0L) {
                val imported = child.importSolutions(memory.working.take(1).map { it.solution }, budget.child((budget.evaluations - budget.used).coerceAtLeast(0), 0))
                if (imported !is Ok) return imported
                candidates.addAll(imported.value.candidates)
            }
            val remaining = budget.evaluations - (budget.used - begin)
            if (remaining <= 0 || budget.expired) return Ok(SearchStepResult(
                candidates = candidates,
                evaluations = budget.used - begin,
                steps = steps,
                termination = SearchTermination.EvaluationLimit
            ))
            val result = child.step(budget.child(remaining, 1))
            if (result !is Ok) return result
            val report = result.value
            candidates.addAll(report.candidates)
            steps += report.steps
            reason = report.termination
            diagnostics.addAll(report.diagnostics)
            if (!initialized[cursor] && report.steps > 0) {
                initialized[cursor] = true
                val seeds = memory.working.map { it.solution.toList() }
                if (seeds.isNotEmpty()) {
                    val imported = child.importSolutions(seeds, budget.child((budget.evaluations - (budget.used - begin)).coerceAtLeast(0), 0))
                    if (imported !is Ok) return imported
                    candidates.addAll(imported.value.candidates)
                }
            }
            memory.offer(report.candidates)
            if (report.steps > 0) {
                ++completed
                if (sequential) {
                    if (completed % phaseSteps == 0L && cursor < ids.lastIndex) ++cursor
                } else cursor = (cursor + 1) % children.size
            }
            if (reason != SearchTermination.SliceComplete) return Ok(SearchStepResult(
                candidates = candidates,
                evaluations = budget.used - begin,
                steps = steps,
                termination = reason,
                diagnostics = diagnostics.toList()
            ))
        }
        return Ok(SearchStepResult(
            candidates = candidates,
            evaluations = budget.used - begin,
            steps = steps,
            termination = reason,
            diagnostics = diagnostics.toList()
        ))
    }

    override suspend fun importSolutions(solutions: List<Solution<V>>, budget: SearchSlice): Ret<SearchStepResult<O, V>> {
        if (!initialized[cursor]) return Ok(SearchStepResult(
            candidates = emptyList(),
            evaluations = 0,
            steps = 0,
            termination = SearchTermination.SliceComplete
        ))
        return children.getValue(ids[cursor]).importSolutions(solutions, budget)
    }

    override fun snapshot(): Ret<SearchSessionSnapshot> {
        val snapshots = linkedMapOf<String, SearchSessionSnapshot>()
        for ((id, child) in children) {
            when (val result = child.snapshot()) {
                is Ok -> snapshots[id] = result.value
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
        }
        val codec = SearchCodecs.list(SearchCodecs.individual(problem))
        return codec.encode(memory.archive).then { archive ->
            codec.encode(memory.working).map { working ->
                SearchSessionSnapshot(
                    initialized = initialized.any { it },
                    iteration = completed,
                    stagnation = 0,
                    state = Json.encodeToJsonElement(CompositeSnapshot(
                        children = snapshots,
                        random = streams.mapValues { it.value.state },
                        cursor = cursor,
                        steps = completed,
                        initialized = initialized.toList(),
                        archive = archive,
                        working = working
                    )),
                    archive = archive
                )
            }
        }
    }

    override fun restore(snapshot: SearchSessionSnapshot): Try {
        return searchBoundary { Json.decodeFromJsonElement<CompositeSnapshot>(snapshot.state) }.then { saved ->
            if (snapshot.version != 1 || saved.children.keys != children.keys || saved.random.keys != streams.keys || saved.initialized.size != children.size || saved.cursor !in ids.indices || saved.steps < 0) {
                return@then searchInvalid("incompatible composite state")
            }
            for (id in ids) {
                val result = children.getValue(id).restore(saved.children.getValue(id))
                if (result !is Ok) return@then result
            }
            SearchCodecs.list(SearchCodecs.individual(problem)).decode(saved.archive).then { archive ->
                SearchCodecs.list(SearchCodecs.individual(problem)).decode(saved.working).map { working ->
                    memory.restore(archive, working)
                    for ((id, stream) in streams) stream.state = saved.random.getValue(id)
                    cursor = saved.cursor
                    completed = saved.steps
                    for (index in initialized.indices) initialized[index] = saved.initialized[index]
                    success
                }
            }
        }
    }

    override fun close(): Try {
        for (child in children.values) {
            val result = child.close()
            if (result !is Ok) return result
        }
        return ok
    }

    override fun drainTransitions(): List<SelectionTransition> {
        return children.flatMap { (id, child) ->
            (child as? PolicyCarrier)?.drainTransitions()?.map { it.copy(scope = "$id/${it.scope}") } ?: emptyList()
        }
    }

    override fun policies(): Map<String, PolicySnapshot> {
        return children.flatMap { (id, child) ->
            (child as? PolicyCarrier)?.policies()?.map { (key, value) -> "$id/$key" to value } ?: emptyList()
        }.toMap()
    }

    override fun restorePolicies(policies: Map<String, PolicySnapshot>): Try {
        if (policies.keys != policies().keys) return searchInvalid("incompatible composite policies")
        for ((id, child) in children) {
            val carrier = child as? PolicyCarrier ?: continue
            val prefix = "$id/"
            val result = carrier.restorePolicies(policies.filterKeys { it.startsWith(prefix) }.mapKeys { it.key.removePrefix(prefix) })
            if (result !is Ok) return result
        }
        return ok
    }
}
