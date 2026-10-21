/**
 * 柯西分布 / Cauchy distribution
*/
package fuookami.ospf.kotlin.math.distribution

import kotlin.math.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64

/**
 * 柯西分布 / Cauchy distribution
 *
 * 重尾分布，常用于柯西变异以增强跳出局部最优的能力。 / A heavy-tailed distribution commonly used by Cauchy mutation to escape local optima.
 *
 * @property location 位置参数 / Location parameter
 * @property scale 尺度参数 / Scale parameter
*/
data class Cauchy(
    val location: Flt64 = Flt64.zero,
    val scale: Flt64 = Flt64.one
) : Distribution {
    override fun validate(): Ret<Unit> {
        return if (scale.value > 0.0) {
            ok(Unit)
        } else {
            Failed(ErrorCode.IllegalArgument, "scale must be positive / 尺度参数必须为正")
        }
    }

    override fun sample(entropy: Generator<Flt64>): Flt64 {
        val u = uniformUnit(entropy)
        return Flt64(location.value + scale.value * tan(PI * (u - 0.5)))
    }
}
