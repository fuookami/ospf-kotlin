/**
 * 金融吸引子
 * Finance Attractor
*/
package fuookami.ospf.kotlin.math.chaotic

import kotlin.random.Random
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.geometry.*
import fuookami.ospf.kotlin.math.nextFlt64

/**
 * 金融吸引子
 * Finance Attractor
 *
 * @property alpha 系统参数 alpha / System parameter alpha
 * @property beta 系统参数 beta / System parameter beta
 * @property zeta 系统参数 zeta / System parameter zeta
 * @property h 时间步长 / Time step size
*/
data class FinanceAttractor<V : FloatingNumber<V>>(val alpha: V, val beta: V, val zeta: V, val h: V) :
    Extractor<Point<Dim3, V>, Point<Dim3, V>> {
    override operator fun invoke(p: Point<Dim3, V>): Point<Dim3, V> {
        val x = p[0];
        val y = p[1];
        val z = p[2]
        val dx = (x.constants.one / beta - alpha) * x + z + x * y
        val dy = -beta * y - x * x
        val dz = -x - zeta * z
        return Point<Dim3, V>(listOf(x + h * dx, y + h * dy, z + h * dz), Dim3)
    }

    companion object {
        operator fun invoke(
            alpha: Flt64 = Flt64(0.001),
            beta: Flt64 = Flt64(0.2),
            zeta: Flt64 = Flt64(1.1),
            h: Flt64 = Flt64(0.01)
        ): FinanceAttractor<Flt64> = FinanceAttractor(alpha, beta, zeta, h)
    }
}

/**
 * 金融吸引子生成器
 * Finance Attractor Generator
*/
data class FinanceAttractorGenerator(
    val attractor: FinanceAttractor<Flt64> = FinanceAttractor(),
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
