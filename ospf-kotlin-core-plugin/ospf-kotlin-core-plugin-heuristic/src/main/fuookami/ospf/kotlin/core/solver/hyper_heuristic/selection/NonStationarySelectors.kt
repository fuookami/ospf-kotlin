package fuookami.ospf.kotlin.core.solver.hyper_heuristic.selection

import kotlin.math.*
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.solver.heuristic.*

@Serializable
private data class CreditObservation(val action: String, val reward: Double)

@Serializable
private data class CreditDispatch(val action: String, val previous: String?)

@Serializable
private data class CreditState(
    val definition: List<Double>,
    val window: List<CreditObservation>,
    val quality: Map<String, Double>,
    val pairs: Map<String, Map<String, Double>>,
    val probabilities: Map<String, Double>,
    val lastUsed: Map<String, Long>,
    val previous: String?,
    val pending: Map<Long, CreditDispatch>,
    val clock: Long
)

/** 非平稳信用分配器，状态按动作 ID 恢复 / Non-stationary credit assignment restored by action IDs.
 * @property type frr-mab、choice-function 或 adaptive-pursuit / Selector identifier
 * @property actions 稳定动作 ID / Stable action IDs
 * @property learning 是否更新信用 / Whether credit is learned
 * @property windowSize 最近反馈窗口 / Recent feedback window
 * @property exploration UCB 探索系数 / UCB exploration coefficient
 * @property decay 秩衰减 / Rank decay
 * @property rate 指数更新率 / Exponential learning rate
 * @property minimumProbability 全动作最小概率 / Minimum probability over all actions
 * @property recencyWeight 协同选择的等待权重 / Waiting weight for choice-function selection
 */
