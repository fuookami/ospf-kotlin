/**
 * Newton 迭代
 * Newton Iterate
 */
package fuookami.ospf.kotlin.math.chaotic

import kotlin.random.Random
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.geometry.*
import fuookami.ospf.kotlin.math.nextFlt64

/**
 * @property three 常量 3 / Constant 3
 * @property four 常量 4 / Constant 4
 * @property twoThirds 常量 2/3 / Constant 2/3
 * @property two 常量 2 / Constant 2
 */
data class NewtonIterate<V : FloatingNumber<V>>(val three: V, val four: V, val twoThirds: V, val two: V) :
    Extractor<Point<Dim2, V>, Point<Dim2, V>> {
    override operator fun invoke(p: Point<Dim2, V>): Point<Dim2, V> {
        val x = p[0];
        val y = p[1]
        val zero = x.constants.zero
        if (x leq zero) return Point<Dim2, V>(listOf(zero, zero), Dim2)
        val x2 = x * x;
        val y2 = y * y
        val d = three * ((x2 - y2) * (x2 - y2) + four * x2 * y2)
        if (d eq zero) return Point<Dim2, V>(listOf(zero, zero), Dim2)
        return Point<Dim2, V>(listOf(twoThirds * x + (x2 - y2) / d, twoThirds * y - (two * x * y) / d), Dim2)
    }

    companion object {
        operator fun invoke(): NewtonIterate<Flt64> =
            NewtonIterate(Flt64(3.0), Flt64(4.0), Flt64(2.0) / Flt64(3.0), Flt64.two)
    }
}

data class NewtonIterateGenerator(
    val map: NewtonIterate<Flt64> = NewtonIterate(),
    private var _x: Point<Dim2, Flt64> = point2(
        Random.nextFlt64(Flt64.one, Flt64(2.0)),
        Random.nextFlt64(Flt64.zero, Flt64.one)
    )
) : Generator<Point<Dim2, Flt64>> {
    val x by ::_x;
    override operator fun invoke(): Point<Dim2, Flt64> {
        val x = _x.copy(); _x = map(x); return x
    }
}
