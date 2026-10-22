package fuookami.ospf.kotlin.core.solver.hyper_heuristic

import kotlinx.serialization.*
import kotlinx.serialization.json.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.solver.heuristic.then
import fuookami.ospf.kotlin.core.solver.heuristic.searchInvalid
import fuookami.ospf.kotlin.core.solver.heuristic.searchBoundary
import fuookami.ospf.kotlin.core.solver.heuristic.ConstraintHandlingConfig
import fuookami.ospf.kotlin.core.solver.hyper_heuristic.configuration.HyperHeuristicBuilder

/** 组件引用，不使用反射类名 / Component reference without reflective class names.
 * @property type 注册标识 / Registered identifier
 * @property parameters 组件参数 / Component parameters
 * @property version 组件版本 / Component version
 */
@Serializable
data class ComponentSpec(
    val type: String,
    val parameters: JsonObject = JsonObject(emptyMap()),
    val version: Int = 1
)

/** 初始化种子来源 / Initialization seed source. */
@Serializable
enum class InitializationMode { Model, WorkingSet }

/** 解导入能力开关，启用时遵循算法原生导入语义 / Import capability switch, retaining native algorithm import semantics. */
@Serializable
enum class ImportMode { Native, Disabled }

/** 算法实例配置 / Algorithm instance configuration.
 * @property id 稳定实例 ID / Stable instance ID
 * @property algorithm 算法组件 / Algorithm component
 * @property size 种群规模 / Population size
 * @property updates 更新算子池 / Update pool
 * @property mutations 变异算子池 / Mutation pool
 * @property neighborhoods 邻域池 / Neighborhood pool
 * @property parameters 参数源池 / Parameter source pools
 * @property selection 槽位选择策略 / Slot selection policy
 * @property scalarization 显式标量化引用 / Explicit scalarization reference
 * @property children 组合子会话 / Composite child sessions
 * @property initialization 初始化种子来源 / Initialization seed source
 * @property importMode 导入能力 / Import capability
 * @property constraints 可选原生约束处理 / Optional native constraint handling
 */
@Serializable
data class AlgorithmConfig(
    val id: String,
    val algorithm: ComponentSpec,
    val size: Int = 20,
    val updates: List<ComponentSpec> = emptyList(),
    val mutations: List<ComponentSpec> = emptyList(),
    val neighborhoods: List<ComponentSpec> = emptyList(),
    val parameters: Map<String, List<ComponentSpec>> = emptyMap(),
    val selection: ComponentSpec = ComponentSpec("round-robin"),
    val scalarization: String? = null,
    val children: List<AlgorithmConfig> = emptyList(),
    val initialization: InitializationMode = InitializationMode.Model,
    val importMode: ImportMode = ImportMode.Native,
    val constraints: ConstraintHandlingConfig? = null
)

/** 执行方式 / Execution mode. */
@Serializable
enum class ExecutionMode { Serial, Synchronous, Asynchronous }

/** 学习方式 / Learning mode. */
@Serializable
enum class LearningMode { Train, Continue, Frozen }

/** 预算配置，至少设置一个全局限额 / Budget configuration requiring at least one global limit.
 * @property evaluations 全局评估上限 / Global evaluation limit
 * @property decisions 全局决策上限 / Global decision limit
 * @property timeMillis 有效运行时间上限 / Active runtime limit
 * @property sliceEvaluations 每片段评估额度 / Evaluations per slice
 * @property sliceSteps 每片段完整步数 / Complete steps per slice
 * @property allocator 预算分配组件 / Budget allocation component
 * @property steps 全局完整低层步数上限 / Global complete low-level step limit
 */
@Serializable
data class BudgetConfig(
    val evaluations: Long? = null,
    val decisions: Long? = null,
    val timeMillis: Long? = null,
    val sliceEvaluations: Long = 256,
    val sliceSteps: Long = 1,
    val allocator: ComponentSpec = ComponentSpec("fixed"),
    val steps: Long? = null
)

/** 并行配置 / Parallel execution configuration.
 * @property mode 执行方式 / Execution mode
 * @property parallelism 最大并发会话数 / Maximum concurrent sessions
 * @property seed 根随机种子 / Root random seed
 */
@Serializable
data class ExecutionConfig(
    val mode: ExecutionMode = ExecutionMode.Synchronous,
    val parallelism: Int = 1,
    val seed: Long = 1
)

