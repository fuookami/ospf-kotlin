package fuookami.ospf.kotlin.core.solver.heuristic

import kotlin.math.pow
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.core.model.basic.Solution
import fuookami.ospf.kotlin.core.model.callback.AbstractCallBackModelInterface
import fuookami.ospf.kotlin.core.solver.heuristic.operator.compareStrictly

/** 原生搜索约束模式 / Native search constraint mode. */
@Serializable
enum class ConstraintMode { FeasibilityFirst, Epsilon, StochasticRanking }

/** 统一约束处理，不覆盖原始业务目标 / Unified constraint handling without overwriting business objectives.
 * @property mode 处理模式 / Handling mode
 * @property initialEpsilon 初始容差；null 从初始化候选校准 / Initial tolerance; null calibrates from initialization
 * @property horizon epsilon 归零的完整步数 / Complete steps until epsilon reaches zero
 * @property exponent 收紧曲线指数 / Tightening exponent
 * @property objectiveProbability 随机排序按原目标比较的概率 / Probability of objective comparison in stochastic ranking
 */
@Serializable
data class ConstraintHandlingConfig(
    val mode: ConstraintMode = ConstraintMode.FeasibilityFirst,
    val initialEpsilon: Double? = null,
    val horizon: Long = 1000,
    val exponent: Double = 2.0,
    val objectiveProbability: Double = 0.45
) {
    /** 校验配置 / Validate configuration.
     * @return 校验结果 / Validation result
     */
    fun validate(): Try {
        return if (initialEpsilon?.let { !it.isFinite() || it < 0 } == true || horizon <= 0 ||
            !exponent.isFinite() || exponent <= 0 || !objectiveProbability.isFinite() || objectiveProbability !in 0.0..1.0) {
            searchInvalid("约束处理配置无效 / Invalid constraint handling configuration")
        } else ok
    }
}

@Serializable
internal data class ConstraintSearchState(
    val version: Int = 1,
    val config: ConstraintHandlingConfig,
    val steps: Long,
    val scale: Double?,
    val random: Long
)

private data class ConstraintReading(val violation: Double, val feasible: Boolean)

/** 模型级类型化排序桥 / Model-level typed ordering bridge. */
internal interface ConstraintSearchModel<O, V> where V : RealNumber<V>, V : NumberField<V> {
    val constraintHandling: SearchConstraintHandling<O, V>?
}

internal class SearchConstraintHandling<O, V>(
    val config: ConstraintHandlingConfig,
    private val problem: SearchProblem<O, V>,
    private val random: SearchRandom
) where V : RealNumber<V>, V : NumberField<V> {
    private var steps = 0L
    private var scale = config.initialEpsilon
    private var pendingMaximum = 0.0
    private val readings = hashMapOf<Solution<V>, ConstraintReading>()
    var failure: String? = null
        private set

    val epsilon: Double get() = (scale ?: 0.0) * (1.0 - steps.toDouble() / config.horizon).coerceIn(0.0, 1.0).pow(config.exponent)

    fun save(): ConstraintSearchState = ConstraintSearchState(config = config, steps = steps, scale = scale, random = random.state)

    fun load(state: ConstraintSearchState): Try {
        if (state.version != 1 || state.config != config || state.steps < 0 || state.scale?.let { !it.isFinite() || it < 0 } == true ||
            (config.initialEpsilon != null && state.scale != config.initialEpsilon) || (state.steps > 0 && state.scale == null)) {
            return searchInvalid("约束处理状态不兼容 / Incompatible constraint handling state")
        }
        steps = state.steps
        scale = state.scale
        random.state = state.random
        begin()
        return ok
    }

    fun begin() {
        readings.clear()
        pendingMaximum = 0.0
        failure = null
    }

    fun commit() {
        if (scale == null) scale = pendingMaximum
        ++steps
        readings.clear()
    }

    fun observe(solution: Solution<V>) {
        reading(solution)
    }

    private fun reading(solution: Solution<V>): ConstraintReading? {
        readings[solution]?.let { return it }
        if (failure != null) return null
        val result = searchBoundary {
            val callback = problem.constraintViolation ?: return@searchBoundary searchInvalid<Flt64>("缺少违约度回调 / Missing violation callback")
            callback(solution.toList())
        }.then { it }
        if (result !is Ok) {
            failure = "违约度回调失败 / Violation callback failed: $result"
            return null
        }
        val violation = result.value.toDouble()
        val feasible = searchBoundary { problem.feasible(solution) }
        if (!violation.isFinite() || violation < 0 || feasible !is Ok) {
            failure = "违约度必须有限且非负 / Violation must be finite and nonnegative: $violation; $feasible"
            return null
        }
        pendingMaximum = maxOf(pendingMaximum, violation)
        val value = ConstraintReading(violation, feasible.value)
        readings[solution.toList()] = value
        return value
    }

    private fun objective(lhs: Individual<O, V>, rhs: Individual<O, V>): Int {
        return compareStrictly(problem.model, lhs.fitness, rhs.fitness)
    }

    private fun tier(reading: ConstraintReading, relaxed: Boolean): Pair<Int, Double> {
        return if (reading.feasible || relaxed && epsilon > 0 && reading.violation <= epsilon) 0 to 0.0 else 1 to reading.violation
    }

    fun compareTier(lhs: Individual<O, V>, rhs: Individual<O, V>): Int {
        val left = reading(lhs.solution) ?: return 0
        val right = reading(rhs.solution) ?: return 0
        val relaxed = config.mode == ConstraintMode.Epsilon
        val a = tier(left, relaxed)
        val b = tier(right, relaxed)
        return if (a.first != b.first) a.first.compareTo(b.first) else a.second.compareTo(b.second)
    }

    fun compare(lhs: Individual<O, V>, rhs: Individual<O, V>): Int {
        val compared = compareTier(lhs, rhs)
        return if (compared == 0) objective(lhs, rhs) else compared
    }

    fun <T : Individual<O, V>> groups(entries: List<T>): List<List<T>> {
        val groups = entries.groupBy { individual ->
            reading(individual.solution)?.let { tier(it, config.mode == ConstraintMode.Epsilon) } ?: (1 to Double.POSITIVE_INFINITY)
        }
        return groups.keys.sortedWith(compareBy<Pair<Int, Double>> { it.first }.thenBy { it.second }).map { groups.getValue(it) }
    }

    fun <T : Individual<O, V>> rank(entries: List<T>): List<T> {
        if (config.mode != ConstraintMode.StochasticRanking) return entries.sortedWith(::compare)
        val ranked = entries.toMutableList()
        repeat(ranked.size) {
            var swapped = false
            for (index in 0 until ranked.size - 1) {
                val a = ranked[index]
                val b = ranked[index + 1]
                val left = reading(a.solution) ?: return entries
                val right = reading(b.solution) ?: return entries
                val byObjective = left.feasible && right.feasible || random().toDouble() < config.objectiveProbability
                val compared = if (byObjective) objective(a, b) else compare(a, b)
                if (compared > 0) {
                    ranked[index] = b
                    ranked[index + 1] = a
                    swapped = true
                }
            }
            if (!swapped) return ranked
        }
        return ranked
    }
}

