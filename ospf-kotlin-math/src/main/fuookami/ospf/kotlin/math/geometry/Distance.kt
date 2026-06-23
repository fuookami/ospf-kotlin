/**
 * 距离度量
 * Distance Metrics
 *
 * 定义几何空间中的距离度量策略，支持欧几里得、曼哈顿、闵可夫斯基和切比雪夫距离。
 * Defines distance metric strategies in geometric space, supporting Euclidean, Manhattan, Minkowski, and Chebyshev distances.
 */
package fuookami.ospf.kotlin.math.geometry

import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber
import fuookami.ospf.kotlin.utils.functional.sumOf

/**
 * 将距离计算结果转换为目标数值类型
 * Cast the distance calculation result to the target numeric type
 *
 * @param V 数值类型 / The numeric type
 * @param value 距离计算结果 / The distance calculation result
 * @return 转换后的数值 / The casted numeric value
 */
@Suppress("UNCHECKED_CAST")
private fun <V : FloatingNumber<V>> castDistanceValue(value: Any?): V {
    // 安全不变量：距离运算在同一 V 数域中闭包，sqrt/pow 结果与输入域一致。
    // Safety invariant: distance operations are closed in the same V domain, so sqrt/pow results are V-compatible.
    return value as V
}

/**
 * 距离度量策略的密封接口，支持欧几里得、曼哈顿、闵可夫斯基和切比雪夫距离。
 * Sealed interface for distance metric strategies, supporting Euclidean, Manhattan, Minkowski, and Chebyshev distances.
 */
sealed interface Distance {
    /**
     * 计算两点之间的距离
     * Compute the distance between two points
     *
     * @param D 维度类型 / The dimension type
     * @param V 数值类型 / The numeric type
     * @param lhs 第一个点 / The first point
     * @param rhs 第二个点 / The second point
     * @return 两点之间的距离 / The distance between the two points
     */
    operator fun <D : Dimension, V : FloatingNumber<V>> invoke(lhs: Point<D, V>, rhs: Point<D, V>): V

    /**
     * 欧几里得距离，即两点间直线距离 sqrt(sum((a-b)^2))。
     * Euclidean distance: straight-line distance sqrt(sum((a-b)^2)).
     */
    data object Euclidean : Distance {
        override operator fun <D : Dimension, V : FloatingNumber<V>> invoke(lhs: Point<D, V>, rhs: Point<D, V>): V {
            val v = lhs[0]
            return castDistanceValue((lhs.indices.sumOf(v.constants) { (lhs[it] - rhs[it]).sqr() }).sqrt())
        }
    }

    /**
     * 曼哈顿距离，即各坐标差的绝对值之和 sum(|a-b|)。
     * Manhattan distance: sum of absolute differences along each axis, sum(|a-b|).
     */
    data object Manhattan : Distance {
        override operator fun <D : Dimension, V : FloatingNumber<V>> invoke(lhs: Point<D, V>, rhs: Point<D, V>): V {
            val v = lhs[0]
            return lhs.indices.sumOf(v.constants) { (lhs[it] - rhs[it]).abs() }
        }
    }

    /**
     * 闵可夫斯基距离，曼哈顿和欧几里得距离的推广，p=1 为曼哈顿，p=2 为欧几里得。
     * Minkowski distance: generalization of Manhattan and Euclidean; p=1 is Manhattan, p=2 is Euclidean.
     *
     * @param p 距离阶数 / The order of the distance metric
     */
    class Minkowski(val p: Int) : Distance {
        override operator fun <D : Dimension, V : FloatingNumber<V>> invoke(lhs: Point<D, V>, rhs: Point<D, V>): V {
            val v = lhs[0]
            val sum = lhs.indices.sumOf(v.constants) { (lhs[it] - rhs[it]).abs().pow(p) }
            val exponentValue = run {
                var result = v.constants.zero
                for (i in 0 until p) { result += v.constants.one }
                result
            }
            return castDistanceValue(sum.pow(v.constants.one / exponentValue))
        }
    }

    /**
     * 切比雪夫距离，即各坐标差的绝对值的最大值 max(|a-b|)。
     * Chebyshev distance: maximum of absolute differences along any axis, max(|a-b|).
     */
    data object Chebyshev : Distance {
        override operator fun <D : Dimension, V : FloatingNumber<V>> invoke(lhs: Point<D, V>, rhs: Point<D, V>): V {
            return (lhs.indices.maxOf { (lhs[it] - rhs[it]).abs() })
        }
    }
}
