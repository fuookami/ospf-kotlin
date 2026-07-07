/**
 * 区间交换变换
 * Interval Exchange Transformation
 */
package fuookami.ospf.kotlin.math.chaotic

import kotlin.random.Random
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.nextFlt64

/**
 * 区间交换变换
 * Interval Exchange Transformation
 *
 * @property lambda 各子区间长度 / Lengths of subintervals
 * @property pi 置换 / Permutation
 */
data class IntervalExchangeTransformation<V : FloatingNumber<V>>(val lambda: List<V>, val pi: List<Int>) :
    Extractor<V, V> {
    init {
        require(lambda.size == pi.size) { "lambda and pi must have the same size" }
    }

    override operator fun invoke(x: V): V {
        val n = lambda.size
        var sum = x.constants.zero
        var intervalIndex = 0
        for (i in 0 until n) {
            if (x ls sum + lambda[i]) {
                intervalIndex = i; break
            }
            sum = sum + lambda[i]
            if (i == n - 1) intervalIndex = i
        }
        val relativePos = (x - sum) / lambda[intervalIndex]
        val targetInterval = pi[intervalIndex]
        var targetStart = x.constants.zero
        for (i in 0 until targetInterval) {
            targetStart = targetStart + lambda[i]
        }
        return targetStart + relativePos * lambda[targetInterval]
    }
}

/**
 * Interval exchange transformation generator that iteratively produces chaotic sequences.
 * 区间交换变换生成器，通过迭代产生混沌序列。
 *
 * @property map the interval exchange transformation instance / 区间交换变换实例
 * @property _x the internal state variable for iteration / 迭代用的内部状态变量
 */
data class IntervalExchangeTransformationGenerator(
    val map: IntervalExchangeTransformation<Flt64>,
    private var _x: Flt64 = Random.nextFlt64(
        Flt64.decimalPrecision,
        Flt64.one
    )
) : Generator<Flt64> {
    val x by ::_x;
    override operator fun invoke(): Flt64 {
        val x = _x.copy(); _x = map(x); return x
    }
}
