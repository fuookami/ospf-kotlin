/**
 * Coullet 吸引�?
 * Coullet Attractor
 *
 * Coullet 吸引子是�?Coullet 提出的三维混沌系统，具有三次非线性项�?
 * 该系统是研究低维混沌系统的重要模型，展现出典型的混沌动力学特性�?
 * 常用于混沌动力学研究、非线性系统分析和教学演示�?
 *
 * The Coullet attractor is a three-dimensional chaotic system proposed by Coullet, featuring cubic nonlinear terms.
 * This system serves as an important model for studying low-dimensional chaotic systems, exhibiting typical chaotic dynamical properties.
 * Commonly used for chaos dynamics research, nonlinear systems analysis, and educational demonstrations.
 */
package fuookami.ospf.kotlin.math.chaotic_operator

import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.value_range.*

import fuookami.ospf.kotlin.utils.functional.Extractor
import fuookami.ospf.kotlin.utils.functional.Generator
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.geometry.Point
import fuookami.ospf.kotlin.math.geometry.Dim3
import fuookami.ospf.kotlin.math.geometry.point3
import fuookami.ospf.kotlin.math.nextFlt64
import kotlin.random.Random

data class CoulletAttractor<V : FloatingNumber<V>>(
    val alpha: V,
    val beta: V,
    val delta: V,
    val zeta: V,
    val h: V
) : Extractor<Point<Dim3, V>, Point<Dim3, V>> {
    override operator fun invoke(x: Point<Dim3, V>): Point<Dim3, V> {
        val dx = x[1]
        val dy = x[2]
        val dz = alpha * x[0] + beta * x[1] + delta * x[2] + delta * x[0].cub()
        return Point<Dim3, V>(listOf(x[0] + h * dx, x[1] + h * dy, x[2] + h * dz), Dim3)
    }

    companion object {
        operator fun invoke(
            alpha: Flt64 = Flt64(0.8),
            beta: Flt64 = Flt64(-1.1),
            delta: Flt64 = Flt64(-1.0),
            zeta: Flt64 = Flt64(-0.45),
            h: Flt64 = Flt64(0.01)
        ): CoulletAttractor<fuookami.ospf.kotlin.math.algebra.number.Flt64> {
            return CoulletAttractor(alpha, beta, delta, zeta, h)
        }
    }
}

data class CoulletAttractorGenerator(
    val coulletAttractor: CoulletAttractor<fuookami.ospf.kotlin.math.algebra.number.Flt64> = CoulletAttractor(),
    private var _x: Point<Dim3, Flt64> = point3(
        Random.nextFlt64(Flt64.zero, Flt64.one),
        Random.nextFlt64(Flt64.zero, Flt64.one),
        Random.nextFlt64(Flt64.zero, Flt64.one)
    )
) : Generator<Point<Dim3, Flt64>> {
    companion object {
        operator fun invoke(
            alpha: Flt64 = Flt64(0.8),
            beta: Flt64 = Flt64(-1.1),
            delta: Flt64 = Flt64(-1.0),
            zeta: Flt64 = Flt64(-0.45),
            h: Flt64 = Flt64(0.01),
            x: Point<Dim3, Flt64> = point3(
                Random.nextFlt64(Flt64.zero, Flt64.one),
                Random.nextFlt64(Flt64.zero, Flt64.one),
                Random.nextFlt64(Flt64.zero, Flt64.one)
            )
        ): CoulletAttractorGenerator {
            return CoulletAttractorGenerator(
                CoulletAttractor(alpha, beta, delta, zeta, h),
                x
            )
        }
    }

    val x by ::_x

    override operator fun invoke(): Point<Dim3, Flt64> {
        val x = _x.copy()
        _x = coulletAttractor(x)
        return x
    }
}
