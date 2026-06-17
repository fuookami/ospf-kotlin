/**
 * 辛映射
 * Symplectic Map
 */
package fuookami.ospf.kotlin.math.chaotic

import kotlin.random.Random
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.geometry.*
import fuookami.ospf.kotlin.math.nextFlt64

/**
 * 辛映射
 * Symplectic Map
 *
 * @property h 时间步长 / Time step size
 */
data class SymplecticMap<V : FloatingNumber<V>>(val h: V) : Extractor<Point<Dim2, V>, Point<Dim2, V>> {
    override operator fun invoke(p: Point<Dim2, V>): Point<Dim2, V> {
        val x = p[0];
        val y = p[1]
        val onePlusX = x.constants.one + x
        val dx = x / onePlusX
        val dy = y * onePlusX * onePlusX
        return Point<Dim2, V>(listOf(x + h * dx, y + h * dy), Dim2)
    }

    companion object {
        operator fun invoke(h: Flt64 = Flt64(0.01)): SymplecticMap<Flt64> = SymplecticMap(h)
    }
}

data class SymplecticMapGenerator(
    val map: SymplecticMap<Flt64> = SymplecticMap(),
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
