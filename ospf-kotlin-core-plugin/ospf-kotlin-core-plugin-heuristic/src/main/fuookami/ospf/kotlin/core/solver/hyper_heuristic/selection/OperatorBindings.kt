package fuookami.ospf.kotlin.core.solver.hyper_heuristic.selection

import kotlinx.serialization.*
import kotlinx.serialization.json.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.core.model.basic.Solution
import fuookami.ospf.kotlin.core.model.callback.AbstractCallBackModelInterface
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.solver.heuristic.*
import fuookami.ospf.kotlin.core.solver.heuristic.operator.update.SolutionUpdate
import fuookami.ospf.kotlin.core.solver.heuristic.operator.mutation.Mutation
import fuookami.ospf.kotlin.core.solver.heuristic.operator.evolution.ParameterSource
import fuookami.ospf.kotlin.core.solver.heuristic.operator.neighborhood.Neighborhood
import fuookami.ospf.kotlin.core.solver.hyper_heuristic.memory.*
import fuookami.ospf.kotlin.core.solver.hyper_heuristic.learning.*

/** 有状态扩展组件的显式恢复协议 / Explicit recovery protocol for stateful extension components. */
interface RecoverableComponent {
    /** 导出组件状态 / Export component state.
     * @return JSON 状态 / JSON state
     */
    fun save(): Ret<JsonElement>

    /** 恢复组件状态 / Restore component state.
     * @param state JSON 状态 / JSON state
     * @return 恢复结果 / Restore result
     */
    fun load(state: JsonElement): Try
}

/** 类型明确的候选池 / Typed candidate pool.
 * @property id 槽位标识 / Slot identifier
 * @property candidates 稳定动作 ID 到实现 / Stable action IDs to implementations
 * @property policy 选择策略 / Selection policy
 * @property random 独立选择随机流 / Independent selection random stream
 * @property stateless 已声明无状态的组件 ID / Components explicitly declared stateless
 */
