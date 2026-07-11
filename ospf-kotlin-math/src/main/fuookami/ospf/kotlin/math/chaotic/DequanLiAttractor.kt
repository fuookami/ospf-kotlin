/**
 * Dequan Li 吸引子
 * Dequan Li Attractor
*/
package fuookami.ospf.kotlin.math.chaotic

import kotlin.random.Random
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.geometry.*
import fuookami.ospf.kotlin.math.nextFlt64

/**
 * Dequan Li 吸引子
 * Dequan Li Attractor
 *
 * @property alpha 系统参数 alpha / System parameter alpha
 * @property beta 系统参数 beta / System parameter beta
 * @property delta 系统参数 delta / System parameter delta
 * @property epsilon 系统参数 epsilon / System parameter epsilon
 * @property zeta 系统参数 zeta / System parameter zeta
 * @property rho 系统参数 rho / System parameter rho
 * @property h 时间步长 / Time step size
*/
data class DequanLiAttractor<V : FloatingNumber<V>>(
    val alpha: V, val beta: V, val delta: V, val epsilon: V, val zeta: V, val rho: V, val h: V
) : Extractor<Point<Dim3, V>, Point<Dim3, V>> {
    override operator fun invoke(p: Point<Dim3, V>): Point<Dim3, V> {
        val x = p[0];
        val y = p[1];
        val z = p[2]
        val dx = alpha * (y - x) + delta * x * z
        val dy = rho * x + zeta * y - x * z
        val dz = beta * z + x * y - epsilon * x * x
        return Point<Dim3, V>(listOf(x + h * dx, y + h * dy, z + h * dz), Dim3)
    }

    companion object {
        operator fun invoke(
            alpha: Flt64 = Flt64(40.0),
            beta: Flt64 = Flt64(1.833),
            delta: Flt64 = Flt64(0.16),
            epsilon: Flt64 = Flt64(0.65),
            zeta: Flt64 = Flt64(20.0),
            rho: Flt64 = Flt64(55.0),
            h: Flt64 = Flt64(0.01)
        ): DequanLiAttractor<Flt64> = DequanLiAttractor(alpha, beta, delta, epsilon, zeta, rho, h)
    }
}

/**
 * Dequan Li 吸引子生成器
 * Dequan Li Attractor Generator
 *
 * @property attractor Dequan Li 吸引子实例 / Dequan Li Attractor instance
 * @property _x 初始状态向量 / Initial state vector
*/
data class DequanLiAttractorGenerator(
    val attractor: DequanLiAttractor<Flt64> = DequanLiAttractor(),
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
