/**
 * Lorenz-Stenflo 吸引子（四维）
 * Lorenz-Stenflo Attractor (4D)
 */
package fuookami.ospf.kotlin.math.chaotic

import kotlin.random.Random
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.geometry.*
import fuookami.ospf.kotlin.math.nextFlt64

/**
 * Lorenz-Stenflo 吸引子（四维）
 * Lorenz-Stenflo Attractor (4D)
 *
 * @property alpha 系统参数 alpha / System parameter alpha
 * @property beta 系统参数 beta / System parameter beta
 * @property delta 系统参数 delta / System parameter delta
 * @property zeta 系统参数 zeta / System parameter zeta
 * @property h 时间步长 / Time step size
 */
data class LorenzStenfloAttractor<V : FloatingNumber<V>>(
    val alpha: V,
    val beta: V,
    val delta: V,
    val zeta: V,
    val h: V
) : Extractor<Point<Dim4, V>, Point<Dim4, V>> {
    override operator fun invoke(p: Point<Dim4, V>): Point<Dim4, V> {
        val x = p[0];
        val y = p[1];
        val z = p[2];
        val u = p[3]
        val dx = alpha * (y - x) + delta * u
        val dy = x * (zeta - z) - y
        val dz = x * y - beta * z
        val du = -x - alpha * u
        return Point<Dim4, V>(listOf(x + h * dx, y + h * dy, z + h * dz, u + h * du), Dim4)
    }

    companion object {
        operator fun invoke(
            alpha: Flt64 = Flt64(2.0),
            beta: Flt64 = Flt64(0.7),
            delta: Flt64 = Flt64(1.5),
            zeta: Flt64 = Flt64(26.0),
            h: Flt64 = Flt64(0.01)
        ): LorenzStenfloAttractor<Flt64> = LorenzStenfloAttractor(alpha, beta, delta, zeta, h)
    }
}

data class LorenzStenfloAttractorGenerator(
    val attractor: LorenzStenfloAttractor<Flt64> = LorenzStenfloAttractor(),
    private var _x: Point<Dim4, Flt64> = point4(
        Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
        Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
        Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
        Random.nextFlt64(Flt64.decimalPrecision, Flt64.one)
    )
) : Generator<Point<Dim4, Flt64>> {
    val x by ::_x
    override operator fun invoke(): Point<Dim4, Flt64> {
        val x = _x.copy(); _x = attractor(x); return x
    }
}
