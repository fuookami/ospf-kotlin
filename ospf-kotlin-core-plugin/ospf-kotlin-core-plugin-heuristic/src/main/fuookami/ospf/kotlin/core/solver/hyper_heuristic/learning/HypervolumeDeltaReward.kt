package fuookami.ospf.kotlin.core.solver.hyper_heuristic.learning

import java.lang.StrictMath.log as ln
import java.lang.StrictMath.sqrt
import kotlin.math.*
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.core.solver.heuristic.*
import fuookami.ospf.kotlin.core.solver.hyper_heuristic.selection.RecoverableComponent

/** 超体积计算模式，不自动降级 / Hypervolume mode without implicit fallback. */
@Serializable
enum class HypervolumeMode { Exact, Sampled }

/** 一次超体积增量及采样误差 / Hypervolume increment and sampling uncertainty.
 * @property delta 非负增量 / Nonnegative increment
 * @property exact 是否精确 / Whether exact
 * @property samples 实际样本数 / Actual samples
 * @property standardError 采样标准误差 / Sampling standard error
 * @property errorBound 99% Hoeffding 绝对误差界 / 99% Hoeffding absolute error bound
 * @property work 已用几何计算额度 / Geometric work consumed
 * @property calibrated 是否已冻结参考点 / Whether the reference is frozen
 */
@Serializable
data class HypervolumeMeasurement(
    val delta: Double,
    val exact: Boolean,
    val samples: Int = 0,
    val standardError: Double = 0.0,
    val errorBound: Double = 0.0,
    val work: Long = 0,
    val calibrated: Boolean = true
)

@Serializable
private data class HypervolumeState(
    val version: Int = 1,
    val definition: String,
    val reference: List<Double>,
    val minimum: List<Double>,
    val maximum: List<Double>,
    val observations: Int,
    val measurement: HypervolumeMeasurement?
)

/** 多维超体积信用，精确切片或显式可复现采样 / Multidimensional HV credit using exact slicing or explicit reproducible sampling.
 * @property referencePoint 固定劣参考点；空列表启用校准后冻结 / Fixed worse reference; empty calibrates and freezes
 * @property mode 精确或采样模式 / Exact or sampled mode
 * @property maxWork 每次计算的几何操作上限 / Geometric operation limit per calculation
 * @property samples 采样模式样本数 / Sample count in sampled mode
 * @property seed 可复现的公共随机数种子 / Reproducible common-random-number seed
 * @property calibrationObservations 自动参考点冻结前的有效观测数 / Valid observations before freezing an automatic reference
 */
