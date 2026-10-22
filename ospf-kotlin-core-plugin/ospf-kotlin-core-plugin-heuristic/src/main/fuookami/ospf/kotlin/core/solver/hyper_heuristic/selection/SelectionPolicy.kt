package fuookami.ospf.kotlin.core.solver.hyper_heuristic.selection

import kotlin.math.*
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.core.solver.heuristic.*
import fuookami.ospf.kotlin.core.solver.heuristic.operator.rl.QTable

/** 选择上下文 / Selection context.
 * @property state 离散状态 / Discrete state
 * @property features 无量纲特征 / Dimensionless features
 * @property decision 决策序号 / Decision sequence
 * @property stagnation 停滞计数 / Stagnation count
 */
@Serializable
data class SelectionContext(
    val state: Int = 0,
    val features: List<Flt64> = emptyList(),
    val decision: Long = 0,
    val stagnation: Long = 0
)

/** 已执行动作的转移，反馈以动作 ID 配对 / Executed transition paired by action ID.
 * @property episode 实例及种子标识 / Instance and seed identifier
 * @property decision 派发序号 / Dispatch sequence
 * @property action 实际动作 ID / Executed action ID
 * @property before 派发状态 / Dispatch state
 * @property after 结果状态 / Outcome state
 * @property available 派发时的动作掩码 / Dispatch action mask
 * @property nextAvailable 后继动作掩码 / Successor action mask
 * @property reward 奖励 / Reward
 * @property evaluations 请求成本 / Request cost
 * @property terminal 是否终止 / Whether terminal
 * @property policyVersion 派发时策略版本 / Policy version at dispatch
 * @property featureVersion 特征版本 / Feature version
 * @property scope 算法或稳定算子路径 / Algorithm or stable operator path
 * @property steps 完整低层步数 / Complete low-level steps
 * @property failed 动作是否失败 / Whether execution failed
 * @property mergedImprovement 全局归并是否改进 / Whether global merging improved the archive
 */
@Serializable
data class SelectionTransition(
    val episode: String,
    val decision: Long,
    val action: String,
    val before: SelectionContext,
    val after: SelectionContext,
    val available: List<String>,
    val nextAvailable: List<String>,
    val reward: Flt64,
    val evaluations: Long,
    val terminal: Boolean,
    val policyVersion: Long,
    val featureVersion: String = "progress:1",
    val scope: String = "algorithm",
    val steps: Long = 0,
    val failed: Boolean = false,
    val mergedImprovement: Boolean = false
)

/** 可移植策略快照，动作按 ID 对齐 / Portable policy snapshot aligned by action IDs.
 * @property version 格式版本 / Format version
 * @property type 策略类型 / Policy type
 * @property actions 稳定动作 ID / Stable action IDs
 * @property revision 学习更新次数 / Learning update count
 * @property cursor 调度游标 / Scheduling cursor
 * @property counts Bandit 观测数 / Bandit observation counts
 * @property sums Bandit 奖励和 / Bandit reward sums
 * @property table Q 表 / Q table
 * @property extension 自定义选择器的无损状态 / Lossless custom selector state
 */
@Serializable
data class PolicySnapshot(
    val version: Int = 1,
    val type: String,
    val actions: List<String>,
    val revision: Long = 0,
    val cursor: Long = 0,
    val counts: List<Long> = emptyList(),
    val sums: List<Double> = emptyList(),
    val table: List<List<Double>> = emptyList(),
    val extension: JsonElement = JsonNull
)

/** 算法与算子共用的选择协议 / Selection protocol shared by algorithms and operators. */
interface SelectionPolicy {
    /** 策略修订号 / Policy revision. */
    val revision: Long

    /** 可处理的离散状态数，null 表示任意 / Supported discrete-state count, null means unrestricted. */
    val stateCount: Int? get() = null

