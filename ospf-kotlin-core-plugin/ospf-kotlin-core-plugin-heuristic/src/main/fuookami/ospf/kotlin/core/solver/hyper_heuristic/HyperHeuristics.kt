package fuookami.ospf.kotlin.core.solver.hyper_heuristic

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.core.model.basic.Solution
import fuookami.ospf.kotlin.core.model.callback.AbstractCallBackModelInterface
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.solver.heuristic.*
import fuookami.ospf.kotlin.core.solver.heuristic.operator.compareStrictly
import fuookami.ospf.kotlin.core.solver.heuristic.operator.multi_objective.*
import fuookami.ospf.kotlin.core.solver.hyper_heuristic.configuration.*

/** 超启发式组合入口 / Hyper-heuristic composition entry points. */
object HyperHeuristics {
    /** 通用单目标注册表 / Generic single-objective registry.
     * @param converter 数值适配 / Value adapter
     * @return 新注册表 / New registry
     */
    fun <O, V> registry(converter: IntoValue<V>): SearchRegistry<O, V>
        where V : RealNumber<V>, V : NumberField<V> = SearchRegistry(converter)

    /** 多目标注册表，单目标算法必须显式引用标量化 / Multiobjective registry requiring explicit scalarization for scalar algorithms.
     * @param converter 数值适配 / Value adapter
     * @param scalarizations 已注册的标量化函数，输出越小越优 / Registered scalarizations, smaller is better
     * @return 包含 NSGA-II/III 的注册表 / Registry including NSGA-II/III
     */
    fun <V> multiObjectiveRegistry(
        converter: IntoValue<V>,
        scalarizations: Map<String, (List<Flt64>) -> Flt64> = emptyMap()
    ): SearchRegistry<List<Flt64>, V> where V : RealNumber<V>, V : NumberField<V> {
        val registry = SearchRegistry<List<Flt64>, V>(converter)
        for (id in listOf("nsga-ii", "nsga-iii")) {
            registry.registerAlgorithm(id, supportsConstraints = true) { context ->
                BuiltInAlgorithms.nsga(
                    config = context.config,
                    problem = context.problem,
                    random = context.random,
                    converter = converter,
                    bindings = context.bindings
                )
            }
        }
        for (id in BuiltInAlgorithms.nativeIds) {
            registry.replaceNative(id) { context ->
                val scalarization = scalarizations[context.config.scalarization]
                    ?: return@replaceNative searchInvalid("${context.config.id} requires a registered scalarization")
                val canonical = context.problem
                val evaluated = linkedMapOf<Solution<V>, List<Flt64>>()
                val view = objectiveModel(
                    source = canonical.model,
                    defaultObjective = Flt64.infinity,
                    objective = { solution ->
                        canonical.model.objective(solution)?.takeIf { canonical.valid(it) }?.let { objectives ->
                            evaluated[solution.toList()] = objectives
                            scalarization(objectives)
                        }
                    },
                    compare = { lhs, rhs -> if (lhs < rhs) Order.Less() else if (rhs < lhs) Order.Greater() else null }
                )
                val problem = SearchProblem(
                    id = canonical.id,
                    fingerprint = canonical.fingerprint,
                    model = view,
                    objectiveCodec = SearchCodecs.flt64,
                    valueCodec = canonical.valueCodec,
                    dominates = { lhs, rhs -> lhs < rhs },
                    valid = { it.toDouble().isFinite() },
                    feasible = canonical.feasible,
                    state = canonical.state,
                    encoding = canonical.encoding,
                    validSolution = canonical.validSolution,
                    admissible = canonical.admissible,
                    constraintViolation = canonical.constraintViolation
                )
                val constrained = context.config.constraints?.let { configuration ->
                    problem.withConstraints(configuration, context.random.split("scalar-constraints"))
                } ?: Ok(problem)
                if (constrained !is Ok) return@replaceNative searchFailure(constrained)
                BuiltInAlgorithms.create(
                    config = context.config,
                    problem = constrained.value,
                    random = context.random,
                    converter = converter,
                    bindings = context.bindings
                ).map { native ->
                    object : SearchSession<List<Flt64>, V> {
                        override val supportsImport: Boolean get() = native.supportsImport

                        private fun canonical(result: SearchStepResult<Flt64, V>): SearchStepResult<List<Flt64>, V> {
                            return SearchStepResult(
                                candidates = result.candidates.mapNotNull { candidate -> evaluated[candidate.solution]?.let { SolutionWithFitness(candidate.solution, it) } },
                                evaluations = result.evaluations,
                                steps = result.steps,
                                termination = result.termination,
                                diagnostics = result.diagnostics
                            )
                        }

                        override suspend fun step(budget: SearchSlice): Ret<SearchStepResult<List<Flt64>, V>> {
                            evaluated.clear()
                            return native.step(budget).map { canonical(it) }
                        }

                        override suspend fun importSolutions(solutions: List<Solution<V>>, budget: SearchSlice): Ret<SearchStepResult<List<Flt64>, V>> {
                            evaluated.clear()
                            return native.importSolutions(solutions, budget).map { canonical(it) }
                        }

                        override fun snapshot(): Ret<SearchSessionSnapshot> = native.snapshot()
                        override fun restore(snapshot: SearchSessionSnapshot): Try = native.restore(snapshot)
                        override fun close(): Try = native.close()
                    }
                }
            }
        }
        registry.archiveTrimmer = { entries, amount ->
            val distances = CrowdingDistance.assign(entries.map { it.fitness })
            entries.indices.sortedWith(compareByDescending<Int> { distances[it] }.thenBy { it }).take(amount).sorted().map { entries[it] }
        }
        return registry
    }
}

