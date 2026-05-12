/**
 * 洛伦兹系绌
 * Lorenz System
 *
 * 洛伦兹系统是甌Edward Lorenz 二1963 年提出的著名三维混沌系统。
 * 该系统最初用于研究大气对流，意外发现了混沌现象，开创了混沌理论的研究。
 * 洛伦兹吸引子（蝴蝶效应）是最著名的混沌系统之一，常用于混沌动力学研究、气象模型分析和教学演示。
 *
 * The Lorenz system is a famous three-dimensional chaotic system proposed by Edward Lorenz in 1963.
 * This system was originally used for studying atmospheric convection, unexpectedly discovering chaotic phenomena, pioneering chaos theory research.
 * The Lorenz attractor (butterfly effect) is one of the most famous chaotic systems, commonly used for chaos dynamics research, meteorological model analysis, and educational demonstrations.
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

/**
 * 洛伦兹系绌
 * Lorenz System
 */
data class LorenzSystem<V : FloatingNumber<V>>(
    val a: V,
    val b: V,
    val c: V,
    val h: V
) : Extractor<Point<Dim3, V>, Point<Dim3, V>> {
    override operator fun invoke(x: Point<Dim3, V>): Point<Dim3, V> {
        val dx = a * (x[1] - x[0])
        val dy = c * x[0] - x[0] * x[2] - x[1]
        val dz = x[0] * x[1] - b * x[2]
        return Point<Dim3, V>(listOf(x[0] + h * dx, x[1] + h * dy, x[2] + h * dz), Dim3)
    }

    companion object {
        operator fun invoke(
            a: Flt64 = Flt64(10.0),
            b: Flt64 = Flt64(28.0),
            c: Flt64 = Flt64(8.0 / 3.0),
            h: Flt64 = Flt64(0.01)
        ): LorenzSystem<fuookami.ospf.kotlin.math.algebra.number.Flt64> {
            return LorenzSystem(a, b, c, h)
        }
    }
}

/**
 * 洛伦兹系统生成器
 * Lorenz System Generator
 */
data class LorenzSystemGenerator(
    val lorenzSystem: LorenzSystem<fuookami.ospf.kotlin.math.algebra.number.Flt64> = LorenzSystem(),
    private var _x: Point<Dim3, Flt64> = point3(
        Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
        Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
        Random.nextFlt64(Flt64.decimalPrecision, Flt64.one)
    )
) : Generator<Point<Dim3, Flt64>> {
    companion object {
        operator fun invoke(
            a: Flt64,
            b: Flt64,
            c: Flt64,
            h: Flt64,
            x: Point<Dim3, Flt64> = point3(
                Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
                Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
                Random.nextFlt64(Flt64.decimalPrecision, Flt64.one)
            )
        ): LorenzSystemGenerator {
            return LorenzSystemGenerator(
                LorenzSystem(
                    a = a,
                    b = b,
                    c = c,
                    h = h
                ),
                x
            )
        }
    }

    val x by ::_x

    override operator fun invoke(): Point<Dim3, Flt64> {
        val x = _x.copy()
        _x = lorenzSystem(_x)
        return x
    }
}
