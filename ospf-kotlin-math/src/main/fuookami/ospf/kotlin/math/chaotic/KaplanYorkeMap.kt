/**
 * Kaplan-Yorke 映射
 * Kaplan-Yorke Map
*/
package fuookami.ospf.kotlin.math.chaotic

import kotlin.random.Random
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.geometry.*
import fuookami.ospf.kotlin.math.nextFlt64

/**
 * Kaplan-Yorke 映射
 * Kaplan-Yorke Map
 *
 * @property a 系统参数 a / System parameter a
 * @property fourPi 常量 4*pi / Constant 4*pi
*/
data class KaplanYorkeMap<V : FloatingNumber<V>>(val a: V, val fourPi: V) : Extractor<Point<Dim2, V>, Point<Dim2, V>> {
    @Suppress("UNCHECKED_CAST")
    override operator fun invoke(p: Point<Dim2, V>): Point<Dim2, V> {
        val x = p[0];
        val y = p[1]
        val newX = (x.constants.two * x) mod x.constants.one
        val newY = a * y + (fourPi * x).cos() as V
        return Point<Dim2, V>(listOf(newX, newY), Dim2)
    }

    companion object {
        operator fun invoke(a: Flt64 = Flt64(0.2)): KaplanYorkeMap<Flt64> = KaplanYorkeMap(a, Flt64(4.0) * Flt64.pi)
    }
}

/**
 * Kaplan-Yorke 映射生成器
 * Kaplan-Yorke Map Generator
 *
 * @property map Kaplan-Yorke 映射实例 / Kaplan-Yorke Map instance
 * @property _x 当前状态点（包含 x 和 y 坐标）/ Current state point (containing x and y coordinates)
*/
data class KaplanYorkeMapGenerator(
    val map: KaplanYorkeMap<Flt64> = KaplanYorkeMap(),
    private var _x: Point<Dim2, Flt64> = point2(
        Random.nextFlt64(
            Flt64.decimalPrecision,
            Flt64.one
        ), Random.nextFlt64(Flt64.decimalPrecision, Flt64.one)
    )
) : Generator<Point<Dim2, Flt64>> {

    /** 当前状态点的只读视图 / Read-only view of the current state point */
    val x by ::_x;
    override operator fun invoke(): Point<Dim2, Flt64> {
        val x = _x.copy(); _x = map(x); return x
    }
}
