/**
 * 伽马分布 / Gamma distribution
*/
package fuookami.ospf.kotlin.math.distribution

import kotlin.math.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64

/**
 * 伽马分布 / Gamma distribution
 *
 * 使用 Marsaglia–Tsang 方法采样，形状参数小于 1 时通过 Gamma(a + 1) * U^(1 / a) 变换。 / Sampled with the Marsaglia–Tsang method; when the shape parameter is below 1 it is handled by the Gamma(a + 1) * U^(1 / a) transform.
 *
 * @property shape 形状参数 k / Shape parameter k
 * @property scale 尺度参数 θ / Scale parameter θ
*/
data class Gamma(
    val shape: Flt64 = Flt64.one,
    val scale: Flt64 = Flt64.one
) : Distribution {
    override fun validate(): Ret<Unit> {
        return when {
            shape.value <= 0.0 -> Failed(ErrorCode.IllegalArgument, "shape must be positive / 形状参数必须为正")
            scale.value <= 0.0 -> Failed(ErrorCode.IllegalArgument, "scale must be positive / 尺度参数必须为正")
            else -> ok(Unit)
        }
    }

    override fun sample(entropy: Generator<Flt64>): Flt64 {
        val k = shape.value
        if (k < 1.0) {
            val u = max(uniformUnit(entropy), UNIT_EPSILON)
            val base = Gamma(Flt64(k + 1.0), scale).sample(entropy).value
            return Flt64(base * u.pow(1.0 / k))
        }

        val d = k - 1.0 / 3.0
        val c = 1.0 / sqrt(9.0 * d)
        while (true) {
            val x = standardGaussian(entropy)
            val v = 1.0 + c * x
            if (v <= 0.0) {
                continue
            }
            val v3 = v * v * v
            val u = uniformUnit(entropy)
            if (u <= 0.0) {
                continue
            }
            if (ln(u) < 0.5 * x * x + d - d * v3 + d * ln(v3)) {
                return Flt64(d * v3 * scale.value)
            }
        }
    }
}
