/**
 * Bouali 吸引子
 * Bouali Attractor
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
 * @property zeta 系统参数 zeta / System parameter zeta
 * @property h 时间步长 / Time step size
 * @property c4 常量 4 / Constant 4
 * @property c15 常量 1.5 / Constant 1.5
 * @property c005 常量 0.05 / Constant 0.05
*/
data class BoualiAttractor<V : FloatingNumber<V>>(
    val alpha: V,
    val zeta: V,
    val h: V,
    val c4: V,
    val c15: V,
    val c005: V
) : Extractor<Point<Dim3, V>, Point<Dim3, V>> {
    override operator fun invoke(p: Point<Dim3, V>): Point<Dim3, V> {
        val x = p[0];
        val y = p[1];
        val z = p[2]
        val dx = x * (c4 - y) + alpha * z
        val dy = -y * (x.constants.one - x * x)
        val dz = -x * (c15 - zeta * z) - c005 * z
        return Point<Dim3, V>(listOf(x + h * dx, y + h * dy, z + h * dz), Dim3)
    }

    companion object {
        operator fun invoke(
            alpha: Flt64 = Flt64(0.3),
            zeta: Flt64 = Flt64.one,
            h: Flt64 = Flt64(0.01)
        ): BoualiAttractor<Flt64> = BoualiAttractor(alpha, zeta, h, Flt64(4.0), Flt64(1.5), Flt64(0.05))
    }
}

/**
 * Bouali 吸引子生成器
 * Bouali Attractor Generator
 *
 * @property attractor Bouali 吸引子实例 / Bouali attractor instance
*/
data class BoualiAttractorGenerator(
    val attractor: BoualiAttractor<Flt64> = BoualiAttractor(),
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
