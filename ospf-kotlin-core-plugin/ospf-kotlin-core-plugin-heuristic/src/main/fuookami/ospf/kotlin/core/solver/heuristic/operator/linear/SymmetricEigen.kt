/**
 * 对称矩阵特征分解 / Symmetric matrix eigendecomposition
*/
package fuookami.ospf.kotlin.core.solver.heuristic.operator.linear

import kotlin.math.*

/**
 * 特征分解结果。 / Eigendecomposition result.
 *
 * @property values 特征值 / Eigenvalues
 * @property vectors 特征向量，`vectors[column][row]` 与 [values] 一一对应 / Eigenvectors, `vectors[column][row]` aligned with [values]
*/
data class SymmetricEigenResult(
    val values: List<Double>,
    val vectors: List<List<Double>>
) {
    /** 是否成功分解 / Whether the decomposition succeeded */
    val isValid: Boolean get() = values.isNotEmpty()
}

/**
 * 对称矩阵的 Jacobi 特征分解。 / Jacobi eigendecomposition of symmetric matrices.
 *
 * CMA-ES 每一代都需要把协方差矩阵分解为 `C = B·D²·Bᵀ` 以便按主方向采样。协方差矩阵天然对称，
 * 因此用 Jacobi 旋转法最稳妥：它对对称矩阵无条件收敛，且不需要额外的库依赖。 /
 * CMA-ES decomposes the covariance matrix as `C = B·D²·Bᵀ` every generation so it can sample along the principal axes. A
 * covariance matrix is symmetric by construction, which makes Jacobi rotations the safest choice: they converge unconditionally
 * for symmetric matrices and need no extra library.
 *
 * 维度就是决策变量个数（通常几十以内），因此 O(n³) 的复杂度完全可接受。 /
 * The dimension is the number of decision variables (usually well under a hundred), so the O(n³) cost is entirely acceptable.
*/
data object SymmetricEigen {

    /**
     * 分解对称矩阵 / Decompose a symmetric matrix
     *
     * @param matrix 对称矩阵 / Symmetric matrix
     * @param maxSweeps 最大扫描次数 / Maximum number of sweeps
     * @param tolerance 收敛阈值 / Convergence tolerance
     * @return 特征分解结果；输入非法时返回无效结果而不抛异常 / Eigendecomposition, or an invalid result for invalid input (never throws)
    */
    fun decompose(
        matrix: List<List<Double>>,
        maxSweeps: Int = 60,
        tolerance: Double = 1.0e-12
    ): SymmetricEigenResult {
        val size = matrix.size
        if (size == 0 || matrix.any { it.size != size }) {
            return SymmetricEigenResult(emptyList(), emptyList())
        }

        val working = Array(size) { row -> DoubleArray(size) { column -> matrix[row][column] } }
        val vectors = Array(size) { row -> DoubleArray(size) { column -> if (row == column) 1.0 else 0.0 } }

        for (sweep in 0 until maxSweeps) {
            var offDiagonal = 0.0
            for (row in 0 until size) {
                for (column in row + 1 until size) {
                    offDiagonal += working[row][column] * working[row][column]
                }
            }
            if (offDiagonal <= tolerance) {
                break
            }

            for (first in 0 until size) {
                for (second in first + 1 until size) {
                    val pair = working[first][second]
                    if (abs(pair) <= tolerance) {
                        continue
                    }

                    // 标准 Jacobi 旋转：把 (first, second) 这一对非对角元消掉。
                    // Standard Jacobi rotation: annihilate the (first, second) off-diagonal entry.
                    val theta = (working[second][second] - working[first][first]) / (2.0 * pair)
                    val sign = if (theta >= 0.0) 1.0 else -1.0
                    val t = sign / (abs(theta) + sqrt(theta * theta + 1.0))
                    val cosine = 1.0 / sqrt(t * t + 1.0)
                    val sine = t * cosine

                    for (index in 0 until size) {
                        val left = working[index][first]
                        val right = working[index][second]
                        working[index][first] = cosine * left - sine * right
                        working[index][second] = sine * left + cosine * right
                    }
                    for (index in 0 until size) {
                        val upper = working[first][index]
                        val lower = working[second][index]
                        working[first][index] = cosine * upper - sine * lower
                        working[second][index] = sine * upper + cosine * lower
                    }
                    for (index in 0 until size) {
                        val left = vectors[index][first]
                        val right = vectors[index][second]
                        vectors[index][first] = cosine * left - sine * right
                        vectors[index][second] = sine * left + cosine * right
                    }
                }
            }
        }

        val values = List(size) { working[it][it] }
        if (values.any { !it.isFinite() }) {
            return SymmetricEigenResult(emptyList(), emptyList())
        }
        // 转置为「每列一个特征向量」，便于按主方向逐维采样。
        // Transpose so that each entry is one eigenvector, convenient for per-axis sampling.
        val columns = List(size) { column -> List(size) { row -> vectors[row][column] } }
        return SymmetricEigenResult(values, columns)
    }
}
