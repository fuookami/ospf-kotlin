/**
 * 耦合洛伦兹吸引子
 * Coupled Lorenz Attractor
 *
 * 耦合洛伦兹吸引子是由两个相互耦合皌Lorenz 系统组成的混沌系统。
 * 通过耦合参数，两丌Lorenz 系统相互影响，展现出复杂的同步和混沌动力学行为。
 * 常用于混沌同步研究、复杂网络动力学和耦合系统分析。
 *
 * The coupled Lorenz attractor is a chaotic system composed of two mutually coupled Lorenz systems.
 * Through coupling parameters, the two Lorenz systems interact, exhibiting complex synchronization and chaotic dynamical behavior.
 * Commonly used for chaos synchronization research, complex network dynamics, and coupled systems analysis.
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

data class CoupledLorenzAttractor<V : FloatingNumber<V>>(
    val beta: V,
    val gamma1: V,
    val gamma2: V,
    val epsilon: V,
    val omicron: V,
    val h: V
) : Extractor<Pair<Point<Dim3, V>, Point<Dim3, V>>, Pair<Point<Dim3, V>, Point<Dim3, V>>> {
    override operator fun invoke(x: Pair<Point<Dim3, V>, Point<Dim3, V>>): Pair<Point<Dim3, V>, Point<Dim3, V>> {
        val (x1, x2) = x
        val dx1 = omicron * (x1[1] - x1[0])
        val dy1 = dy(gamma1, x1)
        val dz1 = dz(x1)
        val dx2 = omicron * (x2[1] - x2[0]) + epsilon * (x1[0] - x2[0])
        val dy2 = dy(gamma2, x2)
        val dz2 = dz(x2)
        return Point<Dim3, V>(listOf(x1[0] + h * dx1, x1[1] + h * dy1, x1[2] + h * dz1), Dim3) to
            Point<Dim3, V>(listOf(x2[0] + h * dx2, x2[1] + h * dy2, x2[2] + h * dz2), Dim3)
    }

    private fun dy(gamma: V, x: Point<Dim3, V>): V {
        return gamma * x[0] - x[1] - x[0] * x[2]
    }

    private fun dz(x: Point<Dim3, V>): V {
        return beta * x[2] + x[0] * x[1]
    }

    companion object {
        operator fun invoke(
            beta: Flt64 = Flt64(8.0 / 3.0),
            gamma1: Flt64 = Flt64(35.0),
            gamma2: Flt64 = Flt64(1.15),
            epsilon: Flt64 = Flt64(2.85),
            omicron: Flt64 = Flt64(2.85),
            h: Flt64 = Flt64(0.01)
        ): CoupledLorenzAttractor<fuookami.ospf.kotlin.math.algebra.number.Flt64> {
            return CoupledLorenzAttractor(beta, gamma1, gamma2, epsilon, omicron, h)
        }
    }
}

data class CoupledLorenzAttractorGenerator(
    val coupledLorenzAttractor: CoupledLorenzAttractor<fuookami.ospf.kotlin.math.algebra.number.Flt64> = CoupledLorenzAttractor(),
    private var _x: Point<Dim3, Flt64> = point3(
        Random.nextFlt64(Flt64.zero, Flt64.one),
        Random.nextFlt64(Flt64.zero, Flt64.one),
        Random.nextFlt64(Flt64.zero, Flt64.one)
    ),
    private var _y: Point<Dim3, Flt64> = point3(
        Random.nextFlt64(Flt64.zero, Flt64.one),
        Random.nextFlt64(Flt64.zero, Flt64.one),
        Random.nextFlt64(Flt64.zero, Flt64.one)
    )
) : Generator<Pair<Point<Dim3, Flt64>, Point<Dim3, Flt64>>> {
    companion object {
        operator fun invoke(
            beta: Flt64 = Flt64(8.0 / 3.0),
            gamma1: Flt64 = Flt64(35.0),
            gamma2: Flt64 = Flt64(1.15),
            epsilon: Flt64 = Flt64(2.85),
            omicron: Flt64 = Flt64(2.85),
            h: Flt64 = Flt64(0.01),
            x: Point<Dim3, Flt64> = point3(
                Random.nextFlt64(Flt64.zero, Flt64.one),
                Random.nextFlt64(Flt64.zero, Flt64.one),
                Random.nextFlt64(Flt64.zero, Flt64.one)
            ),
            y: Point<Dim3, Flt64> = point3(
                Random.nextFlt64(Flt64.zero, Flt64.one),
                Random.nextFlt64(Flt64.zero, Flt64.one),
                Random.nextFlt64(Flt64.zero, Flt64.one)
            )
        ): CoupledLorenzAttractorGenerator {
            return CoupledLorenzAttractorGenerator(
                CoupledLorenzAttractor(beta, gamma1, gamma2, epsilon, omicron, h),
                x,
                y
            )
        }
    }

    val x by ::_x
    val y by ::_y

    override fun invoke(): Pair<Point<Dim3, Flt64>, Point<Dim3, Flt64>> {
        val x = _x.copy()
        val y = _y.copy()
        val ret = coupledLorenzAttractor(x to y)
        _x = ret.first
        _y = ret.second
        return x to y
    }
}
