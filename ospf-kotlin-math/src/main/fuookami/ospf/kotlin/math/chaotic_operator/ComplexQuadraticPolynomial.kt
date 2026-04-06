/**
 * 复二次多项式
 * Complex Quadratic Polynomial
 *
 * 复二次多项式是复数域上的迭代映射，是 Mandelbrot 集和 Julia 集研究的基础。
 * 通过迭代复二次多项式可以生成丰富的分形结构，展现出复杂的混沌动力学行为。
 * 常用于分形几何研究、混沌可视化分析和复动力学理论研究。
 *
 * The complex quadratic polynomial is an iterative map on the complex domain, serving as the foundation for Mandelbrot set and Julia set research.
 * Iterating complex quadratic polynomials generates rich fractal structures, exhibiting complex chaotic dynamical behavior.
 * Commonly used for fractal geometry research, chaos visualization analysis, and complex dynamics theory research.
 */
package fuookami.ospf.kotlin.math.chaotic_operator

import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.value_range.*

import fuookami.ospf.kotlin.utils.functional.Extractor
import fuookami.ospf.kotlin.utils.functional.Generator
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.geometry.Point2
import fuookami.ospf.kotlin.math.geometry.point2
import fuookami.ospf.kotlin.math.nextFlt64
import org.kotlinmath.complex
import org.kotlinmath.pow
import kotlin.random.Random

data class ComplexQuadraticPolynomial(
    val c: Point2 = point2(
        Random.nextFlt64(-Flt64.one, Flt64.one),
        Random.nextFlt64(-Flt64.one, Flt64.one)
    ),
    val d: Flt64 = Flt64.two
) : Extractor<Point2, Point2> {
    override operator fun invoke(x: Point2): Point2 {
        val complexNumber = pow(complex(x[0].value, x[1].value), complex(d.value, 0.0)) + complex(c[0].value, c[1].value)
        return point2(
            Flt64(complexNumber.re),
            Flt64(complexNumber.im)
        )
    }
}

data class ComplexQuadraticPolynomialGenerator(
    val complexQuadraticPolynomial: ComplexQuadraticPolynomial = ComplexQuadraticPolynomial(),
    private var _x: Point2 = point2(
        Random.nextFlt64(-Flt64.one, Flt64.one),
        Random.nextFlt64(-Flt64.one, Flt64.one)
    )
) : Generator<Point2> {
    companion object {
        operator fun invoke(
            c: Point2 = point2(
                Random.nextFlt64(-Flt64.one, Flt64.one),
                Random.nextFlt64(-Flt64.one, Flt64.one)
            ),
            d: Flt64 = Flt64.two,
            x: Point2 = point2(
                Random.nextFlt64(-Flt64.one, Flt64.one),
                Random.nextFlt64(-Flt64.one, Flt64.one)
            )
        ): ComplexQuadraticPolynomialGenerator {
            return ComplexQuadraticPolynomialGenerator(
                ComplexQuadraticPolynomial(c, d),
                x
            )
        }
    }

    val x by ::_x

    override operator fun invoke(): Point2 {
        val x = _x.copy()
        _x = complexQuadraticPolynomial(x)
        return x
    }
}







