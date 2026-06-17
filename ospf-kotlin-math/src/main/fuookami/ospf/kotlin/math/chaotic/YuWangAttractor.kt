/**
 * Yu-Wang 吸引子
 * Yu-Wang Attractor
 */
package fuookami.ospf.kotlin.math.chaotic

import kotlin.random.Random
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.geometry.*
import fuookami.ospf.kotlin.math.nextFlt64

/**
 * Yu-Wang 吸引子
 * Yu-Wang Attractor
 *
 * @property alpha 系统参数 alpha / System parameter alpha
 * @property beta 系统参数 beta / System parameter beta
 * @property delta 系统参数 delta / System parameter delta
 * @property zeta 系统参数 zeta / System parameter zeta
 * @property h 时间步长 / Time step size
 */
data class YuWangAttractor<V : FloatingNumber<V>>(val alpha: V, val beta: V, val delta: V, val zeta: V, val h: V) :
    Extractor<Point<Dim3, V>, Point<Dim3, V>> {
    @Suppress("UNCHECKED_CAST")
    override operator fun invoke(p: Point<Dim3, V>): Point<Dim3, V> {
        val x = p[0];
        val y = p[1];
        val z = p[2]
        val dx = alpha * (y - x)
        val dy = beta * x - zeta * x * z
        val dz = (x * y).exp() as V - delta * z
        return Point<Dim3, V>(listOf(x + h * dx, y + h * dy, z + h * dz), Dim3)
    }

    companion object {
        operator fun invoke(
            alpha: Flt64 = Flt64(10.0),
            beta: Flt64 = Flt64(40.0),
            delta: Flt64 = Flt64(2.5),
            zeta: Flt64 = Flt64(2.0),
            h: Flt64 = Flt64(0.01)
        ): YuWangAttractor<Flt64> = YuWangAttractor(alpha, beta, delta, zeta, h)
    }
}

data class YuWangAttractorGenerator(
    val attractor: YuWangAttractor<Flt64> = YuWangAttractor(),
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
