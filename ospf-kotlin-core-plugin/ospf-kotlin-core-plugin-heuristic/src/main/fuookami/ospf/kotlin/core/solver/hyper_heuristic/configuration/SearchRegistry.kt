package fuookami.ospf.kotlin.core.solver.hyper_heuristic.configuration

import kotlinx.serialization.json.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.solver.heuristic.*
import fuookami.ospf.kotlin.core.solver.heuristic.operator.update.*
import fuookami.ospf.kotlin.core.solver.heuristic.operator.mutation.*
import fuookami.ospf.kotlin.core.solver.heuristic.operator.evolution.*
import fuookami.ospf.kotlin.core.solver.heuristic.operator.neighborhood.*
import fuookami.ospf.kotlin.core.solver.hyper_heuristic.*
import fuookami.ospf.kotlin.core.solver.hyper_heuristic.memory.*
import fuookami.ospf.kotlin.core.solver.hyper_heuristic.learning.*
import fuookami.ospf.kotlin.core.solver.hyper_heuristic.selection.*
import fuookami.ospf.kotlin.core.solver.hyper_heuristic.scheduling.BudgetAllocator
import fuookami.ospf.kotlin.core.solver.hyper_heuristic.scheduling.FixedBudgetAllocator
import fuookami.ospf.kotlin.core.solver.hyper_heuristic.scheduling.CompositeSearchSession

/** 算法创建上下文 / Algorithm construction context.
 * @property config 算法配置 / Algorithm configuration
 * @property problem 独占问题 / Exclusively owned problem
 * @property random 可恢复随机流 / Restorable random stream
 * @property bindings 类型安全槽位 / Type-safe slots
 */
data class AlgorithmContext<O, V>(
    val config: AlgorithmConfig,
    val problem: SearchProblem<O, V>,
    val random: SearchRandom,
    val bindings: OperatorBindings
) where V : RealNumber<V>, V : NumberField<V>

private class Entry<T>(
    val stateless: Boolean,
    val encodings: Set<SearchEncoding> = setOf(SearchEncoding.Continuous),
    val create: (ComponentSpec) -> Ret<T>
)

/** 显式组件注册表，每次创建独立组件 / Explicit registry creating independent components.
 * @property converter 低层数值适配 / Low-level value adapter
 */
class SearchRegistry<O, V>(val converter: IntoValue<V>) where V : RealNumber<V>, V : NumberField<V> {
    private val problems = linkedMapOf<String, (ComponentSpec, SearchRandom) -> Ret<SearchProblem<O, V>>>()
    private val statelessProblems = hashSetOf<String>()
    private val statelessComponents = hashSetOf(
        "features:progress", "reward:improvement", "reward:cost-aware", "acceptance:not-worse", "acceptance:always",
        "exchange:none", "exchange:periodic", "allocator:fixed", "reward:time-normalized", "reward:dominance"
    )

    internal fun componentRecoverable(
        kind: String,
        spec: ComponentSpec,
        component: Any
    ): Boolean {
        return "$kind:${spec.type}" in statelessComponents || component is RecoverableComponent
    }
    private val algorithms = linkedMapOf<String, (AlgorithmContext<O, V>) -> Ret<SearchSession<O, V>>>()
    private val constraintAlgorithms = (BuiltInAlgorithms.nativeIds + setOf("nsga-ii", "nsga-iii")).toMutableSet()
    private val updates = linkedMapOf<String, Entry<SolutionUpdate>>()
    private val mutations = linkedMapOf<String, Entry<Mutation>>()
    private val neighborhoods = linkedMapOf<String, Entry<Neighborhood>>()
    private val parameters = linkedMapOf<String, Entry<ParameterSource>>()
    private val selectors = linkedMapOf<String, (ComponentSpec, List<String>, Boolean) -> Ret<SelectionPolicy>>()
    private val featureFactories = linkedMapOf<String, () -> FeatureExtractor>("progress" to { ProgressFeatures })
    private val rewardFactories = linkedMapOf<String, () -> RewardPolicy>("improvement" to { ImprovementRewardPolicy }, "cost-aware" to { CostAwareRewardPolicy })
    private val acceptanceFactories = linkedMapOf<String, () -> AcceptancePolicy<O, V>>(
        "not-worse" to { NotWorseAcceptancePolicy() },
        "always" to { AcceptancePolicy { _, _, _ -> true } }
    )
    private val exchangeFactories = linkedMapOf<String, (ComponentSpec) -> Ret<ExchangePolicy<O, V>>>()
    private val allocatorFactories = linkedMapOf<String, (ComponentSpec) -> Ret<BudgetAllocator>>(
        "fixed" to { spec -> emptyParameters(spec).map { FixedBudgetAllocator } }
    )

    /** 多目标档案裁剪扩展 / Multiobjective archive truncation extension. */
    var archiveTrimmer: (List<Individual<O, V>>, Int) -> List<Individual<O, V>> = { values, amount -> values.take(amount) }