/** 从现有单目标回调模型创建问题边界 / Create a problem boundary from a scalar callback model.
 * @param id 问题标识 / Problem ID
 * @param fingerprint 数据与目标定义指纹 / Data and objective fingerprint
 * @param model 独占模型 / Exclusively owned model
 * @param objectiveCodec 目标编码 / Objective codec
 * @param valueCodec 解值编码 / Solution value codec
 * @param valid 目标有效性检查 / Objective validity check
 * @param state 回调恢复状态 / Callback recovery state
 * @param encoding 解编码 / Solution encoding
 * @param validSolution 编码与维度校验 / Encoding and dimension validation
 * @param objectiveCoordinates 显式归一化最小化坐标 / Explicit normalized minimization coordinates
 * @param constraintViolation 非负归一化违约度，不替换业务目标 / Nonnegative normalized violation, never replacing business objectives
 * @return 搜索问题 / Search problem
 */
fun <O, V> singleObjectiveProblem(
    id: String,
    fingerprint: String,
    model: AbstractCallBackModelInterface<*, O, V>,
    objectiveCodec: SearchCodec<O>,
    valueCodec: SearchCodec<V>,
    valid: (O) -> Boolean = { it !is Flt64 || it.toDouble().isFinite() },
    state: SearchProblemState? = null,
    encoding: SearchEncoding = SearchEncoding.Continuous,
    validSolution: (Solution<V>) -> Boolean = { it.size == model.tokens.tokensInSolver.size },
    objectiveCoordinates: ((O) -> List<Flt64>)? = null,
    constraintViolation: ((Solution<V>) -> Ret<Flt64>)? = null
): SearchProblem<O, V> where V : RealNumber<V>, V : NumberField<V> {
    return SearchProblem(
        id = id,
        fingerprint = fingerprint,
        model = model,
        objectiveCodec = objectiveCodec,
        valueCodec = valueCodec,
        dominates = { lhs, rhs -> compareStrictly(model, lhs, rhs) < 0 },
        equivalent = { lhs, rhs -> compareStrictly(model, lhs, rhs) == 0 },
        valid = valid,
        state = state,
        encoding = encoding,
        validSolution = validSolution,
        objectiveCoordinates = objectiveCoordinates,
        constraintViolation = constraintViolation
    )
}

/** 多目标边界，向量维度和有限性在评估入口检查 / Multiobjective boundary validating dimensions and finiteness at evaluation.
 * @param id 问题标识 / Problem ID
 * @param fingerprint 数据与目标定义指纹 / Data and objective fingerprint
 * @param model 独占模型 / Exclusively owned model
 * @param objectivesOf 目标向量提取 / Objective vector extraction
 * @param directions true 表示最小化 / True means minimization
 * @param valueCodec 解值编码 / Solution value codec
 * @param state 回调恢复状态 / Callback recovery state
 * @param encoding 解编码 / Solution encoding
 * @param validSolution 编码与维度校验 / Encoding and dimension validation
 * @param objectiveCoordinates 显式归一化最小化坐标 / Explicit normalized minimization coordinates
 * @param constraintViolation 非负归一化违约度，不替换业务目标 / Nonnegative normalized violation, never replacing business objectives
 * @return 搜索问题 / Search problem
 */
fun <V> multiObjectiveProblem(
    id: String,
    fingerprint: String,
    model: AbstractCallBackModelInterface<*, *, V>,
    objectivesOf: (Solution<V>) -> List<Flt64>?,
    directions: List<Boolean>,
    valueCodec: SearchCodec<V>,
    state: SearchProblemState? = null,
    encoding: SearchEncoding = SearchEncoding.Continuous,
    validSolution: (Solution<V>) -> Boolean = { it.size == model.tokens.tokensInSolver.size },
    objectiveCoordinates: (List<Flt64>) -> List<Flt64> = { values -> values.mapIndexed { index, value -> if (directions[index]) value else -value } },
    constraintViolation: ((Solution<V>) -> Ret<Flt64>)? = null
): SearchProblem<List<Flt64>, V> where V : RealNumber<V>, V : NumberField<V> {
    val dominance = ParetoDominance(directions)
    val view = objectiveModel(
        source = model,
        defaultObjective = emptyList<Flt64>(),
        objective = objectivesOf,
        compare = { lhs, rhs ->
            when {
                dominance.dominates(lhs, rhs) -> Order.Less()
                dominance.dominates(rhs, lhs) -> Order.Greater()
                else -> null
            }
        }
    )
    return SearchProblem(
        id = id,
        fingerprint = fingerprint,
        model = view,
        objectiveCodec = SearchCodecs.list(SearchCodecs.flt64),
        valueCodec = valueCodec,
        dominates = { lhs, rhs -> dominance.dominates(lhs, rhs) },
        multiObjective = true,
        valid = { it.size == directions.size && it.isNotEmpty() && it.all { value -> value.toDouble().isFinite() } },
        state = state,
        encoding = encoding,
        validSolution = validSolution,
        objectiveCoordinates = objectiveCoordinates,
        constraintViolation = constraintViolation
    )
}
