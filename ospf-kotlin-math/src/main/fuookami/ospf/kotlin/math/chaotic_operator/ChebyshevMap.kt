/**
 * 切比雪夫映射
 * Chebyshev Map
 *
 * 切比雪夫映射是基于切比雪夫多项式的一维混沌映射。
 * 该映射利用切比雪夫多项式的性质产生混沌序列，具有良好的遍历性和随机性。
 * 常用于混沌加密、伪随机数生成和混沌优化算法。
 *
 * The Chebyshev map is a one-dimensional chaotic map based on Chebyshev polynomials.
 * This map generates chaotic sequences using properties of Chebyshev polynomials, exhibiting good ergodicity and randomness.
 * Commonly used for chaos encryption, pseudo-random number generation, and chaotic optimization algorithms.
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
 * 切比雪夫映射
 * Chebyshev Map
 */
data class ChebyshevMap<V : FloatingNumber<V>>(
    val a: V
) : Extractor<V, V> {
    override operator fun invoke(x: V): V {
        val v = a
        return if (x geq -v.constants.one && x leq v.constants.one) {
            val acosValue = castNullableToV(x.acos()) ?: return v.constants.zero
            castToV((a * acosValue).cos())
        } else {
            v.constants.zero
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun castToV(value: Any): V {
        // 安全不变量：V 实现 FloatingNumber<V>，cos 返回值与输入保持同一运行时数值类型。
        // Safety invariant: V implements FloatingNumber<V>, and cos keeps the same runtime numeric type as input.
        return value as V
    }

    @Suppress("UNCHECKED_CAST")
    private fun castNullableToV(value: Any?): V? {
        // 安全不变量：acos 在定义域内返回与输入同一数值族；定义域外保持 null。
        // Safety invariant: acos returns the same numeric family as input in-domain; out-of-domain remains null.
        return value as V?
    }

    companion object {
        operator fun invoke(
            a: Flt64 = Random.nextFlt64(Flt64.two, Flt64.ten)
        ): ChebyshevMap<fuookami.ospf.kotlin.math.algebra.number.Flt64> {
            return ChebyshevMap(a)
        }
    }
}

/**
 * 切比雪夫映射生成噌
 * Chebyshev Map Generator
 */
data class ChebyshevMapGenerator(
    val chebyshevMap: ChebyshevMap<fuookami.ospf.kotlin.math.algebra.number.Flt64> = ChebyshevMap(),
    private var _x: Flt64 = Random.nextFlt64(-Flt64.one, Flt64.one)
) : Generator<fuookami.ospf.kotlin.math.algebra.number.Flt64> {
    companion object {
        operator fun invoke(
            a: Flt64 = Random.nextFlt64(Flt64.two, Flt64.ten),
            x: Flt64 = Random.nextFlt64(-Flt64.one, Flt64.one)
        ): ChebyshevMapGenerator {
            return ChebyshevMapGenerator(
                ChebyshevMap(a),
                x
            )
        }
    }

    val x by ::_x

    override operator fun invoke(): Flt64 {
        val x = _x.copy()
        _x = chebyshevMap(x)
        return x
    }
}
