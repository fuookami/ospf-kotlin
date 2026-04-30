package fuookami.ospf.kotlin.core.model.intermediate

import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.Flt64

/**
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
    fun forEachEntry(action: (index: Int, value: V) -> Unit) {
        for (entry in entries) {
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
    fun forEachEntry(row: Int, action: (colIndex: Int, value: V) -> Unit) {
        val r = rows.getOrNull(row) ?: return
        r.forEachEntry(action)
    }

    /**
     * Iterate over all rows, invoking [action] with (rowIndex, SparseVector).
     */
    fun forEachRow(action: (rowIndex: Int, row: SparseVector<V>) -> Unit) {
        for ((index, row) in rows.withIndex()) {
            action(index, row)
        }
    }

    /**
     * Build the transpose of this matrix: each entry (r, c, v) becomes (c, r, v).
     */
    fun transpose(): SparseMatrix<V> {
        val result = SparseMatrix<V>()
        // First pass: determine column count
        val maxCol = rows.flatMap { it.entries }.maxOfOrNull { it.index } ?: -1
        if (maxCol < 0) return result
        // Pre-allocate rows in result
        for (c in 0..maxCol) {
            result.addRow(SparseVector())
        }
        // Fill
        for ((r, row) in rows.withIndex()) {
            for (entry in row.entries) {
                result.rows[entry.index].add(r, entry.value)
            }
        }
        return result
    }

    override fun toString(): String = "SparseMatrix($rows)"

    companion object {
        fun <V : RealNumber<V>> invoke(): SparseMatrix<V> = SparseMatrix()
    }
}

typealias SparseVectorFlt64 = SparseVector<Flt64>
typealias SparseMatrixFlt64 = SparseMatrix<Flt64>

/**
 * Negate a [SparseMatrixFlt64] in-place: multiply every entry by -1.
 */
fun SparseMatrixFlt64.negateInPlace(): SparseMatrixFlt64 {
    for (row in rows) {
        for (i in row.entries.indices) {
            val e = row.entries[i]
            row.entries[i] = SparseVectorEntry(e.index, -e.value)
        }
    }
    return this
}

/**
 * Return a negated copy of this [SparseMatrixFlt64].
 */
fun SparseMatrixFlt64.negated(): SparseMatrixFlt64 {
    val result = SparseMatrixFlt64()
    for (row in rows) {
        val newRow = SparseVectorFlt64()
        for (entry in row.entries) {
            newRow.add(entry.index, -entry.value)
        }
        result.addRow(newRow)
    }
    return result
}

/**
 * Scale a [SparseMatrixFlt64] in-place by [factor].
 */
fun SparseMatrixFlt64.scaleInPlace(factor: Flt64): SparseMatrixFlt64 {
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
    fun forEachEntry(action: (colIndex1: Int, colIndex2: Int?, coefficient: Flt64) -> Unit) {
        for (entry in entries) {
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
    fun forEachEntry(row: Int, action: (colIndex1: Int, colIndex2: Int?, coefficient: Flt64) -> Unit) {
        val r = rows.getOrNull(row) ?: return
        r.forEachEntry(action)
    }

    override fun toString(): String = "SparseQuadraticMatrix($rows)"

    companion object {
        fun invoke(): SparseQuadraticMatrix = SparseQuadraticMatrix()
    }
}
