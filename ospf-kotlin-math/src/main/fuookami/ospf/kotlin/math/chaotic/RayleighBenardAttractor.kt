/**
 * Rayleigh-Benard 吸引子
 * Rayleigh-Benard Attractor
 */
package fuookami.ospf.kotlin.math.chaotic

import kotlin.random.Random
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.geometry.*
import fuookami.ospf.kotlin.math.nextFlt64

/**
 * Rayleigh-Benard 吸引子
 * Rayleigh-Benard Attractor
 *
 * @property alpha 系统参数 alpha / System parameter alpha
 * @property beta 系统参数 beta / System parameter beta
 * @property gamma 系统参数 gamma / System parameter gamma
 * @property h 时间步长 / Time step size
 */
data class RayleighBenardAttractor<V : FloatingNumber<V>>(val alpha: V, val beta: V, val gamma: V, val h: V) :
    Extractor<Point<Dim3, V>, Point<Dim3, V>> {
    override operator fun invoke(p: Point<Dim3, V>): Point<Dim3, V> {
        val x = p[0];
        val y = p[1];
        val z = p[2]
        val dx = -alpha * x + alpha * y
        val dy = gamma * x - y - x * z
        val dz = x * y - beta * z
        return Point<Dim3, V>(listOf(x + h * dx, y + h * dy, z + h * dz), Dim3)
    }

    companion object {
        operator fun invoke(
            alpha: Flt64 = Flt64(9.0),
            beta: Flt64 = Flt64(5.0),
            gamma: Flt64 = Flt64(12.0),
            h: Flt64 = Flt64(0.01)
        ): RayleighBenardAttractor<Flt64> = RayleighBenardAttractor(alpha, beta, gamma, h)
    }
}

/**
 * Rayleigh-Benard 吸引子生成器
 * Rayleigh-Benard Attractor Generator
 *
 * @property attractor 吸引子实例 / Attractor instance
 * @property x 当前状态向量 / Current state vector
 */
data class RayleighBenardAttractorGenerator(
    val attractor: RayleighBenardAttractor<Flt64> = RayleighBenardAttractor(),
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
