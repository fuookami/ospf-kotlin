/**
 * 托马斯吸引子
 * Thomas Attractor
 */
package fuookami.ospf.kotlin.math.chaotic

import kotlin.random.Random
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.geometry.*
import fuookami.ospf.kotlin.math.nextFlt64

/**
 * 托马斯吸引子
 * Thomas Attractor
 *
 * @property beta 系统参数 beta / System parameter beta
 * @property h 时间步长 / Time step size
 */
data class ThomasAttractor<V : FloatingNumber<V>>(val beta: V, val h: V) : Extractor<Point<Dim3, V>, Point<Dim3, V>> {
    @Suppress("UNCHECKED_CAST")
    override operator fun invoke(p: Point<Dim3, V>): Point<Dim3, V> {
        val x = p[0];
        val y = p[1];
        val z = p[2]
        val dx = y.sin() as V - beta * x
        val dy = z.sin() as V - beta * y
        val dz = x.sin() as V - beta * z
        return Point<Dim3, V>(listOf(x + h * dx, y + h * dy, z + h * dz), Dim3)
    }

    companion object {
        operator fun invoke(beta: Flt64 = Flt64(0.19), h: Flt64 = Flt64(0.01)): ThomasAttractor<Flt64> =
            ThomasAttractor(beta, h)
    }
}

data class ThomasAttractorGenerator(
    val attractor: ThomasAttractor<Flt64> = ThomasAttractor(),
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
