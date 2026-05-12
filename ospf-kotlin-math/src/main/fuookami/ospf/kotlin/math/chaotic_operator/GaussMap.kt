/**
 * 高斯映射
 * Gauss Map
 *
 * 高斯映射是一个简单的一维混沌映射，通过取模运算产生混沌行为。
 * 该映射与 Gauss 递归算法相关，展现出复杂的混沌动力学特性。
 * 常用于混沌理论研究、伪随机数生成和混沌迭代分析。
 *
 * The Gauss map is a simple one-dimensional chaotic map that generates chaotic behavior through modulo operations.
 * This map is related to Gauss's recursive algorithm, exhibiting complex chaotic dynamical properties.
 * Commonly used for chaos theory research, pseudo-random number generation, and chaotic iteration analysis.
 */
package fuookami.ospf.kotlin.math.chaotic_operator

import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.value_range.*

import fuookami.ospf.kotlin.utils.functional.Extractor
import fuookami.ospf.kotlin.utils.functional.Generator
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.nextFlt64
import kotlin.random.Random

/**
 * 高斯映射
 * Gauss Map
 */
data class GaussMap<V : FloatingNumber<V>>(
    val mu: V
) : Extractor<V, V> {
    override operator fun invoke(x: V): V {
        val v = mu
        return if (x eq v.constants.zero) {
            v.constants.zero
        } else {
            mu / x
        }
    }

    companion object {
        operator fun invoke(
            mu: Flt64 = Random.nextFlt64(Flt64.one, Flt64.ten)
        ): GaussMap<fuookami.ospf.kotlin.math.algebra.number.Flt64> {
            return GaussMap(mu)
        }
    }
}

/**
 * 高斯映射生成噌
 * Gauss Map Generator
 */
data class GaussMapGenerator(
    val gaussMap: GaussMap<fuookami.ospf.kotlin.math.algebra.number.Flt64> = GaussMap(),
    private var _x: Flt64 = Random.nextFlt64(Flt64.decimalPrecision, Flt64.one)
) : Generator<fuookami.ospf.kotlin.math.algebra.number.Flt64> {
    companion object {
        operator fun invoke(
            mu: Flt64,
            x: Flt64 = Random.nextFlt64(Flt64.decimalPrecision, Flt64.one)
        ): GaussMapGenerator {
            return GaussMapGenerator(
                GaussMap(mu),
                x
            )
        }
    }

    val x by ::_x

    override operator fun invoke(): Flt64 {
        val x = _x.copy()
        _x = gaussMap(x)
        return x
    }
}
