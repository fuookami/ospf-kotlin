/**
 * 池田映射（2D 版本）
 * Ikeda Map (2D version)
 */
package fuookami.ospf.kotlin.math.chaotic

import kotlin.random.Random
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.geometry.*
import fuookami.ospf.kotlin.math.nextFlt64

/**
 * @property u 系统参数 u / System parameter u
 * @property t0 常量 0.4 / Constant 0.4
 * @property t1 常量 6.0 / Constant 6.0
 */
data class IkedaMap<V : FloatingNumber<V>>(val u: V, val t0: V, val t1: V) : Extractor<Point<Dim2, V>, Point<Dim2, V>> {
    @Suppress("UNCHECKED_CAST")
    override operator fun invoke(p: Point<Dim2, V>): Point<Dim2, V> {
        val x = p[0];
        val y = p[1]
        val one = x.constants.one
        val t = t0 - t1 / (one + x * x + y * y)
        val sint = t.sin() as V
        val cost = t.cos() as V
        return Point<Dim2, V>(
            listOf(
                one + u * (x * cost - y * sint),
                u * (x * sint + y * cost)
            ), Dim2
        )
    }

    companion object {
        operator fun invoke(u: Flt64 = Flt64(0.918)): IkedaMap<Flt64> = IkedaMap(u, Flt64(0.4), Flt64(6.0))
    }
}

data class IkedaMapGenerator(
    val map: IkedaMap<Flt64> = IkedaMap(),
    private var _x: Point<Dim2, Flt64> = point2(
        Random.nextFlt64(Flt64.zero, Flt64.one),
        Random.nextFlt64(Flt64.zero, Flt64.one)
    )
) : Generator<Point<Dim2, Flt64>> {
    val x by ::_x;
    override operator fun invoke(): Point<Dim2, Flt64> {
        val x = _x.copy(); _x = map(x); return x
    }
}
