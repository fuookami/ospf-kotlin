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
package fuookami.ospf.kotlin.math.chaotic

import kotlin.random.Random
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.nextFlt64

/**
 * 切比雪夫映射
 * Chebyshev Map
 *
 * @property a 切比雪夫多项式阶数参数 / Chebyshev polynomial order parameter
 */
data class ChebyshevMap<V : FloatingNumber<V>>(
    val a: V
) : Extractor<V, V> {
    override operator fun invoke(x: V): V {
        val v = a
        return if (x geq -v.constants.one && x leq v.constants.one) {
            val acosValue = castNullableToNumber(x.acos()) ?: return v.constants.zero
            castToNumber((a * acosValue).cos())
        } else {
            v.constants.zero
        }
    }

    /**
     * 将 cos 运算结果转换为类型 V
     * Cast cos operation result to type V
     *
     * 安全不变量：V 实现 FloatingNumber<V>，cos 返回值与输入保持同一运行时数值类型。
     * Safety invariant: V implements FloatingNumber<V>, and cos keeps the same runtime numeric type as input.
     *
     * @param value cos 运算的结果值 / The result value from cos operation
     * @return 转换后的类型 V / The cast value of type V
     */
    @Suppress("UNCHECKED_CAST")
    private fun castToNumber(value: Any): V {
        return value as V
    }

    /**
     * 将 acos 运算结果转换为可空类型 V
     * Cast acos operation result to nullable type V
     *
     * 安全不变量：acos 在定义域内返回与输入同一数值族；定义域外保持 null。
     * Safety invariant: acos returns the same numeric family as input in-domain; out-of-domain remains null.
     *
     * @param value acos 运算的结果值 / The result value from acos operation
     * @return 转换后的可空类型 V，定义域外返回 null / The cast nullable type V, or null if out of domain
     */
    @Suppress("UNCHECKED_CAST")
    private fun castNullableToNumber(value: Any?): V? {
        return value as V?
    }

    companion object {
        /**
         * Creates a ChebyshevMap instance with Flt64 parameters.
         * 创建使用 Flt64 参数的切比雪夫映射实例。
         *
         * @param a the Chebyshev polynomial order parameter / 切比雪夫多项式阶数参数
         * @return a new ChebyshevMap instance / 新的切比雪夫映射实例
         */
        operator fun invoke(
            a: Flt64 = Random.nextFlt64(Flt64.two, Flt64.ten)
        ): ChebyshevMap<Flt64> {
            return ChebyshevMap(a)
        }
    }
}

/**
 * Chebyshev map generator that produces chaotic sequences iteratively.
 * 切比雪夫映射生成器，通过迭代产生混沌序列。
 *
 * @property chebyshevMap the underlying Chebyshev map instance / 底层切比雪夫映射实例
 * @property _x the internal state variable for iteration / 迭代用的内部状态变量
 */
data class ChebyshevMapGenerator(
    val chebyshevMap: ChebyshevMap<Flt64> = ChebyshevMap(),
    private var _x: Flt64 = Random.nextFlt64(-Flt64.one, Flt64.one)
) : Generator<Flt64> {
    companion object {
        /**
         * Creates a ChebyshevMapGenerator instance with specified parameters.
         * 创建使用指定参数的切比雪夫映射生成器实例。
         *
         * @param a the Chebyshev polynomial order parameter / 切比雪夫多项式阶数参数
         * @param x the initial state value / 初始状态值
         * @return a new ChebyshevMapGenerator instance / 新的切比雪夫映射生成器实例
         */
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

    /** The current state value exposed as read-only / 当前状态值的只读暴露 */
    val x by ::_x

    override operator fun invoke(): Flt64 {
        val x = _x.copy()
        _x = chebyshevMap(x)
        return x
    }
}
