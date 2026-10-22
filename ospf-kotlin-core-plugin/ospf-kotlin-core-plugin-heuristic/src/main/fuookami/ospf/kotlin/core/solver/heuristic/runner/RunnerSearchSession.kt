package fuookami.ospf.kotlin.core.solver.heuristic.runner

import kotlin.time.Duration.Companion.nanoseconds
import kotlinx.coroutines.*
import kotlinx.serialization.json.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.core.model.basic.Solution
import fuookami.ospf.kotlin.core.model.callback.AbstractCallBackModelInterface
import fuookami.ospf.kotlin.core.solver.heuristic.*

/** 评估模型标记，用于禁止缺失目标回退绕过预算 / Evaluation marker preventing fallback from bypassing the budget. */
internal interface SearchEvaluation {
    val rejected: Boolean
}

private class EvaluationModel<T, O, V>(
    private val delegate: AbstractCallBackModelInterface<T, O, V>,
    private val problem: SearchProblem<O, V>,
    private val budget: SearchSlice
) : AbstractCallBackModelInterface<T, O, V> by delegate, SearchEvaluation
    where V : RealNumber<V>, V : NumberField<V> {
    var exhausted = false
    override var rejected = false
    val candidates = ArrayList<Individual<O, V>>()

    override fun objective(solution: Solution<V>): O? {
        rejected = true
        if (!budget.acquire()) {
            exhausted = true
            return null
        }
        if (!problem.validSolution(solution) || !problem.feasible(solution)) {
            return null
        }
        budget.global.objectiveCalled()
        val objective = delegate.objective(solution) ?: return null
        if (!problem.valid(objective)) {
            return null
        }
        rejected = false
        candidates.add(SolutionWithFitness(solution.toList(), objective))
        return objective
    }
}

private class StateBox<T>(val value: T)

/** 原生状态的会话适配器，不暴露无类型状态 / Native-state session adapter without untyped state.
 * @param S 算法状态 / Algorithm state
 * @param O 目标类型 / Objective type
 * @param V 解值类型 / Solution value type
 * @property runner 生命周期骨架 / Lifecycle skeleton
 * @property problem 独占问题 / Exclusively owned problem
 * @property codec 原生状态编码，可为空以禁止恢复 / Native codec, null disables recovery
 * @property importer 算法特定的解导入 / Algorithm-specific solution import
 * @param initialIteration 初始迭代 / Initial iteration
 */
