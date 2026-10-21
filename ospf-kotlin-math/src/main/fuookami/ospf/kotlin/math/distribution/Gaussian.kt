/**
 * 高斯（正态）分布 / Gaussian (normal) distribution
*/
package fuookami.ospf.kotlin.math.distribution

import kotlin.math.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64

/**
 * 标准正态采样，使用 Box–Muller 变换 / Standard normal sampling using the Box–Muller transform
 *
 * @param entropy 均匀分布熵源 / Uniform entropy source
 * @return 标准正态采样值 / Standard normal sample
*/
internal fun standardGaussian(entropy: Generator<Flt64>): Double {
    val u1 = max(uniformUnit(entropy), UNIT_EPSILON)
    val u2 = uniformUnit(entropy)
    return sqrt(-2.0 * ln(u1)) * cos(2.0 * PI * u2)
}

/**
 * 高斯分布 / Gaussian distribution
 *
 * @property mean 均值 / Mean
 * @property standardDeviation 标准差 / Standard deviation
*/
data class Gaussian(
    val mean: Flt64 = Flt64.zero,
    val standardDeviation: Flt64 = Flt64.one
) : Distribution {
    override fun validate(): Ret<Unit> {
        return if (standardDeviation.value >= 0.0) {
            ok(Unit)
        } else {
            Failed(ErrorCode.IllegalArgument, "standardDeviation must be non-negative / 标准差不得为负")
        }
    }

    override fun sample(entropy: Generator<Flt64>): Flt64 {
        return Flt64(mean.value + standardDeviation.value * standardGaussian(entropy))
    }
}
