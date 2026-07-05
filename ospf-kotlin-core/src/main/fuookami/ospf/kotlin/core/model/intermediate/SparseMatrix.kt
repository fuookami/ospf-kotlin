/**
 * 稀疏矩阵
 * Sparse matrix
 */
package fuookami.ospf.kotlin.core.model.intermediate

import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber

/**
 * 稀疏向量中的单个条目，将列索引与值配对。
 * A single entry in a [SparseVector], pairing a column index with a value.
 *
 * @property index 列索引 / Column index
 * @property value 条目值 / Entry value
 */
data class SparseVectorEntry<V : RealNumber<V>>(
    val index: Int,
    val value: V
)

/**
 * 稀疏向量，由 (index, value) 对列表支持。
 * Sparse vector backed by a list of (index, value) pairs.
 *
 * @property entries 条目列表 / Entry list
 */
class SparseVector<V : RealNumber<V>>(
    val entries: MutableList<SparseVectorEntry<V>> = mutableListOf()
) {
    /**
     * 添加一个条目到向量末尾。
     * Append an entry to the end of this vector.
     *
     * @param index 列索引 / Column index
     * @param value 条目值 / Entry value
     */
    fun add(index: Int, value: V) {
        entries.add(SparseVectorEntry(index, value))
    }

    /**
     * 条目数量
     * Number of entries
     *
     * @return 条目数量 / Number of entries
     */
    fun len(): Int = entries.size

    /**
     * 是否为空
     * Whether this vector is empty
     *
     * @return 若为空则返回 true / true if this vector is empty
     */
    fun isEmpty(): Boolean = entries.isEmpty()

    /**
     * 遍历所有条目，调用 [action] 处理 (index, value)。
     * Iterate over all entries in this vector, invoking [action] with (index, value).
     *
     * @param action 对每个条目执行的回调 / Callback to execute for each entry
     * @param action index 列索引 / Column index
     * @param action value 条目值 / Entry value
     */
    inline fun forEachEntry(action: (index: Int, value: V) -> Unit) {
        for (i in entries.indices) {
            val entry = entries[i]
            action(entry.index, entry.value)
        }
    }

    override fun toString(): String = "SparseVector($entries)"

    companion object {
        /** 创建空的 SparseVector / Create an empty SparseVector */
        fun <V : RealNumber<V>> invoke(): SparseVector<V> = SparseVector()
    }
}

/**
 * 稀疏矩阵，由 [SparseVector] 行列表支持。
 * Sparse matrix backed by a list of [SparseVector] rows.
 *
 * @property rows 行列表 / Row list
 */
class SparseMatrix<V : RealNumber<V>>(
    val rows: MutableList<SparseVector<V>> = mutableListOf()
) {
    /**
     * 添加一行到矩阵末尾。
     * Append a row to the end of this matrix.
     *
     * @param row 要添加的稀疏向量行 / Sparse vector row to append
     */
    fun addRow(row: SparseVector<V>) {
        rows.add(row)
    }

    /**
     * 矩阵行数
     * Number of rows in this matrix
     *
     * @return 行数 / Number of rows
     */
    fun numRows(): Int = rows.size

    /**
     * 第 [row] 行的非零条目数
     * Number of non-zero entries in row [row]
     *
     * @param row 行索引 / Row index
     * @return 该行的非零条目数 / Number of non-zero entries in the specified row
     */
    fun rowSize(row: Int): Int = rows.getOrNull(row)?.len() ?: 0

    /**
     * 获取指定行，越界时返回 null。
     * Get the row at [index], or null if out of bounds.
     *
     * @param index 行索引 / Row index
     * @return 对应的稀疏向量行，越界时返回 null / The sparse vector row, or null if out of bounds
     */
    fun getRow(index: Int): SparseVector<V>? = rows.getOrNull(index)

    /**
     * 遍历第 [row] 行的所有非零条目，调用 [action] 处理 (colIndex, value)。越界时为空操作。
     * Iterate over all non-zero entries in row [row], invoking [action] with (colIndex, value).
     * If [row] is out of bounds, the call is a no-op.
     *
     * @param row 行索引 / Row index
     * @param action 对每个条目执行的回调 / Callback to execute for each entry
     * @param action colIndex 列索引 / Column index
     * @param action value 条目值 / Entry value
     */
    inline fun forEachEntry(row: Int, action: (colIndex: Int, value: V) -> Unit) {
        if (row < 0 || row >= rows.size) {
            return
        }
        val entries = rows[row].entries
        for (i in entries.indices) {
            val entry = entries[i]
            action(entry.index, entry.value)
        }
    }

    /**
     * 遍历所有行，调用 [action] 处理 (rowIndex, SparseVector)。
     * Iterate over all rows, invoking [action] with (rowIndex, SparseVector).
     *
     * @param action 对每行执行的回调 / Callback to execute for each row
     * @param action rowIndex 行索引 / Row index
     * @param action row 稀疏向量行 / Sparse vector row
     */
    inline fun forEachRow(action: (rowIndex: Int, row: SparseVector<V>) -> Unit) {
        for (i in rows.indices) {
            action(i, rows[i])
        }
    }

    /**
     * 构建转置矩阵：每个条目 (r, c, v) 变为 (c, r, v)。
     * Build the transpose of this matrix: each entry (r, c, v) becomes (c, r, v).
     *
     * @return 转置后的稀疏矩阵 / The transposed sparse matrix
     */
    fun transpose(): SparseMatrix<V> {
        var maxCol = -1
        for (rowIndex in rows.indices) {
            val entries = rows[rowIndex].entries
            for (entryIndex in entries.indices) {
                val entry = entries[entryIndex]
                if (entry.index > maxCol) {
                    maxCol = entry.index
                }
            }
        }
        if (maxCol < 0) {
            return SparseMatrix()
        }

        val columnCounts = IntArray(maxCol + 1)
        for (r in rows.indices) {
            val entries = rows[r].entries
            for (entryIndex in entries.indices) {
                val entry = entries[entryIndex]
                columnCounts[entry.index] += 1
            }
        }

        val resultRows = MutableList(maxCol + 1) { c ->
            SparseVector<V>(ArrayList(columnCounts[c]))
        }
        for (r in rows.indices) {
            val entries = rows[r].entries
            for (entryIndex in entries.indices) {
                val entry = entries[entryIndex]
                resultRows[entry.index].entries.add(
                    SparseVectorEntry(
                        index = r,
                        value = entry.value
                    )
                )
            }
        }
        return SparseMatrix(resultRows)
    }

    override fun toString(): String = "SparseMatrix($rows)"

    companion object {
        /** 创建空的 SparseMatrix / Create an empty SparseMatrix */
        fun <V : RealNumber<V>> invoke(): SparseMatrix<V> = SparseMatrix()
    }
}

