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

sealed interface Distance {
    operator fun <D : Dimension> invoke(lhs: Point<D>, rhs: Point<D>): Flt64

    data object Euclidean : Distance {
        override operator fun <D : Dimension> invoke(lhs: Point<D>, rhs: Point<D>): Flt64 {
            return (lhs.indices.sumOf(Flt64) { (lhs[it] - rhs[it]).sqr() }).sqrt()
        }
    }

    data object Manhattan : Distance {
        override operator fun <D : Dimension> invoke(lhs: Point<D>, rhs: Point<D>): Flt64 {
            return lhs.indices.sumOf(Flt64) { (lhs[it] - rhs[it]).abs() }
        }
    }

    class Minkowski(val p: Int) : Distance {
        override operator fun <D : Dimension> invoke(lhs: Point<D>, rhs: Point<D>): Flt64 {
            return (lhs.indices.sumOf(Flt64) { (lhs[it] - rhs[it]).abs().pow(p) }).pow(Flt64(1.0 / p.toDouble())) as Flt64
        }
    }

    data object Chebyshev : Distance {
        override operator fun <D : Dimension> invoke(lhs: Point<D>, rhs: Point<D>): Flt64 {
            return (lhs.indices.maxOf { (lhs[it] - rhs[it]).abs() })
        }
    }
}







