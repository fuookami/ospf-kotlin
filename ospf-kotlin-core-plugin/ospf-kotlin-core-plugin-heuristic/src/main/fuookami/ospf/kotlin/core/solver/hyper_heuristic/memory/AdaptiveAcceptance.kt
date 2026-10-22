package fuookami.ospf.kotlin.core.solver.hyper_heuristic.memory

import kotlin.math.*
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.core.solver.heuristic.*
import fuookami.ospf.kotlin.core.solver.hyper_heuristic.selection.RecoverableComponent

@Serializable
private data class LateState(val length: Int, val cursor: Int, val history: JsonElement)

/** 无温度的单目标延迟接受，拒绝时同样推进历史 / Temperature-free scalar late acceptance; rejections also advance history.
 * @property codec 业务目标的无损 codec / Lossless business objective codec
 * @property length 历史长度，并非零参数 / History length, not parameter-free
 */
class LateAcceptance<O, V>(
    private val codec: SearchCodec<O>,
    val length: Int = 50
) : AcceptancePolicy<O, V>, RecoverableComponent where V : RealNumber<V>, V : NumberField<V> {
    private var history = emptyList<O>()
    private var cursor = 0

    override fun accept(problem: SearchProblem<O, V>, working: List<Individual<O, V>>, candidate: Individual<O, V>): Boolean {
        if (problem.multiObjective || length <= 0) return false
        val current = working.firstOrNull() ?: candidate
        if (history.isEmpty()) history = List(length) { current.fitness }
        val accepted = !problem.dominates(current.fitness, candidate.fitness) || !problem.dominates(history[cursor], candidate.fitness)
        history = history.toMutableList().also { it[cursor] = if (accepted) candidate.fitness else current.fitness }
        cursor = (cursor + 1) % length
        return accepted
    }

    override fun save(): Ret<JsonElement> = SearchCodecs.list(codec).encode(history).map {
        Json.encodeToJsonElement(LateState(length, cursor, it))
    }

    override fun load(state: JsonElement): Try = searchBoundary { Json.decodeFromJsonElement<LateState>(state) }.then { saved ->
        if (saved.length != length || length <= 0 || saved.cursor !in 0 until length) return@then searchInvalid("invalid late acceptance definition")
        SearchCodecs.list(codec).decode(saved.history).then { values ->
            if (values.isNotEmpty() && values.size != length) searchInvalid("invalid late acceptance history")
            else {
                history = values
                cursor = saved.cursor
                ok
            }
        }
    }
}

@Serializable
private data class RecordState(val ratio: Double, val horizon: Long, val clock: Long, val best: Double?, val scale: Double)

/** 单目标动态记录阈值，使用显式最小化坐标 / Dynamic scalar record threshold using explicit minimization coordinates.
 * @property initialRatio 初始容差比例 / Initial tolerance ratio
 * @property horizon 收紧到零的候选数 / Candidate count at zero tolerance
 */
class RecordToRecordAcceptance<O, V>(
    val initialRatio: Double = 0.05,
    val horizon: Long = 1000
) : AcceptancePolicy<O, V>, RecoverableComponent where V : RealNumber<V>, V : NumberField<V> {
    private var clock = 0L
    private var best: Double? = null
    private var scale = 0.0

    override fun accept(problem: SearchProblem<O, V>, working: List<Individual<O, V>>, candidate: Individual<O, V>): Boolean {
        val projection = problem.objectiveCoordinates ?: return false
        if (problem.multiObjective || initialRatio !in 0.0..1.0 || horizon <= 0) return false
        val points = (working.map { it.fitness } + candidate.fitness).map { projection(it) }
        if (points.any { it.size != 1 || !it[0].toDouble().isFinite() }) return false
        val value = points.last()[0].toDouble()
        val record = min(best ?: Double.POSITIVE_INFINITY, points.minOf { it[0].toDouble() })
        best = record
        scale = max(scale, points.maxOf { abs(it[0].toDouble()) })
        val tolerance = initialRatio * (1.0 - clock.toDouble() / horizon).coerceIn(0.0, 1.0) * scale
        ++clock
        return value - record <= tolerance
    }

    override fun save(): Ret<JsonElement> = Ok(Json.encodeToJsonElement(RecordState(
        ratio = initialRatio, horizon = horizon, clock = clock, best = best, scale = scale
    )))

    override fun load(state: JsonElement): Try = searchBoundary { Json.decodeFromJsonElement<RecordState>(state) }.then { saved ->
        if (saved.ratio != initialRatio || saved.horizon != horizon || saved.clock < 0 || !saved.scale.isFinite() || saved.scale < 0 || saved.best?.isFinite() == false) {
            searchInvalid("invalid record-to-record state")
        } else {
            clock = saved.clock
            best = saved.best
            scale = saved.scale
            ok
        }
    }
}
