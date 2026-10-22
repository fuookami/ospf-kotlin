package fuookami.ospf.kotlin.core.solver.hyper_heuristic.learning

import java.nio.file.*
import kotlinx.coroutines.*
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.core.solver.heuristic.*
import fuookami.ospf.kotlin.core.solver.hyper_heuristic.*
import fuookami.ospf.kotlin.core.solver.hyper_heuristic.selection.*
import fuookami.ospf.kotlin.core.solver.hyper_heuristic.persistence.*
import fuookami.ospf.kotlin.core.solver.hyper_heuristic.configuration.SearchRegistry

/** 训练实例定义 / Training instance definition.
 * @property id 数据集内唯一 ID / Dataset-unique ID
 * @property problem 已注册的问题及输入参数 / Registered problem and input parameters
 */
@Serializable
data class TrainingCase(val id: String, val problem: ComponentSpec)

/** 先按实例划分数据，再分配种子 / Split by instance before assigning seeds.
 * @property training 训练实例 / Training instances
 * @property validation 验证实例 / Validation instances
 * @property test 测试实例 / Test instances
 */
data class TrainingSplit(
    val training: List<TrainingCase>,
    val validation: List<TrainingCase> = emptyList(),
    val test: List<TrainingCase> = emptyList()
) {
    /** 检查实例身份不泄漏 / Check that instance identities do not leak across splits.
     * @return 校验结果 / Validation result
     */
    fun validate(): Try {
        val all = training + validation + test
        if (training.isEmpty() || all.any { it.id.isBlank() } || all.map { it.id }.distinct().size != all.size || all.map { it.problem }.distinct().size != all.size) {
            return searchInvalid("training, validation and test instances must be nonempty/unique and disjoint")
        }
        return ok
    }
}

/** 一次实例与种子运行 / One instance-and-seed run.
 * @property instance 实例 ID / Instance ID
 * @property seed 随机种子 / Random seed
 * @property result 完整搜索结果 / Complete search result
 */
data class TrainingObservation<O, V>(val instance: String, val seed: Long, val result: SearchResult<O, V>)
    where V : RealNumber<V>, V : NumberField<V>

/** 训练及保留集报告 / Training and holdout report.
 * @property policy 最终训练策略 / Final trained policy
 * @property training 训练运行 / Training runs
 * @property validation 冻结验证运行 / Frozen validation runs
 * @property test 冻结测试运行 / Frozen test runs
 */
data class TrainingReport<O, V>(
    val policy: PolicyArchive,
    val training: List<TrainingObservation<O, V>>,
    val validation: List<TrainingObservation<O, V>>,
    val test: List<TrainingObservation<O, V>>
) where V : RealNumber<V>, V : NumberField<V>

/** 相同预算下的质量与成本分布 / Quality and cost distribution under equal budgets.
 * @property observations 包含失败的全部实例/种子运行 / All instance/seed runs including failures
 * @property quality 业务度量的有效质量样本 / Valid quality samples from a domain metric
 * @property failureRate 失败运行比例 / Fraction of failed runs
 * @property evaluations 总评估请求数 / Total evaluation requests
 * @property activeNanos 总有效运行时间 / Total active runtime
 */
data class PolicyEvaluationReport<O, V>(
    val observations: List<TrainingObservation<O, V>>,
    val quality: List<Flt64>,
    val failureRate: Flt64,
    val evaluations: Long,
    val activeNanos: Long
) where V : RealNumber<V>, V : NumberField<V>

/** 表格策略训练器，不依赖外部学习服务 / Tabular policy trainer without external learning services.
 * @property registry 问题和组件注册表 / Problem and component registry
 */
