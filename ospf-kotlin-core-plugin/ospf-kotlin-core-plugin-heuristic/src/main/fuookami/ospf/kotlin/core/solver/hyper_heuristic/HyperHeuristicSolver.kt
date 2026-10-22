package fuookami.ospf.kotlin.core.solver.hyper_heuristic

import java.nio.file.Path
import java.util.concurrent.atomic.AtomicBoolean
import java.util.IdentityHashMap
import kotlin.time.*
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.milliseconds
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.channels.Channel
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.core.model.basic.Solution
import fuookami.ospf.kotlin.core.solver.heuristic.*
import fuookami.ospf.kotlin.core.solver.hyper_heuristic.memory.*
import fuookami.ospf.kotlin.core.solver.hyper_heuristic.learning.*
import fuookami.ospf.kotlin.core.solver.hyper_heuristic.selection.*
import fuookami.ospf.kotlin.core.solver.hyper_heuristic.scheduling.*
import fuookami.ospf.kotlin.core.solver.hyper_heuristic.persistence.*
import fuookami.ospf.kotlin.core.solver.hyper_heuristic.configuration.*

internal class SearchWorker<O, V>(
    val id: String,
    val problem: SearchProblem<O, V>,
    val random: SearchRandom,
    val session: SearchSession<O, V>,
    val initialization: InitializationMode,
    var initialized: Boolean = false,
    var disabled: Boolean = false
) where V : RealNumber<V>, V : NumberField<V>

private class Dispatch<O, V>(
    val worker: SearchWorker<O, V>,
    val decision: Long,
    val context: SelectionContext,
    val available: List<String>,
    val revision: Long,
    val reference: List<Individual<O, V>>,
    val seeds: List<Solution<V>>,
    val allowance: Long,
    val stepAllowance: Long
) where V : RealNumber<V>, V : NumberField<V> {
    var consumed = 0L
}

private class Outcome<O, V>(val dispatch: Dispatch<O, V>, val result: Ret<SearchStepResult<O, V>>)
    where V : RealNumber<V>, V : NumberField<V>

/** 超启发式求解器，配置可复用，运行状态不可共享 / Hyper-heuristic solver with reusable configuration and isolated runs.
 * @property config 可执行配置 / Executable configuration
 * @property registry 显式组件注册表 / Explicit component registry
 */
