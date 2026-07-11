/**
 * Chen-Lee 吸引孌
 * Chen-Lee Attractor
 *
 * Chen-Lee 吸引子是甌Chen 和Lee 提出的三维混沌系统。
 * 该系统具有独特的非线性结构，展现出丰富的混沌动力学行为。
 * 常用于混沌动力学研究、混沌加密和混沌同步应用。
 *
 * The Chen-Lee attractor is a three-dimensional chaotic system proposed by Chen and Lee.
 * This system features unique nonlinear structures, exhibiting rich chaotic dynamical behavior.
 * Commonly used for chaos dynamics research, chaos encryption, and chaos synchronization applications.
*/
package fuookami.ospf.kotlin.math.chaotic

import kotlin.random.Random
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.geometry.*
import fuookami.ospf.kotlin.math.nextFlt64

/**
 * Chen-Lee 吸引子
 * Chen-Lee Attractor
 *
 * @property alpha 系统参数 alpha / System parameter alpha
 * @property beta 系统参数 beta / System parameter beta
 * @property delta 系统参数 delta / System parameter delta
 * @property h 时间步长 / Time step size
*/
data class ChenLeeAttractor<V : FloatingNumber<V>>(
    val alpha: V,
    val beta: V,
    val delta: V,
    val h: V
) : Extractor<Point<Dim3, V>, Point<Dim3, V>> {
    override operator fun invoke(x: Point<Dim3, V>): Point<Dim3, V> {
        val v = alpha
        val dx = alpha * x[0] - x[1] * x[2]
        val dy = beta * x[1] + x[0] * x[2]
        val dz = delta * x[2] + x[0] * x[1] / v.constants.three
        return Point<Dim3, V>(listOf(x[0] + h * dx, x[1] + h * dy, x[2] + h * dz), Dim3)
    }

    companion object {
        operator fun invoke(
            alpha: Flt64 = Flt64(5.0),
            beta: Flt64 = Flt64(-10.0),
            delta: Flt64 = Flt64(0.38),
            h: Flt64 = Flt64(0.01)
        ): ChenLeeAttractor<Flt64> {
            return ChenLeeAttractor(alpha, beta, delta, h)
        }
    }
}

/**
 * Chen-Lee 吸引子生成器
 * Chen-Lee Attractor Generator
*/
data class ChenLeeAttractorGenerator(
    val chenLeeAttractor: ChenLeeAttractor<Flt64> = ChenLeeAttractor(),
    private var _x: Point<Dim3, Flt64> = point3(
        Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
        Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
        Random.nextFlt64(Flt64.decimalPrecision, Flt64.one)
    )
) : Generator<Point<Dim3, Flt64>> {
    companion object {
        operator fun invoke(
            alpha: Flt64 = Flt64(5.0),
            beta: Flt64 = Flt64(-10.0),
            delta: Flt64 = Flt64(0.38),
            h: Flt64 = Flt64(0.01),
            x: Point<Dim3, Flt64> = point3(
                Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
                Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
                Random.nextFlt64(Flt64.decimalPrecision, Flt64.one)
            )
        ): ChenLeeAttractorGenerator {
            return ChenLeeAttractorGenerator(
                ChenLeeAttractor(alpha, beta, delta, h),
                x
            )
        }
    }

    val x by ::_x

    override operator fun invoke(): Point<Dim3, Flt64> {
        val x = _x.copy()
        _x = chenLeeAttractor(x)
        return x
    }
}
