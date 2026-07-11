/**
 * 洛兹映射
 * Lozi Map
 *
 * 洛兹映射是埃农映射的分段线性简化版本，由 Rene Lozi 提出。
 * 该映射用绝对值替代平方运算，产生类似的混沌行为但更易于分析。
 * 常用于混沌理论研究和分形几何教学。
 *
 * The Lozi map is a piecewise linear simplification of the Henon map, proposed by Rene Lozi.
 * This map replaces the squared term with an absolute value, producing similar chaotic behavior but is easier to analyze.
 * Commonly used for chaos theory research and fractal geometry education.
*/
package fuookami.ospf.kotlin.math.chaotic

import kotlin.random.Random
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.geometry.*
import fuookami.ospf.kotlin.math.nextFlt64

/**
 * 洛兹映射
 * Lozi Map
 *
 * 公式 / Formula:
 * x_{n+1} = 1 + y - a * |x|
 * y_{n+1} = b * x
 *
 * @property a 系统参数 a / System parameter a
 * @property b 系统参数 b / System parameter b
*/
data class LoziMap<V : FloatingNumber<V>>(
    val a: V,
    val b: V
) : Extractor<Point<Dim2, V>, Point<Dim2, V>> {
    override operator fun invoke(p: Point<Dim2, V>): Point<Dim2, V> {
        val x = p[0]
        val y = p[1]
        return Point<Dim2, V>(listOf(x.constants.one + y - a * x.abs(), b * x), Dim2)
    }

    companion object {
        operator fun invoke(
            a: Flt64 = Flt64(1.7),
            b: Flt64 = Flt64(0.5)
        ): LoziMap<Flt64> {
            return LoziMap(a, b)
        }
    }
}

/**
 * 洛兹映射生成器
 * Lozi Map Generator
*/
data class LoziMapGenerator(
    val loziMap: LoziMap<Flt64> = LoziMap(),
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
        ): LoziMapGenerator {
            return LoziMapGenerator(
                LoziMap(a, b),
                x
            )
        }
    }

    val x by ::_x

    override operator fun invoke(): Point<Dim2, Flt64> {
        val x = _x.copy()
        _x = loziMap(x)
        return x
    }
}
