/**
 * Sakarya 吸引子
 * Sakarya Attractor
*/
package fuookami.ospf.kotlin.math.chaotic

import kotlin.random.Random
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.geometry.*
import fuookami.ospf.kotlin.math.nextFlt64

/**
 * Sakarya 吸引子
 * Sakarya Attractor
 *
 * @property alpha 系统参数 alpha / System parameter alpha
 * @property beta 系统参数 beta / System parameter beta
 * @property h 时间步长 / Time step size
*/
data class SakaryaAttractor<V : FloatingNumber<V>>(val alpha: V, val beta: V, val h: V) :
    Extractor<Point<Dim3, V>, Point<Dim3, V>> {
    override operator fun invoke(p: Point<Dim3, V>): Point<Dim3, V> {
        val x = p[0];
        val y = p[1];
        val z = p[2]
        val dx = -x + y + y * z
        val dy = -x - y + alpha * x * z
        val dz = z - beta * x * y
        return Point<Dim3, V>(listOf(x + h * dx, y + h * dy, z + h * dz), Dim3)
    }

    companion object {
        operator fun invoke(
            alpha: Flt64 = Flt64(0.4),
            beta: Flt64 = Flt64(0.3),
            h: Flt64 = Flt64(0.01)
        ): SakaryaAttractor<Flt64> = SakaryaAttractor(alpha, beta, h)
    }
}

/**
 * Sakarya attractor generator that iteratively produces chaotic sequences.
 * Sakarya 吸引子生成器，通过迭代产生混沌序列。
 *
 * @property attractor the Sakarya attractor instance / Sakarya 吸引子实例
 * @property _x the internal state variable for iteration / 迭代用的内部状态变量
*/
data class SakaryaAttractorGenerator(
    val attractor: SakaryaAttractor<Flt64> = SakaryaAttractor(),
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
