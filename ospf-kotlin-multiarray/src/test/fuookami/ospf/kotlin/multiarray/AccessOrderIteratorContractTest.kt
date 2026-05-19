package fuookami.ospf.kotlin.multiarray

import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*

class AccessOrderIteratorContractTest {

    @Test
    fun `iterator returns independent snapshots`() {
        val shape = Shape2(2, 3)
        val iterator = MultiIndexIterator(shape, AccessOrder.RowMajor)

        val first = iterator.next()
        val second = iterator.next()

        // First and second should be independent arrays
        assertNotSame(first, second, "Iterator should return independent IntArray instances")

        // Modifying second should not affect first
        second[0] = 999
        assertEquals(0, first[0], "Modifying later iteration should not corrupt earlier result")
    }

    @Test
    fun `iterator contract hasNext then next`() {
        val shape = Shape1(3)
        val iterator = MultiIndexIterator(shape, AccessOrder.RowMajor)

        // Should iterate exactly 3 times
        assertTrue(iterator.hasNext())
        iterator.next() // [0]

        assertTrue(iterator.hasNext())
        iterator.next() // [1]

        assertTrue(iterator.hasNext())
        iterator.next() // [2]

        assertFalse(iterator.hasNext())

        // After hasNext() returns false, next() should throw
        assertThrows<NoSuchElementException> {
            iterator.next()
        }
    }

    @Test
    fun `RowMajor iteration order`() {
        val shape = Shape2(2, 3)
        val indices = shape.indices(AccessOrder.RowMajor).toList()

        assertEquals(6, indices.size)
        // RowMajor: last dimension varies fastest
        assertTrue(intArrayOf(0, 0).contentEquals(indices[0]))
        assertTrue(intArrayOf(0, 1).contentEquals(indices[1]))
        assertTrue(intArrayOf(0, 2).contentEquals(indices[2]))
        assertTrue(intArrayOf(1, 0).contentEquals(indices[3]))
        assertTrue(intArrayOf(1, 1).contentEquals(indices[4]))
        assertTrue(intArrayOf(1, 2).contentEquals(indices[5]))
    }

    @Test
    fun `ColumnMajor iteration order`() {
        val shape = Shape2(2, 3)
        val indices = shape.indices(AccessOrder.ColumnMajor).toList()

        assertEquals(6, indices.size)
        // ColumnMajor: first dimension varies fastest
        assertTrue(intArrayOf(0, 0).contentEquals(indices[0]))
        assertTrue(intArrayOf(1, 0).contentEquals(indices[1]))
        assertTrue(intArrayOf(0, 1).contentEquals(indices[2]))
        assertTrue(intArrayOf(1, 1).contentEquals(indices[3]))
        assertTrue(intArrayOf(0, 2).contentEquals(indices[4]))
        assertTrue(intArrayOf(1, 2).contentEquals(indices[5]))
    }

    @Test
    fun `iterWithOrder on view uses requested order`() {
        val array = MultiArray.newBy(Shape2(2, 3)) { i, _ -> i }
        val view = array[_a, _a]

        val rowMajorValues = view.iterWithOrder(AccessOrder.RowMajor).toList()
        val columnMajorValues = view.iterWithOrder(AccessOrder.ColumnMajor).toList()

        assertEquals(listOf(0, 1, 2, 3, 4, 5), rowMajorValues)
        assertEquals(listOf(0, 3, 1, 4, 2, 5), columnMajorValues)
    }

    @Test
    fun `fromList respects accessOrder`() {
        val shape = Shape2(2, 3)
        val columnMajorList = listOf(0, 3, 1, 4, 2, 5)

        val immutable = MultiArray.fromList(shape, columnMajorList, AccessOrder.ColumnMajor)
        val mutable = MutableMultiArray.fromList(shape, columnMajorList, AccessOrder.ColumnMajor)

        assertEquals(listOf(0, 1, 2, 3, 4, 5), immutable.flatten(AccessOrder.RowMajor))
        assertEquals(listOf(0, 1, 2, 3, 4, 5), mutable.flatten(AccessOrder.RowMajor))
        assertEquals(listOf(0, 3, 1, 4, 2, 5), immutable.flatten(AccessOrder.ColumnMajor))
        assertEquals(listOf(0, 3, 1, 4, 2, 5), mutable.flatten(AccessOrder.ColumnMajor))
    }
}