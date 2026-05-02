/**
 * Bouali 吸引子
 * Bouali Attractor
 *
 * Bouali 吸引子是一个三维混沌系统，由 Bouali 提出。
 * 该系统展现出复杂的混沌轨迹，具有独特的吸引子几何结构。
 * 常用于混沌信号生成、非线性动力学研究和混沌加密应用。
 *
 * The Bouali attractor is a three-dimensional chaotic system proposed by Bouali.
 * This system exhibits complex chaotic trajectories with unique attractor geometric structures.
 * Commonly used for chaotic signal generation, nonlinear dynamics research, and chaos encryption applications.
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
 * Bouali 吸引子
 * Bouali Attractor
 */
data class BoualiAttractor(
    val alpha: Flt64 = Flt64(0.3),
    val zeta: Flt64 = Flt64.one,
    val h: Flt64 = Flt64(0.01)
) : Extractor<Point3, Point3> {
    override operator fun invoke(x: Point3): Point3 {
        val dx = x[0] * (Flt64(4) - x[1]) + alpha * x[2]
        val dy = -x[1] * (Flt64.one - x[0].sqr())
        val dz = -x[0] * (Flt64(1.5) - zeta * x[2]) - Flt64(0.05) * x[2]
        return point3(
            x[0] + h * dx,
            x[1] + h * dy,
            x[2] + h * dz
        )
    }
}

/**
 * Bouali 吸引子生成器
 * Bouali Attractor Generator
 */
data class BoualiAttractorGenerator(
    val boualiAttractor: BoualiAttractor = BoualiAttractor(),
    private var _x: Point3 = point3(
        Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
        Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
        Random.nextFlt64(Flt64.decimalPrecision, Flt64.one)
    )
) : Generator<Point3> {
    companion object {
        operator fun invoke(
            alpha: Flt64 = Flt64(0.3),
            zeta: Flt64 = Flt64.one,
            h: Flt64 = Flt64(0.01),
            x: Point3 = point3(
                Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
                Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
                Random.nextFlt64(Flt64.decimalPrecision, Flt64.one)
            )
        ): BoualiAttractorGenerator {
            return BoualiAttractorGenerator(
                BoualiAttractor(
                    alpha = alpha,
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
        _x = boualiAttractor(x)
        return x
    }
}