/**
 * 就地取反稀疏矩阵：每个条目乘以 -1。
 * Negate a [SparseMatrix] in-place: multiply every entry by -1.
 *
 * @return 就地取反后的稀疏矩阵（即 this）/ The negated sparse matrix in-place (this)
 */
fun SparseMatrix<Flt64>.negateInPlace(): SparseMatrix<Flt64> {
    for (row in rows) {
        for (i in row.entries.indices) {
            val e = row.entries[i]
            row.entries[i] = SparseVectorEntry(e.index, -e.value)
        }
    }
    return this
}

/**
 * 返回取反后的副本。
 * Return a negated copy of this [SparseMatrix].
 *
 * @return 取反后的新稀疏矩阵 / A new negated sparse matrix
 */
fun SparseMatrix<Flt64>.negated(): SparseMatrix<Flt64> {
    val result = SparseMatrix<Flt64>()
    for (row in rows) {
        val newRow = SparseVector<Flt64>()
        for (entry in row.entries) {
            newRow.add(entry.index, -entry.value)
        }
        result.addRow(newRow)
    }
    return result
}

/**
 * 就地缩放稀疏矩阵。
 * Scale a [SparseMatrix] in-place by [factor].
 *
 * @param factor 缩放因子 / Scale factor
 * @return 缩放后的稀疏矩阵（即 this）/ The scaled sparse matrix in-place (this)
 */
fun SparseMatrix<Flt64>.scaleInPlace(factor: Flt64): SparseMatrix<Flt64> {
    for (row in rows) {
        for (i in row.entries.indices) {
            val e = row.entries[i]
            row.entries[i] = SparseVectorEntry(e.index, e.value * factor)
        }
    }
    return this
}

/**
 * 稀疏二次向量中的单个条目，配对 (colIndex1, colIndex2?, coefficient)。
 * 用于二次约束行，其中每项可以是线性（colIndex2 == null）或二次（colIndex2 != null）。
 * A single entry in a [SparseQuadraticVector], pairing (colIndex1, colIndex2?, coefficient).
 * Used for quadratic constraint rows where each term may be linear (colIndex2 == null)
 * or quadratic (colIndex2 != null).
 *
 * @property colIndex1 第一列索引 / First column index
 * @property colIndex2 第二列索引（null 表示线性项）/ Second column index (null for linear term)
 * @property coefficient 系数 / Coefficient
 */
data class SparseQuadraticEntry(
    val colIndex1: Int,
    val colIndex2: Int?,
    val coefficient: Flt64
)

/**
 * 稀疏二次向量：二次约束条目的一行。
 * Sparse quadratic vector: a row of quadratic constraint entries.
 *
 * @property entries 条目列表 / Entry list
 */
