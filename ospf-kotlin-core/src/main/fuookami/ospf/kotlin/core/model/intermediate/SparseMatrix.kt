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
 */
data class SparseVectorEntry<V : RealNumber<V>>(
    val index: Int,
    val value: V
)

/**
 * Sparse vector backed by a list of (index, value) pairs.
 * Aligns with Rust `SparseVector<V>` in basic_linear_triad_model.rs.
 */
class SparseVector<V : RealNumber<V>>(
    val entries: MutableList<SparseVectorEntry<V>> = mutableListOf()
) {
    fun add(index: Int, value: V) {
        entries.add(SparseVectorEntry(index, value))
    }

    fun len(): Int = entries.size

    fun isEmpty(): Boolean = entries.isEmpty()

    /**
     * Iterate over all entries in this vector, invoking [action] with (index, value).
     */
    inline fun forEachEntry(action: (index: Int, value: V) -> Unit) {
        for (i in entries.indices) {
            val entry = entries[i]
            action(entry.index, entry.value)
        }
    }

    override fun toString(): String = "SparseVector($entries)"

    companion object {
        fun <V : RealNumber<V>> invoke(): SparseVector<V> = SparseVector()
    }
}

/**
 * Sparse matrix backed by a list of [SparseVector] rows.
 * Aligns with Rust `SparseMatrix<V>` in basic_linear_triad_model.rs.
 */
class SparseMatrix<V : RealNumber<V>>(
    val rows: MutableList<SparseVector<V>> = mutableListOf()
) {
    fun addRow(row: SparseVector<V>) {
        rows.add(row)
    }

    /**
     * Number of rows in this matrix.
     */
    fun numRows(): Int = rows.size

    /**
     * Number of non-zero entries in row [row].
     */
    fun rowSize(row: Int): Int = rows.getOrNull(row)?.len() ?: 0

    fun getRow(index: Int): SparseVector<V>? = rows.getOrNull(index)

    /**
     * Iterate over all non-zero entries in row [row], invoking [action] with (colIndex, value).
     * If [row] is out of bounds, the call is a no-op.
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
     * Iterate over all rows, invoking [action] with (rowIndex, SparseVector).
     */
    inline fun forEachRow(action: (rowIndex: Int, row: SparseVector<V>) -> Unit) {
        for (i in rows.indices) {
            action(i, rows[i])
        }
    }

    /**
     * Build the transpose of this matrix: each entry (r, c, v) becomes (c, r, v).
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
        fun <V : RealNumber<V>> invoke(): SparseMatrix<V> = SparseMatrix()
    }
}

/**
 * Negate a [SparseMatrix<fuookami.ospf.kotlin.math.algebra.number.Flt64>] in-place: multiply every entry by -1.
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
 * Return a negated copy of this [SparseMatrix<fuookami.ospf.kotlin.math.algebra.number.Flt64>].
 */
fun SparseMatrix<Flt64>.negated(): SparseMatrix<Flt64> {
    val result = SparseMatrix<Flt64>()
    for (row in rows) {
        val newRow = SparseVector<fuookami.ospf.kotlin.math.algebra.number.Flt64>()
        for (entry in row.entries) {
            newRow.add(entry.index, -entry.value)
        }
        result.addRow(newRow)
    }
    return result
}

/**
 * Scale a [SparseMatrix<fuookami.ospf.kotlin.math.algebra.number.Flt64>] in-place by [factor].
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
 * A single entry in a [SparseQuadraticVector], pairing (colIndex1, colIndex2?, coefficient).
 * Used for quadratic constraint rows where each term may be linear (colIndex2 == null)
 * or quadratic (colIndex2 != null).
 */
data class SparseQuadraticEntry(
    val colIndex1: Int,
    val colIndex2: Int?,
    val coefficient: Flt64
)

/**
 * Sparse quadratic vector: a row of quadratic constraint entries.
 */
class SparseQuadraticVector(
    val entries: MutableList<SparseQuadraticEntry> = mutableListOf()
) {
    fun add(colIndex1: Int, colIndex2: Int?, coefficient: Flt64) {
        entries.add(SparseQuadraticEntry(colIndex1, colIndex2, coefficient))
    }

    fun len(): Int = entries.size

    fun isEmpty(): Boolean = entries.isEmpty()

    /**
     * Iterate over all entries, invoking [action] with (colIndex1, colIndex2, coefficient).
     */
    inline fun forEachEntry(action: (colIndex1: Int, colIndex2: Int?, coefficient: Flt64) -> Unit) {
        for (i in entries.indices) {
            val entry = entries[i]
            action(entry.colIndex1, entry.colIndex2, entry.coefficient)
        }
    }

    override fun toString(): String = "SparseQuadraticVector($entries)"

    companion object {
        fun invoke(): SparseQuadraticVector = SparseQuadraticVector()
    }
}

/**
 * Sparse quadratic matrix: a list of [SparseQuadraticVector] rows.
 * Used as the sparse representation of quadratic constraint LHS.
 */
class SparseQuadraticMatrix(
    val rows: MutableList<SparseQuadraticVector> = mutableListOf()
) {
    fun addRow(row: SparseQuadraticVector) {
        rows.add(row)
    }

    /**
     * Number of rows in this matrix.
     */
    fun numRows(): Int = rows.size

    /**
     * Number of entries in row [row].
     */
    fun rowSize(row: Int): Int = rows.getOrNull(row)?.len() ?: 0

    fun getRow(index: Int): SparseQuadraticVector? = rows.getOrNull(index)

    /**
     * Iterate over all entries in row [row], invoking [action] with (colIndex1, colIndex2, coefficient).
     * If [row] is out of bounds, the call is a no-op.
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
        fun invoke(): SparseQuadraticMatrix = SparseQuadraticMatrix()
    }
}
