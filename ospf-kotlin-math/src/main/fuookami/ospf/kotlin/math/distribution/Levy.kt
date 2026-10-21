/**
 * 莱维稳定分布 / Lévy stable distribution
*/
package fuookami.ospf.kotlin.math.distribution

import kotlin.math.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64

/**
 * 对称莱维稳定分布 / Symmetric Lévy stable distribution
 *
 * 使用 Chambers–Mallows–Stuck 方法采样。α = 1.5 时即为莱维飞行的经典步长分布；
 * α 趋于 2 时退化为高斯分布。 / Sampled with the Chambers–Mallows–Stuck method. α = 1.5 yields the classic Lévy flight step distribution,
 * while α approaching 2 degenerates to a Gaussian distribution.
 *
 * @property alpha 稳定性参数 α ∈ (0, 2] / Stability parameter α ∈ (0, 2]
*/
data class Levy(
    val alpha: Flt64 = Flt64(1.5)
) : Distribution {
    override fun validate(): Ret<Unit> {
        return if (alpha.value > 0.0 && alpha.value <= 2.0) {
            ok(Unit)
        } else {
            Failed(ErrorCode.IllegalArgument, "alpha must be within (0, 2] / α 必须位于 (0, 2]")
        }
    }

    override fun sample(entropy: Generator<Flt64>): Flt64 {
        val a = alpha.value
        if (a >= 2.0) {
            return Flt64(standardGaussian(entropy) * sqrt(2.0))
        }

        val u = uniformUnit(entropy).coerceIn(UNIT_EPSILON, 1.0 - UNIT_EPSILON)
        val v = (u - 0.5) * PI
        if (a == 1.0) {
            return Flt64(tan(v))
        }

        val w = -ln(max(uniformUnit(entropy), UNIT_EPSILON))
        val first = sin(a * v) / cos(v).pow(1.0 / a)
        val second = (cos(v - a * v) / w).pow((1.0 - a) / a)
        return Flt64(first * second)
    }
}
