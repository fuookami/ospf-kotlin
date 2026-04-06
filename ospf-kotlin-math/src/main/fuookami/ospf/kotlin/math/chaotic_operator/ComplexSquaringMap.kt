/**
 * 复平方映射
 * Complex Squaring Map
 *
 * 复平方映射是将复数平方的简单迭代映射，是 Julia 理论研究的基础。
 * 该映射通过不断平方复数，产生逃逸或收敛的动力学行为，形成独特的分形边界。
 * 常用于分形几何研究、混沌可视化和复动力学分析。
 *
 * The complex squaring map is a simple iterative map that squares complex numbers, serving as the foundation for Julia set theory research.
 * This map generates escaping or converging dynamical behavior through repeated squaring of complex numbers, forming unique fractal boundaries.
 * Commonly used for fractal geometry research, chaos visualization, and complex dynamics analysis.
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

data object ComplexSquaringMap : Extractor<Point2, Point2> {
    override operator fun invoke(x: Point2): Point2 {
        val complexNumber = pow(complex(x[0].value, x[1].value), complex(2.0, 0.0))
        return Point2(
            Flt64(complexNumber.re),
            Flt64(complexNumber.im)
        )
    }
}

data class ComplexSquaringMapGenerator(
    val complexSquaringMap: ComplexSquaringMap = ComplexSquaringMap,
    private var _x: Point2 = point2(
        Random.nextFlt64(-Flt64.one, Flt64.one),
        Random.nextFlt64(-Flt64.one, Flt64.one)
    )
) : Generator<Point2> {
    companion object {
        operator fun invoke(
            complexSquaringMap: ComplexSquaringMap = ComplexSquaringMap,
            x: Point2 = point2(
                Random.nextFlt64(-Flt64.one, Flt64.one),
                Random.nextFlt64(-Flt64.one, Flt64.one)
            )
        ): ComplexSquaringMapGenerator {
            return ComplexSquaringMapGenerator(
                complexSquaringMap,
                x
            )
        }
    }

    val x by ::_x

    override operator fun invoke(): Point2 {
        val x = _x.copy()
        _x = ComplexSquaringMap(x)
        return x
    }
}







