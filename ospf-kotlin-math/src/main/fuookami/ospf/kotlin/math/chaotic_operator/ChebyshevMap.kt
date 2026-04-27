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
data class ChebyshevMap(
    val a: Flt64 = Random.nextFlt64(Flt64.two, Flt64.ten),
) : Extractor<Flt64, Flt64> {
    override operator fun invoke(x: Flt64): Flt64 {
        return if (x geq -Flt64.one && x leq Flt64.one) {
            (a * x.acos()!!).cos()
        } else {
            Flt64.zero
        }
    }
}

/**
 * 切比雪夫映射生成器
 * Chebyshev Map Generator
 */
data class ChebyshevMapGenerator(
    val chebyshevMap: ChebyshevMap = ChebyshevMap(),
    private var _x: Flt64 = Random.nextFlt64(-Flt64.one, Flt64.one)
) : Generator<Flt64> {
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







