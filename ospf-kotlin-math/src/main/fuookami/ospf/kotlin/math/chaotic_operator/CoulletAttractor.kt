/**
 * Coullet 吸引子
 * Coullet Attractor
 *
 * Coullet 吸引子是由 Coullet 提出的三维混沌系统，具有三次非线性项。
 * 该系统是研究低维混沌系统的重要模型，展现出典型的混沌动力学特性。
 * 常用于混沌动力学研究、非线性系统分析和教学演示。
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
import fuookami.ospf.kotlin.math.geometry.Point3
import fuookami.ospf.kotlin.math.geometry.point3
import fuookami.ospf.kotlin.math.nextFlt64
import kotlin.random.Random

/**
 * Coullet 吸引子
 * Coullet Attractor
 */
data class CoulletAttractor(
    val alpha: Flt64 = Flt64(0.8),
    val beta: Flt64 = Flt64(-1.1),
    val delta: Flt64 = Flt64(-1.0),
    val zeta: Flt64 = Flt64(-0.45),
    val h: Flt64 = Flt64(0.01)
) : Extractor<Point3, Point3> {
    override operator fun invoke(x: Point3): Point3 {
        val dx = x[1]
        val dy = x[2]
        val dz = alpha * x[0] + beta * x[1] + delta * x[2] + delta * x[0].cub()
        return point3(
            x[0] + h * dx,
            x[1] + h * dy,
            x[2] + h * dz
        )
    }
}

/**
 * Coullet 吸引子生成器
 * Coullet Attractor Generator
 */
data class CoulletAttractorGenerator(
    val coulletAttractor: CoulletAttractor = CoulletAttractor(),
    private var _x: Point3 = point3(
        Random.nextFlt64(Flt64.zero, Flt64.one),
        Random.nextFlt64(Flt64.zero, Flt64.one),
        Random.nextFlt64(Flt64.zero, Flt64.one)
    )
) : Generator<Point3> {
    companion object {
        operator fun invoke(
            alpha: Flt64 = Flt64(0.8),
            beta: Flt64 = Flt64(-1.1),
            delta: Flt64 = Flt64(-1.0),
            zeta: Flt64 = Flt64(-0.45),
            h: Flt64 = Flt64(0.01),
            x: Point3 = point3(
                Random.nextFlt64(Flt64.zero, Flt64.one),
                Random.nextFlt64(Flt64.zero, Flt64.one),
                Random.nextFlt64(Flt64.zero, Flt64.one)
            )
        ): CoulletAttractorGenerator {
            return CoulletAttractorGenerator(
                CoulletAttractor(
                    alpha = alpha,
                    beta = beta,
                    delta = delta,
                    zeta = zeta,
                    h = h
                ),
                x
            )
        }
    }

    val x by ::_x

    override operator fun invoke(): Point3 {
        val x = _x.copy()
        _x = coulletAttractor(x)
        return x
    }
}