class PolicyTrainer<O, V>(private val registry: SearchRegistry<O, V>) where V : RealNumber<V>, V : NumberField<V> {
    /** 在实例集上训练，验证与测试始终冻结 / Train on instances, always freezing validation and test policies.
     * @param config 基础配置 / Base configuration
     * @param split 实例划分 / Instance split
     * @param seeds 种子集合 / Seed set
     * @param initial 可选继续训练策略 / Optional continuation policy
     * @return 训练报告 / Training report
     */
    suspend fun train(
        config: HyperHeuristicConfig,
        split: TrainingSplit,
        seeds: List<Long>,
        initial: PolicyArchive? = null
    ): Ret<TrainingReport<O, V>> {
        val valid = split.validate()
        if (valid !is Ok) return searchFailure(valid)
        if (seeds.isEmpty() || seeds.distinct().size != seeds.size) return searchInvalid("seeds must be nonempty and unique")
        var policy = initial
        val training = ArrayList<TrainingObservation<O, V>>()
        for (instance in split.training) {
            for (seed in seeds) {
                val current = config.copy(problem = instance.problem, execution = config.execution.copy(seed = seed), learning = config.learning.copy(mode = LearningMode.Train))
                val opened = HyperHeuristicSolver(current, registry).open(policy = policy)
                if (opened !is Ok) return searchFailure(opened)
                val run = opened.value
                try {
                    val result = run.run()
                    if (result !is Ok) return searchFailure(result)
                    val exported = run.exportPolicy()
                    if (exported !is Ok) return searchFailure(exported)
                    policy = exported.value
                    training.add(TrainingObservation(instance.id, seed, result.value))
                } finally {
                    withContext(NonCancellable) { run.close() }
                }
            }
        }
        val trained = policy ?: return searchInvalid("no training episode completed")
        val validation = PolicyEvaluator(registry).evaluate(config, split.validation, seeds, trained)
        if (validation !is Ok) return searchFailure(validation)
        val test = PolicyEvaluator(registry).evaluate(config, split.test, seeds, trained)
        if (test !is Ok) return searchFailure(test)
        return Ok(TrainingReport(
            policy = trained.copy(metadata = trained.metadata + mapOf("trainingInstances" to split.training.joinToString(",") { it.id }, "seeds" to seeds.joinToString(","))),
            training = training,
            validation = validation.value,
            test = test.value
        ))
    }

    /** 重放已执行转移，Bandit 只更新实际动作 / Replay executed transitions; Bandit updates only observed actions.
     * @param policy 目标策略 / Target policy
     * @param transitions 历史转移 / Historical transitions
     * @param featureVersion 要求的特征协议 / Required feature protocol
     * @param scope 需要重放的算法或算子作用域 / Algorithm or operator scope to replay
     * @return 更新结果 / Update result
     */
    fun replay(
        policy: SelectionPolicy,
        transitions: List<SelectionTransition>,
        featureVersion: String,
        scope: String = "algorithm"
    ): Try {
        val selected = transitions.filter { it.scope == scope }
        if (selected.any { it.featureVersion != featureVersion } || selected.map { Pair(it.episode, it.decision) }.distinct().size != selected.size) return searchInvalid("incompatible features or duplicate transitions")
        for (transition in selected) {
            val result = policy.observe(transition)
            if (result !is Ok) return result
        }
        return ok
    }
}

/** 冻结策略评估器，报告原始质量档案而非假定标量指标 / Frozen-policy evaluator reporting raw quality archives rather than assuming a scalar metric.
 * @property registry 问题与组件注册表 / Problem and component registry
 */
class PolicyEvaluator<O, V>(private val registry: SearchRegistry<O, V>) where V : RealNumber<V>, V : NumberField<V> {
    /** 在相同预算下评估 / Evaluate under identical configured budgets.
     * @param config 基础配置 / Base configuration
     * @param cases 保留实例 / Holdout instances
     * @param seeds 种子 / Seeds
     * @param policy 可选学习策略，null 用于固定/随机/轮询基线 / Optional learned policy; null enables configured baselines
     * @return 运行分布 / Run distribution
     */
    suspend fun evaluate(
        config: HyperHeuristicConfig,
        cases: List<TrainingCase>,
        seeds: List<Long>,
        policy: PolicyArchive? = null
    ): Ret<List<TrainingObservation<O, V>>> {
        val valid = config.validate()
        if (valid !is Ok) return searchFailure(valid)
        if (config.budget.evaluations == null || seeds.isEmpty()) return searchInvalid("evaluation requires an evaluation limit and seeds")
        val reports = ArrayList<TrainingObservation<O, V>>()
        for (instance in cases) {
            for (seed in seeds) {
                val current = config.copy(problem = instance.problem, execution = config.execution.copy(seed = seed), learning = config.learning.copy(mode = LearningMode.Frozen))
                val opened = HyperHeuristicSolver(current, registry).open(policy = policy)
                val result = if (opened is Ok) {
                    try {
                        val searched = opened.value.run()
                        val result = opened.value.result()
                        if (searched is Ok) result else result.copy(
                            termination = SearchTermination.Failed,
                            diagnostics = result.diagnostics + searched.toString()
                        )
                    } finally {
                        withContext(NonCancellable) { opened.value.close() }
                    }
                } else {
                    SearchResult<O, V>(
                        archive = emptyList(),
                        multiObjective = false,
                        termination = SearchTermination.Failed,
                        evaluations = 0,
                        decisions = 0,
                        steps = 0,
                        activeNanos = 0,
                        diagnostics = listOf(opened.toString())
                    )
                }
                reports.add(TrainingObservation(instance.id, seed, result))
            }
        }
        return Ok(reports)
    }

