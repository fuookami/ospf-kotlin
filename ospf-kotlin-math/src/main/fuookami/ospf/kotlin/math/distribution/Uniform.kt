/**
 * 均匀分布 / Uniform distribution
*/
package fuookami.ospf.kotlin.math.distribution

import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64

/**
 * 连续均匀分布 / Continuous uniform distribution
 *
 * 在 [lowerBound, upperBound] 区间内均匀采样。 / Samples uniformly within [lowerBound, upperBound].
 *
 * @property lowerBound 下界 / Lower bound
 * @property upperBound 上界 / Upper bound
*/
data class Uniform(
    val lowerBound: Flt64 = Flt64.zero,
    val upperBound: Flt64 = Flt64.one
) : Distribution {
    override fun validate(): Ret<Unit> {
        return if (lowerBound.value <= upperBound.value) {
            ok(Unit)
        } else {
            Failed(ErrorCode.IllegalArgument, "lowerBound must not exceed upperBound / 下界不得大于上界")
        }
    }

    override fun sample(entropy: Generator<Flt64>): Flt64 {
        val width = upperBound.value - lowerBound.value
        return Flt64(lowerBound.value + uniformUnit(entropy) * width)
    }
}