    /** 根据动作掩码选择 / Select using an action mask.
     * @param context 决策状态 / Decision state
     * @param available 可用动作 / Available actions
     * @param random 独立随机流 / Independent random stream
     * @return 稳定动作 ID / Stable action ID
     */
    fun select(
        context: SelectionContext,
        available: List<String>,
        random: SearchRandom
    ): Ret<String>

    /** 反馈实际执行的转移 / Observe the actually executed transition.
     * @param transition 已执行转移 / Executed transition
     * @return 更新结果 / Update result
     */
    fun observe(transition: SelectionTransition): Try

    /** 导出策略 / Export policy.
     * @return 策略快照 / Policy snapshot
     */
    fun snapshot(): PolicySnapshot

    /** 根据动作 ID 恢复 / Restore by action IDs.
     * @param snapshot 策略快照 / Policy snapshot
     * @return 恢复结果 / Restore result
     */
    fun restore(snapshot: PolicySnapshot): Try
}

/** 与搜索状态分离的算子策略传递 / Operator-policy transfer separated from search state. */
interface PolicyCarrier {
    /** 导出内层策略 / Export inner policies.
     * @return 槽位路径到策略 / Slot paths to policies
     */
    fun policies(): Map<String, PolicySnapshot>

    /** 消费已完成的算子轨迹 / Consume completed operator transitions.
     * @return 自上次消费后的记录 / Records since the previous drain
     */
    fun drainTransitions(): List<SelectionTransition> = emptyList()

    /** 恢复内层学习参数 / Restore inner learned parameters.
     * @param policies 槽位路径到策略 / Slot paths to policies
     * @return 恢复结果 / Restore result
     */
    fun restorePolicies(policies: Map<String, PolicySnapshot>): Try
}

/** 内置表格与规则选择器 / Built-in tabular and rule selector.
 * @property type random、round-robin、fixed、stagnation、epsilon-greedy、ucb1 或 q-learning / Selector identifier
 * @property actions 完整动作定义 / Complete action definition
 * @property learning 是否更新学习参数 / Whether to update learned parameters
 * @property epsilon 探索概率 / Exploration probability
 * @property alpha Q 学习率 / Q learning rate
 * @property gamma Q 折扣率 / Q discount
 * @property states 离散状态数 / Discrete state count
 * @property stagnationLimit 停滞切换阈值 / Stagnation switching threshold
 */
