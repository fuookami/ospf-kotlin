package fuookami.ospf.kotlin.multiarray

import kotlin.random.Random
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*

class ShapeColumnMajorInverseTest {

    @Test
    fun `Shape2 ColumnMajor index vector roundtrip`() {
        val shape = Shape2.withOrder(3, 4, StorageOrder.ColumnMajor)

        for (i in 0 until shape.size) {
            val vec = shape.vectorValue(i)
            val idx = shape.indexValue(vec)
            assertEquals(i, idx, "index(vector($i)) should equal $i")
        }
    }

    @Test
    fun `Shape3 ColumnMajor index vector roundtrip`() {
        val shape = Shape3.withOrder(2, 3, 4, StorageOrder.ColumnMajor)

        for (i in 0 until shape.size) {
            val vec = shape.vectorValue(i)
            val idx = shape.indexValue(vec)
            assertEquals(i, idx, "index(vector($i)) should equal $i")
        }
    }

    @Test
    fun `Shape4 ColumnMajor index vector roundtrip`() {
        val shape = Shape4.withOrder(2, 3, 4, 5, StorageOrder.ColumnMajor)

        for (i in 0 until shape.size) {
            val vec = shape.vectorValue(i)
            val idx = shape.indexValue(vec)
            assertEquals(i, idx, "index(vector($i)) should equal $i")
        }
    }

    @Test
    fun `DynShape ColumnMajor index vector roundtrip`() {
        val shape = DynShape.withOrder(intArrayOf(2, 3, 4, 5), StorageOrder.ColumnMajor)

        for (i in 0 until shape.size) {
            val vec = shape.vectorValue(i)
            val idx = shape.indexValue(vec)
            assertEquals(i, idx, "index(vector($i)) should equal $i")
        }
    }

    @Test
    fun `Shape3 ColumnMajor random points`() {
        val shape = Shape3.withOrder(5, 7, 11, StorageOrder.ColumnMajor)
        val random = Random(42)

        repeat(100) {
            val i = random.nextInt(shape.size)
            val vec = shape.vectorValue(i)
            val idx = shape.indexValue(vec)
            assertEquals(i, idx)
        }
    }

    /** 获取形状指定向量的线性索引值 / Get linear index value for shape at given vector */
    private fun Shape.indexValue(vector: IntArray): Int {
        return index(vector).value ?: fail("index should succeed")
    }

    /** 获取形状指定线性索引的向量值 / Get vector value for shape at given linear index */
    private fun Shape.vectorValue(index: Int): IntArray {
        return vector(index).value ?: fail("vector should succeed")
    }
}
