/**
 * 电容方程
 * Capacitance Equation
 *
 * 电容方程是描述电容电路中非线性动力学行为的混沌模型�?
 * 该方程通过分段线性函数引入非线性，模拟电子电路中的混沌振荡现象�?
 * 常用于非线性电路分析、混沌电路设计和电子系统动力学研究�?
 *
 * The capacitance equation is a chaotic model describing nonlinear dynamical behavior in capacitive circuits.
 * This equation introduces nonlinearity through piecewise linear functions, simulating chaotic oscillation phenomena in electronic circuits.
 * Commonly used for nonlinear circuit analysis, chaotic circuit design, and electronic system dynamics research.
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

data class CapacitanceEquation<V : FloatingNumber<V>>(
    val a: V,
    val b: V,
    val c: V,
    val d: V,
    val e: V,
    val h: V
) : Extractor<Point<Dim3, V>, Point<Dim3, V>> {
    override operator fun invoke(x: Point<Dim3, V>): Point<Dim3, V> {
        val v = a
        val g = if (x[0] gr v.constants.one) {
            e * x[0] - (e - d)
        } else if (x[0] ls -v.constants.one) {
            e * x[1] + (e - d)
        } else {
            d * x[0]
        }
        val dx = a * ((c - v.constants.one) * g + x[1])
        val dy = g - x[1] + x[2]
        val dz = -b * x[1]
        return Point<Dim3, V>(listOf(x[0] + h * dx, x[1] + h * dy, x[2] + h * dz), Dim3)
    }

    companion object {
        operator fun invoke(
            a: Flt64 = Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
            b: Flt64 = Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
            c: Flt64 = Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
            d: Flt64 = Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
            e: Flt64 = Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
            h: Flt64 = Flt64(0.01)
        ): CapacitanceEquation<fuookami.ospf.kotlin.math.algebra.number.Flt64> {
            return CapacitanceEquation(a, b, c, d, e, h)
        }
    }
}

data class CapacitanceEquationGenerator(
    val capacitanceEquation: CapacitanceEquation<fuookami.ospf.kotlin.math.algebra.number.Flt64> = CapacitanceEquation(),
    private var _x: Point<Dim3, Flt64> = point3(
        Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
        Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
        Random.nextFlt64(Flt64.decimalPrecision, Flt64.one)
    )
) : Generator<Point<Dim3, Flt64>> {
    companion object {
        operator fun invoke(
            a: Flt64 = Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
            b: Flt64 = Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
            c: Flt64 = Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
            d: Flt64 = Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
            e: Flt64 = Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
            h: Flt64 = Flt64(0.01),
            x: Point<Dim3, Flt64> = point3(
                Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
                Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
                Random.nextFlt64(Flt64.decimalPrecision, Flt64.one)
            )
        ): CapacitanceEquationGenerator {
            return CapacitanceEquationGenerator(
                CapacitanceEquation(a, b, c, d, e, h),
                x
            )
        }
    }

    val x by ::_x

    override operator fun invoke(): Point<Dim3, Flt64> {
        val x = _x.copy()
        _x = capacitanceEquation(x)
        return x
    }
}