class HypervolumeDeltaReward(
    val referencePoint: List<Flt64> = emptyList(),
    val mode: HypervolumeMode = HypervolumeMode.Exact,
    val maxWork: Long = 1_000_000,
    val samples: Int = 16384,
    val seed: Long = 1,
    val calibrationObservations: Int = 32
) : RewardPolicy, RecoverableComponent {
    override val requiresCoordinates = true
    private var reference = referencePoint.map { it.toDouble() }
    private var minimum = emptyList<Double>()
    private var maximum = emptyList<Double>()
    private var observations = 0

    /** 最近一次计算及误差指标，检查点保留 / Last calculation and uncertainty, retained by checkpoints. */
    var lastMeasurement: HypervolumeMeasurement? = null
        private set

    private val definition: String get() = listOf(
        referencePoint.joinToString { it.toDouble().toString() }, mode.name, maxWork, samples, seed, calibrationObservations
    ).joinToString("|")

    /** 验证资源额度与参考点 / Validate resource limits and reference.
     * @return 校验结果 / Validation result
     */
    fun validate(): Try {
        return if (maxWork <= 0 || samples <= 0 || calibrationObservations <= 0 ||
            referencePoint.any { !it.toDouble().isFinite() }) {
            searchInvalid("超体积参数无效 / Invalid hypervolume parameters")
        } else ok
    }

    override fun reward(context: RewardContext): Flt64 {
        return when (val result = evaluate(context)) {
            is Ok -> result.value
            else -> Flt64(Double.NaN)
        }
    }

    override fun evaluate(context: RewardContext): Ret<Flt64> = measure(context).map { Flt64(it.delta) }

    /** 计算增量而不伪造不可用的精确值 / Compute the increment without fabricating unavailable exact results.
     * @param context 归一化最小化坐标 / Normalized minimization coordinates
     * @return 增量、误差或资源不足错误 / Increment, uncertainty or resource-limit failure
     */
    fun measure(context: RewardContext): Ret<HypervolumeMeasurement> {
        val valid = validate()
        if (valid !is Ok) return searchFailure(valid)
        val points = (context.reference + context.candidates).map { point -> point.map { it.toDouble() } }
        val dimension = reference.size.takeIf { it > 0 } ?: minimum.size.takeIf { it > 0 } ?: points.firstOrNull()?.size ?: 0
        if (points.any { it.size != dimension || it.isEmpty() || it.any { value -> !value.isFinite() } }) {
            return searchInvalid("超体积坐标维度或数值无效 / Invalid hypervolume coordinate dimensions or values")
        }
        if (reference.isEmpty()) {
            for (point in points) {
                minimum = if (minimum.isEmpty()) point else point.indices.map { min(minimum[it], point[it]) }
                maximum = if (maximum.isEmpty()) point else point.indices.map { max(maximum[it], point[it]) }
            }
            observations = (observations.toLong() + context.candidates.size).coerceAtMost(calibrationObservations.toLong()).toInt()
            if (observations < calibrationObservations || minimum.isEmpty()) {
                return complete(HypervolumeMeasurement(delta = 0.0, exact = mode == HypervolumeMode.Exact, calibrated = false))
            }
            reference = minimum.indices.map { axis ->
                val spread = maximum[axis] - minimum[axis]
                maximum[axis] + max(spread, max(abs(maximum[axis]), 1.0)) * 0.1
            }
            if (reference.any { !it.isFinite() } || reference.indices.any { reference[it] <= maximum[it] }) {
                reference = emptyList()
                return searchInvalid("自动参考点溢出，请配置显式参考点 / Automatic reference overflow; configure an explicit reference")
            }
            // 冻结的这一动作不跨参考定义计信用 / The freezing action is not credited across reference definitions.
            return complete(HypervolumeMeasurement(delta = 0.0, exact = mode == HypervolumeMode.Exact))
        }
        if (dimension == 0 || reference.any { !it.isFinite() }) return searchInvalid("超体积参考点无效 / Invalid hypervolume reference")
        val work = GeometryWork(maxWork)
        val old = context.reference.map { point -> point.map { it.toDouble() } }
        val fresh = context.candidates.map { point -> point.map { it.toDouble() } }
        return if (mode == HypervolumeMode.Exact) {
            if (dimension > 256) return searchInvalid("精确超体积递归深度超限，请显式选择采样 / Exact HV recursion depth exceeded; explicitly select sampling")
            exactVolume(old, reference, work).then { before ->
                exactVolume(old + fresh, reference, work).then { after ->
                    val delta = (after - before).coerceAtLeast(0.0)
                    if (!delta.isFinite()) searchInvalid("超体积溢出 / Hypervolume overflow")
                    else complete(HypervolumeMeasurement(delta = delta, exact = true, work = work.used))
                }
            }
        } else sampled(old, fresh, work).then { complete(it) }
    }

    private fun complete(value: HypervolumeMeasurement): Ret<HypervolumeMeasurement> {
        if (!value.delta.isFinite() || !value.standardError.isFinite() || !value.errorBound.isFinite()) {
            return searchInvalid("超体积或误差指标溢出 / Hypervolume or uncertainty overflow")
        }
        lastMeasurement = value
        return Ok(value)
    }

    private fun sampled(old: List<List<Double>>, fresh: List<List<Double>>, work: GeometryWork): Ret<HypervolumeMeasurement> {
        val points = (old + fresh).filter { point -> point.indices.all { point[it] < reference[it] } }
        if (points.isEmpty()) return Ok(HypervolumeMeasurement(delta = 0.0, exact = false))
        val lower = reference.indices.map { axis -> points.minOf { it[axis] } }
        val widths = reference.indices.map { reference[it] - lower[it] }
        val box = widths.fold(1.0) { value, width -> value * width }
        if (!box.isFinite()) return searchInvalid("采样积分区域溢出 / Sampling integration box overflow")
        val random = SearchRandom(seed)
        var hits = 0
        repeat(samples) {
            val sample = reference.indices.map { lower[it] + random().toDouble() * widths[it] }
            fun dominated(entries: List<List<Double>>): Boolean? {
                for (point in entries) {
                    if (!work.consume()) return null
                    if (point.indices.all { point[it] <= sample[it] }) return true
                }
                return false
            }
            val wasCovered = dominated(old) ?: return work.failure()
            val isNew = if (wasCovered) false else dominated(fresh) ?: return work.failure()
            if (isNew) ++hits
        }
        val probability = hits.toDouble() / samples
        return Ok(HypervolumeMeasurement(
            delta = box * probability,
            exact = false,
            samples = samples,
            standardError = box * sqrt(probability * (1 - probability) / samples),
            errorBound = box * sqrt(ln(200.0) / (2 * samples)),
            work = work.used
        ))
    }

    override fun save(): Ret<JsonElement> = Ok(Json.encodeToJsonElement(HypervolumeState(
        definition = definition,
        reference = reference,
        minimum = minimum,
        maximum = maximum,
        observations = observations,
        measurement = lastMeasurement
    )))

    override fun load(state: JsonElement): Try {
        return searchBoundary { Json.decodeFromJsonElement<HypervolumeState>(state) }.then { saved ->
            val dimension = saved.reference.size.takeIf { it > 0 } ?: saved.minimum.size
            if (saved.version != 1 || saved.definition != definition || saved.observations !in 0..calibrationObservations ||
                (saved.reference + saved.minimum + saved.maximum).any { !it.isFinite() } ||
                saved.minimum.size != saved.maximum.size || saved.minimum.indices.any { saved.minimum[it] > saved.maximum[it] } ||
                (saved.minimum.isNotEmpty() && saved.minimum.size != dimension) ||
                (referencePoint.isNotEmpty() && saved.reference != referencePoint.map { it.toDouble() }) ||
                (referencePoint.isNotEmpty() && (saved.minimum.isNotEmpty() || saved.observations != 0)) ||
                (referencePoint.isEmpty() && saved.reference.isNotEmpty() &&
                    (saved.observations != calibrationObservations || saved.minimum.isEmpty() ||
                        saved.reference.indices.any { saved.reference[it] <= saved.maximum[it] })) ||
                (saved.observations > 0 && saved.minimum.isEmpty()) ||
                saved.measurement?.let { it.delta < 0 || !it.delta.isFinite() || it.samples < 0 || it.work !in 0..maxWork ||
                    it.exact != (mode == HypervolumeMode.Exact) || it.calibrated != saved.reference.isNotEmpty() ||
                    it.samples !in setOf(0, if (mode == HypervolumeMode.Sampled) samples else 0) ||
                    !it.standardError.isFinite() || it.standardError < 0 || !it.errorBound.isFinite() || it.errorBound < 0 } == true) {
                return@then searchInvalid("超体积状态不兼容 / Incompatible hypervolume state")
            }
            reference = saved.reference
            minimum = saved.minimum
            maximum = saved.maximum
            observations = saved.observations
            lastMeasurement = saved.measurement
            ok
        }
    }
}

