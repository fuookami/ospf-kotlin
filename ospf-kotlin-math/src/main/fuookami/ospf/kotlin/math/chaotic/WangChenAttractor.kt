/**
 * 王-陈吸引子
 * Wang-Chen Attractor
 *
 * 王-陈吸引子是一个四维超混沌吸引子系统，
 * 通过非线性耦合项产生复杂的混沌动力学行为。
 * 常用于超混沌系统分析、加密通信和复杂动力学研究。
 *
 * The Wang-Chen attractor is a four-dimensional hyperchaotic attractor system
 * that produces complex chaotic dynamics through nonlinear coupling terms.
 * Commonly used for hyperchaotic system analysis, encrypted communication, and complex dynamics research.
*/
package fuookami.ospf.kotlin.math.chaotic

import kotlin.random.Random
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.geometry.*
import fuookami.ospf.kotlin.math.nextFlt64

/**
 * 王-陈吸引子
 * Wang-Chen Attractor
 *
 * @property alpha 系统参数 alpha（x 与 y 之间线性耦合系数）/ System parameter alpha (linear coupling coefficient between x and y)
 * @property beta 系统参数 beta（x 方向线性增益系数）/ System parameter beta (linear gain coefficient in x direction)
 * @property gamma 系统参数 gamma（x 与 z 之间非线性耦合系数）/ System parameter gamma (nonlinear coupling coefficient between x and z)
 * @property delta 系统参数 delta（z 方向耗散系数）/ System parameter delta (dissipation coefficient in z direction)
 * @property h 时间步长 / Time step size
*/
data class WangChenAttractor<V : FloatingNumber<V>>(
    val alpha: V,
    val beta: V,
    val gamma: V,
    val delta: V,
    val h: V
) : Extractor<Point<Dim4, V>, Point<Dim4, V>> {
    override operator fun invoke(p: Point<Dim4, V>): Point<Dim4, V> {
        val x = p[0]
        val y = p[1]
        val z = p[2]
        val w = p[3]
        val dx = alpha * (y - x)
        val dy = beta * x - gamma * x * z + w
        val dz = x * y - delta * z
        val dw = -y * z
        return Point<Dim4, V>(listOf(x + h * dx, y + h * dy, z + h * dz, w + h * dw), Dim4)
    }

    companion object {
        operator fun invoke(
            alpha: Flt64 = Flt64(10.0),
            beta: Flt64 = Flt64(40.0),
            gamma: Flt64 = Flt64(1.0),
            delta: Flt64 = Flt64(2.5),
            h: Flt64 = Flt64(0.01)
        ): WangChenAttractor<Flt64> = WangChenAttractor(alpha, beta, gamma, delta, h)
    }
}

/**
 * 王-陈吸引子生成器
 * Wang-Chen Attractor Generator
 *
 * @property attractor 王-陈吸引子实例 / Wang-Chen attractor instance
*/
data class WangChenAttractorGenerator(
    val attractor: WangChenAttractor<Flt64> = WangChenAttractor(),
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