    init {
        for (id in BuiltInAlgorithms.nativeIds) {
            algorithms[id] = { context ->
                if (context.problem.multiObjective) searchInvalid("$id requires explicit scalarization in a multiobjective problem")
                else BuiltInAlgorithms.create(
                    config = context.config,
                    problem = context.problem,
                    random = context.random,
                    converter = converter,
                    bindings = context.bindings
                )
            }
        }
        BuiltInAlgorithms.populationUpdates.forEach { (id, factory) -> updates[id] = Entry(true) { spec -> emptyParameters(spec).map { factory() } } }
        updates["single-point"] = Entry(true) { spec -> emptyParameters(spec).map { SinglePointCross } }
        updates["arithmetic"] = Entry(true) { spec -> emptyParameters(spec).map { ArithmeticCross } }
        updates["order"] = Entry(true, SearchEncoding.entries.toSet()) { spec -> emptyParameters(spec).map { OrderCross } }
        updates["cycle"] = Entry(true, SearchEncoding.entries.toSet()) { spec -> emptyParameters(spec).map { CycleCross } }
        updates["sbx"] = Entry(true) { spec -> numericParameters(spec, setOf("eta")).map { SimulatedBinaryCross(Flt64(spec.parameters.number("eta", 20.0))) } }
        mutations["none"] = Entry(true, SearchEncoding.entries.toSet()) { spec -> emptyParameters(spec).map { NoMutation } }
        mutations["gaussian"] = Entry(true) { spec -> numericParameters(spec, setOf("sigma")).map { GaussianMutation(Flt64(spec.parameters.number("sigma", 0.1))) } }
        mutations["polynomial"] = Entry(true) { spec -> emptyParameters(spec).map { PolynomialMutation() } }
        mutations["swap"] = Entry(true, SearchEncoding.entries.toSet()) { spec -> emptyParameters(spec).map { SwapMutation } }
        mutations["inversion"] = Entry(true, SearchEncoding.entries.toSet()) { spec -> emptyParameters(spec).map { InversionMutation } }
        neighborhoods["perturbation"] = Entry(true) { spec -> numericParameters(spec, setOf("amount")).map { RandomPerturbation(spec.parameters.integer("amount", 1)) } }
        parameters["constant"] = Entry(true) { spec -> numericParameters(spec, setOf("value")).map { ConstantSource(Flt64(spec.parameters.number("value", 1.0))) } }
        parameters["linear"] = Entry(true) { spec -> numericParameters(spec, setOf("from", "to", "limit")).map {
            LinearSource(
                from = Flt64(spec.parameters.number("from", 0.9)),
                to = Flt64(spec.parameters.number("to", 0.4)),
                limit = UInt64(spec.parameters.integer("limit", 1000))
            )
        } }
        parameters["geometric"] = Entry(true) { spec -> numericParameters(spec, setOf("initial", "ratio")).map {
            GeometricSource(Flt64(spec.parameters.number("initial", 100.0)), Flt64(spec.parameters.number("ratio", 0.95)))
        } }
        updates["pattern-search"] = Entry(true) { spec ->
            numericParameters(spec, setOf("initialStep", "contraction", "sweeps")).then {
                val step = spec.parameters.number("initialStep", 0.1)
                val contraction = spec.parameters.number("contraction", 0.5)
                val sweeps = spec.parameters.integer("sweeps", 32)
                if (step <= 0 || contraction <= 0 || contraction >= 1 || sweeps <= 0) searchInvalid("invalid pattern-search parameters")
                else Ok(PatternSearchUpdate(initialStep = step, contraction = contraction, sweeps = sweeps))
            }
        }
        updates["opposition"] = Entry(true) { spec -> emptyParameters(spec).map { OppositionUpdate } }
        for (id in listOf("pipeline", "vnd", "elite-local", "mutation-stage", "neighborhood-stage")) {
            updates[id] = Entry(true, SearchEncoding.entries.toSet()) { spec -> compositeUpdate(spec, 0, SearchEncoding.Continuous) }
        }
        mutations["levy"] = Entry(true) { spec -> emptyParameters(spec).map { LevyMutation() } }
        for (id in listOf("frr-mab", "choice-function", "adaptive-pursuit")) {
            selectors[id] = { spec, actions, learning ->
                numericParameters(spec, setOf("window", "exploration", "decay", "rate", "minimumProbability", "recencyWeight")).then {
                    val policy = NonStationarySelector(
                        type = id,
                        actions = actions,
                        learning = learning,
                        windowSize = spec.parameters.integer("window", 50),
                        exploration = spec.parameters.number("exploration", 0.5),
                        decay = spec.parameters.number("decay", 0.8),
                        rate = spec.parameters.number("rate", 0.2),
                        minimumProbability = spec.parameters.number("minimumProbability", 0.01),
                        recencyWeight = spec.parameters.number("recencyWeight", 0.1)
                    )
                    policy.validate().map { policy }
                }
            }
        }
        exchangeFactories["none"] = { spec -> emptyParameters(spec).map { ExchangePolicy { _, _, _ -> emptyList() } } }
        exchangeFactories["periodic"] = { spec ->
            numericParameters(spec, setOf("period", "amount")).then {
                val period = spec.parameters.integer("period", 5)
                val amount = spec.parameters.integer("amount", 1)
                if (period <= 0 || amount <= 0) searchInvalid("invalid exchange period/amount")
                else Ok(ExchangePolicy { decision, _, working -> if (decision > 0 && decision % period == 0L) working.take(amount) else emptyList() })
            }
        }
    }

