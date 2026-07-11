/**
 * 正弦映射
 * Sine Map
 *
 * 正弦映射是基于正弦函数的一维混沌映射。
 * 该映射利用正弦函数的非线性产生混沌行为，与逻辑斯蒂映射具有相似的分岔结构。
 * 常用于混沌加密、伪随机数生成和混沌动力学研究。
 *
 * The sine map is a one-dimensional chaotic map based on the sine function.
 * This map generates chaotic behavior using the nonlinearity of the sine function, exhibiting a bifurcation structure similar to the logistic map.
 * Commonly used for chaos encryption, pseudo-random number generation, and chaotic dynamics research.
*/
package fuookami.ospf.kotlin.math.chaotic

import kotlin.random.Random
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.nextFlt64

/**
 * 正弦映射
 * Sine Map
 *
 * 公式 / Formula: x_{n+1} = mu * sin(pi * x)
 *
 * @property mu 系统参数 mu，取值范围 [0, 1] / System parameter mu, range [0, 1]
*/
data class SineMap<V : FloatingNumber<V>>(
    val mu: V
) : Extractor<V, V> {
    @Suppress("UNCHECKED_CAST")
    override operator fun invoke(x: V): V {
        val pi = x.constants.pi
        return mu * (pi * x).sin() as V
    }

    companion object {
        operator fun invoke(
            mu: Flt64 = Flt64.one
        ): SineMap<Flt64> {
            return SineMap(mu)
        }
    }
}

/**
 * 正弦映射生成器
 * Sine Map Generator
*/
data class SineMapGenerator(
    val sineMap: SineMap<Flt64> = SineMap(),
    private var _x: Flt64 = Random.nextFlt64(Flt64.decimalPrecision, Flt64.one)
) : Generator<Flt64> {
    companion object {
        operator fun invoke(
            mu: Flt64,
            x: Flt64 = Random.nextFlt64(Flt64.decimalPrecision, Flt64.one)
        ): SineMapGenerator {
            return SineMapGenerator(
                SineMap(mu),
                x
            )
        }
    }

    val x by ::_x

    override operator fun invoke(): Flt64 {
        val x = _x.copy()
        _x = sineMap(x)
        return x
    }
}
