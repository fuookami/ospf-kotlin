/**
 * Zaslavskii 映射
 * Zaslavskii Map
 */
package fuookami.ospf.kotlin.math.chaotic

import kotlin.random.Random
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.geometry.*
import fuookami.ospf.kotlin.math.nextFlt64

/**
 * @property epsilon 系统参数 epsilon / System parameter epsilon
 * @property upsilon 系统参数 upsilon / System parameter upsilon
 * @property r 系统参数 r / System parameter r
 * @property mu 预计算参数 mu = (1 - exp(-r)) / r / Precomputed parameter
 * @property twoPi 常量 2*pi / Constant 2*pi
 */
data class ZaslavskiiMap<V : FloatingNumber<V>>(val epsilon: V, val upsilon: V, val r: V, val mu: V, val twoPi: V) :
    Extractor<Point<Dim2, V>, Point<Dim2, V>> {
    @Suppress("UNCHECKED_CAST")
    override operator fun invoke(p: Point<Dim2, V>): Point<Dim2, V> {
        val x = p[0];
        val y = p[1]
        val one = x.constants.one
        val cosVal = (twoPi * x).cos() as V
        val expR = (-r).exp() as V
        val newX = (x + upsilon * (one + mu * y) + epsilon * upsilon * mu * cosVal) mod one
        val newY = expR * (y + epsilon * cosVal)
        return Point<Dim2, V>(listOf(newX, newY), Dim2)
    }

    companion object {
        operator fun invoke(
            epsilon: Flt64 = Flt64(5.0),
            upsilon: Flt64 = Flt64(0.2),
            r: Flt64 = Flt64(2.0)
        ): ZaslavskiiMap<Flt64> {
            val mu = (Flt64.one - (-r).exp()) / r
            return ZaslavskiiMap(epsilon, upsilon, r, mu, Flt64(2.0) * Flt64.pi)
        }
    }
}

/**
 * Zaslavskii 映射生成器
 * Zaslavskii Map Generator
 *
 * @property map Zaslavskii 映射实例 / Zaslavskii map instance
 */
data class ZaslavskiiMapGenerator(
    val map: ZaslavskiiMap<Flt64> = ZaslavskiiMap(),
    private var _x: Point<Dim2, Flt64> = point2(
        Random.nextFlt64(
            Flt64.decimalPrecision,
            Flt64.one
        ), Random.nextFlt64(Flt64.decimalPrecision, Flt64.one)
    )
) : Generator<Point<Dim2, Flt64>> {
    val x by ::_x;
    override operator fun invoke(): Point<Dim2, Flt64> {
        val x = _x.copy(); _x = map(x); return x
    }
}