class SparseQuadraticVector(
    val entries: MutableList<SparseQuadraticEntry> = mutableListOf()
) {
    /**
     * 添加一个二次条目到向量末尾。
     * Append a quadratic entry to the end of this vector.
     *
     * @param colIndex1 第一列索引 / First column index
     * @param colIndex2 第二列索引（null 表示线性项）/ Second column index (null for linear term)
     * @param coefficient 系数 / Coefficient
     */
    fun add(colIndex1: Int, colIndex2: Int?, coefficient: Flt64) {
        entries.add(SparseQuadraticEntry(colIndex1, colIndex2, coefficient))
    }

    /**
     * 条目数量
     * Number of entries
     *
     * @return 条目数量 / Number of entries
     */
    fun len(): Int = entries.size

    /**
     * 是否为空
     * Whether this vector is empty
     *
     * @return 若为空则返回 true / true if this vector is empty
     */
    fun isEmpty(): Boolean = entries.isEmpty()

    /**
     * 遍历所有条目，调用 [action] 处理 (colIndex1, colIndex2, coefficient)。
     * Iterate over all entries, invoking [action] with (colIndex1, colIndex2, coefficient).
     *
     * @param action 对每个条目执行的回调 / Callback to execute for each entry
     * @param action colIndex1 第一列索引 / First column index
     * @param action colIndex2 第二列索引（null 表示线性项）/ Second column index (null for linear term)
     * @param action coefficient 系数 / Coefficient
     */
    inline fun forEachEntry(action: (colIndex1: Int, colIndex2: Int?, coefficient: Flt64) -> Unit) {
        for (i in entries.indices) {
            val entry = entries[i]
            action(entry.colIndex1, entry.colIndex2, entry.coefficient)
        }
    }

    override fun toString(): String = "SparseQuadraticVector($entries)"

    companion object {
        /**
         * 创建空的 SparseQuadraticVector。
         * Create an empty SparseQuadraticVector.
         *
         * @return 空的 SparseQuadraticVector 实例 / An empty SparseQuadraticVector instance
         */
        fun invoke(): SparseQuadraticVector = SparseQuadraticVector()
    }
}

/**
 * 稀疏二次矩阵：由 [SparseQuadraticVector] 行列表支持。
 * 用作二次约束左侧的稀疏表示。
 * Sparse quadratic matrix: a list of [SparseQuadraticVector] rows.
 * Used as the sparse representation of quadratic constraint LHS.
 *
 * @property rows 行列表 / Row list
 */
class SparseQuadraticMatrix(
    val rows: MutableList<SparseQuadraticVector> = mutableListOf()
) {
    /**
     * 添加一行到二次矩阵末尾。
     * Append a row to the end of this quadratic matrix.
     *
     * @param row 要添加的稀疏二次向量行 / Sparse quadratic vector row to append
     */
    fun addRow(row: SparseQuadraticVector) {
        rows.add(row)
    }

    /**
     * 矩阵行数
     * Number of rows in this matrix
     *
     * @return 行数 / Number of rows
     */
    fun numRows(): Int = rows.size

    /**
     * 第 [row] 行的条目数
     * Number of entries in row [row]
     *
     * @param row 行索引 / Row index
     * @return 该行的条目数 / Number of entries in the specified row
     */
    fun rowSize(row: Int): Int = rows.getOrNull(row)?.len() ?: 0

    /**
     * 获取指定行，越界时返回 null。
     * Get the row at [index], or null if out of bounds.
     *
     * @param index 行索引 / Row index
     * @return 对应的稀疏二次向量行，越界时返回 null / The sparse quadratic vector row, or null if out of bounds
     */
    fun getRow(index: Int): SparseQuadraticVector? = rows.getOrNull(index)

    /**
     * 遍历第 [row] 行的所有条目，调用 [action] 处理 (colIndex1, colIndex2, coefficient)。越界时为空操作。
     * Iterate over all entries in row [row], invoking [action] with (colIndex1, colIndex2, coefficient).
     * If [row] is out of bounds, the call is a no-op.
     *
     * @param row 行索引 / Row index
     * @param action 对每个条目执行的回调 / Callback to execute for each entry
     * @param action colIndex1 第一列索引 / First column index
     * @param action colIndex2 第二列索引（null 表示线性项）/ Second column index (null for linear term)
     * @param action coefficient 系数 / Coefficient
     */
    inline fun forEachEntry(row: Int, action: (colIndex1: Int, colIndex2: Int?, coefficient: Flt64) -> Unit) {
        if (row < 0 || row >= rows.size) {
            return
        }
        val entries = rows[row].entries
        for (i in entries.indices) {
            val entry = entries[i]
            action(entry.colIndex1, entry.colIndex2, entry.coefficient)
        }
    }

    override fun toString(): String = "SparseQuadraticMatrix($rows)"

    companion object {
        /**
         * 创建空的 SparseQuadraticMatrix。
         * Create an empty SparseQuadraticMatrix.
         *
         * @return 空的 SparseQuadraticMatrix 实例 / An empty SparseQuadraticMatrix instance
         */
        fun invoke(): SparseQuadraticMatrix = SparseQuadraticMatrix()
    }
}
