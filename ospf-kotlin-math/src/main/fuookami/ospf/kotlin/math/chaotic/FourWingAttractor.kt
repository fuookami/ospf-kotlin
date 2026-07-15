/**
 * 四翼吸引子
 * Four-Wing Attractor
*/
package fuookami.ospf.kotlin.math.chaotic

import kotlin.random.Random
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.geometry.*
import fuookami.ospf.kotlin.math.nextFlt64

/**
 * 四翼吸引子
 * Four-Wing Attractor
 *
 * @property alpha 系统参数 alpha / System parameter alpha
 * @property beta 系统参数 beta / System parameter beta
 * @property delta 系统参数 delta / System parameter delta
 * @property zeta 系统参数 zeta / System parameter zeta
 * @property kappa 系统参数 kappa / System parameter kappa
 * @property h 时间步长 / Time step size
*/
data class FourWingAttractor<V : FloatingNumber<V>>(
    val alpha: V,
    val beta: V,
    val delta: V,
    val zeta: V,
    val kappa: V,
    val h: V
) : Extractor<Point<Dim3, V>, Point<Dim3, V>> {
    override operator fun invoke(p: Point<Dim3, V>): Point<Dim3, V> {
        val x = p[0];
        val y = p[1];
        val z = p[2]
        val dx = alpha * x - beta * y * z
        val dy = -zeta * y + x * z
        val dz = kappa * x - delta * z + x * z
        return Point<Dim3, V>(listOf(x + h * dx, y + h * dy, z + h * dz), Dim3)
    }

    companion object {
        operator fun invoke(
            alpha: Flt64 = Flt64(4.0),
            beta: Flt64 = Flt64(6.0),
            delta: Flt64 = Flt64(5.0),
            zeta: Flt64 = Flt64(10.0),
            kappa: Flt64 = Flt64(1.0),
            h: Flt64 = Flt64(0.01)
        ): FourWingAttractor<Flt64> = FourWingAttractor(alpha, beta, delta, zeta, kappa, h)
    }
}

/**
 * 四翼吸引子生成器
 * Four-Wing Attractor Generator
*/
data class FourWingAttractorGenerator(
    val attractor: FourWingAttractor<Flt64> = FourWingAttractor(),
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
