/**
 * 姜饼人映射
 * Gingerbreadman Map
 */
package fuookami.ospf.kotlin.math.chaotic

import kotlin.random.Random
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.geometry.*
import fuookami.ospf.kotlin.math.nextFlt64

/**
 * 姜饼人映射
 * Gingerbreadman Map
 *
 * @property one 常量 1 / Constant 1
 */
data class GingerbreadmanMap<V : FloatingNumber<V>>(val one: V) : Extractor<Point<Dim2, V>, Point<Dim2, V>> {
    override operator fun invoke(p: Point<Dim2, V>): Point<Dim2, V> {
        val x = p[0];
        val y = p[1]
        return Point<Dim2, V>(listOf(one - y + x.abs(), x), Dim2)
    }

    companion object {
        operator fun invoke(): GingerbreadmanMap<Flt64> = GingerbreadmanMap(Flt64.one)
    }
}

/**
 * 姜饼人映射生成器
 * Gingerbreadman Map Generator
 *
 * @property map 姜饼人映射实例 / Gingerbreadman map instance
 */
data class GingerbreadmanMapGenerator(
    val map: GingerbreadmanMap<Flt64> = GingerbreadmanMap(),
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
