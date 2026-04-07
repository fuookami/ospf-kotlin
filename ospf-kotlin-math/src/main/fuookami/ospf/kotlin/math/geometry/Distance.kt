/**
 * Distance（距离计算）
 * Distance Calculation
 *
 * 提供多种距离度量算法，用于计算点之间的距离。
 * Provides various distance metric algorithms for calculating distances between points.
 *
 * 主要距离度量：
 * Main distance metrics:
 * - Euclidean: 欧几里得距离（L2 范数）/ Euclidean distance (L2 norm)
 * - Manhattan: 曼哈顿距离（L1 范数）/ Manhattan distance (L1 norm)
 * - Minkowski: 闵可夫斯基距离（Lp 范数）/ Minkowski distance (Lp norm)
 * - Chebyshev: 切比雪夫距离（棋盘距离）/ Chebyshev distance (chessboard distance)
 *
 * 应用场景：聚类分析、最近邻搜索、空间索引、路径规划等。
 * Applications: clustering analysis, nearest neighbor search, spatial indexing, path planning, etc.
 */
package fuookami.ospf.kotlin.math.geometry

import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.value_range.*

import fuookami.ospf.kotlin.math.functional.sumOf
import fuookami.ospf.kotlin.math.algebra.number.Flt64

/**
 * Distance - 距离度量接口
 * Distance - Distance metric interface
 *
 * 定义计算两点之间距离的通用接口。
 * Defines a generic interface for calculating distances between points.
 */
sealed interface Distance {
    /**
     * 计算两点之间的距离
     * Calculate the distance between two points
     *
     * @param lhs 左点 / Left point
     * @param rhs 右点 / Right point
     * @return 距离值 / Distance value
     */
    operator fun <D : Dimension> invoke(lhs: Point<D>, rhs: Point<D>): Flt64

    /**
     * Euclidean - 欧几里得距离
     * Euclidean - Euclidean distance
     *
     * 计算 L2 范数距离，即两点之间的直线距离。
     * Calculates L2 norm distance, i.e., the straight-line distance between two points.
     *
     * 公式：d = sqrt(sum((x_i - y_i)^2))
     * Formula: d = sqrt(sum((x_i - y_i)^2))
     */
    data object Euclidean : Distance {
        override operator fun <D : Dimension> invoke(lhs: Point<D>, rhs: Point<D>): Flt64 {
            return (lhs.indices.sumOf(Flt64) { (lhs[it] - rhs[it]).sqr() }).sqrt()
        }
    }

    /**
     * Manhattan - 曼哈顿距离
     * Manhattan - Manhattan distance
     *
     * 计算 L1 范数距离，即各维度差值之和。
     * Calculates L1 norm distance, i.e., the sum of absolute differences across all dimensions.
     *
     * 公式：d = sum(|x_i - y_i|)
     * Formula: d = sum(|x_i - y_i|)
     */
    data object Manhattan : Distance {
        override operator fun <D : Dimension> invoke(lhs: Point<D>, rhs: Point<D>): Flt64 {
            return lhs.indices.sumOf(Flt64) { (lhs[it] - rhs[it]).abs() }
        }
    }

    /**
     * Minkowski - 闵可夫斯基距离
     * Minkowski - Minkowski distance
     *
     * 计算 Lp 范数距离，是欧几里得距离和曼哈顿距离的推广形式。
     * Calculates Lp norm distance, a generalization of Euclidean and Manhattan distances.
     *
     * 公式：d = (sum(|x_i - y_i|^p))^(1/p)
     * Formula: d = (sum(|x_i - y_i|^p))^(1/p)
     *
     * @property p 范数参数，p=1 为曼哈顿距离，p=2 为欧几里得距离 / Norm parameter, p=1 is Manhattan, p=2 is Euclidean
     */
    class Minkowski(val p: Int) : Distance {
        override operator fun <D : Dimension> invoke(lhs: Point<D>, rhs: Point<D>): Flt64 {
            return (lhs.indices.sumOf(Flt64) { (lhs[it] - rhs[it]).abs().pow(p) }).pow(Flt64(1.0 / p.toDouble())) as Flt64
        }
    }

    /**
     * Chebyshev - 切比雪夫距离
     * Chebyshev - Chebyshev distance
     *
     * 计算 L-infinity 范数距离，即各维度差值的最大值。
     * Calculates L-infinity norm distance, i.e., the maximum absolute difference across all dimensions.
     *
     * 也称为棋盘距离，用于棋类游戏中两点之间的最小移动步数。
     * Also known as chessboard distance, used for minimum steps between two points in chess-like games.
     *
     * 公式：d = max(|x_i - y_i|)
     * Formula: d = max(|x_i - y_i|)
     */
    data object Chebyshev : Distance {
        override operator fun <D : Dimension> invoke(lhs: Point<D>, rhs: Point<D>): Flt64 {
            return (lhs.indices.maxOf { (lhs[it] - rhs[it]).abs() })
        }
    }
}