    /** 注册问题工厂，工厂必须创建独立模型 / Register a problem factory that must create an independent model.
     * @param id 稳定 ID / Stable ID
     * @param supportsConstraints 是否使用框架个体比较及批量排序协议 / Whether the session honors framework individual comparison and batch ordering
     * @param statelessCallbacks 是否明确声明回调无状态 / Whether callbacks are explicitly stateless
     * @param factory 问题构造 / Problem construction
     * @return 注册结果 / Registration result
     */
    fun registerProblem(
        id: String,
        statelessCallbacks: Boolean = false,
        factory: (ComponentSpec, SearchRandom) -> Ret<SearchProblem<O, V>>
    ): Try {
        return register(
            map = problems,
            id = id,
            value = factory
        ).map {
            if (statelessCallbacks) statelessProblems.add(id)
            success
        }
    }

    internal fun recoverableProblem(spec: ComponentSpec, problem: SearchProblem<O, V>): Boolean = spec.type in statelessProblems || problem.state != null

    /** 注册算法，原生状态由返回会话负责 / Register an algorithm whose session owns native state.
     * @param id 稳定 ID / Stable ID
     * @param supportsConstraints 是否使用框架个体比较及批量排序协议 / Whether the session honors framework individual comparison and batch ordering
     * @param factory 算法构造 / Algorithm construction
     * @return 注册结果 / Registration result
     */
    fun registerAlgorithm(
        id: String,
        supportsConstraints: Boolean = false,
        factory: (AlgorithmContext<O, V>) -> Ret<SearchSession<O, V>>
    ): Try {
        return register(map = algorithms, id = id, value = factory).map {
            if (supportsConstraints) constraintAlgorithms.add(id)
            success
        }
    }

    /** 注册更新算子 / Register an update operator.
     * @param id 稳定 ID / Stable ID
     * @param stateless 是否明确无状态 / Whether explicitly stateless
     * @param encodings 支持的解编码 / Supported solution encodings
     * @param factory 构造函数 / Factory
     * @return 注册结果 / Registration result
     */
    fun registerUpdate(
        id: String,
        stateless: Boolean = false,
        encodings: Set<SearchEncoding> = setOf(SearchEncoding.Continuous),
        factory: (ComponentSpec) -> Ret<SolutionUpdate>
    ): Try = register(updates, id, Entry(stateless, encodings, factory))

    /** 注册变异算子 / Register a mutation operator.
     * @param id 稳定 ID / Stable ID
     * @param stateless 是否明确无状态 / Whether explicitly stateless
     * @param encodings 支持的解编码 / Supported solution encodings
     * @param factory 构造函数 / Factory
     * @return 注册结果 / Registration result
     */
    fun registerMutation(
        id: String,
        stateless: Boolean = false,
        encodings: Set<SearchEncoding> = setOf(SearchEncoding.Continuous),
        factory: (ComponentSpec) -> Ret<Mutation>
    ): Try = register(mutations, id, Entry(stateless, encodings, factory))

    /** 注册邻域算子 / Register a neighborhood operator.
     * @param id 稳定 ID / Stable ID
     * @param stateless 是否明确无状态 / Whether explicitly stateless
     * @param encodings 支持的解编码 / Supported solution encodings
     * @param factory 构造函数 / Factory
     * @return 注册结果 / Registration result
     */
    fun registerNeighborhood(
        id: String,
        stateless: Boolean = false,
        encodings: Set<SearchEncoding> = setOf(SearchEncoding.Continuous),
        factory: (ComponentSpec) -> Ret<Neighborhood>
    ): Try = register(neighborhoods, id, Entry(stateless, encodings, factory))

    /** 注册参数源 / Register a parameter source.
     * @param id 稳定 ID / Stable ID
     * @param stateless 是否明确无状态 / Whether explicitly stateless
     * @param encodings 支持的解编码 / Supported solution encodings
     * @param factory 构造函数 / Factory
     * @return 注册结果 / Registration result
     */
    fun registerParameter(
        id: String,
        stateless: Boolean = false,
        factory: (ComponentSpec) -> Ret<ParameterSource>
    ): Try = register(parameters, id, Entry(stateless, SearchEncoding.entries.toSet(), factory))

    /** 注册选择器 / Register a selector.
     * @param id 稳定 ID / Stable ID
     * @param factory 配置、动作 ID、学习开关到选择器 / Configuration, action IDs and learning flag to selector
     * @return 注册结果 / Registration result
     */
    fun registerSelection(id: String, factory: (ComponentSpec, List<String>, Boolean) -> Ret<SelectionPolicy>): Try = register(selectors, id, factory)

    /** 注册特征提取器 / Register a feature extractor.
     * @param id 稳定 ID / Stable ID
     * @param stateless 是否明确无状态，否则需恢复协议 / Whether explicitly stateless; otherwise recovery is required
     * @param factory 无状态构造函数 / Stateless factory
     * @return 注册结果 / Registration result
     */
    fun registerFeatures(
        id: String,
        stateless: Boolean = false,
        factory: () -> FeatureExtractor
    ): Try = register(featureFactories, id, factory).map {
        if (stateless) statelessComponents.add("features:$id")
        success
    }

