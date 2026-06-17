/**
 * Rabinovich-Fabrikant 方程
 * Rabinovich-Fabrikant Equation
 */
package fuookami.ospf.kotlin.math.chaotic

import kotlin.random.Random
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.geometry.*
import fuookami.ospf.kotlin.math.nextFlt64

/**
 * Rabinovich-Fabrikant 方程
 * Rabinovich-Fabrikant Equation
 *
 * @property a 系统参数 a / System parameter a
 * @property b 系统参数 b / System parameter b
 * @property h 时间步长 / Time step size
 */
data class RabinovichFabrikantEquation<V : FloatingNumber<V>>(val a: V, val b: V, val h: V) :
    Extractor<Point<Dim3, V>, Point<Dim3, V>> {
    override operator fun invoke(p: Point<Dim3, V>): Point<Dim3, V> {
        val x = p[0];
        val y = p[1];
        val z = p[2]
        val three = x.constants.two + x.constants.one
        val dx = y * (z - x.constants.one + x * x) + b * x
        val dy = x * (three * z + x.constants.one - x * x) + b * y
        val dz = -x.constants.two * z * (a + x * y)
        return Point<Dim3, V>(listOf(x + h * dx, y + h * dy, z + h * dz), Dim3)
    }

    companion object {
        operator fun invoke(
            a: Flt64 = Flt64(1.1),
            b: Flt64 = Flt64(0.9),
            h: Flt64 = Flt64(0.01)
        ): RabinovichFabrikantEquation<Flt64> = RabinovichFabrikantEquation(a, b, h)
    }
}

data class RabinovichFabrikantEquationGenerator(
    val attractor: RabinovichFabrikantEquation<Flt64> = RabinovichFabrikantEquation(),
    private var _x: Point<Dim3, Flt64> = point3(
        Random.nextFlt64(
            Flt64.decimalPrecision,
            Flt64.one
        ),
        Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
        Random.nextFlt64(Flt64.decimalPrecision, Flt64.one)
    )
) : Generator<Point<Dim3, Flt64>> {
    val x by ::_x;
    override operator fun invoke(): Point<Dim3, Flt64> {
        val x = _x.copy(); _x = attractor(x); return x
    }
}
