/**
 * Qi 吸引子（四维超混沌）
 * Qi Attractor (4D Hyperchaotic)
 */
package fuookami.ospf.kotlin.math.chaotic

import kotlin.random.Random
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.geometry.*
import fuookami.ospf.kotlin.math.nextFlt64

/**
 * Qi 吸引子（四维超混沌）
 * Qi Attractor (4D Hyperchaotic)
 *
 * @property alpha 系统参数 alpha / System parameter alpha
 * @property beta 系统参数 beta / System parameter beta
 * @property delta 系统参数 delta / System parameter delta
 * @property zeta 系统参数 zeta / System parameter zeta
 * @property h 时间步长 / Time step size
 */
data class QiAttractor<V : FloatingNumber<V>>(val alpha: V, val beta: V, val delta: V, val zeta: V, val h: V) :
    Extractor<Point<Dim4, V>, Point<Dim4, V>> {
    override operator fun invoke(p: Point<Dim4, V>): Point<Dim4, V> {
        val x = p[0];
        val y = p[1];
        val z = p[2];
        val u = p[3]
        val dx = alpha * (y - x) + y * z * u
        val dy = beta * (x + y) - x * z * u
        val dz = -zeta * z + x * y * u
        val du = -delta * u + x * y * z
        return Point<Dim4, V>(listOf(x + h * dx, y + h * dy, z + h * dz, u + h * du), Dim4)
    }

    companion object {
        operator fun invoke(
            alpha: Flt64 = Flt64(30.0),
            beta: Flt64 = Flt64(10.0),
            delta: Flt64 = Flt64(10.0),
            zeta: Flt64 = Flt64(1.0),
            h: Flt64 = Flt64(0.001)
        ): QiAttractor<Flt64> = QiAttractor(alpha, beta, delta, zeta, h)
    }
}

/**
 * Qi attractor generator that iteratively produces hyperchaotic sequences.
 * Qi 吸引子生成器，通过迭代产生超混沌序列。
 *
 * @property attractor the Qi attractor instance / Qi 吸引子实例
 * @property _x the internal state variable for iteration / 迭代用的内部状态变量
 */
data class QiAttractorGenerator(
    val attractor: QiAttractor<Flt64> = QiAttractor(),
    private var _x: Point<Dim4, Flt64> = point4(
        Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
        Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
        Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
        Random.nextFlt64(Flt64.decimalPrecision, Flt64.one)
    )
) : Generator<Point<Dim4, Flt64>> {
    val x by ::_x
    override operator fun invoke(): Point<Dim4, Flt64> {
        val x = _x.copy(); _x = attractor(x); return x
    }
}
