/**
 * 面包师映射
 * Baker's Map
 */
package fuookami.ospf.kotlin.math.chaotic

import kotlin.random.Random
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.geometry.*
import fuookami.ospf.kotlin.math.nextFlt64

/**
 * 面包师映射
 * Baker's Map
 *
 * 面包师映射是一种经典的二维混沌映射，模拟面包师揉面团的折叠与拉伸操作。
 * 该映射具有混合性和拓扑传递性，是遍历理论研究中的典型例子。
 *
 * The Baker's map is a classic two-dimensional chaotic map that simulates the folding and stretching
 * operations of a baker kneading dough. This map possesses mixing and topological transitivity properties,
 * and is a typical example in ergodic theory research.
 *
 * 公式 / Formula:
 * x_{n+1} = 2 * x_n mod 1
 * y_{n+1} = (floor(2 * x_n) + y_n) / 2
 */
data object BakersMap : Extractor<Point<Dim2, Flt64>, Point<Dim2, Flt64>> {
    override operator fun invoke(x: Point<Dim2, Flt64>): Point<Dim2, Flt64> {
        return point2(
            (Flt64.two * x[0]) mod Flt64.one,
            ((Flt64.two * x[0]).floor() + x[1]) / Flt64.two
        )
    }
}

/**
 * 面包师映射生成器
 * Baker's Map Generator
 *
 * @property bakersMap 面包师映射实例 / Baker's map instance
 */
data class BakersMapGenerator(
    val bakersMap: BakersMap = BakersMap,
    private var _x: Point<Dim2, Flt64> = point2(
        Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
        Random.nextFlt64(Flt64.decimalPrecision, Flt64.one)
    )
) : Generator<Point<Dim2, Flt64>> {
    val x by ::_x
    override operator fun invoke(): Point<Dim2, Flt64> {
        val x = _x.copy(); _x = bakersMap(x); return x
    }
}
