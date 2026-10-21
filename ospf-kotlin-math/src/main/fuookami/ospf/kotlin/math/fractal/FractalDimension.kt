/**
 * 分形维数估计 / Fractal dimension estimation
*/
package fuookami.ospf.kotlin.math.fractal

import kotlin.math.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.geometry.*

/**
 * 一维最小二乘斜率 / One-dimensional least-squares slope
 *
 * @param xs 自变量序列 / Independent variable sequence
 * @param ys 因变量序列 / Dependent variable sequence
 * @return 斜率，无法拟合时返回 null / Slope, or null when it cannot be fitted
*/
private fun leastSquaresSlope(xs: List<Double>, ys: List<Double>): Double? {
    if (xs.size < 2 || xs.size != ys.size) {
        return null
    }
    val count = xs.size.toDouble()
    var sumX = 0.0
    var sumY = 0.0
    for (index in xs.indices) {
        sumX += xs[index]
        sumY += ys[index]
    }
    val meanX = sumX / count
    val meanY = sumY / count
    var numerator = 0.0
    var denominator = 0.0
    for (index in xs.indices) {
        val dx = xs[index] - meanX
        numerator += dx * (ys[index] - meanY)
        denominator += dx * dx
    }
    return if (denominator <= 0.0) null else numerator / denominator
}

/**
 * 用盒计数法估计二维点集的分形维数 / Estimate the fractal dimension of a 2D point set by box counting
 *
 * 在归一化包围盒上按 [scales] 逐级划分网格，统计非空盒数量 N(ε)，
 * 由 ln N(ε) 对 ln ε 的斜率得到维数 D。 / Divides the normalized bounding box into grids at each scale in [scales], counts the non-empty boxes N(ε),
 * and derives the dimension D from the slope of ln N(ε) against ln ε.
 *
 * @param points 二维点集 / 2D point set
 * @param scales 网格划分数序列（每项须 >= 2）/ Grid division sequence (each item must be >= 2)
 * @return 盒计数维数 / Box-counting dimension
*/
fun boxCountingDimension(
    points: List<Point<Dim2, Flt64>>,
    scales: List<Int> = listOf(2, 4, 8, 16, 32)
): Ret<Flt64> {
    if (points.size < 2) {
        return Failed(ErrorCode.IllegalArgument, "at least two points are required / 至少需要两个点")
    }
    if (scales.size < 2 || scales.any { it < 2 }) {
        return Failed(ErrorCode.IllegalArgument, "scales must contain at least two values >= 2 / 网格划分数需至少两个不小于 2 的值")
    }

    var minX = Double.POSITIVE_INFINITY
    var maxX = Double.NEGATIVE_INFINITY
    var minY = Double.POSITIVE_INFINITY
    var maxY = Double.NEGATIVE_INFINITY
    for (point in points) {
        val x = point[0].value
        val y = point[1].value
        minX = min(minX, x)
        maxX = max(maxX, x)
        minY = min(minY, y)
        maxY = max(maxY, y)
    }

    val width = maxX - minX
    val height = maxY - minY
    if (width <= 0.0 && height <= 0.0) {
        return Failed(ErrorCode.IllegalArgument, "points are degenerate / 点集退化，无法估计维数")
    }

    val logEpsilons = ArrayList<Double>(scales.size)
    val logCounts = ArrayList<Double>(scales.size)
    for (scale in scales) {
        val occupied = HashSet<Long>()
        for (point in points) {
            val ix = if (width <= 0.0) {
                0
            } else {
                (((point[0].value - minX) / width) * scale).toInt().coerceIn(0, scale - 1)
            }
            val iy = if (height <= 0.0) {
                0
            } else {
                (((point[1].value - minY) / height) * scale).toInt().coerceIn(0, scale - 1)
            }
            occupied.add(iy.toLong() * scale + ix)
        }
        if (occupied.isEmpty()) {
            continue
        }
        logEpsilons.add(ln(1.0 / scale))
        logCounts.add(ln(occupied.size.toDouble()))
    }

    val slope = leastSquaresSlope(logEpsilons, logCounts)
        ?: return Failed(ErrorCode.IllegalArgument, "not enough scales to fit the dimension / 网格划分数不足，无法拟合维数")
    return ok(Flt64(-slope))
}

