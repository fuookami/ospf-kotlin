/**
 * 陈氏系统
 * Chen System
 *
 * 陈氏系统是由 Guanrong Chen 于 1999 年提出的三维连续时间混沌系统。
 * 该系统与 Lorenz 系统结构相似但具有不同的混沌行为，是研究混沌控制的重要模型。
 * 常用于混沌控制研究、混沌同步和混沌加密应用。
 *
 * The Chen system is a three-dimensional continuous-time chaotic system proposed by Guanrong Chen in 1999.
 * This system has a similar structure to the Lorenz system but exhibits different chaotic behavior, serving as an important model for chaos control research.
 * Commonly used for chaos control research, chaos synchronization, and chaos encryption applications.
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
 * 陈氏系统
 * Chen System
 */
data class ChenSystem(
    val a: Flt64 = Flt64(10.0),
    val b: Flt64 = Flt64(8.0 / 3.0),
    val c: Flt64 = Flt64(137.0 / 5.0),
    val h: Flt64 = Flt64(0.01)
) : Extractor<Point3, Point3> {
    override operator fun invoke(x: Point3): Point3 {
        val dx = a * (x[1] - x[0])
        val dy = (c - a) * x[0] - x[0] * x[2] + c * x[1]
        val dz = x[0] * x[1] - b * x[2]
        return point3(
            x[0] + h * dx,
            x[1] + h * dy,
            x[2] + h * dz
        )
    }
}

/**
 * 陈氏系统生成器
 * Chen System Generator
 */
data class ChenSystemGenerator(
    val chenSystem: ChenSystem = ChenSystem(),
    private var _x: Point3 = point3(
        Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
        Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
        Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
    )
) : Generator<Point3> {
    companion object {
        operator fun invoke(
            a: Flt64 = Flt64(10.0),
            b: Flt64 = Flt64(8.0 / 3.0),
            c: Flt64 = Flt64(137.0 / 5.0),
            h: Flt64 = Flt64(0.01),
            x: Point3 = point3(
                Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
                Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
                Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
            )
        ): ChenSystemGenerator {
            return ChenSystemGenerator(
                ChenSystem(
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

    override operator fun invoke(): Point3 {
        val x = _x.copy()
        _x = chenSystem(x)
        return x
    }
}






