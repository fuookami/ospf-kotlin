/**
 * 随机过程算子 / Stochastic operators
*/
package fuookami.ospf.kotlin.core.solver.heuristic.operator.stochastic

import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.distribution.*

/**
 * 高维莱维飞行，把重尾幅值与单位方向组合为完整步长。 / High-dimensional Lévy flight combining a heavy-tailed magnitude with a unit direction into a complete step.
 *
 * @property dimension 维度 / Dimensionality
 * @property alpha 稳定性参数 / Stability parameter
 * @property scale 幅值缩放 / Magnitude scaling
*/
class LevyFlight(
    val dimension: Int,
    val alpha: Flt64 = Flt64(1.5),
    val scale: Flt64 = Flt64.one
) {
    /**
     * 采样一步莱维飞行 / Sample one Lévy flight step
     *
     * @param entropy 均匀分布熵源 / Uniform entropy source
     * @return 步长向量 / Step vector
    */
    operator fun invoke(entropy: Generator<Flt64>): Ret<List<Flt64>> {
        val magnitude = when (val sampled = Levy(alpha)(entropy)) {
            is Ok -> sampled.value.toDouble() * scale.toDouble()
            is Failed -> return failed(sampled.error)
            is Fatal -> return Fatal(sampled.errors)
        }
        val direction = when (val sampled = DirectionSampler(dimension)(entropy)) {
            is Ok -> sampled.value
            is Failed -> return failed(sampled.error)
            is Fatal -> return Fatal(sampled.errors)
        }
        return ok(direction.map { component -> Flt64(component.toDouble() * magnitude) })
    }
}

/**
 * 逐维噪声步长，用于变异与位置更新 / Per-dimension noise step used by mutation and position updates
 *
 * @property dimension 维度 / Dimensionality
 * @property distribution 噪声分布 / Noise distribution
*/
class NoiseStep(
    val dimension: Int,
    val distribution: Distribution = Gaussian()
) {
    /**
     * 采样一步噪声 / Sample one noise step
     *
     * @param entropy 均匀分布熵源 / Uniform entropy source
     * @return 步长向量 / Step vector
    */
    operator fun invoke(entropy: Generator<Flt64>): Ret<List<Flt64>> {
        val samples = ArrayList<Flt64>(dimension)
        repeat(dimension) {
            when (val sampled = distribution(entropy)) {
                is Ok -> samples.add(sampled.value)
                is Failed -> return failed(sampled.error)
                is Fatal -> return Fatal(sampled.errors)
            }
        }
        return ok(samples)
    }
}
