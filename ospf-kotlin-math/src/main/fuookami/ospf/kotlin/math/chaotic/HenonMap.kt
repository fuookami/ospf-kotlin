/**
 * 埃农映射
 * Henon Map
 *
 * 埃农映射是由 Michel Henon 于 1976 年提出的经典二维混沌映射。
 * 该映射是 Lorenz 吸引子的截面映射简化模型，展现出奇异吸引子行为。
 * 常用于混沌理论研究、分形几何和动力系统教学。
 *
 * The Henon map is a classic two-dimensional chaotic map proposed by Michel Henon in 1976.
 * This map is a simplified model of the Lorenz attractor's Poincare section, exhibiting strange attractor behavior.
 * Commonly used for chaos theory research, fractal geometry, and dynamical systems education.
*/
package fuookami.ospf.kotlin.math.chaotic

import kotlin.random.Random
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.geometry.*
import fuookami.ospf.kotlin.math.nextFlt64

/**
 * 埃农映射
 * Henon Map
 *
 * 公式 / Formula:
 * x_{n+1} = 1 - a * x^2 + y
 * y_{n+1} = b * x
 *
 * @property a 系统参数 a / System parameter a
 * @property b 系统参数 b / System parameter b
*/
data class HenonMap<V : FloatingNumber<V>>(
    val a: V,
    val b: V
) : Extractor<Point<Dim2, V>, Point<Dim2, V>> {
    override operator fun invoke(p: Point<Dim2, V>): Point<Dim2, V> {
        val x = p[0]
        val y = p[1]
        return Point<Dim2, V>(listOf(x.constants.one - a * x * x + y, b * x), Dim2)
    }

    companion object {
        operator fun invoke(
            a: Flt64 = Flt64(1.4),
            b: Flt64 = Flt64(0.3)
        ): HenonMap<Flt64> {
            return HenonMap(a, b)
        }
    }
}

/**
 * 埃农映射生成器
 * Henon Map Generator
 * @property henonMap 埃农映射实例 / Henon map instance
 * @property _x 当前状态点 / Current state point
*/
data class HenonMapGenerator(
    val henonMap: HenonMap<Flt64> = HenonMap(),
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
        ): HenonMapGenerator {
            return HenonMapGenerator(
                HenonMap(a, b),
                x
            )
        }
    }

    /**
     * 当前状态点。
     * The current state point.
    */
    val x by ::_x

    override operator fun invoke(): Point<Dim2, Flt64> {
        val x = _x.copy()
        _x = henonMap(x)
        return x
    }
}
