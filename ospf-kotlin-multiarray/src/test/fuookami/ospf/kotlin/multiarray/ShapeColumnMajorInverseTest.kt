package fuookami.ospf.kotlin.multiarray

import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import kotlin.random.Random

class ShapeColumnMajorInverseTest {

    @Test
    fun `Shape2 ColumnMajor index vector roundtrip`() {
        val shape = Shape2.withOrder(3, 4, StorageOrder.ColumnMajor)

        for (i in 0 until shape.size) {
            val vec = shape.vector(i)
            val idx = shape.index(vec)
            assertEquals(i, idx, "index(vector($i)) should equal $i")
        }
    }

    @Test
    fun `Shape3 ColumnMajor index vector roundtrip`() {
        val shape = Shape3.withOrder(2, 3, 4, StorageOrder.ColumnMajor)

        for (i in 0 until shape.size) {
            val vec = shape.vector(i)
            val idx = shape.index(vec)
            assertEquals(i, idx, "index(vector($i)) should equal $i")
        }
    }

    @Test
    fun `Shape4 ColumnMajor index vector roundtrip`() {
        val shape = Shape4.withOrder(2, 3, 4, 5, StorageOrder.ColumnMajor)

        for (i in 0 until shape.size) {
            val vec = shape.vector(i)
            val idx = shape.index(vec)
            assertEquals(i, idx, "index(vector($i)) should equal $i")
        }
    }

    @Test
    fun `DynShape ColumnMajor index vector roundtrip`() {
        val shape = DynShape.withOrder(intArrayOf(2, 3, 4, 5), StorageOrder.ColumnMajor)

        for (i in 0 until shape.size) {
            val vec = shape.vector(i)
            val idx = shape.index(vec)
            assertEquals(i, idx, "index(vector($i)) should equal $i")
        }
    }

    @Test
    fun `Shape3 ColumnMajor random points`() {
        val shape = Shape3.withOrder(5, 7, 11, StorageOrder.ColumnMajor)
        val random = Random(42)

        repeat(100) {
            val i = random.nextInt(shape.size)
            val vec = shape.vector(i)
            val idx = shape.index(vec)
            assertEquals(i, idx)
        }
    }
}