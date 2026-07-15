/**
 * Rossler 吸引子
 * Rossler Attractor
*/
package fuookami.ospf.kotlin.math.chaotic

import kotlin.random.Random
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.geometry.*
import fuookami.ospf.kotlin.math.nextFlt64

/**
 * Rossler 吸引子
 * Rossler Attractor
 *
 * @property alpha 系统参数 alpha / System parameter alpha
 * @property beta 系统参数 beta / System parameter beta
 * @property zeta 系统参数 zeta / System parameter zeta
 * @property h 时间步长 / Time step size
*/
data class RosslerAttractor<V : FloatingNumber<V>>(
    val alpha: V, val beta: V, val zeta: V, val h: V
) : Extractor<Point<Dim3, V>, Point<Dim3, V>> {
    override operator fun invoke(p: Point<Dim3, V>): Point<Dim3, V> {
        val x = p[0];
        val y = p[1];
        val z = p[2]
        val dx = -y - z
        val dy = x + alpha * y
        val dz = beta + z * (x - zeta)
        return Point<Dim3, V>(listOf(x + h * dx, y + h * dy, z + h * dz), Dim3)
    }

    companion object {
        operator fun invoke(
            alpha: Flt64 = Flt64(0.2),
            beta: Flt64 = Flt64(0.2),
            zeta: Flt64 = Flt64(5.7),
            h: Flt64 = Flt64(0.01)
        ): RosslerAttractor<Flt64> = RosslerAttractor(alpha, beta, zeta, h)
    }
}

/**
 * Rossler 吸引子生成器
 * Rossler Attractor Generator
 *
 * @property attractor Rossler 吸引子实例 / Rossler attractor instance
 * @property _x 当前状态向量（内部可变，通过 x 属性只读暴露） / Current state vector (mutable internally, exposed read-only via x property)
*/
data class RosslerAttractorGenerator(
    val attractor: RosslerAttractor<Flt64> = RosslerAttractor(),
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
