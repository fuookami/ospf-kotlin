/**
 * Rucklidge 吸引子
 * Rucklidge Attractor
*/
package fuookami.ospf.kotlin.math.chaotic

import kotlin.random.Random
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.geometry.*
import fuookami.ospf.kotlin.math.nextFlt64

/**
 * Rucklidge 吸引子
 * Rucklidge Attractor
 *
 * @property alpha 系统参数 alpha / System parameter alpha
 * @property kappa 系统参数 kappa / System parameter kappa
 * @property h 时间步长 / Time step size
*/
data class RucklidgeAttractor<V : FloatingNumber<V>>(val alpha: V, val kappa: V, val h: V) :
    Extractor<Point<Dim3, V>, Point<Dim3, V>> {
    override operator fun invoke(p: Point<Dim3, V>): Point<Dim3, V> {
        val x = p[0];
        val y = p[1];
        val z = p[2]
        val dx = -kappa * x + alpha * y - y * z
        val dy = x
        val dz = -z + y * y
        return Point<Dim3, V>(listOf(x + h * dx, y + h * dy, z + h * dz), Dim3)
    }

    companion object {
        operator fun invoke(
            alpha: Flt64 = Flt64(2.0),
            kappa: Flt64 = Flt64(6.7),
            h: Flt64 = Flt64(0.01)
        ): RucklidgeAttractor<Flt64> = RucklidgeAttractor(alpha, kappa, h)
    }
}

/**
 * Rucklidge attractor generator that iteratively produces chaotic sequences.
 * Rucklidge 吸引子生成器，通过迭代产生混沌序列。
 *
 * @property attractor the Rucklidge attractor instance / Rucklidge 吸引子实例
 * @property _x the internal state variable for iteration / 迭代用的内部状态变量
*/
data class RucklidgeAttractorGenerator(
    val attractor: RucklidgeAttractor<Flt64> = RucklidgeAttractor(),
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
