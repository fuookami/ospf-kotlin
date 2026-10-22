package fuookami.ospf.kotlin.core.solver.hyper_heuristic.persistence

import kotlinx.serialization.*
import kotlinx.serialization.json.*
import fuookami.ospf.kotlin.core.solver.heuristic.SearchSessionSnapshot
import fuookami.ospf.kotlin.core.solver.hyper_heuristic.selection.*

/** 与实例搜索状态分离的可迁移策略 / Transferable policy independent of instance search state.
 * @property version 格式版本 / Format version
 * @property definition 算法、特征、奖励和超参指纹 / Algorithm, feature, reward and hyperparameter fingerprint
 * @property featureVersion 特征版本 / Feature version
 * @property root 算法选择策略 / Algorithm selection policy
 * @property operators 各算法的算子策略 / Operator policies per algorithm
 * @property metadata 训练元数据 / Training metadata
 * @property definitions 算法、特征、奖励及学习参数定义 / Algorithm, feature, reward and learning definitions
 */
@Serializable
data class PolicyArchive(
    val version: Int = 1,
    val definition: String,
    val featureVersion: String,
    val root: PolicySnapshot,
    val operators: Map<String, Map<String, PolicySnapshot>>,
    val metadata: Map<String, String> = emptyMap(),
    val definitions: JsonObject = JsonObject(emptyMap())
)

/** 分支检查点 / Branch checkpoint.
 * @property session 完整算法状态 / Complete algorithm state
 * @property random 分支随机状态 / Branch random state
 * @property initialized 是否已完成初始化 / Whether initialization completed
 * @property disabled 是否不再派发 / Whether no longer dispatchable
 * @property problemState 回调状态 / Callback state
 */
@Serializable
data class WorkerCheckpoint(
    val session: SearchSessionSnapshot,
    val random: Long,
    val initialized: Boolean,
    val disabled: Boolean,
    val problemState: JsonElement = JsonNull
)

/** 静止边界的完整检查点 / Complete checkpoint at a quiescent boundary.
 * @property version 格式版本 / Format version
 * @property configuration 配置指纹 / Configuration fingerprint
 * @property problem 输入及目标指纹 / Input and objective fingerprint
 * @property policy 可迁移策略 / Transferable policy
 * @property workers 分支状态 / Branch states
 * @property archive 有效档案 / Valid archive
 * @property working 已接受工作解 / Accepted working solutions
 * @property evaluations 已用评估 / Consumed evaluations
 * @property dispatched 已派发序号 / Dispatched sequence
 * @property completed 已完成决策 / Completed decisions
 * @property steps 完整低层步数 / Complete low-level steps
 * @property stagnation 全局停滞 / Global stagnation
 * @property activeNanos 有效运行时间 / Active runtime
 * @property random 协调器随机流 / Coordinator random stream
 * @property diagnostics 诊断 / Diagnostics
 * @property problemState 协调器问题状态 / Coordinator problem state
 * @property objectiveCalls 实际模型目标调用数 / Actual model objective invocations
 * @property transitions 已完成的双层轨迹 / Completed two-level trajectories
 * @property successes 全局改善次数 / Global improvement count
 * @property lastCost 最近动作评估成本 / Evaluation cost of the latest action
 * @property configurationData 完整运行配置 / Complete run configuration
 * @property components 全局扩展组件状态 / Global extension component state
 */
@Serializable
data class RunCheckpoint(
    val version: Int = 1,
    val configuration: String,
    val problem: String,
    val policy: PolicyArchive,
    val workers: Map<String, WorkerCheckpoint>,
    val archive: JsonElement,
    val working: JsonElement,
    val evaluations: Long,
    val dispatched: Long,
    val completed: Long,
    val steps: Long,
    val stagnation: Long,
    val activeNanos: Long,
    val random: Long,
    val diagnostics: List<String> = emptyList(),
    val problemState: JsonElement = JsonNull,
    val objectiveCalls: Long = 0,
    val transitions: List<SelectionTransition> = emptyList(),
    val successes: Long = 0,
    val lastCost: Long = 0,
    val configurationData: JsonElement = JsonNull,
    val components: Map<String, JsonElement> = emptyMap()
)
