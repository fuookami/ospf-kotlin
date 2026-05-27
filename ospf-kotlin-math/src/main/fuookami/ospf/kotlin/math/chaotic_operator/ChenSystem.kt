/**
 * 陈氏系统
 * Chen System
 *
 * 陈氏系统是由 Guanrong Chen 二1999 年提出的三维连续时间混沌系统。
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
import fuookami.ospf.kotlin.math.geometry.Point
import fuookami.ospf.kotlin.math.geometry.Dim3
import fuookami.ospf.kotlin.math.nextFlt64
import fuookami.ospf.kotlin.math.geometry.point3
import kotlin.random.Random

/**
 * 陈氏系统
 * Chen System
 *
 * @property a 系统参数 a / System parameter a
 * @property b 系统参数 b / System parameter b
 * @property c 系统参数 c / System parameter c
 * @property h 时间步长 / Time step size
 */
data class ChenSystem<V : FloatingNumber<V>>(
    val a: V,
    val b: V,
    val c: V,
    val h: V
) : Extractor<Point<Dim3, V>, Point<Dim3, V>> {
    override operator fun invoke(x: Point<Dim3, V>): Point<Dim3, V> {
        val dx = a * (x[1] - x[0])
        val dy = (c - a) * x[0] - x[0] * x[2] + c * x[1]
        val dz = x[0] * x[1] - b * x[2]
        return Point<Dim3, V>(listOf(x[0] + h * dx, x[1] + h * dy, x[2] + h * dz), Dim3)
    }

    companion object {
        operator fun invoke(
            a: Flt64 = Flt64(10.0),
            b: Flt64 = Flt64(8.0 / 3.0),
            c: Flt64 = Flt64(137.0 / 5.0),
            h: Flt64 = Flt64(0.01)
        ): ChenSystem<fuookami.ospf.kotlin.math.algebra.number.Flt64> {
            return ChenSystem(a, b, c, h)
        }
    }
}

data class ChenSystemGenerator(
    val chenSystem: ChenSystem<fuookami.ospf.kotlin.math.algebra.number.Flt64> = ChenSystem(),
    private var _x: Point<Dim3, Flt64> = point3(
        Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
        Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
        Random.nextFlt64(Flt64.decimalPrecision, Flt64.one)
    )
) : Generator<Point<Dim3, Flt64>> {
    companion object {
        operator fun invoke(
            a: Flt64 = Flt64(10.0),
            b: Flt64 = Flt64(8.0 / 3.0),
            c: Flt64 = Flt64(137.0 / 5.0),
            h: Flt64 = Flt64(0.01),
            x: Point<Dim3, Flt64> = point3(
                Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
                Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
                Random.nextFlt64(Flt64.decimalPrecision, Flt64.one)
            )
        ): ChenSystemGenerator {
            return ChenSystemGenerator(
                ChenSystem(a, b, c, h),
                x
            )
        }
    }

    val x by ::_x

    override operator fun invoke(): Point<Dim3, Flt64> {
        val x = _x.copy()
        _x = chenSystem(x)
        return x
    }
}