private class GeometryWork(val limit: Long) {
    var used = 0L
        private set

    fun consume(): Boolean {
        if (used >= limit) return false
        ++used
        return true
    }

    fun <T> failure(): Ret<T> = searchInvalid("超体积几何计算额度耗尽 / Hypervolume geometric work exhausted: $limit")
}

private fun exactVolume(points: List<List<Double>>, reference: List<Double>, work: GeometryWork): Ret<Double> {
    val eligible = ArrayList<List<Double>>()
    for (point in points.distinct()) {
        if (!work.consume()) return work.failure()
        if (point.indices.all { point[it] < reference[it] }) eligible.add(point)
    }
    if (eligible.isEmpty()) return Ok(0.0)
    val front = ArrayList<List<Double>>()
    for (point in eligible) {
        var dominated = false
        for (other in eligible) {
            if (!work.consume()) return work.failure()
            if (other != point && point.indices.all { other[it] <= point[it] }) {
                dominated = true
                break
            }
        }
        if (!dominated) front.add(point)
    }
    val dimension = reference.size
    if (front.size == 1) return finiteVolume(reference.indices.fold(1.0) { area, axis -> area * (reference[axis] - front[0][axis]) })
    if (dimension == 1) return finiteVolume(reference[0] - front.minOf { it[0] })
    if (dimension == 2) {
        var height = reference[1]
        var area = 0.0
        for (point in front.sortedBy { it[0] }) {
            if (!work.consume()) return work.failure()
            if (point[1] < height) {
                area += (reference[0] - point[0]) * (height - point[1])
                height = point[1]
            }
        }
        return finiteVolume(area)
    }
    val axis = dimension - 1
    val levels = front.map { it[axis] }.distinct().sorted()
    var volume = 0.0
    for ((index, level) in levels.withIndex()) {
        if (!work.consume()) return work.failure()
        val active = front.filter { it[axis] <= level }.map { it.dropLast(1) }
        val sliced = exactVolume(active, reference.dropLast(1), work)
        if (sliced !is Ok) return sliced
        volume += sliced.value * ((levels.getOrNull(index + 1) ?: reference[axis]) - level)
    }
    return finiteVolume(volume)
}

private fun finiteVolume(value: Double): Ret<Double> {
    return if (value.isFinite() && value >= 0) Ok(value) else searchInvalid("超体积溢出 / Hypervolume overflow")
}
