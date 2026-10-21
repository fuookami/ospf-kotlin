/**
 * 截断分布 / Truncated distribution
*/
package fuookami.ospf.kotlin.math.distribution

import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64

/**
 * 截断分布包装 / Truncated distribution wrapper
 *
 * 通过拒绝采样把任意分布限制在 [lowerBound, upperBound] 区间；
 * 超过最大尝试次数后退化为区间裁剪。 / Restricts an arbitrary distribution to [lowerBound, upperBound] via rejection sampling,
 * and falls back to interval clamping after the maximum number of attempts.
 *
 * @property distribution 被截断的分布 / The distribution being truncated
 * @property lowerBound 下界 / Lower bound
 * @property upperBound 上界 / Upper bound
 * @property maxAttempts 最大拒绝采样次数 / Maximum number of rejection attempts
*/
data class Truncated(
    val distribution: Distribution,
    val lowerBound: Flt64,
    val upperBound: Flt64,
    val maxAttempts: Int = 128
) : Distribution {
    override fun validate(): Ret<Unit> {
        return when {
            lowerBound.value > upperBound.value -> Failed(
                ErrorCode.IllegalArgument,
                "lowerBound must not exceed upperBound / 下界不得大于上界"
            )

            maxAttempts <= 0 -> Failed(
                ErrorCode.IllegalArgument,
                "maxAttempts must be positive / 最大尝试次数必须为正"
            )

            else -> distribution.validate()
        }
    }

    override fun sample(entropy: Generator<Flt64>): Flt64 {
        repeat(maxAttempts) {
            val value = distribution.sample(entropy)
            if (value.value >= lowerBound.value && value.value <= upperBound.value) {
                return value
            }
        }
        return Flt64(distribution.sample(entropy).value.coerceIn(lowerBound.value, upperBound.value))
    }
}