class HyperHeuristicSolver<O, V>(
    val config: HyperHeuristicConfig,
    val registry: SearchRegistry<O, V>
) where V : RealNumber<V>, V : NumberField<V> {
    /** 创建新运行或恢复静止检查点 / Create a run or restore a quiescent checkpoint.
     * @param checkpoint 可选完整检查点 / Optional complete checkpoint
     * @param policy 可选跨实例策略 / Optional cross-instance policy
     * @return 独立运行，调用方负责关闭 / Independent run that the caller must close
     */
    suspend fun open(checkpoint: RunCheckpoint? = null, policy: PolicyArchive? = null): Ret<HyperHeuristicRun<O, V>> {
        val valid = config.validate()
        if (valid !is Ok) return searchFailure(valid)
        if (checkpoint != null && policy != null) return searchInvalid("choose a checkpoint or a policy, not both")
        val random = SearchRandom(config.execution.seed)
        val problemResult = registry.problem(config.problem, random)
        if (problemResult !is Ok) return searchFailure(problemResult)
        val problem = problemResult.value
        val workers = ArrayList<SearchWorker<O, V>>()
        suspend fun fail(message: String): Ret<HyperHeuristicRun<O, V>> {
            for (worker in workers) {
                worker.session.close()
                searchBoundary { worker.problem.model.close() }
            }
            searchBoundary { problem.model.close() }
            return searchInvalid(message)
        }
        if (config.persistence.enabled && !registry.recoverableProblem(config.problem, problem)) return fail("problem callbacks must be declared stateless or expose SearchProblemState")
        val selector = registry.selection(config.selection, config.algorithms.map { it.id }, config.learning.mode != LearningMode.Frozen)
        val features = registry.features(config.learning.features)
        val reward = registry.reward(config.learning.reward)
        val acceptance = registry.acceptance(config.memory.acceptance)
        val exchange = registry.exchange(config.memory.exchange)
        val allocator = registry.allocator(config.budget.allocator)
        if (selector !is Ok || features !is Ok || reward !is Ok || acceptance !is Ok || exchange !is Ok || allocator !is Ok) return fail("invalid selector, features, reward, acceptance, exchange or allocator")
        if (features.value.stateCount?.let { needed -> selector.value.stateCount?.let { it < needed } ?: false } == true) {
            return fail("selector state space is smaller than feature space")
        }
        val components = linkedMapOf(
            "features" to Pair(config.learning.features, features.value),
            "reward" to Pair(config.learning.reward, reward.value),
            "acceptance" to Pair(config.memory.acceptance, acceptance.value),
            "exchange" to Pair(config.memory.exchange, exchange.value),
            "allocator" to Pair(config.budget.allocator, allocator.value)
        )
        if (config.persistence.enabled && components.any { (kind, entry) -> !registry.componentRecoverable(kind, entry.first, entry.second) }) {
            return fail("global components must be explicitly stateless or implement RecoverableComponent")
        }
        val recoverable = components.mapNotNull { (kind, entry) -> (entry.second as? RecoverableComponent)?.let { kind to it } }.toMap()
        if (config.persistence.enabled) {
            for ((id, component) in recoverable) {
                if (searchBoundary { component.save() }.then { it } !is Ok) return fail("component $id cannot encode its initial state")
            }
            if (searchBoundary { problem.state?.save() ?: Ok(JsonNull) }.then { it } !is Ok) return fail("problem cannot encode callback state")
        }
        val models = IdentityHashMap<Any, Boolean>()
        models[problem.model] = true
        val tokens = IdentityHashMap<Any, Boolean>()
        tokens[problem.model.tokens] = true
        for (algorithm in config.algorithms.sortedBy { it.id }) {
            val stream = SearchRandom(config.execution.seed).split("algorithm:${algorithm.id}")
            val created = registry.problem(config.problem, stream)
            if (created !is Ok) return fail("problem factory failed for ${algorithm.id}: $created")
            val branch = created.value
            if (models.containsKey(branch.model) || tokens.containsKey(branch.model.tokens)) return fail("problem factory shared a model or token table")
            models[branch.model] = true
            tokens[branch.model.tokens] = true
            if (branch.id != problem.id || branch.fingerprint != problem.fingerprint || branch.multiObjective != problem.multiObjective) {
                searchBoundary { branch.model.close() }
                return fail("problem factory returned incompatible problem definitions")
            }
            if (config.persistence.enabled && !registry.recoverableProblem(config.problem, branch)) {
                searchBoundary { branch.model.close() }
                return fail("branch callbacks are not recoverable")
            }
            val session = registry.create(algorithm, branch, stream, config.learning.mode != LearningMode.Frozen, reward.value)
            if (session !is Ok) {
                searchBoundary { branch.model.close() }
                return fail("algorithm ${algorithm.id} could not be created: $session")
            }
            workers.add(SearchWorker(
                id = algorithm.id,
                problem = branch,
                random = stream,
                session = session.value,
                initialization = algorithm.initialization
            ))
            if (config.memory.exchange.type != "none" && !session.value.supportsImport) return fail("algorithm ${algorithm.id} does not support imports; CMA-ES requires restartOnImport=true")
            if (config.persistence.enabled && searchBoundary { session.value.snapshot() }.then { it } !is Ok) return fail("algorithm ${algorithm.id} contains non-recoverable state")
        }
        val run = HyperHeuristicRun(
            config = config,
            problem = problem,
            workers = workers,
            policy = selector.value,
            features = features.value,
            reward = reward.value,
            acceptance = acceptance.value,
            exchange = exchange.value,
            allocator = allocator.value,
            trimmer = registry.archiveTrimmer,
            random = random,
            components = recoverable,
            context = currentCoroutineContext()
        )
        val restored = searchBoundary { when {
            checkpoint != null -> run.restore(checkpoint)
            policy != null -> run.restorePolicy(policy)
            else -> ok
        } }.then { it }
        if (restored !is Ok) {
            run.close()
            return restored.map { run }
        }
        return Ok(run)
    }

    /** 执行完整搜索并关闭所有资源 / Execute a complete search and close resources.
     * @param checkpoint 可选检查点 / Optional checkpoint
     * @param policy 可选策略 / Optional policy
     * @return 搜索结果 / Search result
     */
    suspend operator fun invoke(checkpoint: RunCheckpoint? = null, policy: PolicyArchive? = null): Ret<SearchResult<O, V>> {
        val opened = open(checkpoint, policy)
        if (opened !is Ok) return opened.map { SearchResult<O, V>(
            archive = emptyList(),
            multiObjective = false,
            termination = SearchTermination.Failed,
            evaluations = 0,
            decisions = 0,
            steps = 0,
            activeNanos = 0
        ) }
        var closed: Try = ok
        val result = try {
            searchBoundary { opened.value.run() }.then { it }
        } finally {
            closed = withContext(NonCancellable) { opened.value.close() }
        }
        return if (result is Ok && closed !is Ok) searchFailure(closed) else result
    }
}

