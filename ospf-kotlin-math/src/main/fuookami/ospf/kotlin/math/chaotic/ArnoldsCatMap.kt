/**
 * Arnold 猫映射
 * Arnold's Cat Map
 */
package fuookami.ospf.kotlin.math.chaotic

import kotlin.random.Random
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.geometry.*
import fuookami.ospf.kotlin.math.nextFlt64

/**
 * @property two 常量 2 / Constant 2
 */
data class ArnoldsCatMap<V : FloatingNumber<V>>(val two: V) : Extractor<Point<Dim2, V>, Point<Dim2, V>> {
    override operator fun invoke(x: Point<Dim2, V>): Point<Dim2, V> {
        val one = x[0].constants.one
        return Point<Dim2, V>(listOf((two * x[0] + x[1]) mod one, (x[0] + x[1]) mod one), Dim2)
    }

    companion object {
        operator fun invoke(): ArnoldsCatMap<Flt64> = ArnoldsCatMap(Flt64.two)
    }
}

/**
 * Arnold 猫映射生成器
 * Arnold's Cat Map generator
 *
 * @property map Arnold 猫映射实例 / Arnold's Cat Map instance
 * @property _x 当前状态点 / Current state point
 */
data class ArnoldsCatMapGenerator(
    val map: ArnoldsCatMap<Flt64> = ArnoldsCatMap(),
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