    /** 比较固定算法与选择策略，质量度量由业务显式提供 / Compare algorithms and selectors using an explicit domain quality metric.
     * @param configurations 名称到运行配置，评估上限必须相同 / Named configurations with identical evaluation limits
     * @param cases 共享实例集 / Shared instances
     * @param seeds 共享种子 / Shared seeds
     * @param policies 各配置可选的冻结策略 / Optional frozen policy per configuration
     * @param quality 业务质量度量，无有效解时可返回 null / Domain quality metric, null when unavailable
     * @return 每种策略的质量分布、成本与失败率 / Quality, cost and failure distribution per strategy
     */
    suspend fun compare(
        configurations: Map<String, HyperHeuristicConfig>,
        cases: List<TrainingCase>,
        seeds: List<Long>,
        policies: Map<String, PolicyArchive> = emptyMap(),
        quality: (SearchResult<O, V>) -> Flt64? = { null }
    ): Ret<Map<String, PolicyEvaluationReport<O, V>>> {
        if (configurations.isEmpty() || configurations.values.map { it.budget.evaluations }.distinct().size != 1 ||
            configurations.values.any { it.budget.evaluations == null } || policies.keys.any { it !in configurations }) {
            return searchInvalid("comparisons require equal explicit evaluation budgets and known policy IDs")
        }
        val reports = linkedMapOf<String, PolicyEvaluationReport<O, V>>()
        for ((name, config) in configurations) {
            val evaluated = evaluate(
                config = config,
                cases = cases,
                seeds = seeds,
                policy = policies[name]
            )
            if (evaluated !is Ok) return searchFailure(evaluated)
            val observations = evaluated.value
            val values = searchBoundary { observations.mapNotNull { quality(it.result) } }
            if (values !is Ok) return searchFailure(values)
            if (values.value.any { !it.toDouble().isFinite() }) return searchInvalid("non-finite evaluation quality")
            reports[name] = PolicyEvaluationReport(
                observations = observations,
                quality = values.value,
                failureRate = Flt64(observations.count { it.result.termination == SearchTermination.Failed }.toDouble() / observations.size.coerceAtLeast(1)),
                evaluations = observations.fold(0L) { total, observation -> total + observation.result.evaluations },
                activeNanos = observations.fold(0L) { total, observation -> total + observation.result.activeNanos }
            )
        }
        return Ok(reports)
    }
}

/** JSON Lines 轨迹文件，读取时保留完整记录 / JSON Lines trajectory files preserving complete records. */
object TrajectoryFiles {
    /** 原子写入轨迹 / Atomically write trajectories.
     * @param path 输出位置 / Output path
     * @param transitions 完整转移 / Complete transitions
     * @return 写入结果 / Write result
     */
    fun write(path: Path, transitions: List<SelectionTransition>): Try {
        return searchBoundary {
            val target = path.toAbsolutePath()
            Files.createDirectories(target.parent)
            val temp = Files.createTempFile(target.parent, ".trajectory-", ".tmp")
            try {
                Files.write(temp, transitions.map { Json.encodeToString(it) })
                try {
                    Files.move(temp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
                } catch (_: AtomicMoveNotSupportedException) {
                    Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING)
                }
            } finally {
                Files.deleteIfExists(temp)
            }
            success
        }
    }

    /** 读取并校验轨迹格式 / Read and validate trajectory format.
     * @param path 输入位置 / Input path
     * @return 完整轨迹 / Complete trajectories
     */
    fun read(path: Path): Ret<List<SelectionTransition>> {
        return searchBoundary { Files.readAllLines(path).filter { it.isNotBlank() }.map { Json.decodeFromString<SelectionTransition>(it) } }
    }
}
