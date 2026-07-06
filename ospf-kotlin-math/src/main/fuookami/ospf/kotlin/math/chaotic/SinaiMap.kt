/**
 * 西奈映射
 * Sinai Map
 *
 * 西奈映射是一种二维混沌映射，通过余弦耦合项产生混沌行为。
 * 常用于混沌动力学研究、随机数生成和加密应用。
 *
 * The Sinai map is a two-dimensional chaotic map that produces chaotic behavior
 * through cosine coupling terms.
 * Commonly used for chaotic dynamics research, random number generation, and encryption applications.
 */
package fuookami.ospf.kotlin.math.chaotic

import kotlin.random.Random
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.geometry.*
import fuookami.ospf.kotlin.math.nextFlt64

/**
 * 西奈映射
 * Sinai Map
 *
 * @property r 混沌参数 / Chaos parameter
 * @property twoPi 常量 2*pi / Constant 2*pi
 */
data class SinaiMap<V : FloatingNumber<V>>(
    val r: V,
    val twoPi: V
) : Extractor<Point<Dim2, V>, Point<Dim2, V>> {
    @Suppress("UNCHECKED_CAST")
    override operator fun invoke(p: Point<Dim2, V>): Point<Dim2, V> {
        val x = p[0]
        val y = p[1]
        val one = x.constants.one
        val cosVal = (twoPi * x).cos() as V
        val newX = (x + y + r * cosVal) mod one
        val newY = (y + r * cosVal) mod one
        return Point<Dim2, V>(listOf(newX, newY), Dim2)
    }

    companion object {
        operator fun invoke(
            r: Flt64 = Flt64(0.3)
        ): SinaiMap<Flt64> = SinaiMap(r, Flt64(2.0) * Flt64.pi)
    }
}

/**
 * 西奈映射生成器
 * Sinai Map Generator
 *
 * @property map 西奈映射实例 / Sinai map instance
 */
data class SinaiMapGenerator(
    val map: SinaiMap<Flt64> = SinaiMap(),
    private var _x: Point<Dim2, Flt64> = point2(
        Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
        Random.nextFlt64(Flt64.decimalPrecision, Flt64.one)
    )
) : Generator<Point<Dim2, Flt64>> {
    val x by ::_x
    override operator fun invoke(): Point<Dim2, Flt64> {
        val x = _x.copy(); _x = map(x); return x
    }
}
