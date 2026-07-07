/**
 * Lorenz 修正 1 吸引子
 * Lorenz Mod 1 Attractor
 */
package fuookami.ospf.kotlin.math.chaotic

import kotlin.random.Random
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.geometry.*
import fuookami.ospf.kotlin.math.nextFlt64

/**
 * Lorenz 修正 1 吸引子
 * Lorenz Mod 1 Attractor
 *
 * @property alpha 系统参数 alpha / System parameter alpha
 * @property beta 系统参数 beta / System parameter beta
 * @property delta 系统参数 delta / System parameter delta
 * @property zeta 系统参数 zeta / System parameter zeta
 * @property h 时间步长 / Time step size
 */
data class LorenzMod1Attractor<V : FloatingNumber<V>>(val alpha: V, val beta: V, val delta: V, val zeta: V, val h: V) :
    Extractor<Point<Dim3, V>, Point<Dim3, V>> {
    override operator fun invoke(p: Point<Dim3, V>): Point<Dim3, V> {
        val x = p[0];
        val y = p[1];
        val z = p[2]
        val dx = -alpha * x + y * y - z * z + alpha * zeta
        val dy = x * (y - beta * z) + delta
        val dz = z + x * (beta * y + z)
        return Point<Dim3, V>(listOf(x + h * dx, y + h * dy, z + h * dz), Dim3)
    }

    companion object {
        operator fun invoke(
            alpha: Flt64 = Flt64(0.1),
            beta: Flt64 = Flt64(4.0),
            delta: Flt64 = Flt64(0.08),
            zeta: Flt64 = Flt64(14.0),
            h: Flt64 = Flt64(0.01)
        ): LorenzMod1Attractor<Flt64> = LorenzMod1Attractor(alpha, beta, delta, zeta, h)
    }
}

/**
 * Lorenz Mod 1 attractor generator that iteratively produces chaotic sequences.
 * Lorenz 修正 1 吸引子生成器，通过迭代产生混沌序列。
 *
 * @property attractor the Lorenz Mod 1 attractor instance / Lorenz 修正 1 吸引子实例
 * @property _x the internal state variable for iteration / 迭代用的内部状态变量
 */
data class LorenzMod1AttractorGenerator(
    val attractor: LorenzMod1Attractor<Flt64> = LorenzMod1Attractor(),
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
