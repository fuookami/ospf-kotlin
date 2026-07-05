/**
 * Newton-Leipnik 吸引子
 * Newton-Leipnik Attractor
 */
package fuookami.ospf.kotlin.math.chaotic

import kotlin.random.Random
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.geometry.*
import fuookami.ospf.kotlin.math.nextFlt64

/**
 * @property alpha 系统参数 alpha / System parameter alpha
 * @property beta 系统参数 beta / System parameter beta
 * @property h 时间步长 / Time step size
 * @property c10 常量 10 / Constant 10
 * @property c5 常量 5 / Constant 5
 * @property c04 常量 0.4 / Constant 0.4
 */
data class NewtonLeipnikAttractor<V : FloatingNumber<V>>(
    val alpha: V,
    val beta: V,
    val h: V,
    val c10: V,
    val c5: V,
    val c04: V
) : Extractor<Point<Dim3, V>, Point<Dim3, V>> {
    override operator fun invoke(p: Point<Dim3, V>): Point<Dim3, V> {
        val x = p[0];
        val y = p[1];
        val z = p[2]
        val dx = -alpha * x + y + c10 * y * z
        val dy = -x - c04 * y + c5 * x * z
        val dz = beta * z - c5 * x * y
        return Point<Dim3, V>(listOf(x + h * dx, y + h * dy, z + h * dz), Dim3)
    }

    companion object {
        operator fun invoke(
            alpha: Flt64 = Flt64(0.4),
            beta: Flt64 = Flt64(0.175),
            h: Flt64 = Flt64(0.01)
        ): NewtonLeipnikAttractor<Flt64> = NewtonLeipnikAttractor(alpha, beta, h, Flt64(10.0), Flt64(5.0), Flt64(0.4))
    }
}

/**
 * Newton-Leipnik 吸引子生成器
 * Newton-Leipnik Attractor Generator
 *
 * @property attractor Newton-Leipnik 吸引子实例 / Newton-Leipnik attractor instance
 */
data class NewtonLeipnikAttractorGenerator(
    val attractor: NewtonLeipnikAttractor<Flt64> = NewtonLeipnikAttractor(),
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