    /** 注册奖励函数 / Register a reward function.
     * @param id 稳定 ID / Stable ID
     * @param stateless 是否明确无状态，否则需恢复协议 / Whether explicitly stateless; otherwise recovery is required
     * @param factory 无状态构造函数 / Stateless factory
     * @return 注册结果 / Registration result
     */
    fun registerReward(
        id: String,
        stateless: Boolean = false,
        factory: () -> RewardPolicy
    ): Try = register(rewardFactories, id, factory).map {
        if (stateless) statelessComponents.add("reward:$id")
        success
    }

    /** 注册接受规则 / Register acceptance.
     * @param id 稳定 ID / Stable ID
     * @param stateless 是否明确无状态，否则需恢复协议 / Whether explicitly stateless; otherwise recovery is required
     * @param factory 无状态构造函数 / Stateless factory
     * @return 注册结果 / Registration result
     */
    fun registerAcceptance(
        id: String,
        stateless: Boolean = false,
        factory: () -> AcceptancePolicy<O, V>
    ): Try = register(acceptanceFactories, id, factory).map {
        if (stateless) statelessComponents.add("acceptance:$id")
        success
    }

    /** 注册解交换规则 / Register exchange.
     * @param id 稳定 ID / Stable ID
     * @param stateless 是否明确无状态，否则需恢复协议 / Whether explicitly stateless; otherwise recovery is required
     * @param factory 无状态规则构造 / Stateless policy factory
     * @return 注册结果 / Registration result
     */
    fun registerExchange(
        id: String,
        stateless: Boolean = false,
        factory: (ComponentSpec) -> Ret<ExchangePolicy<O, V>>
    ): Try = register(exchangeFactories, id, factory).map {
        if (stateless) statelessComponents.add("exchange:$id")
        success
    }

    /** 注册预算分配器 / Register a budget allocator.
     * @param id 稳定 ID / Stable ID
     * @param stateless 是否明确无状态，否则需恢复协议 / Whether explicitly stateless; otherwise recovery is required
     * @param factory 无状态分配器构造 / Stateless allocator factory
     * @return 注册结果 / Registration result
     */
    fun registerAllocator(
        id: String,
        stateless: Boolean = false,
        factory: (ComponentSpec) -> Ret<BudgetAllocator>
    ): Try = register(allocatorFactories, id, factory).map {
        if (stateless) statelessComponents.add("allocator:$id")
        success
    }

    internal fun allocator(spec: ComponentSpec): Ret<BudgetAllocator> = boundary(allocatorFactories[spec.type]?.let { factory -> { factory(spec) } }, "allocator: ${spec.type}")

    internal fun problem(spec: ComponentSpec, random: SearchRandom): Ret<SearchProblem<O, V>> {
        if (spec.version != 1) return searchInvalid("unsupported problem version")
        return boundary(problems[spec.type]?.let { factory -> { factory(spec, random) } }, "problem: ${spec.type}")
    }

    internal fun selection(
        spec: ComponentSpec,
        ids: List<String>,
        learning: Boolean
    ): Ret<SelectionPolicy> {
        if (spec.version != 1) return searchInvalid("unsupported selector version")
        selectors[spec.type]?.let { factory -> return searchBoundary { factory(spec, ids, learning) }.then { it } }
        return numericParameters(spec, setOf("epsilon", "alpha", "gamma", "states", "stagnationLimit")).then {
            val policy = TabularSelectionPolicy(
                type = spec.type,
                actions = ids,
                learning = learning,
                epsilon = spec.parameters.number("epsilon", 0.1),
                alpha = spec.parameters.number("alpha", 0.5),
                gamma = spec.parameters.number("gamma", 0.9),
                states = spec.parameters.integer("states", 64),
                stagnationLimit = spec.parameters.integer("stagnationLimit", 10).toLong()
            )
            policy.validate().map { policy }
        }
    }

