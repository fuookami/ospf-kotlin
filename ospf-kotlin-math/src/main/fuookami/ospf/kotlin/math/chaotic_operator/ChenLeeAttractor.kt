/**
 * Chen-Lee 吸引子
 * Chen-Lee Attractor
 *
 * Chen-Lee 吸引子是由 Chen 和 Lee 提出的三维混沌系统。
 * 该系统具有独特的非线性结构，展现出丰富的混沌动力学行为。
 * 常用于混沌动力学研究、混沌加密和混沌同步应用。
 *
 * The Chen-Lee attractor is a three-dimensional chaotic system proposed by Chen and Lee.
 * This system features unique nonlinear structures, exhibiting rich chaotic dynamical behavior.
 * Commonly used for chaos dynamics research, chaos encryption, and chaos synchronization applications.
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

data class ChenLeeAttractor(
    val alpha: Flt64 = Flt64(5.0),
    val beta: Flt64 = Flt64(-10.0),
    val delta: Flt64 = Flt64(0.38),
    val h: Flt64 = Flt64(0.01)
) : Extractor<Point3, Point3> {
    override operator fun invoke(x: Point3): Point3 {
        val dx = alpha * x[0] - x[1] * x[2]
        val dy = beta * x[1] + x[0] * x[2]
        val dz = delta * x[2] + x[0] * x[1] / Flt64(3.0)
        return point3(
            x[0] + h * dx,
            x[1] + h * dy,
            x[2] + h * dz
        )
    }
}

data class ChenLeeAttractorGenerator(
    val chenLeeAttractor: ChenLeeAttractor = ChenLeeAttractor(),
    private var _x: Point3 = point3(
        Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
        Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
        Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
    )
) : Generator<Point3> {
    companion object {
        operator fun invoke(
            alpha: Flt64 = Flt64(5.0),
            beta: Flt64 = Flt64(-10.0),
            delta: Flt64 = Flt64(0.38),
            h: Flt64 = Flt64(0.01),
            x: Point3 = point3(
                Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
                Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
                Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
            )
        ): ChenLeeAttractorGenerator {
            return ChenLeeAttractorGenerator(
                ChenLeeAttractor(
                    alpha = alpha,
                    beta = beta,
                    delta = delta,
                    h = h
                ),
                x
            )
        }
    }

    val x by ::_x

    override operator fun invoke(): Point3 {
        val x = _x.copy()
        _x = chenLeeAttractor(x)
        return x
    }
}






