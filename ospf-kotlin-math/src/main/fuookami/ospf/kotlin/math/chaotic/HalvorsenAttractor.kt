/**
 * Halvorsen 吸引子
 * Halvorsen Attractor
 */
package fuookami.ospf.kotlin.math.chaotic

import kotlin.random.Random
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.geometry.*
import fuookami.ospf.kotlin.math.nextFlt64

/**
 * Halvorsen 吸引子
 * Halvorsen Attractor
 *
 * @property alpha 系统参数 alpha / System parameter alpha
 * @property h 时间步长 / Time step size
 */
data class HalvorsenAttractor<V : FloatingNumber<V>>(val alpha: V, val h: V) :
    Extractor<Point<Dim3, V>, Point<Dim3, V>> {
    override operator fun invoke(p: Point<Dim3, V>): Point<Dim3, V> {
        val x = p[0];
        val y = p[1];
        val z = p[2]
        val four = x.constants.two * x.constants.two
        val dx = -alpha * x - four * y - four * z - y * y
        val dy = -alpha * y - four * z - four * x - z * z
        val dz = -alpha * z - four * x - four * y - x * x
        return Point<Dim3, V>(listOf(x + h * dx, y + h * dy, z + h * dz), Dim3)
    }

    companion object {
        operator fun invoke(alpha: Flt64 = Flt64(1.4), h: Flt64 = Flt64(0.01)): HalvorsenAttractor<Flt64> =
            HalvorsenAttractor(alpha, h)
    }
}

data class HalvorsenAttractorGenerator(
    val attractor: HalvorsenAttractor<Flt64> = HalvorsenAttractor(),
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