/** 单协调器运行，所有归并和学习反馈串行化 / Single-coordinator run serializing all merges and learning feedback.
 * @param O 目标类型 / Objective type
 * @param V 解值类型 / Solution value type
 */
class HyperHeuristicRun<O, V> internal constructor(
    private val config: HyperHeuristicConfig,
    private val problem: SearchProblem<O, V>,
    private val workers: List<SearchWorker<O, V>>,
    private val policy: SelectionPolicy,
    private val features: FeatureExtractor,
    private val reward: RewardPolicy,
    acceptance: AcceptancePolicy<O, V>,
    private val exchange: ExchangePolicy<O, V>,
    private val allocator: BudgetAllocator,
    trimmer: (List<Individual<O, V>>, Int) -> List<Individual<O, V>>,
    private val random: SearchRandom,
    private val components: Map<String, RecoverableComponent>,
    context: CoroutineContext
) where V : RealNumber<V>, V : NumberField<V> {
    private val gate = Mutex()
    private val stopped = AtomicBoolean(false)
    private val scope = CoroutineScope(context + SupervisorJob() + Dispatchers.Default)
    private val completions = Channel<Outcome<O, V>>(Channel.UNLIMITED)
    private val active = linkedMapOf<String, Pair<Dispatch<O, V>, Deferred<Outcome<O, V>>>>()
    private val memory = SearchMemory(
        problem = problem,
        capacity = config.memory.archiveCapacity,
        workingCapacity = config.memory.workingCapacity,
        acceptance = acceptance,
        trim = trimmer
    )
    private var budget = SearchBudget(config.budget.evaluations ?: Long.MAX_VALUE)
    private var accounted = 0L
    private var dispatched = 0L
    private var completed = 0L
    private var steps = 0L
    private var stagnation = 0L
    private var successes = 0L
    private var lastCost = 0L
    private var accumulatedNanos = 0L
    private var activeStart: TimeMark? = null
    private var closed = false
    private val diagnostics = ArrayList<String>()
    private val events = ArrayList<SelectionTransition>()
    private val codec = SearchCodecs.list(SearchCodecs.individual(problem))
    private val configFingerprint = fingerprint(Json.encodeToJsonElement(config))
    private val definitionDocument = buildJsonObject {
        put("algorithms", Json.encodeToJsonElement(config.algorithms.sortedBy { it.id }))
        put("selection", Json.encodeToJsonElement(config.selection))
        put("features", Json.encodeToJsonElement(config.learning.features))
        put("reward", Json.encodeToJsonElement(config.learning.reward))
    }
    private val definition = fingerprint(definitionDocument)

    private fun elapsed(): Long = accumulatedNanos + (activeStart?.elapsedNow()?.inWholeNanoseconds ?: 0)

    private fun termination(): SearchTermination? = when {
        diagnostics.isNotEmpty() -> SearchTermination.Failed
        stopped.get() -> SearchTermination.Stopped
        budget.used >= budget.limit -> SearchTermination.EvaluationLimit
        steps >= (config.budget.steps ?: Long.MAX_VALUE) -> SearchTermination.StepLimit
        dispatched >= (config.budget.decisions ?: Long.MAX_VALUE) -> SearchTermination.StepLimit
        config.budget.timeMillis?.let { elapsed() >= it.milliseconds.inWholeNanoseconds } == true -> SearchTermination.TimeLimit
        workers.all { it.disabled } -> SearchTermination.NoCandidate
        else -> null
    }

    private fun state(): SelectionContext = features.extract(
        SearchProgress(
            decision = dispatched,
            evaluations = accounted,
            evaluationLimit = budget.limit,
            stagnation = stagnation,
            diversity = memory.working.map { it.solution }.distinct().size.toDouble() / memory.working.size.coerceAtLeast(1),
            successRate = Flt64(successes.toDouble() / completed.coerceAtLeast(1)),
            cost = Flt64(lastCost.toDouble() / config.budget.sliceEvaluations)
        )
    )

    /** 请求停止；在途片段仍会完整归并 / Request stopping; in-flight slices are still merged. */
    fun requestStop() {
        stopped.set(true)
    }

    /** 获取当前结果，不触发评估 / Inspect the current result without evaluating.
     * @return 结果快照 / Result snapshot
     */
    fun result(): SearchResult<O, V> = SearchResult(memory.archive, problem.multiObjective, termination() ?: SearchTermination.SliceComplete, budget.used, completed, steps, elapsed(), diagnostics.toList(), budget.objectiveCalls)

    /** 获取已完成轨迹的副本 / Get a copy of completed transitions.
     * @return 轨迹 / Transitions
     */
    fun transitions(): List<SelectionTransition> = events.toList()

    /** 执行一轮同步批次或一次异步完成事件 / Execute a synchronous batch or one asynchronous completion.
     * @return 是否仍有工作 / Whether work remains
     */
    suspend fun advance(): Ret<Boolean> = gate.withLock {
        val result = searchBoundary { advanceLocked() }.then { it }
        if (result !is Ok) {
            diagnostics.add("协调器失败 / Coordinator failed: $result")
            withContext(NonCancellable) { drain() }
            accumulatedNanos = elapsed()
            activeStart = null
        }
        result
    }

    private suspend fun advanceLocked(): Ret<Boolean> {
        if (closed) return searchInvalid("run is closed")
        if (activeStart == null) activeStart = TimeSource.Monotonic.markNow()
        if (termination() == null) {
            val parallelism = if (config.execution.mode == ExecutionMode.Serial) 1 else config.execution.parallelism
            var reserved = active.values.fold(0L) { sum, entry -> sum + entry.first.allowance }
            var reservedSteps = active.values.fold(0L) { sum, entry -> sum + entry.first.stepAllowance }
            val available = workers.filter { !it.disabled && it.id !in active }.map { it.id }.toMutableList()
            while (active.size < parallelism && available.isNotEmpty() && dispatched < (config.budget.decisions ?: Long.MAX_VALUE)) {
                val remaining = budget.limit - accounted - reserved
                val remainingSteps = (config.budget.steps ?: Long.MAX_VALUE) - steps - reservedSteps
                if (remaining <= 0 || remainingSteps <= 0) break
                val extracted = searchBoundary { state() }
                if (extracted !is Ok) return searchFailure(extracted)
                val context = extracted.value
                val selected = searchBoundary { policy.select(context, available, random) }.then { it }
                if (selected !is Ok) return selected.map { false }
                if (selected.value !in available) return searchInvalid("selector returned a masked action")
                val worker = workers.first { it.id == selected.value }
                val allocated = searchBoundary { allocator.allocate(worker.id, config.budget.sliceEvaluations, remaining) }
                if (allocated !is Ok) return searchFailure(allocated)
                val allowance = allocated.value
                if (allowance <= 0 || allowance > remaining || allowance > config.budget.sliceEvaluations) return searchInvalid("allocator returned an invalid allowance")
                val exchanged = searchBoundary {
                    if (!worker.initialized && worker.initialization == InitializationMode.WorkingSet) memory.working.map { it.solution.toList() }
                    else exchange.select(completed, worker.id, memory.working).map { it.solution.toList() }
                }
                if (exchanged !is Ok) return searchFailure(exchanged)
                val seeds = exchanged.value
                val dispatch = Dispatch(
                    worker = worker,
                    decision = dispatched++,
                    context = context,
                    available = available.toList(),
                    revision = policy.revision,
                    reference = memory.archive,
                    seeds = seeds,
                    allowance = allowance,
                    stepAllowance = minOf(config.budget.sliceSteps, remainingSteps)
                )
                available.remove(worker.id)
                reserved += allowance
                reservedSteps += dispatch.stepAllowance
                val job = scope.async {
                    val outcome = Outcome(dispatch, execute(dispatch))
                    if (config.execution.mode == ExecutionMode.Asynchronous) completions.send(outcome)
                    outcome
                }
                active[dispatch.worker.id] = Pair(dispatch, job)
            }
        }
        if (config.execution.mode == ExecutionMode.Asynchronous && termination() == null && active.isNotEmpty()) {
            commit(completions.receive())
        } else {
            drain()
        }
        if (active.isEmpty()) {
            accumulatedNanos = elapsed()
            activeStart = null
        }
        if (diagnostics.isNotEmpty()) return Failed(ErrorCode.ApplicationFailed, diagnostics.joinToString("; "), result())
        return Ok(termination() == null || active.isNotEmpty())
    }

    private suspend fun execute(dispatch: Dispatch<O, V>): Ret<SearchStepResult<O, V>> {
        val duration = config.budget.timeMillis?.let { (it.milliseconds - elapsed().nanoseconds).coerceAtLeast(Duration.ZERO) } ?: Duration.INFINITE
        val slice = SearchSlice(
            global = budget,
            evaluations = dispatch.allowance,
            steps = dispatch.stepAllowance,
            duration = duration
        )
        val worker = dispatch.worker
        val candidates = ArrayList<Individual<O, V>>()
        return try { searchBoundary {
            if (worker.initialized && dispatch.seeds.isNotEmpty()) {
                val imported = worker.session.importSolutions(dispatch.seeds, slice.child(dispatch.allowance, 0))
                if (imported !is Ok) return@searchBoundary imported
                candidates.addAll(imported.value.candidates)
            }
            val report = worker.session.step(slice.child((dispatch.allowance - slice.used).coerceAtLeast(0), dispatch.stepAllowance))
            if (report !is Ok) return@searchBoundary report
            val result = report.value
            var importedCandidates = emptyList<Individual<O, V>>()
            if (!worker.initialized && result.steps > 0 && dispatch.seeds.isNotEmpty()) {
                val imported = worker.session.importSolutions(dispatch.seeds, slice.child((dispatch.allowance - slice.used).coerceAtLeast(0), 0))
                if (imported !is Ok) return@searchBoundary imported
                importedCandidates = imported.value.candidates
            }
            if (result.steps > 0) worker.initialized = true
            Ok(SearchStepResult(
                candidates = candidates + result.candidates + importedCandidates,
                evaluations = slice.used,
                steps = result.steps,
                termination = result.termination,
                diagnostics = result.diagnostics
            ))
        }.then { it } } finally { dispatch.consumed = slice.used }
    }

    private fun commit(outcome: Outcome<O, V>) {
        val result = searchBoundary { commitOutcome(outcome) }
        if (result !is Ok) {
            diagnostics.add("归并回调失败 / Merge callback failed: $result")
            val dispatch = outcome.dispatch
            if (events.none { it.scope == "algorithm" && it.decision == dispatch.decision }) {
                val transition = SelectionTransition(
                    episode = "${problem.id}:${problem.fingerprint}:${config.execution.seed}",
                    decision = dispatch.decision,
                    action = dispatch.worker.id,
                    before = dispatch.context,
                    after = dispatch.context,
                    available = dispatch.available,
                    nextAvailable = emptyList(),
                    reward = Flt64.zero,
                    evaluations = dispatch.consumed,
                    terminal = true,
                    policyVersion = dispatch.revision,
                    featureVersion = features.version,
                    failed = true
                )
                events.add(transition)
                val observed = searchBoundary { policy.observe(transition) }.then { it }
                if (observed !is Ok) diagnostics.add("归并失败反馈失败 / Merge-failure feedback failed: $observed")
            }
        }
    }

    private fun commitOutcome(outcome: Outcome<O, V>) {
        val dispatch = outcome.dispatch
        if (active.remove(dispatch.worker.id) == null) {
            diagnostics.add("重复完成事件 / Duplicate completion: ${dispatch.decision}")
            return
        }
        ++completed
        accounted += dispatch.consumed
        val report = outcome.result
        val operatorEvents = (dispatch.worker.session as? PolicyCarrier)?.drainTransitions() ?: emptyList()
        events.addAll(operatorEvents.map { it.copy(
            episode = "${problem.id}:${problem.fingerprint}:${config.execution.seed}",
            scope = "${dispatch.worker.id}/${it.scope}"
        ) })
        if (report !is Ok) {
            dispatch.worker.disabled = true
            diagnostics.add("算法执行失败 / Algorithm failed: ${dispatch.worker.id}: $report")
            val transition = SelectionTransition(
                episode = "${problem.id}:${problem.fingerprint}:${config.execution.seed}",
                decision = dispatch.decision,
                action = dispatch.worker.id,
                before = dispatch.context,
                after = dispatch.context,
                available = dispatch.available,
                nextAvailable = emptyList(),
                reward = Flt64.zero,
                evaluations = dispatch.consumed,
                terminal = true,
                policyVersion = dispatch.revision,
                featureVersion = features.version,
                failed = true
            )
            events.add(transition)
            val observed = policy.observe(transition)
            if (observed !is Ok) diagnostics.add("失败动作反馈失败 / Failed-action feedback failed: $observed")
            return
        }
        val result = report.value
        steps += result.steps
        if (result.termination == SearchTermination.Failed) {
            dispatch.worker.disabled = true
            diagnostics.addAll(result.diagnostics.ifEmpty { listOf("算法执行失败 / Algorithm failed: ${dispatch.worker.id}") })
        }
        if (result.steps == 0L && result.termination == SearchTermination.EvaluationLimit &&
            config.budget.evaluations == null && config.budget.decisions == null && config.budget.timeMillis == null) {
            dispatch.worker.disabled = true
            diagnostics.add("片段评估额度不足以提交完整步骤 / Slice allowance cannot commit a complete step: ${dispatch.worker.id}")
        }
        val contribution = memory.contribution(dispatch.reference, result.candidates)
        val improved = memory.offer(result.candidates)
        stagnation = if (improved) 0 else stagnation + 1
        if (improved) ++successes
        lastCost = result.evaluations
        if (result.termination == SearchTermination.NoCandidate || result.evaluations == 0L && result.steps == 0L) dispatch.worker.disabled = true
        val value = reward.reward(RewardContext(
            improved = contribution.first,
            contribution = Flt64(contribution.second),
            evaluations = result.evaluations
        ))
        if (!value.toDouble().isFinite()) {
            diagnostics.add("奖励不是有限数 / Reward is not finite")
            return
        }
        val nextAvailable = workers.filter { !it.disabled && it.id !in active }.map { it.id }
        val transition = SelectionTransition(
            episode = "${problem.id}:${problem.fingerprint}:${config.execution.seed}",
            decision = dispatch.decision,
            action = dispatch.worker.id,
            before = dispatch.context,
            after = state(),
            available = dispatch.available,
            nextAvailable = nextAvailable,
            reward = value,
            evaluations = result.evaluations,
            terminal = termination() != null,
            policyVersion = dispatch.revision,
            featureVersion = features.version,
            steps = result.steps,
            mergedImprovement = improved,
            failed = result.termination == SearchTermination.Failed
        )
        events.add(transition)
        val observed = policy.observe(transition)
        if (observed !is Ok) diagnostics.add("策略反馈失败 / Policy feedback failed: $observed")
    }

    private suspend fun drain() {
        if (config.execution.mode == ExecutionMode.Asynchronous) {
            while (active.isNotEmpty()) commit(completions.receive())
        } else {
            for ((_, entry) in active.toSortedMap()) commit(entry.second.await())
        }
    }

    /** 运行到全局停止条件 / Run until a global stopping condition.
     * @return 搜索结果 / Search result
     */
    suspend fun run(): Ret<SearchResult<O, V>> {
        while (true) {
            val advanced = advance()
            if (advanced !is Ok) return advanced.map { result() }
            if (config.persistence.enabled && config.persistence.everyDecisions > 0 && completed > 0 && completed % config.persistence.everyDecisions == 0L) {
                val snapshot = checkpoint()
                if (snapshot !is Ok) return snapshot.map { result() }
                val path = Path.of(config.persistence.checkpointPath).toAbsolutePath()
                val saved = FileSearchStore(path.parent).saveCheckpoint(path.fileName.toString(), snapshot.value)
                if (saved !is Ok) return saved.map { result() }
            }
            if (!advanced.value) {
                val path = config.learning.trajectoryPath
                if (path != null) {
                    val written = TrajectoryFiles.write(Path.of(path), events)
                    if (written !is Ok) return searchFailure(written)
                }
                return Ok(result())
            }
        }
    }

    /** 导出学习参数，不包含实例搜索状态 / Export learned parameters without instance search state.
     * @return 可迁移策略 / Transferable policy
     */
    suspend fun exportPolicy(): Ret<PolicyArchive> = gate.withLock {
        if (closed) return@withLock searchInvalid("run is closed")
        drain()
        accumulatedNanos = elapsed()
        activeStart = null
        Ok(policyArchive())
    }

    private fun policyArchive(): PolicyArchive = PolicyArchive(
        definition = definition,
        featureVersion = features.version,
        root = policy.snapshot(),
        operators = workers.associate { worker -> worker.id to ((worker.session as? PolicyCarrier)?.policies() ?: emptyMap()) },
        definitions = definitionDocument
    )

    internal fun restorePolicy(saved: PolicyArchive): Try {
        if (saved.version != 1 || saved.definition != definition || saved.definitions != definitionDocument || saved.featureVersion != features.version || saved.operators.keys != workers.map { it.id }.toSet()) return searchInvalid("incompatible policy definitions")
        val result = policy.restore(saved.root)
        if (result !is Ok) return result
        for (worker in workers) {
            val carrier = worker.session as? PolicyCarrier
            val policies = saved.operators.getValue(worker.id)
            if (carrier == null && policies.isNotEmpty()) return searchInvalid("missing operator policy carrier")
            val restored = carrier?.restorePolicies(policies) ?: ok
            if (restored !is Ok) return restored
        }
        return ok
    }

    /** 停止派发并归并全部在途结果后保存 / Stop dispatching and merge all in-flight outcomes before saving.
     * @return 完整静止检查点 / Complete quiescent checkpoint
     */
    suspend fun checkpoint(): Ret<RunCheckpoint> = gate.withLock {
        if (!config.persistence.enabled || closed) return@withLock searchInvalid("checkpointing is disabled or run is closed")
        drain()
        accumulatedNanos = elapsed()
        activeStart = null
        val componentStates = linkedMapOf<String, JsonElement>()
        for ((id, component) in components) {
            val saved = searchBoundary { component.save() }.then { it }
            if (saved !is Ok) return@withLock searchFailure(saved)
            componentStates[id] = saved.value
        }
        val snapshots = linkedMapOf<String, WorkerCheckpoint>()
        for (worker in workers) {
            val snapshot = searchBoundary { worker.session.snapshot() }.then { it }
            if (snapshot !is Ok) return@withLock searchFailure(snapshot)
            val problemState = searchBoundary { worker.problem.state?.save() ?: Ok(JsonNull) }.then { it }
            if (problemState !is Ok) return@withLock searchFailure(problemState)
            snapshots[worker.id] = WorkerCheckpoint(
                session = snapshot.value,
                random = worker.random.state,
                initialized = worker.initialized,
                disabled = worker.disabled,
                problemState = problemState.value
            )
        }
        val problemState = searchBoundary { problem.state?.save() ?: Ok(JsonNull) }.then { it }
        if (problemState !is Ok) return@withLock searchFailure(problemState)
        codec.encode(memory.archive).then { archive ->
            codec.encode(memory.working).map { working ->
                RunCheckpoint(
                    configuration = configFingerprint,
                    problem = "${problem.id}:${problem.fingerprint}",
                    policy = policyArchive(),
                    workers = snapshots,
                    archive = archive,
                    working = working,
                    evaluations = budget.used,
                    dispatched = dispatched,
                    completed = completed,
                    steps = steps,
                    stagnation = stagnation,
                    activeNanos = accumulatedNanos,
                    random = random.state,
                    diagnostics = diagnostics.toList(),
                    problemState = problemState.value,
                    objectiveCalls = budget.objectiveCalls,
                    transitions = events.toList(),
                    successes = successes,
                    lastCost = lastCost,
                    configurationData = Json.encodeToJsonElement(config),
                    components = componentStates
                )
            }
        }
    }

    internal fun restore(saved: RunCheckpoint): Try {
        if (!config.persistence.enabled || saved.version != 1 || saved.configuration != configFingerprint || fingerprint(saved.configurationData) != configFingerprint || saved.problem != "${problem.id}:${problem.fingerprint}" ||
            saved.components.keys != components.keys || saved.workers.keys != workers.map { it.id }.toSet() || listOf(saved.evaluations, saved.dispatched, saved.completed, saved.steps, saved.stagnation, saved.activeNanos).any { it < 0 } ||
            saved.objectiveCalls < 0 || saved.objectiveCalls > saved.evaluations || saved.evaluations > budget.limit || saved.completed != saved.dispatched) return searchInvalid("incompatible or non-quiescent checkpoint")
        return codec.decode(saved.archive).then { archive ->
            codec.decode(saved.working).then { working ->
                for ((id, component) in components) {
                    val loaded = component.load(saved.components.getValue(id))
                    if (loaded !is Ok) return@then loaded
                }
                val restored = restorePolicy(saved.policy)
                if (restored !is Ok) return@then restored
                for (worker in workers) {
                    val checkpoint = saved.workers.getValue(worker.id)
                    val result = worker.session.restore(checkpoint.session)
                    if (result !is Ok) return@then result
                    val problemResult = worker.problem.state?.load(checkpoint.problemState) ?: if (checkpoint.problemState == JsonNull) ok else searchInvalid("missing problem state codec")
                    if (problemResult !is Ok) return@then problemResult
                    worker.random.state = checkpoint.random
                    worker.initialized = checkpoint.initialized
                    worker.disabled = checkpoint.disabled
                }
                val problemResult = problem.state?.load(saved.problemState) ?: if (saved.problemState == JsonNull) ok else searchInvalid("missing coordinator problem codec")
                if (problemResult !is Ok) return@then problemResult
                memory.restore(archive, working)
                budget = SearchBudget(
                    limit = budget.limit,
                    consumed = saved.evaluations,
                    objectiveCalls = saved.objectiveCalls
                )
                accounted = saved.evaluations
                dispatched = saved.dispatched
                completed = saved.completed
                steps = saved.steps
                stagnation = saved.stagnation
                successes = saved.successes
                lastCost = saved.lastCost
                accumulatedNanos = saved.activeNanos
                random.state = saved.random
                diagnostics.addAll(saved.diagnostics)
                events.addAll(saved.transitions)
                ok
            }
        }
    }

    /** 取消剩余任务并释放模型；可重复调用 / Cancel remaining tasks and release models; idempotent.
     * @return 关闭结果 / Close result
     */
    suspend fun close(): Try = gate.withLock {
        if (closed) return@withLock ok
        drain()
        accumulatedNanos = elapsed()
        activeStart = null
        scope.cancel()
        active.values.forEach { it.second.cancelAndJoin() }
        active.clear()
        completions.close()
        closed = true
        val errors = ArrayList<fuookami.ospf.kotlin.utils.error.Error<ErrorCode>>()
        fun collect(result: Ret<*>) {
            when (result) {
                is Ok -> Unit
                is Failed -> errors.add(result.error)
                is Fatal -> errors.addAll(result.errors)
            }
        }
        for (worker in workers) {
            collect(searchBoundary { worker.session.close() }.then { it })
            collect(searchBoundary { worker.problem.model.close() })
        }
        collect(searchBoundary { problem.model.close() })
        if (errors.isEmpty()) ok else Fatal(errors)
    }
}
