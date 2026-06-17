/**
 * 达德拉斯吸引子
 * Dadras Attractor
 */
package fuookami.ospf.kotlin.math.chaotic

import kotlin.random.Random
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.geometry.*
import fuookami.ospf.kotlin.math.nextFlt64

/**
 * 达德拉斯吸引子
 * Dadras Attractor
 *
 * @property gamma 系统参数 gamma / System parameter gamma
 * @property epsilon 系统参数 epsilon / System parameter epsilon
 * @property zeta 系统参数 zeta / System parameter zeta
 * @property rho 系统参数 rho / System parameter rho
 * @property sigma 系统参数 sigma / System parameter sigma
 * @property h 时间步长 / Time step size
 */
data class DadrasAttractor<V : FloatingNumber<V>>(
    val gamma: V, val epsilon: V, val zeta: V,
    val rho: V, val sigma: V, val h: V
) : Extractor<Point<Dim3, V>, Point<Dim3, V>> {
    override operator fun invoke(p: Point<Dim3, V>): Point<Dim3, V> {
        val x = p[0];
        val y = p[1];
        val z = p[2]
        val dx = y - rho * x + sigma * y * z
        val dy = gamma * y - x * z + z
        val dz = zeta * x * y - epsilon * z
        return Point<Dim3, V>(listOf(x + h * dx, y + h * dy, z + h * dz), Dim3)
    }

    companion object {
        operator fun invoke(
            gamma: Flt64 = Flt64(1.7), epsilon: Flt64 = Flt64(9.0), zeta: Flt64 = Flt64(2.0),
            rho: Flt64 = Flt64(3.0), sigma: Flt64 = Flt64(2.7), h: Flt64 = Flt64(0.01)
        ): DadrasAttractor<Flt64> = DadrasAttractor(gamma, epsilon, zeta, rho, sigma, h)
    }
}

data class DadrasAttractorGenerator(
    val attractor: DadrasAttractor<Flt64> = DadrasAttractor(),
    private var _x: Point<Dim3, Flt64> = point3(
        Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
        Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
        Random.nextFlt64(Flt64.decimalPrecision, Flt64.one)
    )
) : Generator<Point<Dim3, Flt64>> {
    val x by ::_x
    override operator fun invoke(): Point<Dim3, Flt64> {
        val x = _x.copy(); _x = attractor(x); return x
    }
}
