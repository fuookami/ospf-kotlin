package fuookami.ospf.kotlin.core.solver.heuristic.runner

import kotlinx.coroutines.*
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.core.model.basic.Solution
import fuookami.ospf.kotlin.core.solver.heuristic.*
import fuookami.ospf.kotlin.core.solver.heuristic.operator.neighborhood.RuinRecreateOperator

@Serializable
private data class RuinSessionState(val current: JsonElement, val callbackState: JsonElement, val random: Long)

/** 可直接注册的排列破坏修复会话，完整目标统一计费 / Directly registrable permutation ruin/recreate session with metered complete objectives.
 * @property problem 独占排列问题 / Exclusively owned permutation problem
 * @property operator 业务破坏/修复组合 / Domain destroy/repair combination
 * @property amount 每次移除量 / Elements removed per step
 * @property random 独立可恢复随机流 / Independent restorable random stream
 * @property statelessEstimates 业务估计闭包是否显式声明无状态 / Explicitly stateless domain estimate callbacks
 * @property estimateState 有状态估计的恢复协议 / Recovery protocol for stateful estimates
 */
class RuinRecreateSearchSession<O, V>(
    private val problem: SearchProblem<O, V>,
    private val operator: RuinRecreateOperator<V>,
    private val amount: Int,
    private val random: SearchRandom,
    private val statelessEstimates: Boolean = false,
    private val estimateState: SearchProblemState? = null
) : SearchSession<O, V> where V : RealNumber<V>, V : NumberField<V> {
    private var current: Individual<O, V>? = null
    private var iterations = 0L
    override val supportsImport = true

    override suspend fun step(budget: SearchSlice): Ret<SearchStepResult<O, V>> {
        if (problem.encoding != SearchEncoding.Permutation || amount < 0 || budget.steps < 0) return searchInvalid("ruin/recreate session requires permutation encoding and nonnegative limits")
        val start = budget.used
        val candidates = ArrayList<Individual<O, V>>()
        var steps = 0L
        var reason = SearchTermination.SliceComplete
        while (steps < budget.steps && !budget.expired) {
            currentCoroutineContext().ensureActive()
            val previous = current
            val neighbor = if (previous == null) {
                searchBoundary { problem.model.initialSolutions(UInt64.one).firstOrNull()?.toList() }
            } else operator(previous.solution, amount, random).map { it }
            if (neighbor !is Ok) return searchFailure(neighbor)
            val solution = neighbor.value ?: return Ok(SearchStepResult(
                candidates = candidates, evaluations = budget.used - start, steps = steps, termination = SearchTermination.NoCandidate
            ))
            val evaluated = problem.evaluate(solution, budget)
            if (evaluated !is Ok) return searchFailure(evaluated)
            if (!evaluated.value.admitted) {
                reason = if (budget.expired) SearchTermination.TimeLimit else SearchTermination.EvaluationLimit
                break
            }
            val candidate = evaluated.value.candidate
            if (candidate != null) {
                candidates.add(candidate)
                if (previous == null || !problem.dominates(previous.fitness, candidate.fitness)) current = candidate
            }
            ++iterations
            ++steps
            val flushed = searchBoundary { problem.model.flush() }
            if (flushed !is Ok) return Ok(SearchStepResult(
                candidates = candidates,
                evaluations = budget.used - start,
                steps = steps,
                termination = SearchTermination.Failed,
                diagnostics = listOf("模型刷新失败 / Model flush failed: $flushed")
            ))
            if (current == null) {
                reason = SearchTermination.NoCandidate
                break
            }
        }
        return Ok(SearchStepResult(
            candidates = candidates,
            evaluations = budget.used - start,
            steps = steps,
            termination = if (budget.expired) SearchTermination.TimeLimit else reason
        ))
    }

    override suspend fun importSolutions(solutions: List<Solution<V>>, budget: SearchSlice): Ret<SearchStepResult<O, V>> {
        if (problem.encoding != SearchEncoding.Permutation || amount < 0) return searchInvalid("invalid ruin/recreate session")
        val start = budget.used
        val candidates = ArrayList<Individual<O, V>>()
        var reason = SearchTermination.SliceComplete
        for (solution in solutions) {
            val result = problem.evaluate(solution, budget)
            if (result !is Ok) return searchFailure(result)
            if (!result.value.admitted) {
                reason = SearchTermination.EvaluationLimit
                break
            }
            val candidate = result.value.candidate ?: continue
            candidates.add(candidate)
            val previous = current
            if (previous == null || !problem.dominates(previous.fitness, candidate.fitness)) current = candidate
        }
        return Ok(SearchStepResult(
            candidates = candidates, evaluations = budget.used - start, steps = 0,
            termination = if (budget.expired) SearchTermination.TimeLimit else reason
        ))
    }

    override fun snapshot(): Ret<SearchSessionSnapshot> {
        if (!statelessEstimates && estimateState == null) return searchInvalid("domain estimates must declare statelessness or provide recovery")
        val encoded = current?.let { SearchCodecs.individual(problem).encode(it) } ?: Ok(JsonNull)
        return encoded.then { individual ->
            searchBoundary { estimateState?.save() ?: Ok(JsonNull) }.then { it }.map { callbacks -> SearchSessionSnapshot(
                initialized = current != null,
                iteration = iterations,
                stagnation = 0,
                state = Json.encodeToJsonElement(RuinSessionState(individual, callbacks, random.state)),
                archive = JsonArray(emptyList())
            ) }
        }
    }

    override fun restore(snapshot: SearchSessionSnapshot): Try {
        if (snapshot.version != 1 || snapshot.iteration < 0 || !statelessEstimates && estimateState == null) return searchInvalid("incompatible ruin/recreate snapshot")
        return searchBoundary { Json.decodeFromJsonElement<RuinSessionState>(snapshot.state) }.then { saved ->
            val decoded = if (saved.current == JsonNull) Ok(null) else SearchCodecs.individual(problem).decode(saved.current)
            decoded.then { individual ->
                if (snapshot.initialized != (individual != null)) return@then searchInvalid("inconsistent ruin/recreate initialization")
                val valid = searchBoundary { individual == null || problem.validSolution(individual.solution) &&
                    problem.admissible(individual.solution) && problem.valid(individual.fitness) }
                if (valid !is Ok) return@then searchFailure(valid)
                if (!valid.value) return@then searchInvalid("invalid restored ruin/recreate candidate")
                searchBoundary { estimateState?.load(saved.callbackState) ?: if (saved.callbackState == JsonNull) ok else searchInvalid("unexpected estimate state") }.then { it }.map {
                    current = individual
                    iterations = snapshot.iteration
                    random.state = saved.random
                    success
                }
            }
        }
    }
}
