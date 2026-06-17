/**
 * Martin 迭代
 * Martin Iterate
 */
package fuookami.ospf.kotlin.math.chaotic

import kotlin.random.Random
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.geometry.*
import fuookami.ospf.kotlin.math.nextFlt64

/**
 * Martin 迭代
 * Martin Iterate
 *
 * @property a 系统参数 a / System parameter a
 * @property b 系统参数 b / System parameter b
 * @property c 系统参数 c / System parameter c
 */
/**
 * Martin 迭代
 * Martin Iterate
 *
 * @property a 系统参数 a / System parameter a
 * @property b 系统参数 b / System parameter b
 * @property c 系统参数 c / System parameter c
 */
data class MartinIterate<V : FloatingNumber<V>>(val a: V, val b: V, val c: V) :
    Extractor<Point<Dim2, V>, Point<Dim2, V>> {
    override operator fun invoke(p: Point<Dim2, V>): Point<Dim2, V> {
        val x = p[0];
        val y = p[1]
        val epsilon = x.constants.positiveMinimum

        @Suppress("UNCHECKED_CAST")
        val temp = ((b * x - c).abs().sqrt() as V)
        val g = when {
            x gr epsilon -> y - temp
            x ls -epsilon -> y + temp
            else -> y
        }
        return Point<Dim2, V>(listOf(g, a - x), Dim2)
    }

    companion object {
        operator fun invoke(
            a: Flt64 = Flt64(68.0),
            b: Flt64 = Flt64(75.0),
            c: Flt64 = Flt64(83.0)
        ): MartinIterate<Flt64> = MartinIterate(a, b, c)
    }
}

data class MartinIterateGenerator(
    val map: MartinIterate<Flt64> = MartinIterate(),
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
