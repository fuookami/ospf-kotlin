/**
 * 学生 t 分布 / Student's t distribution
*/
package fuookami.ospf.kotlin.math.distribution

import kotlin.math.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64

/**
 * 学生 t 分布 / Student's t distribution
 *
 * 自由度趋于无穷时退化为标准正态，自由度较小时具有更重的尾部。 / Degenerates to the standard normal as the degrees of freedom grow, and has heavier tails for small degrees of freedom.
 *
 * @property degreesOfFreedom 自由度 ν / Degrees of freedom ν
*/
data class StudentT(
    val degreesOfFreedom: Flt64 = Flt64.one
) : Distribution {
    override fun validate(): Ret<Unit> {
        return if (degreesOfFreedom.value > 0.0) {
            ok(Unit)
        } else {
            Failed(ErrorCode.IllegalArgument, "degreesOfFreedom must be positive / 自由度必须为正")
        }
    }

    override fun sample(entropy: Generator<Flt64>): Flt64 {
        val dof = degreesOfFreedom.value
        val z = standardGaussian(entropy)
        val chiSquared = Gamma(Flt64(dof / 2.0), Flt64(2.0)).sample(entropy).value
        return Flt64(z / sqrt(chiSquared / dof))
    }
}
