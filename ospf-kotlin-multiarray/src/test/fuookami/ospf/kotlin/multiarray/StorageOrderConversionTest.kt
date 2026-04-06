package fuookami.ospf.kotlin.multiarray

import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*

class StorageOrderConversionTest {

    @Test
    fun `toStorageOrder preserves vector access values`() {
        val rowArray = MultiArray.newBy(Shape3(2, 3, 4)) { i, _ -> i }

        val colArray = rowArray.toStorageOrder(StorageOrder.ColumnMajor)

        // Same vector index should give same value
        for (i in 0 until 2) {
            for (j in 0 until 3) {
                for (k in 0 until 4) {
                    assertEquals(rowArray[i, j, k], colArray[i, j, k],
                        "Value at [$i, $j, $k] should be preserved")
                }
            }
        }
    }

    @Test
    fun `roundtrip toStorageOrder preserves all values`() {
        val original = MultiArray.newBy(Shape3(2, 3, 4)) { i, _ -> i }
        val rowMajor = original.toStorageOrder(StorageOrder.RowMajor)
        val backToCol = rowMajor.toStorageOrder(StorageOrder.ColumnMajor)
        val backToRow = backToCol.toStorageOrder(StorageOrder.RowMajor)

        // All roundtrips should preserve values
        for (i in 0 until original.size) {
            assertEquals(original.list[i], backToRow.list[i],
                "Roundtrip conversion should preserve linear index $i")
        }
    }

    @Test
    fun `toStorageOrder changes memory layout not values`() {
        val rowArray = MultiArray.newBy(Shape2(2, 3)) { i, _ -> i }

        // RowMajor: [0,0]=0, [0,1]=1, [0,2]=2, [1,0]=3, [1,1]=4, [1,2]=5
        assertEquals(0, rowArray[0, 0])
        assertEquals(1, rowArray[0, 1])
        assertEquals(2, rowArray[0, 2])
        assertEquals(3, rowArray[1, 0])
        assertEquals(4, rowArray[1, 1])
        assertEquals(5, rowArray[1, 2])

        val colArray = rowArray.toStorageOrder(StorageOrder.ColumnMajor)

        // ColumnMajor: same vector access should give same values
        assertEquals(0, colArray[0, 0])
        assertEquals(1, colArray[0, 1])
        assertEquals(2, colArray[0, 2])
        assertEquals(3, colArray[1, 0])
        assertEquals(4, colArray[1, 1])
        assertEquals(5, colArray[1, 2])

        // But memory layout changed: ColumnMajor linear indices
        // [0,0]=0, [1,0]=3, [0,1]=1, [1,1]=4, [0,2]=2, [1,2]=5
        assertEquals(0, colArray.list[0])
        assertEquals(3, colArray.list[1])
        assertEquals(1, colArray.list[2])
        assertEquals(4, colArray.list[3])
        assertEquals(2, colArray.list[4])
        assertEquals(5, colArray.list[5])
    }
}