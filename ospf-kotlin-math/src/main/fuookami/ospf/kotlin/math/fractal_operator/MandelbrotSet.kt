/**
 * MandelbrotSet（Mandelbrot 集）
 * Mandelbrot Set
 *
 * 提供 Mandelbrot 集迭代运算的实现。
 * Provides implementation of Mandelbrot set iteration operations.
 *
 * 主要功能，
 * Main features:
 * - MandelbrotSet: Mandelbrot 集迭代函敌z -> z^2 + c / Mandelbrot set iteration function z -> z^2 + c
 * - MandelbrotSetGenerator: Mandelbrot 集序列生成器 / Mandelbrot set sequence generator
 *
 * Mandelbrot 集定义：对于复数 c，迭仌z_{n+1} = z_n^2 + c，
 * 若序列不发散（|z| <= 2），刌c 属于 Mandelbrot 集。
 * Mandelbrot set definition: For complex number c, iterate z_{n+1} = z_n^2 + c,
 * if the sequence does not diverge (|z| <= 2), then c belongs to the Mandelbrot set.
 *
 * 应用场景：分形图形生成、复杂动力学研究、数学可视化等。
 * Applications: fractal graphics generation, complex dynamics research, mathematical visualization, etc.
 */
package fuookami.ospf.kotlin.math.fractal_operator

import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.value_range.*
import fuookami.ospf.kotlin.utils.functional.Generator
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.geometry.Point
import fuookami.ospf.kotlin.math.geometry.Dim2
import fuookami.ospf.kotlin.math.geometry.point2

/**
 * Mandelbrot 集迭代函敌
 * Mandelbrot Set iteration function
 */
class MandelbrotSet<V : FloatingNumber<V>>(
    val c: Point<Dim2, V>
) {
    companion object {
        operator fun invoke(real: Flt64, imag: Flt64): MandelbrotSet<fuookami.ospf.kotlin.math.algebra.number.Flt64> {
            return MandelbrotSet(point2(real, imag))
        }
    }

    operator fun invoke(z: Point<Dim2, V>): Point<Dim2, V> {
        val two = z[0].constants.two
        val real = z[0].pow(2) - z[1].pow(2) + c[0]
        val imag = two * z[0] * z[1] + c[1]
        return Point<Dim2, V>(listOf(real, imag), Dim2)
    }
}

/**
 * Mandelbrot 集序列生成器
 * Mandelbrot Set sequence generator
 */
data class MandelbrotSetGenerator(
    val mandelbrotSet: MandelbrotSet<fuookami.ospf.kotlin.math.algebra.number.Flt64> = MandelbrotSet(Flt64.one, Flt64.one),
    private var _z: Point<Dim2, Flt64> = point2()
) : Generator<Point<Dim2, Flt64>> {
    companion object {
        operator fun invoke(
            real: Flt64,
            imag: Flt64,
            z: Point<Dim2, Flt64> = point2()
        ): MandelbrotSetGenerator {
            return MandelbrotSetGenerator(MandelbrotSet(real, imag), z)
        }
    }

    val z by ::_z

    override fun invoke(): Point<Dim2, Flt64> {
        val z = _z.copy()
        _z = mandelbrotSet(_z)
        return z
    }
}