class OperatorPool<T>(
    val id: String,
    val candidates: Map<String, T>,
    val policy: SelectionPolicy,
    val random: SearchRandom,
    private val stateless: Set<String>
) {
    private var action = candidates.keys.first()
    private var used = false
    private var before = SelectionContext()
    private var revision = 0L
    private val transitions = ArrayList<SelectionTransition>()

    internal fun choose(context: SelectionContext): Try {
        return policy.select(context, candidates.keys.toList(), random).map {
            action = it
            before = context
            revision = policy.revision
            used = false
            success
        }
    }

    internal val usedAction: String? get() = if (used) action else null

    internal fun discard() {
        policy.discard(before.decision)
    }

    internal fun current(): T {
        used = true
        return candidates.getValue(action)
    }

    internal fun observe(
        after: SelectionContext,
        reward: Flt64,
        evaluations: Long,
        terminal: Boolean,
        elapsedNanos: Long,
        steps: Long,
        failed: Boolean = false
    ): Try {
        if (!used) return ok
        val transition = SelectionTransition(
            episode = id,
            decision = before.decision,
            action = action,
            before = before,
            after = after,
            available = candidates.keys.toList(),
            nextAvailable = candidates.keys.toList(),
            reward = reward,
            evaluations = evaluations,
            terminal = terminal,
            policyVersion = revision,
            scope = id,
            steps = steps,
            elapsedNanos = elapsedNanos,
            previousAction = policy.predecessor(before.decision),
            failed = failed
        )
        transitions.add(transition)
        return policy.observe(transition)
    }

    internal fun drainTransitions(): List<SelectionTransition> {
        val result = transitions.toList()
        transitions.clear()
        return result
    }

    internal fun save(): Ret<SlotSnapshot> {
        val states = linkedMapOf<String, JsonElement>()
        for ((id, candidate) in candidates) {
            if (id in stateless) continue
            val component = candidate as? RecoverableComponent ?: return searchInvalid("slot $id is not recoverable")
            when (val result = component.save()) {
                is Ok -> states[id] = result.value
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
        }
        return Ok(SlotSnapshot(
            policy = policy.snapshot(),
            random = random.state,
            components = states
        ))
    }

    internal fun load(snapshot: SlotSnapshot): Try {
        if (snapshot.components.keys != candidates.keys - stateless) return searchInvalid("incompatible slot components: $id")
        val result = policy.restore(snapshot.policy)
        if (result !is Ok) return result
        for ((key, state) in snapshot.components) {
            val component = candidates[key] as? RecoverableComponent ?: return searchInvalid("slot $key is not recoverable")
            val loaded = component.load(state)
            if (loaded !is Ok) return loaded
        }
        random.state = snapshot.random
        return ok
    }
}

@Serializable
internal data class SlotSnapshot(
    val policy: PolicySnapshot,
    val random: Long,
    val components: Map<String, JsonElement>
)

/** 骨架支持的类型安全槽位 / Type-safe slots supported by skeletons.
 * @property update 更新池 / Update pool
 * @property mutation 变异池 / Mutation pool
 * @property neighborhood 邻域池 / Neighborhood pool
 * @property parameters 命名参数源池 / Named parameter-source pools
 */
class OperatorBindings(
    val update: OperatorPool<SolutionUpdate>?,
    val mutation: OperatorPool<Mutation>?,
    val neighborhood: OperatorPool<Neighborhood>?,
    val parameters: Map<String, OperatorPool<ParameterSource>>
) {
    internal val pools: List<OperatorPool<*>> get() = listOfNotNull(update, mutation, neighborhood) + parameters.toSortedMap().values

    /** 更新槽位适配器 / Update slot adapter. */
    val solutionUpdate: SolutionUpdate? get() = update?.let { pool ->
        object : SolutionUpdate {
            override fun <V, T> invoke(
                iteration: Iteration,
                parents: List<T>,
                model: AbstractCallBackModelInterface<*, *, V>,
                converter: IntoValue<V>,
                entropy: Generator<Flt64>
            ): List<Solution<V>> where V : RealNumber<V>, V : NumberField<V>, T : Individual<*, V> {
                return pool.current()(iteration, parents, model, converter, entropy)
            }
        }
    }

    /** 变异槽位适配器 / Mutation slot adapter. */
    val mutationOperator: Mutation? get() = mutation?.let { pool ->
        object : Mutation {
            override fun <V, T> invoke(
                iteration: Iteration,
                individual: T,
                model: AbstractCallBackModelInterface<*, *, V>,
                converter: IntoValue<V>,
                entropy: Generator<Flt64>,
                mutationRate: Flt64
            ): Solution<V> where V : RealNumber<V>, V : NumberField<V>, T : Individual<*, V> {
                return pool.current()(iteration, individual, model, converter, entropy, mutationRate)
            }
        }
    }

    /** 邻域槽位适配器 / Neighborhood slot adapter. */
    val neighborhoodOperator: Neighborhood? get() = neighborhood?.let { pool ->
        object : Neighborhood {
            override fun <V> invoke(
                iteration: Iteration,
                individual: Individual<*, V>,
                model: AbstractCallBackModelInterface<*, *, V>,
                entropy: Generator<Flt64>,
                converter: IntoValue<V>
            ): Solution<V> where V : RealNumber<V>, V : NumberField<V> {
                return pool.current()(iteration, individual, model, entropy, converter)
            }
        }
    }

    /** 参数槽位适配器 / Parameter slot adapter.
     * @param name 参数名称 / Parameter name
     * @param default 默认源 / Default source
     * @return 参数源 / Parameter source
     */
    fun parameter(name: String, default: ParameterSource): ParameterSource {
        val pool = parameters[name] ?: return default
        return ParameterSource { iteration -> pool.current()(iteration) }
    }
}

@Serializable
private data class ControlledSnapshot(
    val session: SearchSessionSnapshot,
    val slots: Map<String, SlotSnapshot>,
    val localArchive: JsonElement,
    val initialized: Boolean,
    val decisions: Long,
    val stagnation: Long,
    val rewardState: JsonElement = JsonNull,
    val rewardStateVersion: Int = 0,
    @SerialName("reward") val legacyReward: JsonElement = JsonNull
)

/** 在真实低层步边界选择和反馈算子 / Select and credit operators at actual low-level step boundaries.
 * @property delegate 原生会话 / Native session
 * @property problem 问题语义 / Problem semantics
 * @property bindings 类型安全槽位 / Type-safe slots
 * @property reward 奖励策略 / Reward policy
 */
class ControlledSearchSession<O, V>(
    private val delegate: SearchSession<O, V>,
    private val problem: SearchProblem<O, V>,
    private val bindings: OperatorBindings,
    private val reward: RewardPolicy
) : SearchSession<O, V>, PolicyCarrier where V : RealNumber<V>, V : NumberField<V> {
    private val memory = SearchMemory(
        problem = problem,
        capacity = 100,
        workingCapacity = 20,
        acceptance = NotWorseAcceptancePolicy()
    )
    private var initialized = false
    private var decisions = 0L
    private var stagnation = 0L

    override val supportsImport: Boolean get() = delegate.supportsImport

    override suspend fun step(budget: SearchSlice): Ret<SearchStepResult<O, V>> {
        val candidates = ArrayList<Individual<O, V>>()
        var evaluations = 0L
        var steps = 0L
        var reason = SearchTermination.SliceComplete
        val diagnostics = ArrayList<String>()
        repeat(budget.steps.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()) {
            val before = ProgressFeatures.extract(SearchProgress(
                decision = decisions,
                evaluations = 0,
                evaluationLimit = Long.MAX_VALUE,
                stagnation = stagnation,
                diversity = 0.0
            ))
            if (initialized) {
                for (pool in bindings.pools) {
                    val selected = pool.choose(before)
                    if (selected !is Ok) {
                        bindings.pools.forEach { it.discard() }
                        return selected.map { SearchStepResult<O, V>(
                            candidates = emptyList(),
                            evaluations = evaluations,
                            steps = steps,
                            termination = SearchTermination.Failed
                        ) }
                    }
                }
            }
            val reference = memory.archive
            val slice = budget.child(budget.evaluations - evaluations, 1)
            val started = System.nanoTime()
            val outcome = searchBoundary { delegate.step(slice) }.then { it }
            val elapsedNanos = (System.nanoTime() - started).coerceAtLeast(0)
            if (outcome !is Ok) {
                if (initialized) {
                    for (pool in bindings.pools) {
                        if (pool.usedAction == null) {
                            pool.discard()
                            continue
                        }
                        val observed = searchBoundary { pool.observe(
                            after = before,
                            reward = Flt64.zero,
                            evaluations = slice.used,
                            terminal = true,
                            elapsedNanos = elapsedNanos,
                            steps = 0,
                            failed = true
                        ) }.then { it }
                        if (observed !is Ok) diagnostics.add("失败算子反馈失败 / Failed-operator feedback failed: $observed")
                    }
                }
                return Ok(SearchStepResult(
                    candidates = candidates,
                    evaluations = evaluations + slice.used,
                    steps = steps,
                    termination = SearchTermination.Failed,
                    diagnostics = diagnostics + "低层执行失败 / Low-level execution failed: $outcome"
                ))
            }
            val result = outcome.value
            val contribution = memory.contribution(reference, result.candidates)
            val creditContext = rewardContext(
                problem = problem,
                reference = reference,
                candidates = result.candidates,
                contribution = contribution,
                evaluations = result.evaluations,
                elapsedNanos = elapsedNanos,
                action = "operator"
            )
            stagnation = if (contribution.first) 0 else stagnation + 1
            val after = ProgressFeatures.extract(SearchProgress(
                decision = decisions + 1,
                evaluations = 0,
                evaluationLimit = Long.MAX_VALUE,
                stagnation = stagnation,
                diversity = 0.0
            ))
            if (initialized) {
                for (pool in bindings.pools) {
                    val action = pool.usedAction
                    if (action == null) {
                        pool.discard()
                        continue
                    }
                    val rewarded = searchBoundary { reward.evaluate(creditContext.copy(action = "${pool.id}/$action")) }.then { it }
                    val validReward = rewarded is Ok && rewarded.value.toDouble().isFinite()
                    if (!validReward) diagnostics.add("算子奖励无效 / Invalid operator reward: $rewarded")
                    val observed = searchBoundary { pool.observe(
                        after = after,
                        reward = if (validReward) rewarded.value else Flt64.zero,
                        evaluations = result.evaluations,
                        terminal = !validReward || result.termination != SearchTermination.SliceComplete || budget.global.used >= budget.global.limit,
                        elapsedNanos = elapsedNanos,
                        steps = result.steps,
                        failed = !validReward || result.termination == SearchTermination.Failed
                    ) }.then { it }
                    if (observed !is Ok) diagnostics.add("算子反馈失败 / Operator feedback failed: $observed")
                }
            }
            memory.offer(result.candidates)
            initialized = initialized || result.steps > 0
            ++decisions
            candidates.addAll(result.candidates)
            evaluations += result.evaluations
            steps += result.steps
            reason = if (diagnostics.isEmpty()) result.termination else SearchTermination.Failed
            diagnostics.addAll(result.diagnostics)
            if (reason != SearchTermination.SliceComplete || evaluations >= budget.evaluations || budget.expired) {
                return Ok(SearchStepResult(
                    candidates = candidates,
                    evaluations = evaluations,
                    steps = steps,
                    termination = reason,
                    diagnostics = diagnostics.toList()
                ))
            }
        }
        return Ok(SearchStepResult(
            candidates = candidates,
            evaluations = evaluations,
            steps = steps,
            termination = reason,
            diagnostics = diagnostics.toList()
        ))
    }

    override suspend fun importSolutions(solutions: List<Solution<V>>, budget: SearchSlice): Ret<SearchStepResult<O, V>> = delegate.importSolutions(solutions, budget)

    override fun snapshot(): Ret<SearchSessionSnapshot> {
        val slots = linkedMapOf<String, SlotSnapshot>()
        for (pool in bindings.pools) {
            when (val result = pool.save()) {
                is Ok -> slots[pool.id] = result.value
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
        }
        val rewardState = (reward as? RecoverableComponent)?.save() ?: Ok(JsonNull)
        if (rewardState !is Ok) return searchFailure(rewardState)
        return delegate.snapshot().then { native ->
            SearchCodecs.list(SearchCodecs.individual(problem)).encode(memory.archive).map { archive ->
                native.copy(state = Json.encodeToJsonElement(ControlledSnapshot(
                    session = native,
                    slots = slots,
                    localArchive = archive,
                    initialized = initialized,
                    decisions = decisions,
                    stagnation = stagnation,
                    rewardState = rewardState.value,
                    rewardStateVersion = if (rewardState.value == JsonNull) 0 else 1
                )))
            }
        }
    }

    override fun restore(snapshot: SearchSessionSnapshot): Try {
        return searchBoundary { Json.decodeFromJsonElement<ControlledSnapshot>(snapshot.state) }.then { saved ->
            if (saved.rewardStateVersion !in 0..1 ||
                ("rewardState" in snapshot.state.jsonObject && "reward" in snapshot.state.jsonObject) ||
                (saved.rewardStateVersion != 0 && saved.rewardState == JsonNull)) {
                return@then searchInvalid("不支持或冲突的算子奖励状态 / Unsupported or conflicting operator reward state")
            }
            val state = if (saved.rewardState == JsonNull) saved.legacyReward else saved.rewardState
            val restoredReward = (reward as? RecoverableComponent)?.load(state) ?: if (state == JsonNull) ok else searchInvalid("unexpected reward state")
            if (restoredReward !is Ok) return@then restoredReward
            if (saved.slots.keys != bindings.pools.map { it.id }.toSet()) return@then searchInvalid("operator slots changed")
            for (pool in bindings.pools) {
                val restored = pool.load(saved.slots.getValue(pool.id))
                if (restored !is Ok) return@then restored
            }
            SearchCodecs.list(SearchCodecs.individual(problem)).decode(saved.localArchive).then { archive ->
                delegate.restore(saved.session).map {
                    memory.restore(archive, emptyList())
                    initialized = saved.initialized
                    decisions = saved.decisions
                    stagnation = saved.stagnation
                    success
                }
            }
        }
    }

    override fun close(): Try = delegate.close()

    override fun drainTransitions(): List<SelectionTransition> = bindings.pools.flatMap { it.drainTransitions() }

    override fun policies(): Map<String, PolicySnapshot> = bindings.pools.associate { it.id to it.policy.snapshot() }

    override fun restorePolicies(policies: Map<String, PolicySnapshot>): Try {
        if (policies.keys != bindings.pools.map { it.id }.toSet()) return searchInvalid("incompatible operator policy slots")
        for (pool in bindings.pools) {
            val restored = pool.policy.restore(policies.getValue(pool.id))
            if (restored !is Ok) return restored
        }
        return ok
    }
}
