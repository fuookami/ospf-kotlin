package fuookami.ospf.kotlin.core.intermediate_model

import kotlin.test.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.core.model.intermediate.*

class SparseMatrixTransposeTest {
    @Test
    fun transposeShouldHandleEmptyMatrix() {
        val matrix = SparseMatrix<Flt64>()

        val transposed = matrix.transpose()

        assertEquals(0, transposed.numRows())
    }

    @Test
    fun transposeShouldPreserveSparseGapRows() {
        val matrix = SparseMatrix<Flt64>()
        val row = SparseVector<Flt64>()
        row.add(0, Flt64.one)
        row.add(2, Flt64(3.0))
        matrix.addRow(row)

        val transposed = matrix.transpose()

        assertEquals(3, transposed.numRows())
        assertEquals(1, transposed.rowSize(0))
        assertEquals(0, transposed.rowSize(1))
        assertEquals(1, transposed.rowSize(2))

        val row0Entry = transposed.getRow(0)!!.entries[0]
        assertEquals(0, row0Entry.index)
        assertTrue(row0Entry.value eq Flt64.one)

        val row2Entry = transposed.getRow(2)!!.entries[0]
        assertEquals(0, row2Entry.index)
        assertTrue(row2Entry.value eq Flt64(3.0))
    }

    @Test
    fun transposeShouldMapRowsToColumns() {
        val matrix = SparseMatrix<Flt64>()
        val row0 = SparseVector<Flt64>()
        row0.add(1, Flt64(2.0))
        val row1 = SparseVector<Flt64>()
        row1.add(0, Flt64(4.0))
        row1.add(2, Flt64(5.0))
        matrix.addRow(row0)
        matrix.addRow(row1)

        val transposed = matrix.transpose()

        assertEquals(3, transposed.numRows())

        val t0 = transposed.getRow(0)!!.entries
        assertEquals(1, t0.size)
        assertEquals(1, t0[0].index)
        assertTrue(t0[0].value eq Flt64(4.0))

        val t1 = transposed.getRow(1)!!.entries
        assertEquals(1, t1.size)
        assertEquals(0, t1[0].index)
        assertTrue(t1[0].value eq Flt64(2.0))

        val t2 = transposed.getRow(2)!!.entries
        assertEquals(1, t2.size)
        assertEquals(1, t2[0].index)
        assertTrue(t2[0].value eq Flt64(5.0))
    }

    @Test
    fun transposeShouldInferMaxColumnFromAllRows() {
        val matrix = SparseMatrix<Flt64>()
        matrix.addRow(SparseVector())
        val row1 = SparseVector<Flt64>()
        row1.add(3, Flt64(7.0))
        matrix.addRow(row1)

        val transposed = matrix.transpose()

        assertEquals(4, transposed.numRows())
        assertEquals(0, transposed.rowSize(0))
        assertEquals(0, transposed.rowSize(1))
        assertEquals(0, transposed.rowSize(2))
        assertEquals(1, transposed.rowSize(3))
        val entry = transposed.getRow(3)!!.entries[0]
        assertEquals(1, entry.index)
        assertTrue(entry.value eq Flt64(7.0))
    }
}