open class NonStationarySelector(
    val type: String,
    val actions: List<String>,
    val learning: Boolean = true,
    val windowSize: Int = 50,
    val exploration: Double = 0.5,
    val decay: Double = 0.8,
    val rate: Double = 0.2,
    val minimumProbability: Double = 0.01,
    val recencyWeight: Double = 0.1
) : SelectionPolicy {
    final override var revision = 0L
        private set
    private val window = ArrayDeque<CreditObservation>()
    private var quality = actions.associateWith { 0.0 }.toMutableMap()
    private var pairs = mutableMapOf<String, MutableMap<String, Double>>()
    private var probabilities = actions.associateWith { 1.0 / actions.size.coerceAtLeast(1) }.toMutableMap()
    private var lastUsed = actions.associateWith { 0L }.toMutableMap()
    private var previous: String? = null
    private var pending = mutableMapOf<Long, CreditDispatch>()
    private var clock = 0L

    /** 校验参数，不隐式裁剪概率 / Validate parameters without silently clipping probabilities.
     * @return 校验结果 / Validation result
     */
    fun validate(): Try {
        if (type !in setOf("frr-mab", "choice-function", "adaptive-pursuit") || actions.isEmpty() ||
            actions.any { it.isBlank() } || actions.distinct().size != actions.size || windowSize !in 1..1_000_000 ||
            listOf(exploration, decay, rate, minimumProbability, recencyWeight).any { !it.isFinite() } ||
            exploration < 0 || decay !in 0.0..1.0 || rate <= 0 || rate > 1 || minimumProbability < 0 ||
            minimumProbability * actions.size > 1 || recencyWeight < 0 || type == "frr-mab" && windowSize < actions.size) return searchInvalid("invalid non-stationary selector parameters")
        return ok
    }

    override fun select(context: SelectionContext, available: List<String>, random: SearchRandom): Ret<String> {
        val valid = validate()
        if (valid !is Ok) return searchFailure(valid)
        if (available.isEmpty() || available.distinct().size != available.size || available.any { it !in actions } || context.decision < 0) {
            return searchInvalid("invalid non-stationary action mask/context")
        }
        val mask = actions.filter { it in available }
        val chosen = when (type) {
            "frr-mab" -> {
                val counts = actions.associateWith { action -> window.count { it.action == action } }
                val scale = (window.maxOfOrNull { it.reward.coerceAtLeast(0.0) } ?: 0.0).let { if (it == 0.0) 1.0 else it }
                val totals = actions.associateWith { action -> window.filter { it.action == action }.map { it.reward.coerceAtLeast(0.0) / scale }.sum() }
                val ranking = actions.sortedByDescending { totals.getValue(it) }
                val credits = ranking.mapIndexed { rank, action -> action to if (totals.getValue(action) > 0) decay.pow(rank) * (actions.size - rank) else 0.0 }.toMap()
                val total = credits.values.sum().coerceAtLeast(1.0)
                if (learning) mask.firstOrNull { counts.getValue(it) == 0 } ?: mask.maxBy {
                    credits.getValue(it) / total + exploration * sqrt(2 * ln(window.size.coerceAtLeast(1).toDouble() + 1) / counts.getValue(it))
                } else mask.maxBy { credits.getValue(it) }
            }
            "choice-function" -> {
                fun normalized(values: Map<String, Double>, action: String): Double {
                    val scale = values.values.maxOfOrNull { abs(it) } ?: 0.0
                    return if (scale == 0.0) 0.0 else (values[action] ?: 0.0) / scale
                }
                val pair = pairs[previous] ?: emptyMap()
                mask.maxBy {
                    normalized(quality, it) + normalized(pair, it) +
                        if (learning) recencyWeight * (clock - lastUsed.getValue(it)).toDouble() else 0.0
                }
            }
            else -> {
                val total = mask.map { probabilities.getValue(it) }.sum()
                val draw = random().toDouble() * total
                var cumulative = 0.0
                mask.firstOrNull { cumulative += probabilities.getValue(it); cumulative > draw } ?: mask.last()
            }
        }
        if (learning && type == "choice-function") {
            if (context.decision in pending) return searchInvalid("decision already awaiting feedback: ${context.decision}")
            pending[context.decision] = CreditDispatch(chosen, previous)
        }
        return Ok(chosen)
    }

    override fun observe(transition: SelectionTransition): Try {
        if (transition.action !in actions || transition.action !in transition.available ||
            (transition.available + transition.nextAvailable).any { it !in actions } ||
            !transition.reward.toDouble().isFinite() || transition.evaluations < 0 || transition.elapsedNanos < 0 ||
            transition.previousAction?.let { it !in actions } == true ||
            pending[transition.decision]?.let { it.action != transition.action } == true) return searchInvalid("invalid credit observation")
        if (!learning) return ok
        val action = transition.action
        val value = transition.reward.toDouble()
        val predecessor = pending.remove(transition.decision)?.previous ?: transition.previousAction
        window.addLast(CreditObservation(action, value))
        while (window.size > windowSize) window.removeFirst()
        quality[action] = (1.0 - rate) * quality.getValue(action) + rate * value
        if (predecessor != null) {
            val row = pairs.getOrPut(predecessor) { mutableMapOf() }
            val old = row[action] ?: 0.0
            row[action] = (1.0 - rate) * old + rate * value
        }
        if (type == "adaptive-pursuit") {
            val winner = actions.maxBy { quality.getValue(it) }
            for (id in actions) {
                val target = if (id == winner) 1.0 - minimumProbability * (actions.size - 1) else minimumProbability
                probabilities[id] = probabilities.getValue(id) + rate * (target - probabilities.getValue(id))
            }
        }
        ++clock
        lastUsed[action] = clock
        previous = if (transition.terminal) null else action
        ++revision
        return ok
    }

    override fun discard(decision: Long) {
        pending.remove(decision)
    }

    override fun predecessor(decision: Long): String? = pending[decision]?.previous

    override fun snapshot(): PolicySnapshot = PolicySnapshot(
        type = type,
        actions = actions,
        revision = revision,
        extension = Json.encodeToJsonElement(CreditState(
            definition = listOf(windowSize.toDouble(), exploration, decay, rate, minimumProbability, recencyWeight),
            window = window.toList(),
            quality = quality,
            pairs = pairs,
            probabilities = probabilities,
            lastUsed = lastUsed,
            previous = previous,
            pending = pending,
            clock = clock
        ))
    )

    override fun restore(snapshot: PolicySnapshot): Try {
        if (snapshot.version != 1 || snapshot.type != type || snapshot.actions.size != actions.size ||
            snapshot.actions.toSet() != actions.toSet() || snapshot.revision < 0) return searchInvalid("incompatible non-stationary policy snapshot")
        return searchBoundary { Json.decodeFromJsonElement<CreditState>(snapshot.extension) }.then { saved ->
            val definition = listOf(windowSize.toDouble(), exploration, decay, rate, minimumProbability, recencyWeight)
            if (saved.definition != definition || saved.clock < 0 || saved.window.size > windowSize ||
                saved.window.any { it.action !in actions || !it.reward.isFinite() } ||
                listOf(saved.quality, saved.probabilities).any { it.keys != actions.toSet() || it.values.any { v -> !v.isFinite() } } ||
                saved.lastUsed.keys != actions.toSet() || saved.lastUsed.values.any { it < 0 || it > saved.clock } ||
                saved.probabilities.values.any { it < minimumProbability || it > 1.0 } || abs(saved.probabilities.values.sum() - 1) > 1e-9 ||
                saved.pairs.any { (id, row) -> id !in actions || row.any { (next, value) -> next !in actions || !value.isFinite() } } ||
                saved.previous?.let { it !in actions } == true || saved.pending.any { (id, dispatch) ->
                    id < 0 || dispatch.action !in actions || dispatch.previous?.let { it !in actions } == true
                }) {
                return@then searchInvalid("invalid non-stationary credit state")
            }
            window.clear()
            window.addAll(saved.window)
            quality = saved.quality.toMutableMap()
            pairs = saved.pairs.mapValues { it.value.toMutableMap() }.toMutableMap()
            probabilities = saved.probabilities.toMutableMap()
            lastUsed = saved.lastUsed.toMutableMap()
            previous = saved.previous
            pending = saved.pending.toMutableMap()
            clock = saved.clock
            revision = snapshot.revision
            ok
        }
    }
}

/** 滑动窗口秩 MAB / Sliding-window rank MAB.
 * @param actions 动作 ID / Action IDs
 * @param learning 学习开关 / Learning switch
 * @param windowSize 窗口长度 / Window length
 */
class FRRMABSelector(actions: List<String>, learning: Boolean = true, windowSize: Int = 50) : NonStationarySelector(
    type = "frr-mab", actions = actions, learning = learning, windowSize = windowSize
)

/** 二阶协同选择 / Second-order choice-function selection.
 * @param actions 动作 ID / Action IDs
 * @param learning 学习开关 / Learning switch
 */
class ChoiceFunctionSelector(actions: List<String>, learning: Boolean = true) : NonStationarySelector(
    type = "choice-function", actions = actions, learning = learning
)

/** 保底探索的自适应追逐 / Adaptive pursuit with minimum exploration.
 * @param actions 动作 ID / Action IDs
 * @param learning 学习开关 / Learning switch
 */
class AdaptivePursuitSelector(actions: List<String>, learning: Boolean = true) : NonStationarySelector(
    type = "adaptive-pursuit", actions = actions, learning = learning
)
