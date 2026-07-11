/**
 * 范德波尔系统
 * Van der Pol System
*/
package fuookami.ospf.kotlin.math.chaotic

import kotlin.random.Random
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.geometry.*
import fuookami.ospf.kotlin.math.nextFlt64

/**
 * 范德波尔系统
 * Van der Pol System
 *
 * @property a 系统参数 a / System parameter a
 * @property h 时间步长 / Time step size
*/
data class VanDerPolSystem<V : FloatingNumber<V>>(val a: V, val h: V) : Extractor<Point<Dim2, V>, Point<Dim2, V>> {
    override operator fun invoke(p: Point<Dim2, V>): Point<Dim2, V> {
        val x = p[0];
        val y = p[1]
        val three = x.constants.two + x.constants.one
        val dx = a * (x - x * x * x / three) - y
        val dy = x / a
        return Point<Dim2, V>(listOf(x + h * dx, y + h * dy), Dim2)
    }

    companion object {
        operator fun invoke(a: Flt64 = Flt64(1.0), h: Flt64 = Flt64(0.01)): VanDerPolSystem<Flt64> =
            VanDerPolSystem(a, h)
    }
}

/**
 * 范德波尔系统生成器
 * Van der Pol System Generator
*/
data class VanDerPolSystemGenerator(
    val attractor: VanDerPolSystem<Flt64> = VanDerPolSystem(),
    private var _x: Point<Dim2, Flt64> = point2(
        Random.nextFlt64(
            Flt64.decimalPrecision,
            Flt64.one
        ), Random.nextFlt64(Flt64.decimalPrecision, Flt64.one)
    )
) : Generator<Point<Dim2, Flt64>> {
    val x by ::_x;
    override operator fun invoke(): Point<Dim2, Flt64> {
        val x = _x.copy(); _x = attractor(x); return x
    }
}