    internal fun features(spec: ComponentSpec): Ret<FeatureExtractor> = emptyParameters(spec).then { featureFactories[spec.type]?.let { searchBoundary(it) } ?: searchInvalid("unknown features: ${spec.type}") }
    internal fun reward(spec: ComponentSpec): Ret<RewardPolicy> {
        return when (spec.type) {
            "time-normalized" -> numericParameters(spec, setOf("epsilonSeconds")).then {
                val epsilon = spec.parameters.number("epsilonSeconds", 1e-9)
                if (epsilon <= 0) searchInvalid("invalid minimum reward duration") else Ok(TimeNormalizedReward(epsilon))
            }
            "extreme-value" -> numericParameters(spec, setOf("window")).map { ExtremeValueReward(spec.parameters.integer("window", 50)) }
            "dominance" -> emptyParameters(spec).map { DominanceReward }
            "hypervolume-delta" -> hypervolumeReward(spec)
            else -> emptyParameters(spec).then { rewardFactories[spec.type]?.let { searchBoundary(it) } ?: searchInvalid("unknown reward: ${spec.type}") }
        }
    }
    internal fun acceptance(
        spec: ComponentSpec,
        problem: SearchProblem<O, V>,
        horizon: Long = Long.MAX_VALUE
    ): Ret<AcceptancePolicy<O, V>> {
        return when (spec.type) {
            "auto" -> emptyParameters(spec).map { AutoAcceptance(problem.objectiveCodec, horizon) }
            "late" -> numericParameters(spec, setOf("length")).then {
                if (problem.multiObjective) searchInvalid("late acceptance requires a scalar objective")
                else Ok(LateAcceptance(problem.objectiveCodec, spec.parameters.integer("length", 50)))
            }
            "record-to-record" -> numericParameters(spec, setOf("initialRatio", "horizon")).then {
                val ratio = spec.parameters.number("initialRatio", 0.05)
                if (problem.multiObjective || problem.objectiveCoordinates == null || ratio !in 0.0..1.0) searchInvalid("record acceptance requires scalar coordinates and a ratio in [0,1]")
                else Ok(RecordToRecordAcceptance(initialRatio = ratio, horizon = spec.parameters.integer("horizon", 1000).toLong()))
            }
            else -> emptyParameters(spec).then { acceptanceFactories[spec.type]?.let { searchBoundary(it) } ?: searchInvalid("unknown acceptance: ${spec.type}") }
        }
    }
    internal fun exchange(spec: ComponentSpec): Ret<ExchangePolicy<O, V>> = boundary(exchangeFactories[spec.type]?.let { factory -> { factory(spec) } }, "exchange: ${spec.type}")

    internal fun create(
        config: AlgorithmConfig,
        problem: SearchProblem<O, V>,
        random: SearchRandom,
        learning: Boolean,
        rewardSpec: ComponentSpec,
        claimReward: (RewardPolicy) -> Try = { ok }
    ): Ret<SearchSession<O, V>> {
        if (config.algorithm.version != 1 || config.size <= 0 || !config.id.matches(Regex("[A-Za-z0-9][A-Za-z0-9._-]*"))) return searchInvalid("invalid algorithm instance")
        val validated = validateAlgorithm(config)
        if (validated !is Ok) return searchFailure(validated)
        if (config.algorithm.type in setOf("island", "island-ga", "memetic")) {
            if (config.constraints != null) return searchInvalid("组合约束必须在叶会话配置 / Configure constraints on composite leaves")
            if (config.importMode == ImportMode.Disabled) return searchInvalid("composites require native imports for child exchange")
            if (config.children.isEmpty() || config.children.map { it.id }.distinct().size != config.children.size || config.updates.isNotEmpty() || config.mutations.isNotEmpty() || config.neighborhoods.isNotEmpty() || config.parameters.isNotEmpty()) {
                return searchInvalid("composites require distinct children and child-local slots")
            }
            val children = linkedMapOf<String, SearchSession<O, V>>()
            val streams = config.children.associate { it.id to random.split(it.id) }
            for (child in config.children) {
                when (val result = create(
                    config = child,
                    problem = problem,
                    random = streams.getValue(child.id),
                    learning = learning,
                    rewardSpec = rewardSpec,
                    claimReward = claimReward
                )) {
                    is Ok -> children[child.id] = result.value
                    is Failed -> {
                        children.values.forEach { it.close() }
                        return Failed(result.error)
                    }
                    is Fatal -> {
                        children.values.forEach { it.close() }
                        return Fatal(result.errors)
                    }
                }
            }
            if (children.values.any { !it.supportsImport }) {
                children.values.forEach { it.close() }
                return searchInvalid("composite children must support solution imports")
            }
            return Ok(CompositeSearchSession(
                children = children,
                streams = streams,
                sequential = config.algorithm.type == "memetic",
                phaseSteps = config.algorithm.parameters.integer(if (config.algorithm.type == "memetic") "phaseSteps" else "migrationPeriod", 5),
                problem = problem
            ))
        }
        val localReward = reward(rewardSpec)
        if (localReward !is Ok) return searchFailure(localReward)
        val claimed = claimReward(localReward.value)
        if (claimed !is Ok) return searchFailure(claimed)
        val factory = algorithms[config.algorithm.type] ?: return searchInvalid("unknown algorithm: ${config.algorithm.type}")
        val effectiveProblem = if (config.constraints == null) Ok(problem) else {
            if (config.algorithm.type !in constraintAlgorithms) return searchInvalid("算法未声明约束排序能力 / Algorithm does not declare constraint ordering support")
            problem.withConstraints(config.constraints, random.split("constraints"))
        }
        if (effectiveProblem !is Ok) return searchFailure(effectiveProblem)
        val supported = BuiltInAlgorithms.slots(config.algorithm.type)
        if ((config.updates.isNotEmpty() && "update" !in supported || config.mutations.isNotEmpty() && "mutation" !in supported ||
            config.neighborhoods.isNotEmpty() && "neighborhood" !in supported) && config.algorithm.type in BuiltInAlgorithms.nativeIds) return searchInvalid("unsupported operator slot")
        if (config.parameters.keys.any { it !in BuiltInAlgorithms.parameters(config.algorithm.type) } && config.algorithm.type in BuiltInAlgorithms.nativeIds) return searchInvalid("unsupported parameter slot")
        if (problem.encoding != SearchEncoding.Continuous && config.algorithm.type in (BuiltInAlgorithms.nativeIds + setOf("nsga-ii", "nsga-iii")) &&
            (config.algorithm.type !in setOf("genetic", "nsga-ii", "nsga-iii") || config.updates.isEmpty() || config.mutations.isEmpty())) return searchInvalid("algorithm $config does not support ${problem.encoding} without explicit compatible slots")
        return bindings(
            config = config,
            random = random,
            learning = learning,
            encoding = problem.encoding
        ).then { slots ->
            searchBoundary { factory(AlgorithmContext(
                config = config,
                problem = effectiveProblem.value,
                random = random,
                bindings = slots
            )) }.then { it }.then { session ->
                if (config.initialization == InitializationMode.WorkingSet && (!session.supportsImport || config.importMode == ImportMode.Disabled)) {
                    session.close()
                    searchInvalid("working-set initialization requires imports")
                } else {
                    val native = if (config.importMode == ImportMode.Native) session else object : SearchSession<O, V> by session {
                        override val supportsImport = false
                        override suspend fun importSolutions(solutions: List<fuookami.ospf.kotlin.core.model.basic.Solution<V>>, budget: SearchSlice): Ret<SearchStepResult<O, V>> = searchInvalid("imports disabled")
                    }
                    Ok(ControlledSearchSession(native, problem, slots, localReward.value))
                }
            }
        }
    }

