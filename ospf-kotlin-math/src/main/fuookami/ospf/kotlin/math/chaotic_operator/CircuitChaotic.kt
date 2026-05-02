/**
 * 电路混沌模型
 * Circuit Chaotic Model
 *
 * 电路混沌模型是描述电子电路中混沌动力学行为的简化模型。
 * 该模型通过非线性项模拟电路中的混沌振荡，展现了电子系统中的复杂动力学现象。
 * 常用于电子电路混沌分析、混沌电路设计和电子系统动力学研究。
 *
 * The circuit chaotic model is a simplified model describing chaotic dynamical behavior in electronic circuits.
 * This model simulates chaotic oscillations in circuits through nonlinear terms, exhibiting complex dynamical phenomena in electronic systems.
 * Commonly used for electronic circuit chaos analysis, chaotic circuit design, and electronic system dynamics research.
 */
package fuookami.ospf.kotlin.math.chaotic_operator

import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.value_range.*

import fuookami.ospf.kotlin.utils.functional.Extractor
import fuookami.ospf.kotlin.utils.functional.Generator
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.geometry.Point
import fuookami.ospf.kotlin.math.geometry.Point2
import fuookami.ospf.kotlin.math.geometry.Dim2
import fuookami.ospf.kotlin.math.geometry.point2
import fuookami.ospf.kotlin.math.nextFlt64
import kotlin.random.Random

/**
 * 电路混沌模型
 * Circuit Chaotic Model
 */
data class CircuitChaotic<V : FloatingNumber<V>>(
    val a: V,
    val b: V,
    val c: V,
    val d: V
) : Extractor<Point<Dim2, V>, Point<Dim2, V>> {
    override fun invoke(x: Point<Dim2, V>): Point<Dim2, V> {
        return Point<Dim2, V>(listOf(a * x[1] - d * x[1].sqr(), -b * x[0] + c * x[1]), Dim2)
    }

    companion object {
        operator fun invoke(
            a: Flt64 = Random.nextFlt64(Flt64.decimalPrecision, Flt64.ten),
            b: Flt64 = Random.nextFlt64(Flt64.decimalPrecision, Flt64.ten),
            c: Flt64 = Random.nextFlt64(Flt64.decimalPrecision, Flt64.ten),
            d: Flt64 = Random.nextFlt64(Flt64.decimalPrecision, Flt64.ten)
        ): CircuitChaotic<Flt64> {
            return CircuitChaotic(a, b, c, d)
        }
    }
}

/**
 * 电路混沌模型生成器
 * Circuit Chaotic Model Generator
 */
data class CircuitChaoticGenerator(
    val circuitChaotic: CircuitChaotic<Flt64> = CircuitChaotic(),
    private var _x: Point2 = point2(
        Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
        Random.nextFlt64(Flt64.decimalPrecision, Flt64.one)
    )
) : Generator<Point2> {
    companion object {
        operator fun invoke(
            a: Flt64 = Random.nextFlt64(Flt64.decimalPrecision, Flt64.ten),
            b: Flt64 = Random.nextFlt64(Flt64.decimalPrecision, Flt64.ten),
            c: Flt64 = Random.nextFlt64(Flt64.decimalPrecision, Flt64.ten),
            d: Flt64 = Random.nextFlt64(Flt64.decimalPrecision, Flt64.ten),
            x: Point2 = point2(
                Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
                Random.nextFlt64(Flt64.decimalPrecision, Flt64.one)
            )
        ): CircuitChaoticGenerator {
            return CircuitChaoticGenerator(
                CircuitChaotic(
                    a = a,
                    b = b,
                    c = c,
                    d = d
                ),
                x
            )
        }
    }

    val x by ::_x

    override fun invoke(): Point2 {
        val x = _x.copy()
        _x = circuitChaotic(x)
        return x
    }
}
