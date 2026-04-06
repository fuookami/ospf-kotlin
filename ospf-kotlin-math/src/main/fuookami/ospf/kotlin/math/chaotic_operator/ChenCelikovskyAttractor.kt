/**
 * Chen-Celikovsky 吸引子
 * Chen-Celikovsky Attractor
 *
 * Chen-Celikovsky 吸引子是 Chen 系统的一种变体，由 Chen 和 Celikovsky 提出。
 * 该系统在 Lorenz 系统和 Chen 系统之间建立起桥梁，展现出独特的混沌动力学特性。
 * 常用于混沌系统研究、控制理论和非线性动力学分析。
 *
 * The Chen-Celikovsky attractor is a variant of the Chen system proposed by Chen and Celikovsky.
 * This system builds a bridge between the Lorenz system and the Chen system, exhibiting unique chaotic dynamical properties.
 * Commonly used for chaotic system research, control theory, and nonlinear dynamics analysis.
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

data class ChenCelikovskyAttractor(
    val alpha: Flt64 = Flt64(36.0),
    val beta: Flt64 = Flt64(3.0),
    val delta: Flt64 = Flt64(20.0),
    val h: Flt64 = Flt64(0.01)
) : Extractor<Point3, Point3> {
    override operator fun invoke(x: Point3): Point3 {
        val dx = alpha * (x[1] - x[0])
        val dy = -x[0] * x[2] + delta * x[1]
        val dz = x[0] * x[1] - beta * x[2]
        return point3(
            x[0] + h * dx,
            x[1] + h * dy,
            x[2] + h * dz
        )
    }
}

data class ChenCelikovskyAttractorGenerator(
    val chenCelikovskyAttractor: ChenCelikovskyAttractor = ChenCelikovskyAttractor(),
    private var _x: Point3 = point3(
        Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
        Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
        Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
    )
) : Generator<Point3> {
    companion object {
        operator fun invoke(
            alpha: Flt64 = Flt64(36.0),
            beta: Flt64 = Flt64(3.0),
            delta: Flt64 = Flt64(20.0),
            h: Flt64 = Flt64(0.01),
            x: Point3 = point3(
                Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
                Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
                Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
            )
        ): ChenCelikovskyAttractorGenerator {
            return ChenCelikovskyAttractorGenerator(
                ChenCelikovskyAttractor(
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
        _x = chenCelikovskyAttractor(x)
        return x
    }
}






