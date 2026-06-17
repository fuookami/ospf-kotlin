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

data object BakersMap : Extractor<Point<Dim2, Flt64>, Point<Dim2, Flt64>> {
    override operator fun invoke(x: Point<Dim2, Flt64>): Point<Dim2, Flt64> {
        return point2(
            (Flt64.two * x[0]) mod Flt64.one,
            ((Flt64.two * x[0]).floor() + x[1]) / Flt64.two
        )
    }
}

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
