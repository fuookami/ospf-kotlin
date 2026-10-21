/**
 * 参数演化算子 / Parameter evolution operators
 *
 * 覆盖「参数随迭代变化」的全部形态：确定性调度、混沌序列、分形投影、分布采样与组合子。 / Covers every shape of "parameter varies with iteration": deterministic schedules, chaotic sequences, fractal projections, distribution sampling, and combinators.
*/
package fuookami.ospf.kotlin.core.solver.heuristic.operator.evolution

import kotlin.math.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.distribution.*
import fuookami.ospf.kotlin.math.geometry.*
import fuookami.ospf.kotlin.math.sequence.*
import fuookami.ospf.kotlin.core.solver.heuristic.Iteration

/**
 * 参数演化源接口，把「参数取值」抽象为一个纯函数。 / Parameter evolution source interface abstracting parameter values as a pure function.
*/
fun interface ParameterSource {

    /**
     * 计算当前迭代的参数值 / Compute the parameter value at the current iteration
     *
     * @param iteration 当前迭代 / Current iteration
     * @return 参数值 / Parameter value
    */
    operator fun invoke(iteration: Iteration): Flt64
}

/**
 * 迭代进度，取值位于 [0, 1] / Iteration progress within [0, 1]
 *
 * @param limit 迭代上限 / Iteration limit
 * @return 进度 / Progress
*/
fun Iteration.progress(limit: UInt64): Flt64 {
    if (limit == UInt64.zero) {
        return Flt64.one
    }
    val ratio = iteration.toFlt64().toDouble() / limit.toFlt64().toDouble()
    return Flt64(ratio.coerceIn(0.0, 1.0))
}

/**
 * 常数参数 / Constant parameter
 *
 * @property value 参数值 / Parameter value
*/
data class ConstantSource(
    val value: Flt64
) : ParameterSource {
    override fun invoke(iteration: Iteration): Flt64 = value
}

/**
 * 线性调度，从 [from] 线性过渡到 [to] / Linear schedule transitioning from [from] to [to]
 *
 * @property from 起始值 / Start value
 * @property to 结束值 / End value
 * @property limit 迭代上限 / Iteration limit
*/
data class LinearSource(
    val from: Flt64,
    val to: Flt64,
    val limit: UInt64
) : ParameterSource {
    override fun invoke(iteration: Iteration): Flt64 {
        val progress = iteration.progress(limit).toDouble()
        return Flt64(from.toDouble() + (to.toDouble() - from.toDouble()) * progress)
    }
}

/**
 * 几何衰减调度（等比数列）/ Geometric decay schedule
 *
 * @property initial 初始值 / Initial value
 * @property ratio 公比 / Common ratio
*/
data class GeometricSource(
    val initial: Flt64,
    val ratio: Flt64
) : ParameterSource {
    override fun invoke(iteration: Iteration): Flt64 {
        val exponent = iteration.iteration.toFlt64().toDouble()
        return Flt64(initial.toDouble() * ratio.toDouble().pow(exponent))
    }
}

/**
 * 指数调度，按时间常数衰减 / Exponential schedule decaying with a time constant
 *
 * @property initial 初始值 / Initial value
 * @property rate 衰减率 / Decay rate
*/
data class ExponentialSource(
    val initial: Flt64,
    val rate: Flt64
) : ParameterSource {
    override fun invoke(iteration: Iteration): Flt64 {
        val exponent = -rate.toDouble() * iteration.iteration.toFlt64().toDouble()
        return Flt64(initial.toDouble() * exp(exponent))
    }
}

/**
 * 混沌参数源，把一维混沌序列线性映射到目标区间 / Chaotic parameter source mapping a 1D chaotic sequence onto the target interval
 *
 * @property generator 混沌序列生成器 / Chaotic sequence generator
 * @property domainLowerBound 混沌序列下界 / Chaotic sequence lower bound
 * @property domainUpperBound 混沌序列上界 / Chaotic sequence upper bound
 * @property lowerBound 目标下界 / Target lower bound
 * @property upperBound 目标上界 / Target upper bound
*/
class ChaoticSource(
    val generator: Generator<Flt64>,
    val domainLowerBound: Flt64 = Flt64.zero,
    val domainUpperBound: Flt64 = Flt64.one,
    val lowerBound: Flt64 = Flt64.zero,
    val upperBound: Flt64 = Flt64.one
) : ParameterSource {
    override fun invoke(iteration: Iteration): Flt64 {
        return Flt64(
            normalize(
                raw = generator()?.toDouble() ?: domainLowerBound.toDouble(),
                domainLower = domainLowerBound.toDouble(),
                domainUpper = domainUpperBound.toDouble(),
                lower = lowerBound.toDouble(),
                upper = upperBound.toDouble()
            )
        )
    }
}