@Suppress("UNCHECKED_CAST")
internal fun <O, V> constraintHandling(model: AbstractCallBackModelInterface<*, O, V>): SearchConstraintHandling<O, V>?
    where V : RealNumber<V>, V : NumberField<V> = (model as? ConstraintSearchModel<O, V>)?.constraintHandling

/** 比较完整个体，保留原目标及解相关约束信息 / Compare complete individuals, retaining original objectives and solution-dependent constraints.
 * @param model 当前评估模型 / Current evaluation model
 * @param lhs 左个体 / Left individual
 * @param rhs 右个体 / Right individual
 * @return 负值表示左方优先 / Negative means the left individual is preferred
 */
fun <O, V> compareIndividuals(
    model: AbstractCallBackModelInterface<*, O, V>,
    lhs: Individual<O, V>,
    rhs: Individual<O, V>
): Int where V : RealNumber<V>, V : NumberField<V> {
    return constraintHandling(model)?.compare(lhs, rhs) ?: compareStrictly(model, lhs.fitness, rhs.fitness)
}

/** 批量排序，随机排序绝不作为 Comparator / Batch ordering; stochastic ranking is never a Comparator.
 * @param model 当前评估模型 / Current evaluation model
 * @param entries 完整个体 / Complete individuals
 * @return 排序结果 / Ranked individuals
 */
fun <O, V, T : Individual<O, V>> rankIndividuals(
    model: AbstractCallBackModelInterface<*, O, V>,
    entries: List<T>
): List<T> where V : RealNumber<V>, V : NumberField<V> {
    return constraintHandling(model)?.rank(entries) ?: entries.sortedWith { a, b -> compareStrictly(model, a.fitness, b.fitness) }
}

internal fun <O, V, T : Individual<O, V>> constraintSurvivors(
    model: AbstractCallBackModelInterface<*, O, V>,
    entries: List<T>,
    amount: Int,
    select: (List<T>, Int) -> List<T>
): List<T> where V : RealNumber<V>, V : NumberField<V> {
    val handling = constraintHandling(model) ?: return select(entries, amount)
    if (handling.config.mode == ConstraintMode.StochasticRanking) return handling.rank(entries).take(amount)
    val kept = ArrayList<T>()
    for (group in handling.groups(entries)) {
        kept.addAll(select(group, (amount - kept.size).coerceAtLeast(0)))
        if (kept.size >= amount) break
    }
    return kept
}

private class ConstraintModelView<T, O, V>(
    private val source: AbstractCallBackModelInterface<T, O, V>,
    override val constraintHandling: SearchConstraintHandling<O, V>
) : AbstractCallBackModelInterface<T, O, V> by source, ConstraintSearchModel<O, V>
    where V : RealNumber<V>, V : NumberField<V>

/** 为独占问题装配原生约束处理，业务只需提供违约度回调 / Attach native constraint handling to an owned problem; only a domain violation callback is needed.
 * @param config 约束处理配置 / Constraint handling configuration
 * @param random 独立且可恢复的排序随机流 / Independent restorable ranking stream
 * @return 保留原目标语义的私有搜索视图 / Private search view retaining original objective semantics
 */
fun <O, V> SearchProblem<O, V>.withConstraints(
    config: ConstraintHandlingConfig,
    random: SearchRandom
): Ret<SearchProblem<O, V>> where V : RealNumber<V>, V : NumberField<V> {
    val valid = config.validate()
    if (valid !is Ok) return searchFailure(valid)
    if (constraintViolation == null || constraintHandling(model) != null) return searchInvalid("约束回调缺失或重复装配 / Missing constraint callback or duplicate attachment")
    val handling = SearchConstraintHandling(config = config, problem = this, random = random)
    return Ok(SearchProblem(
        id = id,
        fingerprint = fingerprint,
        model = ConstraintModelView(model, handling),
        objectiveCodec = objectiveCodec,
        valueCodec = valueCodec,
        dominates = dominates,
        equivalent = equivalent,
        multiObjective = multiObjective,
        valid = this.valid,
        feasible = feasible,
        state = state,
        encoding = encoding,
        validSolution = validSolution,
        objectiveCoordinates = objectiveCoordinates,
        admissible = if (admissible === feasible) { _ -> true } else admissible,
        constraintViolation = constraintViolation
    ))
}
