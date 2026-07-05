/**
 * 达芬方程
 * Duffing Equation
 */
package fuookami.ospf.kotlin.math.chaotic

import kotlin.random.Random
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.geometry.*
import fuookami.ospf.kotlin.math.nextFlt64

/**
 * 达芬方程
 * Duffing Equation
 *
 * @property alpha 系统参数 alpha / System parameter alpha
 * @property beta 系统参数 beta / System parameter beta
 * @property gamma 系统参数 gamma / System parameter gamma
 * @property delta 系统参数 delta / System parameter delta
 * @property omega 系统参数 omega / System parameter omega
 * @property h 时间步长 / Time step size
 */
data class DuffingEquation<V : FloatingNumber<V>>(
    val alpha: V,
    val beta: V,
    val gamma: V,
    val delta: V,
    val omega: V,
    val h: V
) : Extractor<Point<Dim3, V>, Point<Dim3, V>> {
    @Suppress("UNCHECKED_CAST")
    override operator fun invoke(p: Point<Dim3, V>): Point<Dim3, V> {
        val x = p[0];
        val y = p[1];
        val t = p[2]
        val dx = y
        val dy = -alpha * x - gamma * y - beta * x * x * x + delta * (omega * t).cos() as V
        return Point<Dim3, V>(listOf(x + h * dx, y + h * dy, t + h), Dim3)
    }

    companion object {
        operator fun invoke(
            alpha: Flt64 = Flt64(1.0),
            beta: Flt64 = Flt64(5.0),
            gamma: Flt64 = Flt64(0.02),
            delta: Flt64 = Flt64(8.0),
            omega: Flt64 = Flt64(0.5),
            h: Flt64 = Flt64(0.01)
        ): DuffingEquation<Flt64> = DuffingEquation(alpha, beta, gamma, delta, omega, h)
    }
}

/**
 * 达芬方程生成器
 * Duffing Equation generator
 *
 * @property attractor 达芬方程吸引子 / Duffing equation attractor
 */
data class DuffingEquationGenerator(
    val attractor: DuffingEquation<Flt64> = DuffingEquation(),
    private var _x: Point<Dim3, Flt64> = point3(
        Random.nextFlt64(
            Flt64.decimalPrecision,
            Flt64.one
        ), Random.nextFlt64(Flt64.decimalPrecision, Flt64.one), Flt64.zero
    )
) : Generator<Point<Dim3, Flt64>> {
    val x by ::_x;
    override operator fun invoke(): Point<Dim3, Flt64> {
        val x = _x.copy(); _x = attractor(x); return x
    }
}