class TabularSelectionPolicy(
    val type: String,
    val actions: List<String>,
    val learning: Boolean = true,
    val epsilon: Double = 0.1,
    val alpha: Double = 0.5,
    val gamma: Double = 0.9,
    val states: Int = 64,
    val stagnationLimit: Long = 10
) : SelectionPolicy {
    override val stateCount: Int get() = states

    override var revision: Long = 0
        private set
    private var cursor = 0L
    private val counts = MutableList(actions.size) { 0L }
    private val sums = MutableList(actions.size) { 0.0 }
    private val table = QTable(states, actions.size)

    /** 校验超参 / Validate hyperparameters.
     * @return 校验结果 / Validation result
     */
    fun validate(): Try {
        if (type !in setOf("random", "round-robin", "fixed", "stagnation", "epsilon-greedy", "ucb1", "q-learning")) return searchInvalid("unknown selection: $type")
        if (actions.isEmpty() || actions.any { it.isBlank() } || actions.distinct().size != actions.size || states <= 0) return searchInvalid("invalid action/state definitions")
        if (listOf(epsilon, alpha, gamma).any { !it.isFinite() || it !in 0.0..1.0 } || stagnationLimit <= 0) return searchInvalid("invalid learning parameters")
        return ok
    }

    override fun select(
        context: SelectionContext,
        available: List<String>,
        random: SearchRandom
    ): Ret<String> {
        if (available.isEmpty() || available.distinct().size != available.size || available.any { it !in actions }) return searchInvalid("empty or invalid action mask")
        if (context.state !in 0 until states || context.features.any { !it.toDouble().isFinite() }) return searchInvalid("invalid selection state")
        val indices = available.map { actions.indexOf(it) }
        fun randomIndex(): Int = indices[(random().toDouble() * indices.size).toInt().coerceIn(indices.indices)]
        val index = when (type) {
            "random" -> randomIndex()
            "fixed", "round-robin" -> {
                val start = (cursor % actions.size).toInt()
                val chosen = (0 until actions.size).map { (start + it) % actions.size }.first { it in indices }
                cursor = chosen.toLong() + 1
                chosen
            }
            "stagnation" -> {
                if (context.stagnation > 0 && context.stagnation % stagnationLimit == 0L) ++cursor
                indices.firstOrNull { it >= (cursor % actions.size).toInt() } ?: indices.first()
            }
            "q-learning" -> if (learning && random().toDouble() < epsilon) randomIndex() else indices.maxBy { table.valueOf(context.state, it).toDouble() }
            "epsilon-greedy" -> if (learning && random().toDouble() < epsilon) randomIndex() else indices.maxBy { mean(it) }
            "ucb1" -> {
                if (!learning) indices.maxBy { mean(it) }
                else indices.firstOrNull { counts[it] == 0L } ?: indices.maxBy { mean(it) + sqrt(2.0 * ln(counts.sum().toDouble() + 1.0) / counts[it]) }
            }
            else -> return searchInvalid("unknown selection: $type")
        }
        return Ok(actions[index])
    }

    private fun mean(index: Int): Double = if (counts[index] == 0L) 0.0 else sums[index] / counts[index]

    override fun observe(transition: SelectionTransition): Try {
        val index = actions.indexOf(transition.action)
        if (index < 0 || transition.action !in transition.available || !transition.reward.toDouble().isFinite() || transition.evaluations < 0 ||
            transition.before.state !in 0 until states || transition.after.state !in 0 until states ||
            transition.available.any { it !in actions } || transition.nextAvailable.any { it !in actions }) return searchInvalid("invalid observed transition")
        if (!learning) return ok
        if (type == "q-learning") {
            val result = table.update(
                state = transition.before.state,
                action = index,
                reward = transition.reward,
                nextState = transition.after.state,
                alpha = Flt64(alpha),
                gamma = Flt64(gamma),
                terminal = transition.terminal,
                availableActions = transition.nextAvailable.map { actions.indexOf(it) }
            )
            if (result !is Ok) return result.map { success }
        }
        if (type in setOf("q-learning", "epsilon-greedy", "ucb1")) {
            ++counts[index]
            sums[index] += transition.reward.toDouble()
            ++revision
        }
        return ok
    }

    override fun snapshot(): PolicySnapshot {
        return PolicySnapshot(
            type = type,
            actions = actions,
            revision = revision,
            cursor = cursor,
            counts = counts.toList(),
            sums = sums.toList(),
            table = if (type == "q-learning") table.snapshot().map { row -> row.map { it.toDouble() } } else emptyList()
        )
    }

    override fun restore(snapshot: PolicySnapshot): Try {
        if (snapshot.version != 1 || snapshot.type != type || snapshot.actions.toSet() != actions.toSet() ||
            snapshot.actions.size != actions.size || snapshot.revision < 0 || snapshot.cursor < 0 ||
            snapshot.counts.size != actions.size || snapshot.sums.size != actions.size ||
            snapshot.counts.any { it < 0 } || snapshot.sums.any { !it.isFinite() }) return searchInvalid("incompatible policy snapshot")
        val permutation = actions.map { snapshot.actions.indexOf(it) }
        if (type == "q-learning") {
            if (snapshot.table.size != states || snapshot.table.any { it.size != actions.size }) return searchInvalid("incompatible Q table")
            val result = table.restore(snapshot.table.map { row -> permutation.map { Flt64(row[it]) } })
            if (result !is Ok) return result
        }
        for (index in actions.indices) {
            counts[index] = snapshot.counts[permutation[index]]
            sums[index] = snapshot.sums[permutation[index]]
        }
        cursor = snapshot.cursor
        revision = snapshot.revision
        return ok
    }
}
