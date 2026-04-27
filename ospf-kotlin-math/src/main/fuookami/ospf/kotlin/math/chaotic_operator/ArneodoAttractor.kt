/**
 * Arneodo 吸引子
 * Arneodo Attractor
 *
 * Arneodo 吸引子是一个三维连续时间混沌系统，由 Arneodo 等人提出。
 * 该系统具有三次非线性项，是研究低维混沌系统的重要模型。
 * 常用于混沌动力学基础研究和教学演示。
 *
 * The Arneodo attractor is a three-dimensional continuous-time chaotic system proposed by Arneodo et al.
 * This system features cubic nonlinear terms and is an important model for studying low-dimensional chaotic systems.
 * Commonly used for fundamental chaos dynamics research and educational demonstrations.
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
 * Arneodo 吸引子
 * Arneodo Attractor
 */
data class ArneodoAttractor(
    val alpha: Flt64 = Flt64(-5.5),
    val beta: Flt64 = Flt64(3.5),
    val delta: Flt64 = Flt64(-1.0),
    val h: Flt64 = Flt64(0.01)
) : Extractor<Point3, Point3> {
    override operator fun invoke(x: Point3): Point3 {
        val dx = x[1]
        val dy = x[2]
        val dz = -alpha * x[0] - beta * x[1] - x[2] + delta * x[0].cub()
        return point3(
            x[0] + h * dx,
            x[1] + h * dy,
            x[2] + h * dz
        )
    }
}

/**
 * Arneodo 吸引子生成器
 * Arneodo Attractor Generator
 */
data class ArneodoAttractorGenerator(
    val arneodoAttractor: ArneodoAttractor = ArneodoAttractor(),
    private var _x: Point3 = point3(
        Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
        Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
        Random.nextFlt64(Flt64.decimalPrecision, Flt64.one)
    )
) : Generator<Point3> {
    companion object {
        operator fun invoke(
            alpha: Flt64 = Flt64(-5.5),
            beta: Flt64 = Flt64(3.5),
            delta: Flt64 = Flt64(-1.0),
            x: Point3 = point3(
                Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
                Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
                Random.nextFlt64(Flt64.decimalPrecision, Flt64.one)
            )
        ): ArneodoAttractorGenerator {
            return ArneodoAttractorGenerator(
                ArneodoAttractor(
                    alpha = alpha,
                    beta = beta,
                    delta = delta
                ),
                x
            )
        }
    }

    val x by ::_x

    override operator fun invoke(): Point3 {
        val x = _x
        _x = arneodoAttractor(x)
        return x
    }
}