/** 搜索记忆配置 / Search memory configuration.
 * @property archiveCapacity 档案容量 / Archive capacity
 * @property workingCapacity 工作解集容量 / Working-set capacity
 * @property acceptance 接受策略 / Acceptance policy
 * @property exchange 解交换策略 / Exchange policy
 */
@Serializable
data class MemoryConfig(
    val archiveCapacity: Int = 100,
    val workingCapacity: Int = 20,
    val acceptance: ComponentSpec = ComponentSpec("not-worse"),
    val exchange: ComponentSpec = ComponentSpec("none")
)

/** 学习配置 / Learning configuration.
 * @property mode 学习模式 / Learning mode
 * @property features 特征提取器 / Feature extractor
 * @property reward 奖励函数 / Reward function
 * @property trajectoryPath 可选轨迹输出 / Optional trajectory output
 */
@Serializable
data class LearningConfig(
    val mode: LearningMode = LearningMode.Train,
    val features: ComponentSpec = ComponentSpec("progress"),
    val reward: ComponentSpec = ComponentSpec("improvement"),
    val trajectoryPath: String? = null
)

/** 持久化配置 / Persistence configuration.
 * @property enabled 是否要求完整恢复能力 / Require complete recovery support
 * @property checkpointPath 检查点输出位置 / Checkpoint destination
 * @property everyDecisions 保存间隔，零表示仅显式保存 / Save interval, zero means explicit snapshots only
 */
@Serializable
data class PersistenceConfig(
    val enabled: Boolean = false,
    val checkpointPath: String? = null,
    val everyDecisions: Long = 0
)

/** 超启发式可执行配置 / Executable hyper-heuristic configuration.
 * @property version 配置版本 / Configuration version
 * @property problem 问题工厂引用 / Problem factory reference
 * @property algorithms 算法实例 / Algorithm instances
 * @property budget 全局预算 / Global budget
 * @property selection 算法选择策略 / Algorithm selection policy
 * @property execution 执行配置 / Execution configuration
 * @property memory 搜索记忆 / Search memory
 * @property learning 学习配置 / Learning configuration
 * @property persistence 持久化配置 / Persistence configuration
 */
@Serializable
data class HyperHeuristicConfig(
    val version: Int = 1,
    val problem: ComponentSpec,
    val algorithms: List<AlgorithmConfig>,
    val budget: BudgetConfig,
    val selection: ComponentSpec = ComponentSpec("round-robin"),
    val execution: ExecutionConfig = ExecutionConfig(),
    val memory: MemoryConfig = MemoryConfig(),
    val learning: LearningConfig = LearningConfig(),
    val persistence: PersistenceConfig = PersistenceConfig()
) {
    /** 校验结构性约束，组件能力另由注册表校验 / Validate structure; the registry validates component capabilities.
     * @return 校验结果 / Validation result
     */
    fun validate(): Try {
        if (version != 1) return searchInvalid("unsupported configuration version: $version")
        if (algorithms.isEmpty() || algorithms.any { !it.id.matches(Regex("[A-Za-z0-9][A-Za-z0-9._-]*")) || it.size <= 0 } || algorithms.map { it.id }.distinct().size != algorithms.size) {
            return searchInvalid("algorithm IDs must be nonempty and unique; sizes must be positive")
        }
        if (budget.evaluations == null && budget.decisions == null && budget.timeMillis == null && budget.steps == null) return searchInvalid("a global stopping limit is required")
        if (listOfNotNull(budget.evaluations, budget.decisions, budget.timeMillis, budget.steps).any { it < 0 } || budget.sliceEvaluations <= 0 || budget.sliceSteps <= 0) {
            return searchInvalid("invalid budget")
        }
        if (execution.parallelism <= 0 || memory.archiveCapacity <= 0 || memory.workingCapacity <= 0) return searchInvalid("invalid capacities")
        if (persistence.everyDecisions < 0 || persistence.everyDecisions > 0 && (!persistence.enabled || persistence.checkpointPath == null)) {
            return searchInvalid("automatic checkpoints require persistence and a destination")
        }
        return ok
    }
}

/** 类型安全配置入口 / Type-safe configuration entry.
 * @param block 配置构造 / Configuration construction
 * @return 已校验配置 / Validated configuration
 */
fun hyperHeuristic(block: HyperHeuristicBuilder.() -> Unit): Ret<HyperHeuristicConfig> {
    return searchBoundary { HyperHeuristicBuilder().apply(block) }.then { it.build() }
}