    private fun bindings(
        config: AlgorithmConfig,
        random: SearchRandom,
        learning: Boolean,
        encoding: SearchEncoding
    ): Ret<OperatorBindings> {
        return pool(
            id = "update",
            specs = config.updates,
            registry = updates,
            policy = config.selection,
            random = random,
            learning = learning,
            encoding = encoding
        ).then { update ->
            pool(
                id = "mutation",
                specs = config.mutations,
                registry = mutations,
                policy = config.selection,
                random = random,
                learning = learning,
                encoding = encoding
            ).then { mutation ->
                pool(
                    id = "neighborhood",
                    specs = config.neighborhoods,
                    registry = neighborhoods,
                    policy = config.selection,
                    random = random,
                    learning = learning,
                    encoding = encoding
                ).then { neighborhood ->
                    val sources = linkedMapOf<String, OperatorPool<ParameterSource>>()
                    for ((name, specs) in config.parameters) {
                        when (val result = pool(
                            id = name,
                            specs = specs,
                            registry = parameters,
                            policy = config.selection,
                            random = random,
                            learning = learning,
                            encoding = SearchEncoding.Continuous
                        )) {
                            is Ok -> result.value?.let { sources[name] = it }
                            is Failed -> return@then Failed(result.error)
                            is Fatal -> return@then Fatal(result.errors)
                        }
                    }
                    Ok(OperatorBindings(
                        update = update,
                        mutation = mutation,
                        neighborhood = neighborhood,
                        parameters = sources
                    ))
                }
            }
        }
    }