/**
 * 分形参数源，把多维分形序列按 [ChannelProjection] 投影后映射到目标区间 / Fractal parameter source projecting a multi-dimensional fractal sequence through [ChannelProjection] onto the target interval
 *
 * @param D 维度类型 / Dimension type
 * @property generator 分形序列生成器 / Fractal sequence generator
 * @property projection 通道投影 / Channel projection
 * @property domainLowerBound 投影值下界 / Projection value lower bound
 * @property domainUpperBound 投影值上界 / Projection value upper bound
 * @property lowerBound 目标下界 / Target lower bound
 * @property upperBound 目标上界 / Target upper bound
*/
class FractalSource<D : Dimension>(
    val generator: Generator<Point<D, Flt64>>,
    val projection: ChannelProjection = ChannelProjection.Magnitude,
    val domainLowerBound: Flt64 = Flt64.zero,
    val domainUpperBound: Flt64 = Flt64.one,
    val lowerBound: Flt64 = Flt64.zero,
    val upperBound: Flt64 = Flt64.one
) : ParameterSource {
    override fun invoke(iteration: Iteration): Flt64 {
        val point = generator() ?: return lowerBound
        val raw = when (val projected = projection.project(point)) {
            is Ok -> projected.value.toDouble()
            else -> domainLowerBound.toDouble()
        }
        return Flt64(
            normalize(
                raw = raw,
                domainLower = domainLowerBound.toDouble(),
                domainUpper = domainUpperBound.toDouble(),
                lower = lowerBound.toDouble(),
                upper = upperBound.toDouble()
            )
        )
    }
}

/**
 * 分布参数源，按概率分布采样并可裁剪到区间 / Distribution parameter source sampling from a probability distribution with optional interval clamping
 *
 * @property distribution 概率分布 / Probability distribution
 * @property entropy 均匀分布熵源 / Uniform entropy source
 * @property lowerBound 下界（可选）/ Lower bound (optional)
 * @property upperBound 上界（可选）/ Upper bound (optional)
*/
class DistributionSource(
    val distribution: Distribution,
    private val entropy: Generator<Flt64>,
    val lowerBound: Flt64? = null,
    val upperBound: Flt64? = null
) : ParameterSource {
    override fun invoke(iteration: Iteration): Flt64 {
        val sampled = when (val result = distribution(entropy)) {
            is Ok -> result.value
            else -> lowerBound ?: Flt64.zero
        }
        var value = sampled.toDouble()
        lowerBound?.let { value = max(value, it.toDouble()) }
        upperBound?.let { value = min(value, it.toDouble()) }
        return Flt64(value)
    }
}

/**
 * 抖动参数源，在基础参数上叠加对称随机抖动 / Jittered parameter source adding symmetric random jitter on top of a base parameter
 *
 * @property base 基础参数源 / Base parameter source
 * @property entropy 均匀分布熵源 / Uniform entropy source
 * @property amplitude 抖动幅度 / Jitter amplitude
*/
class JitteredSource(
    val base: ParameterSource,
    private val entropy: Generator<Flt64>,
    val amplitude: Flt64 = Flt64(0.05)
) : ParameterSource {
    override fun invoke(iteration: Iteration): Flt64 {
        val unit = (entropy()?.toDouble() ?: 0.5).coerceIn(0.0, 1.0)
        return Flt64(base(iteration).toDouble() + amplitude.toDouble() * (2.0 * unit - 1.0))
    }
}

/**
 * 把原始值从定义域线性映射到目标区间 / Linearly map a raw value from its domain onto the target interval
 *
 * @param raw 原始值 / Raw value
 * @param domainLower 定义域下界 / Domain lower bound
 * @param domainUpper 定义域上界 / Domain upper bound
 * @param lower 目标下界 / Target lower bound
 * @param upper 目标上界 / Target upper bound
 * @return 映射后的值 / Mapped value
*/
private fun normalize(
    raw: Double,
    domainLower: Double,
    domainUpper: Double,
    lower: Double,
    upper: Double
): Double {
    val span = domainUpper - domainLower
    val unit = if (span <= 0.0) 0.5 else ((raw - domainLower) / span).coerceIn(0.0, 1.0)
    return lower + unit * (upper - lower)
}