/**
 * 用相关维数（Grassberger–Procaccia）估计解集的分形维数 / Estimate the fractal dimension of a solution set by the correlation dimension (Grassberger–Procaccia)
 *
 * 以成对距离的经验分布近似相关积分 C(r)，由 ln C(r) 对 ln r 的斜率得到维数 D。
 * 该指标可直接作为种群多样性度量，用于驱动自适应步长。 / Approximates the correlation integral C(r) from the empirical distribution of pairwise distances,
 * and derives D from the slope of ln C(r) against ln r. This metric can directly serve as a population diversity measure that drives adaptive step sizes.
 *
 * @param solutions 解集，每个解是等长的数值向量 / Solution set where each solution is an equal-length numeric vector
 * @param radiusAmount 半径采样数量（须 >= 2）/ Number of sampled radii (must be >= 2)
 * @param pairSampleLimit 成对距离的最大采样数量 / Maximum number of sampled pairwise distances
 * @return 相关维数 / Correlation dimension
*/
fun correlationDimension(
    solutions: List<List<Flt64>>,
    radiusAmount: Int = 12,
    pairSampleLimit: Int = 20000
): Ret<Flt64> {
    if (solutions.size < 2) {
        return Failed(ErrorCode.IllegalArgument, "at least two solutions are required / 至少需要两个解")
    }
    if (radiusAmount < 2) {
        return Failed(ErrorCode.IllegalArgument, "radiusAmount must be at least 2 / 半径采样数量至少为 2")
    }
    if (pairSampleLimit < 2) {
        return Failed(ErrorCode.IllegalArgument, "pairSampleLimit must be at least 2 / 成对采样数量至少为 2")
    }

    val dimension = solutions.first().size
    if (dimension == 0) {
        return Failed(ErrorCode.IllegalArgument, "solution vectors must be non-empty / 解向量不得为空")
    }
    if (solutions.any { it.size != dimension }) {
        return Failed(ErrorCode.IllegalArgument, "solution vectors must share the same dimension / 解向量必须维度一致")
    }

    val distances = ArrayList<Double>()
    for (i in 0 until solutions.size) {
        for (j in i + 1 until solutions.size) {
            var squared = 0.0
            for (k in 0 until dimension) {
                val difference = solutions[i][k].value - solutions[j][k].value
                squared += difference * difference
            }
            distances.add(sqrt(squared))
            if (distances.size >= pairSampleLimit) {
                break
            }
        }
        if (distances.size >= pairSampleLimit) {
            break
        }
    }

    if (distances.size < 2) {
        return Failed(ErrorCode.IllegalArgument, "not enough pairwise distances / 成对距离数量不足")
    }

    val maximum = distances.max()
    if (maximum <= 0.0) {
        return Failed(ErrorCode.IllegalArgument, "all solutions are identical / 所有解完全相同，无法估计维数")
    }

    val logRadii = ArrayList<Double>(radiusAmount)
    val logCorrelations = ArrayList<Double>(radiusAmount)
    val total = distances.size.toDouble()
    for (index in 0 until radiusAmount) {
        val ratio = 0.01 * (0.98 / 0.01).pow(index.toDouble() / (radiusAmount - 1).toDouble())
        val radius = maximum * ratio
        val correlated = distances.count { it < radius }
        if (correlated == 0) {
            continue
        }
        logRadii.add(ln(radius))
        logCorrelations.add(ln(correlated / total))
    }

    val slope = leastSquaresSlope(logRadii, logCorrelations)
        ?: return Failed(ErrorCode.IllegalArgument, "not enough radii to fit the dimension / 半径采样不足，无法拟合维数")
    return ok(Flt64(slope))
}
