package fuookami.ospf.kotlin.core.solver.hyper_heuristic.memory

import kotlin.math.sqrt
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.core.solver.heuristic.*
import fuookami.ospf.kotlin.core.solver.hyper_heuristic.selection.RecoverableComponent

@Serializable
private data class AutoAcceptanceState(
    val version: Int = 1,
    val horizon: Long,
    val capacity: Int,
    val span: Int,
    val decisions: Long,
    val improvements: Int,
    val history: JsonElement
)

/** 按预算与近期改善自适应历史跨度，不需要温度或目标量纲 / Budget- and progress-adaptive history without temperature or objective units.
 * @property codec 原始业务目标编码 / Original business objective codec
 * @property horizon 可用预算的规模提示，不限制运行 / Budget-scale hint, not a stopping limit
 */
class AutoAcceptance<O, V>(
    private val codec: SearchCodec<O>,
    val horizon: Long = Long.MAX_VALUE
) : AcceptancePolicy<O, V>, RecoverableComponent where V : RealNumber<V>, V : NumberField<V> {
    private val capacity = sqrt(horizon.coerceAtLeast(1).toDouble()).toInt().coerceIn(8, 256)
    private var span = (capacity / 2).coerceAtLeast(2)
    private var decisions = 0L
    private var improvements = 0
    private val history = ArrayDeque<O>()

    /** 当前延迟跨度，供诊断而非手工调参 / Current delay span for diagnostics rather than manual tuning. */
    val historySpan: Int get() = span

    override fun accept(
        problem: SearchProblem<O, V>,
        working: List<Individual<O, V>>,
        candidate: Individual<O, V>
    ): Boolean {
        val current = working.firstOrNull()?.fitness ?: candidate.fitness
        val delayed = history.elementAtOrNull((history.size - span).coerceAtLeast(0)) ?: current
        val improved = problem.dominates(candidate.fitness, current)
        val accepted = !problem.dominates(current, candidate.fitness) || !problem.dominates(delayed, candidate.fitness)
        if (improved) ++improvements
        history.addLast(if (accepted) candidate.fitness else current)
        while (history.size > capacity) history.removeFirst()
        ++decisions
        if (decisions % capacity == 0L) {
            span = when {
                improvements == 0 -> (span * 2).coerceAtMost(capacity)
                improvements * 4 >= capacity -> (span / 2).coerceAtLeast(2)
                else -> span
            }
            improvements = 0
        }
        return accepted
    }

    override fun save(): Ret<JsonElement> {
        return SearchCodecs.list(codec).encode(history.toList()).map { encoded ->
            Json.encodeToJsonElement(AutoAcceptanceState(
                horizon = horizon,
                capacity = capacity,
                span = span,
                decisions = decisions,
                improvements = improvements,
                history = encoded
            ))
        }
    }

    override fun load(state: JsonElement): Try {
        return searchBoundary { Json.decodeFromJsonElement<AutoAcceptanceState>(state) }.then { saved ->
            if (saved.version != 1 || saved.horizon != horizon || saved.capacity != capacity ||
                saved.span !in 2..capacity || saved.decisions < 0 ||
                saved.improvements !in 0..(saved.decisions % capacity).toInt()) {
                return@then searchInvalid("自动接受状态不兼容 / Incompatible automatic acceptance state")
            }
            SearchCodecs.list(codec).decode(saved.history).then { values ->
                if (values.size != saved.decisions.coerceAtMost(capacity.toLong()).toInt()) {
                    return@then searchInvalid("自动接受历史不完整 / Incomplete automatic acceptance history")
                }
                history.clear()
                history.addAll(values)
                span = saved.span
                decisions = saved.decisions
                improvements = saved.improvements
                ok
            }
        }
    }
}
