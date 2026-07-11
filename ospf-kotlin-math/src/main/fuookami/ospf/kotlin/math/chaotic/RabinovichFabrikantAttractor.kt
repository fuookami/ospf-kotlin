/**
 * Rabinovich-Fabrikant 吸引子
 * Rabinovich-Fabrikant Attractor
 *
 * 一个描述非平衡介质中波调制不稳定性的混沌吸引子系统。
 * 常用于非平衡态物理、波动力学和复杂系统分析。
 *
 * A chaotic attractor system describing wave modulation instability in non-equilibrium media.
 * Commonly used for non-equilibrium physics, wave dynamics, and complex systems analysis.
*/
package fuookami.ospf.kotlin.math.chaotic

import kotlin.random.Random
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.geometry.*
import fuookami.ospf.kotlin.math.nextFlt64

/**
 * Rabinovich-Fabrikant 吸引子
 * Rabinovich-Fabrikant Attractor
 *
 * @property alpha 系统参数 alpha / System parameter alpha
 * @property gamma 系统参数 gamma / System parameter gamma
 * @property h 时间步长 / Time step size
*/
data class RabinovichFabrikantAttractor<V : FloatingNumber<V>>(
    val alpha: V,
    val gamma: V,
    val h: V
) : Extractor<Point<Dim3, V>, Point<Dim3, V>> {
    override operator fun invoke(p: Point<Dim3, V>): Point<Dim3, V> {
        val x = p[0]
        val y = p[1]
        val z = p[2]
        val dx = gamma * (y - x + x * z)
        val dy = alpha * x - y + x * z
        val dz = -x * y - z
        return Point<Dim3, V>(listOf(x + h * dx, y + h * dy, z + h * dz), Dim3)
    }

    companion object {
        operator fun invoke(
            alpha: Flt64 = Flt64(1.1),
            gamma: Flt64 = Flt64(0.9),
            h: Flt64 = Flt64(0.01)
        ): RabinovichFabrikantAttractor<Flt64> = RabinovichFabrikantAttractor(alpha, gamma, h)
    }
}

/**
 * Rabinovich-Fabrikant 吸引子生成器
 * Rabinovich-Fabrikant Attractor Generator
 *
 * @property attractor Rabinovich-Fabrikant 吸引子实例 / Rabinovich-Fabrikant attractor instance
*/
data class RabinovichFabrikantAttractorGenerator(
    val attractor: RabinovichFabrikantAttractor<Flt64> = RabinovichFabrikantAttractor(),
    private var _x: Point<Dim3, Flt64> = point3(
        Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
        Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
        Random.nextFlt64(Flt64.decimalPrecision, Flt64.one)
    )
) : Generator<Point<Dim3, Flt64>> {
    val x by ::_x
    override operator fun invoke(): Point<Dim3, Flt64> {
        val x = _x.copy(); _x = attractor(x); return x
    }
}
