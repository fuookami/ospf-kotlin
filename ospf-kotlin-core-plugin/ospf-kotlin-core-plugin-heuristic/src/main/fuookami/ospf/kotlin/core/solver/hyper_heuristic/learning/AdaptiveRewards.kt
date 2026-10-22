package fuookami.ospf.kotlin.core.solver.hyper_heuristic.learning

import kotlin.math.*
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.core.solver.heuristic.*
import fuookami.ospf.kotlin.core.solver.hyper_heuristic.selection.RecoverableComponent

/** 从同一派发档案构建信用输入，数值转换集中在问题适配器 / Build credit against a frozen dispatch archive at the problem adapter.
 * @param problem 问题语义 / Problem semantics
 * @param reference 派发档案 / Dispatch archive
 * @param candidates 已评估候选 / Evaluated candidates
 * @param contribution 有效贡献 / Effective contribution
 * @param evaluations 请求数 / Request count
 * @param elapsedNanos 动作耗时 / Action duration
 * @param action 动作 ID / Action ID
 * @return 奖励上下文 / Reward context
 */
internal fun <O, V> rewardContext(
    problem: SearchProblem<O, V>,
    reference: List<Individual<O, V>>,
    candidates: List<Individual<O, V>>,
    contribution: Pair<Boolean, Double>,
    evaluations: Long,
    elapsedNanos: Long,
    action: String
): RewardContext where V : RealNumber<V>, V : NumberField<V> {
    val projection = problem.objectiveCoordinates
    fun coordinates(entries: List<Individual<O, V>>): List<List<Flt64>> {
        if (projection == null) return emptyList()
        return entries.filter { problem.valid(it.fitness) && problem.validSolution(it.solution) && problem.feasible(it.solution) }.map { projection(it.fitness) }
    }
    return RewardContext(
        improved = contribution.first,
        contribution = Flt64(contribution.second),
        evaluations = evaluations,
        elapsedNanos = elapsedNanos,
        action = action,
        reference = coordinates(reference),
        candidates = coordinates(candidates)
    )
}

private fun scalarGain(context: RewardContext): Double {
    val all = context.reference + context.candidates
    if (all.isEmpty()) return context.contribution.toDouble()
    if (all.any { it.size != 1 || !it.single().toDouble().isFinite() }) return Double.NaN
    if (context.reference.isEmpty() || context.candidates.isEmpty()) return 0.0
    return (context.reference.minOf { it.single().toDouble() } - context.candidates.minOf { it.single().toDouble() }).coerceAtLeast(0.0)
}

/** 单位秒收益，显式坐标已在边界归一化 / Gain per second using explicitly normalized coordinates.
 * @property epsilonSeconds 最短计费时长 / Minimum charged duration
 */
class TimeNormalizedReward(val epsilonSeconds: Double = 1e-9) : RewardPolicy {
    override val requiresCoordinates = true

    override fun reward(context: RewardContext): Flt64 {
        if (!epsilonSeconds.isFinite() || epsilonSeconds <= 0 || context.elapsedNanos < 0) return Flt64(Double.NaN)
        return Flt64(scalarGain(context) / (context.elapsedNanos / 1e9 + epsilonSeconds))
    }
}

@Serializable
private data class ExtremeState(val size: Int, val observations: List<Pair<String, Double>>)

/** 按动作保护近期极值突破，全局窗口使旧信用过期 / Protect recent extreme gains per action with global-window expiry.
 * @property windowSize 最近动作窗口 / Recent action window
 */
class ExtremeValueReward(val windowSize: Int = 50) : RewardPolicy, RecoverableComponent {
    private val observations = ArrayDeque<Pair<String, Double>>()
    override val requiresCoordinates = true

    override fun reward(context: RewardContext): Flt64 {
        val gain = scalarGain(context)
        if (windowSize <= 0 || !gain.isFinite()) return Flt64(Double.NaN)
        observations.addLast(context.action to gain)
        while (observations.size > windowSize) observations.removeFirst()
        return Flt64(observations.filter { it.first == context.action }.maxOfOrNull { it.second } ?: 0.0)
    }

    override fun save(): Ret<JsonElement> = Ok(Json.encodeToJsonElement(ExtremeState(windowSize, observations.toList())))

    override fun load(state: JsonElement): Try = searchBoundary { Json.decodeFromJsonElement<ExtremeState>(state) }.then { saved ->
        if (saved.size != windowSize || saved.observations.size > windowSize || saved.observations.any { it.first.isBlank() || !it.second.isFinite() || it.second < 0 }) {
            searchInvalid("invalid extreme-value reward state")
        } else {
            observations.clear()
            observations.addAll(saved.observations)
            ok
        }
    }
}

/** 有效非支配新贡献，不以子代数量冒充质量 / Effective novel non-dominated contribution, never offspring count. */
object DominanceReward : RewardPolicy {
    override fun reward(context: RewardContext): Flt64 = context.contribution
}
