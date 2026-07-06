/**
 * Sprott 吸引子
 * Sprott Attractor
 *
 * Sprott 吸引子是一类简单的混沌吸引子系统，
 * 通过最少的非线性项产生混沌动力学行为。
 * 常用于混沌动力学研究、简单混沌系统分析和时间序列生成。
 *
 * The Sprott attractor is a class of simple chaotic attractor systems
 * that produce chaotic dynamics through minimal nonlinear terms.
 * Commonly used for chaotic dynamics research, simple chaotic system analysis, and time series generation.
 */
package fuookami.ospf.kotlin.math.chaotic

import kotlin.random.Random
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.geometry.*
import fuookami.ospf.kotlin.math.nextFlt64

/**
 * Sprott 吸引子
 * Sprott Attractor
 *
 * @property a 系统参数 a / System parameter a
 * @property b 系统参数 b / System parameter b
 * @property c 系统参数 c / System parameter c
 * @property h 时间步长 / Time step size
 */
data class SprottAttractor<V : FloatingNumber<V>>(
    val a: V,
    val b: V,
    val c: V,
    val h: V
) : Extractor<Point<Dim3, V>, Point<Dim3, V>> {
    override operator fun invoke(p: Point<Dim3, V>): Point<Dim3, V> {
        val x = p[0]
        val y = p[1]
        val z = p[2]
        val dx = a * (y - x)
        val dy = y * (c - x) - y
        val dz = x * y - c * z
        return Point<Dim3, V>(listOf(x + h * dx, y + h * dy, z + h * dz), Dim3)
    }

    companion object {
        operator fun invoke(
            a: Flt64 = Flt64(10.0),
            b: Flt64 = Flt64(28.0),
            c: Flt64 = Flt64(8.0) / Flt64(3.0),
            h: Flt64 = Flt64(0.01)
        ): SprottAttractor<Flt64> = SprottAttractor(a, b, c, h)
    }
}

/**
 * Sprott 吸引子生成器
 * Sprott Attractor Generator
 *
 * @property attractor Sprott 吸引子实例 / Sprott attractor instance
 */
data class SprottAttractorGenerator(
    val attractor: SprottAttractor<Flt64> = SprottAttractor(),
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
