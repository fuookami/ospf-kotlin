/**
 * MandelbrotSet（Mandelbrot 集）
 * Mandelbrot Set
 *
 * 提供 Mandelbrot 集迭代运算的实现。
 * Provides implementation of Mandelbrot set iteration operations.
 *
 * 主要功能：
 * Main features:
 * - MandelbrotSet: Mandelbrot 集迭代函数 z -> z^2 + c / Mandelbrot set iteration function z -> z^2 + c
 * - MandelbrotSetGenerator: Mandelbrot 集序列生成器 / Mandelbrot set sequence generator
 *
 * Mandelbrot 集定义：对于复数 c，迭代 z_{n+1} = z_n^2 + c，
 * 若序列不发散（|z| <= 2），则 c 属于 Mandelbrot 集。
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
import fuookami.ospf.kotlin.math.geometry.Point2
import fuookami.ospf.kotlin.math.geometry.point2

class MandelbrotSet(
    val c: Point2 = Point2(Flt64.one, Flt64.one)
) {
    companion object {
        operator fun invoke(real: Flt64, imag: Flt64): MandelbrotSet {
            return MandelbrotSet(Point2(real, imag))
        }
    }

    operator fun invoke(z: Point2): Point2 {
        val real = z[0].pow(2) - z[1].pow(2) + c[0]
        val imag = Flt64.two * z[0] * z[1] + c[1]
        return Point2(real, imag)
    }
}

data class MandelbrotSetGenerator(
    val mandelbrotSet: MandelbrotSet = MandelbrotSet(),
    private var _z: Point2 = point2()
) : Generator<Point2> {
    companion object {
        operator fun invoke(
            real: Flt64,
            imag: Flt64,
            z: Point2 = point2()
        ): MandelbrotSetGenerator {
            return MandelbrotSetGenerator(MandelbrotSet(Point2(real, imag)), z)
        }
    }

    val z by ::_z

    override fun invoke(): Point2 {
        val z = _z.copy()
        _z = mandelbrotSet(_z)
        return z
    }
}







