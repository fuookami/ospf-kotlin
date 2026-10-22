package fuookami.ospf.kotlin.core.solver.hyper_heuristic.configuration

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.solver.heuristic.*
import fuookami.ospf.kotlin.core.solver.hyper_heuristic.*

/** 防止嵌套算法误写外层配置 / Prevent nested algorithms from mutating an outer DSL scope. */
@DslMarker
annotation class HyperHeuristicDsl

/** 类型安全的运行配置构建器 / Type-safe run-configuration builder.
 * @property problem 已注册问题 / Registered problem
 * @property budget 显式全局预算 / Explicit global budget
 * @property selection 算法选择策略 / Algorithm selector
 * @property execution 执行配置 / Execution configuration
 * @property memory 记忆配置 / Memory configuration
 * @property learning 学习配置 / Learning configuration
 * @property persistence 持久化配置 / Persistence configuration
 */
@HyperHeuristicDsl
class HyperHeuristicBuilder {
    var problem: ComponentSpec? = null
    var budget = BudgetConfig()
    var selection = ComponentSpec("round-robin")
    var execution = ExecutionConfig()
    var memory = MemoryConfig()
    var learning = LearningConfig()
    var persistence = PersistenceConfig()
    private val algorithms = ArrayList<AlgorithmConfig>()

    /** 添加独立算法实例 / Add an independent algorithm instance.
     * @param id 稳定实例 ID / Stable instance ID
     * @param type 注册算法类型 / Registered algorithm type
     * @param block 算法及子算法配置 / Algorithm and child configuration
     */
    fun algorithm(
        id: String,
        type: String,
        block: AlgorithmBuilder.() -> Unit = {}
    ) {
        algorithms.add(AlgorithmBuilder(id, type).apply(block).build())
    }

    internal fun build(): Ret<HyperHeuristicConfig> {
        val problem = problem ?: return searchInvalid("a registered problem is required")
        val config = HyperHeuristicConfig(
            problem = problem,
            algorithms = algorithms.toList(),
            budget = budget,
            selection = selection,
            execution = execution,
            memory = memory,
            learning = learning,
            persistence = persistence
        )
        return config.validate().map { config }
    }
}

/** 保留槽位类型边界的算法 DSL / Algorithm DSL retaining slot boundaries.
 * @param id 稳定实例 ID / Stable instance ID
 * @param type 注册算法类型 / Registered algorithm type
 * @property component 算法参数 / Algorithm parameters
 * @property size 种群规模 / Population size
 * @property selection 算子选择策略 / Operator selector
 * @property updates 更新算子池 / Update pool
 * @property mutations 变异算子池 / Mutation pool
 * @property neighborhoods 邻域池 / Neighborhood pool
 * @property parameters 命名参数源池 / Named parameter-source pools
 * @property scalarization 已注册标量化 ID / Registered scalarization ID
 */
@HyperHeuristicDsl
class AlgorithmBuilder internal constructor(private val id: String, type: String) {
    var component = ComponentSpec(type)
    var size = 20
    var selection = ComponentSpec("round-robin")
    var updates = emptyList<ComponentSpec>()
    var mutations = emptyList<ComponentSpec>()
    var neighborhoods = emptyList<ComponentSpec>()
    var parameters = emptyMap<String, List<ComponentSpec>>()
    var scalarization: String? = null
    var initialization = InitializationMode.Model
    var importMode = ImportMode.Native
    private val children = ArrayList<AlgorithmConfig>()

    /** 添加固定组合中的子会话，顺序即阶段顺序 / Add a child session; order defines phase order.
     * @param id 稳定子实例 ID / Stable child ID
     * @param type 注册算法类型 / Registered algorithm type
     * @param block 子会话配置 / Child configuration
     */
    fun algorithm(
        id: String,
        type: String,
        block: AlgorithmBuilder.() -> Unit = {}
    ) {
        children.add(AlgorithmBuilder(id, type).apply(block).build())
    }

    internal fun build(): AlgorithmConfig {
        return AlgorithmConfig(
            id = id,
            algorithm = component,
            size = size,
            updates = updates,
            mutations = mutations,
            neighborhoods = neighborhoods,
            parameters = parameters,
            selection = selection,
            scalarization = scalarization,
            children = children.toList(),
            initialization = initialization,
            importMode = importMode
        )
    }
}
