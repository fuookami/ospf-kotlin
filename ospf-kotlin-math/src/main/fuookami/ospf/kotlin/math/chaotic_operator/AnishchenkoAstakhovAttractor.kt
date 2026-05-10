/**
 * Anishchenko-Astakhov 吸引�?
 * Anishchenko-Astakhov Attractor
 *
 * Anishchenko-Astakhov 吸引子是一个三维混沌系统，�?Anishchenko �?Astakhov 提出�?
 * 该系统通过切换函数引入非线性，展现出丰富的混沌动力学特性�?
 * 常用于混沌电路模拟和切换系统的动力学研究�?
 *
 * The Anishchenko-Astakhov attractor is a three-dimensional chaotic system proposed by Anishchenko and Astakhov.
 * This system introduces nonlinearity through a switching function and exhibits rich chaotic dynamical properties.
 * Commonly used for chaotic circuit simulation and dynamics research of switching systems.
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
 * Anishchenko-Astakhov 吸引�?
 * Anishchenko-Astakhov Attractor
 */
data class AnishchenkoAstakhovAttractor<V : FloatingNumber<V>>(
    val mu: V,
    val eta: V,
    val h: V
) : Extractor<Point<Dim3, V>, Point<Dim3, V>> {
    override operator fun invoke(x: Point<Dim3, V>): Point<Dim3, V> {
        val v = mu
        val i = if (x[0] geq v.constants.zero) {
            v.constants.one
        } else {
            v.constants.zero
        }
        val dx = mu * x[0] + x[1] - x[0] * x[2]
        val dy = -x[0]
        val dz = -eta * x[2] + eta * i * x[0].sqr()
        return Point<Dim3, V>(listOf(x[0] + h * dx, x[1] + h * dy, x[2] + h * dz), Dim3)
    }

    companion object {
        operator fun invoke(
            mu: Flt64 = Flt64(1.2),
            eta: Flt64 = Flt64(0.5),
            h: Flt64 = Flt64(0.01)
        ): AnishchenkoAstakhovAttractor<fuookami.ospf.kotlin.math.algebra.number.Flt64> {
            return AnishchenkoAstakhovAttractor(mu, eta, h)
        }
    }
}

/**
 * Anishchenko-Astakhov 吸引子生成器
 * Anishchenko-Astakhov Attractor Generator
 */
data class AnishchenkoAstakhovAttractorGenerator(
    val anishchenkoAstakhovAttractor: AnishchenkoAstakhovAttractor<fuookami.ospf.kotlin.math.algebra.number.Flt64> = AnishchenkoAstakhovAttractor(),
    private var _x: Point<Dim3, Flt64> = point3(
        Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
        Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
        Random.nextFlt64(Flt64.decimalPrecision, Flt64.one)
    )
) : Generator<Point<Dim3, Flt64>> {
    companion object {
        operator fun invoke(
            mu: Flt64 = Flt64(1.2),
            eta: Flt64 = Flt64(0.5),
            h: Flt64 = Flt64(0.01),
            x: Point<Dim3, Flt64> = point3(
                Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
                Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
                Random.nextFlt64(Flt64.decimalPrecision, Flt64.one)
            )
        ): AnishchenkoAstakhovAttractorGenerator {
            return AnishchenkoAstakhovAttractorGenerator(
                AnishchenkoAstakhovAttractor(
                    mu = mu,
                    eta = eta,
                    h = h
                ),
                x
            )
        }
    }

    val x by ::_x

    override operator fun invoke(): Point<Dim3, Flt64> {
        val x = _x.copy()
        _x = anishchenkoAstakhovAttractor(x)
        return x
    }
}