    private fun <T> pool(
        id: String,
        specs: List<ComponentSpec>,
        registry: Map<String, Entry<T>>,
        policy: ComponentSpec,
        random: SearchRandom,
        learning: Boolean,
        encoding: SearchEncoding
    ): Ret<OperatorPool<T>?> {
        if (specs.isEmpty()) return Ok(null)
        val values = linkedMapOf<String, T>()
        val stateless = hashSetOf<String>()
        for (spec in specs) {
            if (spec.version != 1) return searchInvalid("unsupported component version")
            val entry = registry[spec.type] ?: return searchInvalid("unknown $id component: ${spec.type}")
            if (encoding !in entry.encodings) return searchInvalid("${spec.type} does not support $encoding")
            val key = "${spec.type}:${spec.version}:${fingerprint(spec.parameters)}"
            if (key in values) return searchInvalid("duplicate $id action")
            val created: Ret<T> = if (id == "update" && spec.type in setOf("pipeline", "vnd", "elite-local", "mutation-stage", "neighborhood-stage")) {
                @Suppress("UNCHECKED_CAST")
                (searchBoundary { compositeUpdate(spec, 0, encoding) }.then { it } as Ret<T>)
            } else searchBoundary { entry.create(spec) }.then { it }
            when (val result = created) {
                is Ok -> values[key] = result.value
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
            if (entry.stateless) stateless.add(key)
        }
        return selection(policy, values.keys.toList(), learning).then { selected ->
            if (selected.stateCount?.let { it < 64 } == true) searchInvalid("operator selection requires 64 progress states")
            else Ok(OperatorPool(
                id = id,
                candidates = values,
                policy = selected,
                random = random.split("slot:$id"),
                stateless = stateless
            ))
        }
    }

    private fun compositeUpdate(spec: ComponentSpec, depth: Int, encoding: SearchEncoding): Ret<SolutionUpdate> {
        if (depth > 16 || spec.version != 1) return searchInvalid("invalid composite depth/version")
        val allowed = when (spec.type) {
            "pipeline" -> setOf("stages")
            "vnd" -> setOf("stages", "maxTrials")
            "elite-local" -> setOf("stages", "amount")
            "mutation-stage" -> setOf("operator", "rate")
            "neighborhood-stage" -> setOf("operator")
            else -> return searchInvalid("unknown composite update")
        }
        if (spec.parameters.keys.any { it !in allowed }) return searchInvalid("unknown composite parameters")
        if (spec.type in setOf("mutation-stage", "neighborhood-stage")) {
            val nested = spec.parameters["operator"] ?: return searchInvalid("missing composite operator")
            val decoded = searchBoundary { kotlinx.serialization.json.Json.decodeFromJsonElement<ComponentSpec>(nested) }
            if (decoded !is Ok) return searchFailure(decoded)
            val child = decoded.value
            if (child.version != 1) return searchInvalid("unsupported nested operator version")
            return if (spec.type == "mutation-stage") {
                val entry = mutations[child.type] ?: return searchInvalid("unknown nested mutation")
                if (!entry.stateless || encoding !in entry.encodings) return searchInvalid("nested mutation requires stateless compatible encoding")
                val rate = spec.parameters["rate"]?.let { (it as? JsonPrimitive)?.doubleOrNull } ?: if ("rate" in spec.parameters) return searchInvalid("invalid mutation rate") else 1.0
                if (rate !in 0.0..1.0) return searchInvalid("invalid mutation rate")
                entry.create(child).map { MutationUpdate(it, Flt64(rate)) }
            } else {
                val entry = neighborhoods[child.type] ?: return searchInvalid("unknown nested neighborhood")
                if (!entry.stateless || encoding !in entry.encodings) return searchInvalid("nested neighborhood requires stateless compatible encoding")
                entry.create(child).map { NeighborhoodUpdate(it) }
            }
        }
        val decoded = searchBoundary { kotlinx.serialization.json.Json.decodeFromJsonElement<List<ComponentSpec>>(
            spec.parameters["stages"] ?: JsonNull
        ) }
        if (decoded !is Ok) return searchFailure(decoded)
        if (decoded.value.isEmpty() || decoded.value.size > 64) return searchInvalid("composite needs 1..64 stages")
        val stages = ArrayList<SolutionUpdate>()
        for (child in decoded.value) {
            val entry = updates[child.type] ?: return searchInvalid("unknown composite stage: ${child.type}")
            if (!entry.stateless || encoding !in entry.encodings || child.version != 1) return searchInvalid("composites require stateless encoding-compatible stages")
            val result = if (child.type in setOf("pipeline", "vnd", "elite-local", "mutation-stage", "neighborhood-stage")) compositeUpdate(child, depth + 1, encoding) else entry.create(child)
            if (result !is Ok) return searchFailure(result)
            stages.add(result.value)
        }
        val trials = spec.parameters["maxTrials"]?.let { (it as? JsonPrimitive)?.intOrNull } ?: if ("maxTrials" in spec.parameters) return searchInvalid("invalid maxTrials") else 50
        val amount = spec.parameters["amount"]?.let { (it as? JsonPrimitive)?.intOrNull } ?: if ("amount" in spec.parameters) return searchInvalid("invalid elite amount") else 1
        if (trials <= 0 || amount <= 0) return searchInvalid("composite limits must be positive")
        return Ok(when (spec.type) {
            "vnd" -> VNDCompositeUpdate(stages, trials)
            "elite-local" -> EliteLocalSearchWrapper(PipelineCompositeUpdate(stages), amount)
            else -> PipelineCompositeUpdate(stages)
        })
    }

    internal fun replaceNative(id: String, factory: (AlgorithmContext<O, V>) -> Ret<SearchSession<O, V>>) {
        algorithms[id] = factory
    }

    private fun <T> boundary(factory: (() -> Ret<T>)?, name: String): Ret<T> {
        return if (factory == null) searchInvalid("unknown $name") else searchBoundary(factory).then { it }
    }

    private fun <T> register(
        map: MutableMap<String, T>,
        id: String,
        value: T
    ): Try {
        if (id.isBlank() || id in map) return searchInvalid("duplicate or empty component ID: $id")
        map[id] = value
        return ok
    }
}

internal fun emptyParameters(spec: ComponentSpec): Try = numericParameters(spec, emptySet())

private fun hypervolumeReward(spec: ComponentSpec): Ret<RewardPolicy> {
    val values = spec.parameters
    val allowed = setOf("reference", "referenceX", "referenceY", "mode", "maxWork", "samples", "seed", "calibrationObservations")
    if (spec.version != 1 || values.keys.any { it !in allowed } ||
        ("reference" in values && ("referenceX" in values || "referenceY" in values)) ||
        (("referenceX" in values) != ("referenceY" in values))) return searchInvalid("超体积配置无效 / Invalid hypervolume configuration")
    val reference = when {
        "reference" in values -> (values["reference"] as? JsonArray)?.map { value ->
            val number = (value as? JsonPrimitive)?.doubleOrNull
            if (number == null || !number.isFinite()) return searchInvalid("超体积参考点无效 / Invalid hypervolume reference")
            Flt64(number)
        } ?: return searchInvalid("超体积参考点必须为数组 / Hypervolume reference must be an array")
        "referenceX" in values -> listOf("referenceX", "referenceY").map { key ->
            val number = (values[key] as? JsonPrimitive)?.doubleOrNull
            if (number == null || !number.isFinite()) return searchInvalid("超体积参考点无效 / Invalid hypervolume reference")
            Flt64(number)
        }
        else -> emptyList()
    }
    val mode = when ((values["mode"] as? JsonPrimitive)?.content ?: if ("mode" in values) "invalid" else "exact") {
        "exact" -> HypervolumeMode.Exact
        "sampled" -> HypervolumeMode.Sampled
        else -> return searchInvalid("未知超体积模式 / Unknown hypervolume mode")
    }
    fun integer(name: String, default: Long): Long? = if (name in values) (values[name] as? JsonPrimitive)?.longOrNull else default
    val work = integer("maxWork", 1_000_000) ?: return searchInvalid("超体积额度必须为整数 / Hypervolume work must be an integer")
    val samples = integer("samples", 16384) ?: return searchInvalid("采样数必须为整数 / Sample count must be an integer")
    val calibration = integer("calibrationObservations", 32) ?: return searchInvalid("校准数必须为整数 / Calibration count must be an integer")
    val seed = integer("seed", 1) ?: return searchInvalid("种子必须为整数 / Seed must be an integer")
    if (samples !in 1..Int.MAX_VALUE.toLong() || calibration !in 1..Int.MAX_VALUE.toLong()) return searchInvalid("超体积计数超限 / Hypervolume count out of range")
    val reward = HypervolumeDeltaReward(
        referencePoint = reference,
        mode = mode,
        maxWork = work,
        samples = samples.toInt(),
        seed = seed,
        calibrationObservations = calibration.toInt()
    )
    return reward.validate().map { reward }
}

internal fun numericParameters(spec: ComponentSpec, allowed: Set<String>): Try {
    if (spec.version != 1 || spec.parameters.keys.any { it !in allowed } || spec.parameters.values.any { (it as? JsonPrimitive)?.doubleOrNull?.isFinite() != true }) {
        return searchInvalid("invalid parameters for ${spec.type}")
    }
    val positiveIntegers = setOf("states", "stagnationLimit", "amount", "period", "limit", "window", "length", "horizon", "sweeps", "maxTrials")
    if (spec.parameters.any { (key, value) -> key in positiveIntegers && ((value as? JsonPrimitive)?.intOrNull?.let { it <= 0 } != false) }) return searchInvalid("invalid integer parameter for ${spec.type}")
    if (spec.parameters.any { (key, value) -> key in setOf("sigma", "eta", "ratio") && value.jsonPrimitive.double <= 0.0 }) return searchInvalid("invalid positive parameter for ${spec.type}")
    if (spec.parameters.integer("states", 64) > 4096) return searchInvalid("too many tabular states")
    return ok
}

private fun validateAlgorithm(config: AlgorithmConfig): Try {
    val type = config.algorithm.type
    val allowed = when (type) {
        in BuiltInAlgorithms.populationUpdates.keys -> setOf("mutationRate")
        "pso", "apso" -> setOf("maxVelocity")
        "clpso" -> setOf("maxVelocity", "learningWeight", "refreshInterval", "stagnationLimit")
        "shade" -> setOf("memorySize", "pBestRate", "minimumSize")
        "l-shade" -> setOf("memorySize", "pBestRate", "minimumSize", "sizeReductionSteps")
        "cma-es" -> setOf("initialSigma", "restartOnImport")
        "nsga-ii", "nsga-iii" -> setOf("mutationRate", "divisions")
        "island", "island-ga" -> setOf("migrationPeriod")
        "memetic" -> setOf("phaseSteps")
        "hill-climbing", "ns", "random-walk", "simulated-annealing" -> emptySet()
        else -> return ok
    }
    val values = config.algorithm.parameters
    if (values.keys.any { it !in allowed } || values.any { (key, value) ->
        if (key == "restartOnImport") (value as? JsonPrimitive)?.booleanOrNull == null
        else (value as? JsonPrimitive)?.doubleOrNull?.isFinite() != true
    }) return searchInvalid("invalid algorithm parameters: $type")
    val integers = setOf("memorySize", "minimumSize", "refreshInterval", "stagnationLimit", "divisions", "sizeReductionSteps", "migrationPeriod", "phaseSteps")
    if (values.any { (key, value) -> key in integers && value.jsonPrimitive.intOrNull?.let { it <= 0 } != false }) return searchInvalid("invalid integer algorithm parameter: $type")
    if (values.number("mutationRate", 0.1) !in 0.0..1.0 || values.number("pBestRate", 0.2) !in 0.0..1.0 || values.number("initialSigma", 0.3) <= 0 || values.number("maxVelocity", 1.0) <= 0) return searchInvalid("invalid algorithm rate/scale: $type")
    if (type in setOf("shade", "l-shade", "de") && config.size < 4) return searchInvalid("$type requires at least four individuals")
    if (type in setOf("shade", "l-shade") && (values.integer("minimumSize", 4) > config.size || values.integer("minimumSize", 4) < 4)) return searchInvalid("invalid minimum SHADE population")
    if (type == "clpso" && config.size < 2) return searchInvalid("CLPSO requires at least two particles")
    return ok
}
