/**
 * 指数分布 / Exponential distribution
*/
package fuookami.ospf.kotlin.math.distribution

import kotlin.math.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64

/**
 * 指数分布 / Exponential distribution
 *
 * @property rate 速率参数 λ / Rate parameter λ
*/
data class Exponential(
    val rate: Flt64 = Flt64.one
) : Distribution {
    override fun validate(): Ret<Unit> {
        return if (rate.value > 0.0) {
            ok(Unit)
        } else {
            Failed(ErrorCode.IllegalArgument, "rate must be positive / 速率参数必须为正")
        }
    }

    override fun sample(entropy: Generator<Flt64>): Flt64 {
        val u = max(uniformUnit(entropy), UNIT_EPSILON)
        return Flt64(-ln(1.0 - u) / rate.value)
    }
}
