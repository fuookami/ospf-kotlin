/**
 * Wang-Sun 吸引子
 * Wang-Sun Attractor
 */
package fuookami.ospf.kotlin.math.chaotic

import kotlin.random.Random
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.geometry.*
import fuookami.ospf.kotlin.math.nextFlt64

/**
 * Wang-Sun 吸引子
 * Wang-Sun Attractor
 *
 * @property alpha 系统参数 alpha / System parameter alpha
 * @property beta 系统参数 beta / System parameter beta
 * @property delta 系统参数 delta / System parameter delta
 * @property epsilon 系统参数 epsilon / System parameter epsilon
 * @property zeta 系统参数 zeta / System parameter zeta
 * @property xi 系统参数 xi / System parameter xi
 * @property h 时间步长 / Time step size
 */
data class WangSunAttractor<V : FloatingNumber<V>>(
    val alpha: V, val beta: V, val delta: V, val epsilon: V, val zeta: V, val xi: V, val h: V
) : Extractor<Point<Dim3, V>, Point<Dim3, V>> {
    override operator fun invoke(p: Point<Dim3, V>): Point<Dim3, V> {
        val x = p[0];
        val y = p[1];
        val z = p[2]
        val dx = alpha * x + zeta * y * z
        val dy = beta * x + delta * y - x * z
        val dz = epsilon * z + xi * x * y
        return Point<Dim3, V>(listOf(x + h * dx, y + h * dy, z + h * dz), Dim3)
    }

    companion object {
        operator fun invoke(
            alpha: Flt64 = Flt64(0.2),
            beta: Flt64 = Flt64(0.001),
            delta: Flt64 = Flt64(1.0),
            epsilon: Flt64 = Flt64(-0.4),
            zeta: Flt64 = Flt64(-1.0),
            xi: Flt64 = Flt64(-1.0),
            h: Flt64 = Flt64(0.01)
        ): WangSunAttractor<Flt64> = WangSunAttractor(alpha, beta, delta, epsilon, zeta, xi, h)
    }
}

/**
 * Wang-Sun 吸引子生成器
 * Wang-Sun Attractor Generator
 */
data class WangSunAttractorGenerator(
    val attractor: WangSunAttractor<Flt64> = WangSunAttractor(),
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
