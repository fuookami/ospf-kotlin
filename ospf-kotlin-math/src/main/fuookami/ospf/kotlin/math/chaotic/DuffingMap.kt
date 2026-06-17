/**
 * 达芬映射
 * Duffing Map
 *
 * 达芬映射是来源于达芬振子离散化的二维混沌映射。
 * 该映射展现出复杂的混沌动力学行为，与达芬方程有密切关联。
 * 常用于非线性动力学研究和混沌同步研究。
 *
 * The Duffing map is a two-dimensional chaotic map derived from the discretization of the Duffing oscillator.
 * This map exhibits complex chaotic dynamical behavior, closely related to the Duffing equation.
 * Commonly used for nonlinear dynamics research and chaos synchronization studies.
 */
package fuookami.ospf.kotlin.math.chaotic

import kotlin.random.Random
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.geometry.*
import fuookami.ospf.kotlin.math.nextFlt64

/**
 * 达芬映射
 * Duffing Map
 *
 * 公式 / Formula:
 * x_{n+1} = y
 * y_{n+1} = -b * x + a * y - y^3
 *
 * @property a 系统参数 a / System parameter a
 * @property b 系统参数 b / System parameter b
 */
data class DuffingMap<V : FloatingNumber<V>>(
    val a: V,
    val b: V
) : Extractor<Point<Dim2, V>, Point<Dim2, V>> {
    override operator fun invoke(p: Point<Dim2, V>): Point<Dim2, V> {
        val x = p[0]
        val y = p[1]
        return Point<Dim2, V>(listOf(y, -b * x + a * y - y * y * y), Dim2)
    }

    companion object {
        operator fun invoke(
            a: Flt64 = Flt64(2.75),
            b: Flt64 = Flt64(0.2)
        ): DuffingMap<Flt64> {
            return DuffingMap(a, b)
        }
    }
}

/**
 * 达芬映射生成器
 * Duffing Map Generator
 */
data class DuffingMapGenerator(
    val duffingMap: DuffingMap<Flt64> = DuffingMap(),
    private var _x: Point<Dim2, Flt64> = point2(
        Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
        Random.nextFlt64(Flt64.decimalPrecision, Flt64.one)
    )
) : Generator<Point<Dim2, Flt64>> {
    companion object {
        operator fun invoke(
            a: Flt64,
            b: Flt64,
            x: Point<Dim2, Flt64> = point2(
                Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
                Random.nextFlt64(Flt64.decimalPrecision, Flt64.one)
            )
        ): DuffingMapGenerator {
            return DuffingMapGenerator(
                DuffingMap(a, b),
                x
            )
        }
    }

    val x by ::_x

    override operator fun invoke(): Point<Dim2, Flt64> {
        val x = _x.copy()
        _x = duffingMap(x)
        return x
    }
}
