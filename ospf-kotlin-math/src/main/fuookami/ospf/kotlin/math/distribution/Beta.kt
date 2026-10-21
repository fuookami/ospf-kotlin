/**
 * 贝塔分布 / Beta distribution
*/
package fuookami.ospf.kotlin.math.distribution

import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64

/**
 * 贝塔分布 / Beta distribution
 *
 * 由两个伽马分布之比得到，取值位于 (0, 1) 区间。 / Obtained from the ratio of two gamma distributions, taking values within (0, 1).
 *
 * @property alpha 形状参数 α / Shape parameter α
 * @property beta 形状参数 β / Shape parameter β
*/
data class Beta(
    val alpha: Flt64 = Flt64.one,
    val beta: Flt64 = Flt64.one
) : Distribution {
    override fun validate(): Ret<Unit> {
        return when {
            alpha.value <= 0.0 -> Failed(ErrorCode.IllegalArgument, "alpha must be positive / α 必须为正")
            beta.value <= 0.0 -> Failed(ErrorCode.IllegalArgument, "beta must be positive / β 必须为正")
            else -> ok(Unit)
        }
    }

    override fun sample(entropy: Generator<Flt64>): Flt64 {
        val x = Gamma(alpha).sample(entropy).value
        val y = Gamma(beta).sample(entropy).value
        val sum = x + y
        return if (sum <= 0.0) {
            Flt64(0.5)
        } else {
            Flt64(x / sum)
        }
    }
}