class RunnerSearchSession<S, O, V>(
    private val runner: AbstractRunner<S, O, V>,
    private val problem: SearchProblem<O, V>,
    private val codec: SearchCodec<S>?,
    private val importer: ((S, List<Individual<O, V>>) -> Ret<S>)?,
    initialIteration: Iteration = Iteration()
) : SearchSession<O, V> where V : RealNumber<V>, V : NumberField<V> {
    private var state: StateBox<S>? = null
    private var iteration = initialIteration
    private val individuals = SearchCodecs.list(SearchCodecs.individual(problem))

    init {
        iteration.pause()
    }

    /** 当前局部档案 / Current local archive. */
    val archive: List<Individual<O, V>> get() = runner.archive.individuals

    override val supportsImport: Boolean get() = importer != null

    override suspend fun step(budget: SearchSlice): Ret<SearchStepResult<O, V>> {
        if (budget.steps < 0 || budget.evaluations < 0) {
            return searchInvalid("negative slice")
        }
        val evaluated = EvaluationModel(problem.model, problem, budget)
        var completed = 0L
        var reason = SearchTermination.SliceComplete
        iteration.resume()
        return try {
            withContext(SearchExecution.serial.asContextElement(true)) {
                while (completed < budget.steps && !budget.expired) {
                    currentCoroutineContext().ensureActive()
                    val previous = state
                    val result = searchBoundary {
                        if (previous == null) runner.initializeStep(evaluated, iteration)
                        else runner.advanceStep(evaluated, iteration, previous.value)
                    }
                    if (result !is Ok) {
                        return@withContext Ok(SearchStepResult(
                            candidates = evaluated.candidates.toList(),
                            evaluations = budget.used,
                            steps = completed,
                            termination = SearchTermination.Failed,
                            diagnostics = listOf(result.toString())
                        ))
                    }
                    if (evaluated.exhausted) {
                        reason = if (budget.expired) SearchTermination.TimeLimit else SearchTermination.EvaluationLimit
                        break
                    }
                    state = StateBox(result.value)
                    val members = runner.stateMembers(result.value)
                    val improved = runner.archive.update(evaluated, members)
                    for (observer in runner.observers) {
                        observer.observe(iteration, members, improved, runner.converter)
                    }
                    iteration.next(improved)
                    problem.model.flush()
                    ++completed
                    if (members.isEmpty()) {
                        reason = SearchTermination.NoCandidate
                        break
                    }
                }
                if (budget.expired) reason = SearchTermination.TimeLimit
                Ok(SearchStepResult(
                    candidates = evaluated.candidates.toList(),
                    evaluations = budget.used,
                    steps = completed,
                    termination = reason
                ))
            }
        } finally {
            iteration.pause()
        }
    }

    override suspend fun importSolutions(solutions: List<Solution<V>>, budget: SearchSlice): Ret<SearchStepResult<O, V>> {
        val current = state ?: return searchInvalid("initialize before importing")
        val import = importer ?: return searchInvalid("session does not support imports")
        val evaluated = EvaluationModel(problem.model, problem, budget)
        for (solution in solutions) {
            val result = searchBoundary { evaluated.objective(solution.toList()) }
            if (result !is Ok) return result.map { SearchStepResult<O, V>(
                candidates = emptyList(),
                evaluations = budget.used,
                steps = 0,
                termination = SearchTermination.Failed
            ) }
            if (evaluated.exhausted) break
        }
        return import(current.value, evaluated.candidates).map { imported ->
            state = StateBox(imported)
            runner.archive.update(evaluated, evaluated.candidates)
            SearchStepResult(
                candidates = evaluated.candidates.toList(),
                evaluations = budget.used,
                steps = 0,
                termination = if (evaluated.exhausted) {
                    if (budget.expired) SearchTermination.TimeLimit else SearchTermination.EvaluationLimit
                } else SearchTermination.SliceComplete
            )
        }
    }

    override fun snapshot(): Ret<SearchSessionSnapshot> {
        val stateCodec = codec ?: return searchInvalid("missing native state codec")
        val current = state
        val encoded = if (current == null) Ok(JsonNull) else stateCodec.encode(current.value)
        return encoded.then { value ->
            individuals.encode(archive).map { entries ->
                SearchSessionSnapshot(
                    initialized = current != null,
                    iteration = iteration.iteration.toLong(),
                    stagnation = iteration.notBetterIteration.toLong(),
                    state = value,
                    archive = entries,
                    activeNanos = iteration.time.inWholeNanoseconds
                )
            }
        }
    }

    override fun restore(snapshot: SearchSessionSnapshot): Try {
        val stateCodec = codec ?: return searchInvalid("missing native state codec")
        if (snapshot.version != 1 || snapshot.iteration < 0 || snapshot.stagnation < 0 || snapshot.activeNanos < 0) return searchInvalid("session version/counters")
        return individuals.decode(snapshot.archive).then { entries ->
            val restored = if (snapshot.initialized) stateCodec.decode(snapshot.state).map { StateBox(it) } else Ok(null)
            restored.map { value ->
                state = value
                iteration = Iteration(
                    _iteration = UInt64(snapshot.iteration.toULong()),
                    _notBetterIteration = UInt64(snapshot.stagnation.toULong()),
                    elapsed = snapshot.activeNanos.nanoseconds
                ).also { it.pause() }
                runner.archive.clear()
                runner.archive.update(problem.model, entries)
                success
            }
        }
    }
}
