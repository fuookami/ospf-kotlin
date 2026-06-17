/**
 * Hadley 吸引子
 * Hadley Attractor
 */
package fuookami.ospf.kotlin.math.chaotic

import kotlin.random.Random
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.geometry.*
import fuookami.ospf.kotlin.math.nextFlt64

/**
 * Hadley 吸引子
 * Hadley Attractor
 *
 * @property alpha 系统参数 alpha / System parameter alpha
 * @property beta 系统参数 beta / System parameter beta
 * @property delta 系统参数 delta / System parameter delta
 * @property zeta 系统参数 zeta / System parameter zeta
 * @property h 时间步长 / Time step size
 */
data class HadleyAttractor<V : FloatingNumber<V>>(
    val alpha: V, val beta: V, val delta: V, val zeta: V, val h: V
) : Extractor<Point<Dim3, V>, Point<Dim3, V>> {
    override operator fun invoke(p: Point<Dim3, V>): Point<Dim3, V> {
        val x = p[0];
        val y = p[1];
        val z = p[2]
        val dx = -y * y - z * z - alpha * x + alpha * zeta
        val dy = x * y - beta * x * z - y + delta
        val dz = beta * x * y + x * z - z
        return Point<Dim3, V>(listOf(x + h * dx, y + h * dy, z + h * dz), Dim3)
    }

    companion object {
        operator fun invoke(
            alpha: Flt64 = Flt64(0.2),
            beta: Flt64 = Flt64(4.0),
            delta: Flt64 = Flt64(1.0),
            zeta: Flt64 = Flt64(8.0),
            h: Flt64 = Flt64(0.01)
        ): HadleyAttractor<Flt64> = HadleyAttractor(alpha, beta, delta, zeta, h)
    }
}

data class HadleyAttractorGenerator(
    val attractor: HadleyAttractor<Flt64> = HadleyAttractor(),
    private var _x: Point<Dim3, Flt64> = point3(
        Random.nextFlt64(
            Flt64.decimalPrecision,
            Flt64.one
        ),
        Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
        Random.nextFlt64(Flt64.decimalPrecision, Flt64.one)
    )
) : Generator<Point<Dim3, Flt64>> {
    val x by ::_x;
    override operator fun invoke(): Point<Dim3, Flt64> {
        val x = _x.copy(); _x = attractor(x); return x
    }
}
