/**
 * Lorenz 吸引子（物理参数命名别名）
 * Lorenz Attractor (physics parameter naming alias)
 */
package fuookami.ospf.kotlin.math.chaotic

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.geometry.*

/**
 * 泛型洛伦兹吸引子系统，用于数值积分。
 * Generic Lorenz attractor system for numerical integration.
 *
 * 洛伦兹吸引子是一个由三个常微分方程组成的混沌系统，最初由 Edward Lorenz 在研究大气对流时提出。
 * The Lorenz attractor is a chaotic system of three ordinary differential equations, originally
 * introduced by Edward Lorenz while studying atmospheric convection.
 *
 * @property sigma 普朗特数 / Prandtl number
 * @property rho 瑞利数 / Rayleigh number
 * @property beta 系统参数 beta / System parameter beta
 * @property h 时间步长 / Time step size
 * @property inner 内部洛伦兹系统实例 / Internal Lorenz system instance
 */
data class LorenzAttractor<V : FloatingNumber<V>>(val sigma: V, val rho: V, val beta: V, val h: V) :
    Extractor<Point<Dim3, V>, Point<Dim3, V>> {
    private val inner = LorenzSystem(sigma, beta, rho, h)
    override operator fun invoke(x: Point<Dim3, V>): Point<Dim3, V> = inner(x)

    companion object {
        operator fun invoke(
            sigma: Flt64 = Flt64(10.0),
            rho: Flt64 = Flt64(28.0),
            beta: Flt64 = Flt64(8.0 / 3.0),
            h: Flt64 = Flt64(0.01)
        ): LorenzAttractor<Flt64> = LorenzAttractor(sigma, rho, beta, h)
    }
}
