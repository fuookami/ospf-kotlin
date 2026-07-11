/**
 * Nose-Hoover 吸引子
 * Nose-Hoover Attractor
*/
package fuookami.ospf.kotlin.math.chaotic

import kotlin.random.Random
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.geometry.*
import fuookami.ospf.kotlin.math.nextFlt64

/**
 * Nose-Hoover 吸引子
 * Nose-Hoover Attractor
 *
 * @property alpha 系统参数 alpha / System parameter alpha
 * @property h 时间步长 / Time step size
*/
data class NoseHooverAttractor<V : FloatingNumber<V>>(val alpha: V, val h: V) :
    Extractor<Point<Dim3, V>, Point<Dim3, V>> {
    override operator fun invoke(p: Point<Dim3, V>): Point<Dim3, V> {
        val x = p[0];
        val y = p[1];
        val z = p[2]
        return Point<Dim3, V>(listOf(x + h * y, y + h * (-x + y * z), z + h * (alpha - y * y)), Dim3)
    }

    companion object {
        operator fun invoke(alpha: Flt64 = Flt64(1.5), h: Flt64 = Flt64(0.01)): NoseHooverAttractor<Flt64> =
            NoseHooverAttractor(alpha, h)
    }
}

/**
 * Nose-Hoover 吸引子生成器
 * Nose-Hoover Attractor Generator
 *
 * @property attractor Nose-Hoover 吸引子实例 / Nose-Hoover attractor instance
*/
data class NoseHooverAttractorGenerator(
    val attractor: NoseHooverAttractor<Flt64> = NoseHooverAttractor(),
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
