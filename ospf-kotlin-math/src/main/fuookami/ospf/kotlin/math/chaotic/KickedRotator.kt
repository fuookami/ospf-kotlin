/**
 * 受击转子
 * Kicked Rotator
 */
package fuookami.ospf.kotlin.math.chaotic

import kotlin.random.Random
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.geometry.*
import fuookami.ospf.kotlin.math.nextFlt64

/**
 * 受击转子
 * Kicked Rotator
 *
 * @property k 系统参数 k / System parameter k
 */
data class KickedRotator<V : FloatingNumber<V>>(val k: V) : Extractor<Point<Dim2, V>, Point<Dim2, V>> {
    @Suppress("UNCHECKED_CAST")
    override operator fun invoke(p: Point<Dim2, V>): Point<Dim2, V> {
        val x = p[0];
        val y = p[1]
        val newX = x + k * (y.sin() as V)
        return Point<Dim2, V>(listOf(newX, y + newX), Dim2)
    }

    companion object {
        operator fun invoke(k: Flt64 = Flt64(0.971635)): KickedRotator<Flt64> = KickedRotator(k)
    }
}

data class KickedRotatorGenerator(
    val map: KickedRotator<Flt64> = KickedRotator(),
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
