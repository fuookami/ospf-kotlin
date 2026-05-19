package fuookami.ospf.kotlin.multiarray

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MultiArrayTest {

    @Test
    fun testMultiArray1Creation() {
        val array = MultiArray(Shape1(5)) { i, _ -> i * 2 }

        assertEquals(5, array.size)
        assertEquals(1, array.dimension)
        assertEquals(0, array[0])
        assertEquals(2, array[1])
        assertEquals(8, array[4])
    }

    @Test
    fun testMultiArray2Creation() {
        val array = MultiArray(Shape2(3, 4)) { i, v -> v[0] * 4 + v[1] }

        assertEquals(12, array.size)
        assertEquals(2, array.dimension)
        assertEquals(0, array[0, 0])
        assertEquals(1, array[0, 1])
        assertEquals(4, array[1, 0])
        assertEquals(11, array[2, 3])
    }

    @Test
    fun testMultiArray3Creation() {
        val array = MultiArray(Shape3(2, 3, 4)) { i, _ -> i }

        assertEquals(24, array.size)
        assertEquals(3, array.dimension)
        assertEquals(0, array[0, 0, 0])
        assertEquals(1, array[0, 0, 1])
        assertEquals(23, array[1, 2, 3])
    }

    @Test
    fun testMultiArrayNewWithDefault() {
        val array = MultiArray.new<Int, Shape1>(Shape1(5))

        assertEquals(5, array.size)
        assertEquals(0, array[0])
        assertEquals(0, array[4])
    }

    @Test
    fun testMultiArrayNewWith() {
        val array = MultiArray.newWith(Shape2(3, 3), 42)

        assertEquals(9, array.size)
        for (i in 0 until 3) {
            for (j in 0 until 3) {
                assertEquals(42, array[i, j])
            }
        }
    }

    @Test
    fun testMultiArrayNewBy() {
        val array = MultiArray.newBy(Shape2(3, 4)) { i, v -> v[0] + v[1] }

        assertEquals(0, array[0, 0])
        assertEquals(1, array[0, 1])
        assertEquals(1, array[1, 0])
        assertEquals(5, array[2, 3])
    }

    @Test
    fun testMutableMultiArrayCreation() {
        val array = MutableMultiArray(Shape2(3, 4)) { _, _ -> 0 }

        assertEquals(12, array.size)
        assertEquals(0, array[0, 0])
    }

    @Test
    fun testMutableMultiArraySet() {
        val array = MutableMultiArray.newWith(Shape2(3, 4), 0)

        array[0, 0] = 1
        array[1, 2] = 5
        array[2, 3] = 10

        assertEquals(1, array[0, 0])
        assertEquals(5, array[1, 2])
        assertEquals(10, array[2, 3])
        assertEquals(0, array[0, 1])
    }

    @Test
    fun testMutableMultiArraySetByIntArray() {
        val array = MutableMultiArray.newWith(Shape2(3, 4), 0)

        array.set(intArrayOf(1, 2), 5)
        assertEquals(5, array[1, 2])
    }

    @Test
    fun testMutableMultiArrayFill() {
        val array = MutableMultiArray.newWith(Shape2(3, 4), 0)

        array.fill(42)

        for (i in 0 until 3) {
            for (j in 0 until 4) {
                assertEquals(42, array[i, j])
            }
        }
    }

    @Test
    fun testMutableMultiArrayFillBy() {
        val array = MutableMultiArray.newWith(Shape2(3, 4), 0)

        array.fillBy { i, v -> v[0] * 4 + v[1] }

        assertEquals(0, array[0, 0])
        assertEquals(1, array[0, 1])
        assertEquals(4, array[1, 0])
        assertEquals(11, array[2, 3])
    }

    @Test
    fun testMutableMultiArrayToImmutable() {
        val mutable = MutableMultiArray.newWith(Shape2(2, 2), 5)
        val immutable = mutable.toImmutable()

        assertEquals(5, immutable[0, 0])
        assertEquals(5, immutable[1, 1])
    }

    @Test
    fun testMultiArrayGetByLinearIndex() {
        val array = MultiArray(Shape2(3, 4)) { i, _ -> i }

        assertEquals(0, array[0])
        assertEquals(1, array[1])
        assertEquals(11, array[11])
    }

    @Test
    fun testMultiArrayGetByVectorIndex() {
        val array = MultiArray(Shape2(3, 4)) { i, _ -> i }

        assertEquals(0, array[intArrayOf(0, 0)])
        assertEquals(1, array[intArrayOf(0, 1)])
        assertEquals(4, array[intArrayOf(1, 0)])
        assertEquals(11, array[intArrayOf(2, 3)])
    }

    @Test
    fun testMultiArrayGetByVararg() {
        val array = MultiArray(Shape2(3, 4)) { i, _ -> i }

        assertEquals(0, array[0, 0])
        assertEquals(1, array[0, 1])
        assertEquals(4, array[1, 0])
        assertEquals(11, array[2, 3])
    }

    @Test
    fun testMultiArrayEnumerate() {
        val array = MultiArray(Shape2(2, 3)) { i, _ -> i * 10 }

        val results = array.enumerate().toList()

        assertEquals(6, results.size)
        for ((linearIdx, vec, value) in results) {
            assertEquals(linearIdx * 10, value)
        }
    }

    @Test
    fun testMultiArrayToMutable() {
        val immutable = MultiArray.newWith(Shape2(2, 2), 5)
        val mutable = immutable.toMutable()

        mutable[0, 0] = 10
        assertEquals(10, mutable[0, 0])
        assertEquals(5, immutable[0, 0])
    }

    @Test
    fun testMultiArrayToList() {
        val array = MultiArray(Shape2(2, 3)) { i, _ -> i }
        val list = array.toList()

        assertEquals(6, list.size)
        assertEquals(listOf(0, 1, 2, 3, 4, 5), list)
    }

    @Test
    fun testMultiArrayToStorageOrder() {
        val arrayRow = MultiArray(Shape2(2, 3)) { i, _ -> i }
        val arrayCol = arrayRow.toStorageOrder(StorageOrder.ColumnMajor)

        assertEquals(6, arrayCol.size)
        assertEquals(StorageOrder.ColumnMajor, arrayCol.shape.storageOrder)
    }

    @Test
    fun testMultiArrayReshape() {
        val array = MultiArray(Shape2(2, 3)) { i, _ -> i }
        val reshaped = array.reshape(Shape1(6), 0)

        assertEquals(6, reshaped.size)
        assertEquals(0, reshaped[0])
        assertEquals(5, reshaped[5])
    }

    @Test
    fun testMultiArrayReshapeWithFill() {
        val array = MultiArray(Shape1(3)) { i, _ -> i }
        val reshaped = array.reshape(Shape1(5), 99)

        assertEquals(5, reshaped.size)
        assertEquals(0, reshaped[0])
        assertEquals(2, reshaped[2])
        assertEquals(99, reshaped[3])
        assertEquals(99, reshaped[4])
    }

    @Test
    fun testMultiArrayReshapeBy() {
        val array = MultiArray(Shape1(3)) { i, _ -> i }
        val reshaped = array.reshapeBy(Shape1(5)) { i, _ -> i * 10 }

        assertEquals(0, reshaped[0])
        assertEquals(2, reshaped[2])
        assertEquals(30, reshaped[3])
        assertEquals(40, reshaped[4])
    }

    @Test
    fun testMultiArrayContains() {
        val array = MultiArray(Shape2(2, 3)) { i, _ -> i }

        assertTrue(0 in array)
        assertTrue(5 in array)
        assertFalse(6 in array)
    }

    @Test
    fun testMultiArrayContainsAll() {
        val array = MultiArray(Shape2(2, 3)) { i, _ -> i }

        assertTrue(array.containsAll(listOf(0, 1, 2)))
        assertFalse(array.containsAll(listOf(0, 6)))
    }

    @Test
    fun testMultiArrayIsEmpty() {
        val array1 = MultiArray.newWith(Shape2(2, 3), 0)
        val array2 = MultiArray.newWith(Shape2(0, 3), 0)

        assertFalse(array1.isEmpty())
        assertTrue(array2.isEmpty())
    }

    @Test
    fun testMultiArrayIterator() {
        val array = MultiArray(Shape2(2, 3)) { i, _ -> i }

        val values = array.toList()
        assertEquals(listOf(0, 1, 2, 3, 4, 5), values)
    }

    @Test
    fun testMultiArrayOf() {
        val array1 = multiArrayOf(5, 42)
        assertEquals(5, array1.size)
        assertEquals(42, array1[0])

        val array2 = multiArrayOf(3, 4, 42)
        assertEquals(12, array2.size)
        assertEquals(42, array2[0, 0])

        val array3 = multiArrayOf(2, 3, 4, 42)
        assertEquals(24, array3.size)
        assertEquals(42, array3[0, 0, 0])
    }

    @Test
    fun testMutableMultiArrayOf() {
        val array1 = mutableMultiArrayOf(5, 42)
        assertEquals(5, array1.size)
        array1[0] = 100
        assertEquals(100, array1[0])

        val array2 = mutableMultiArrayOf(3, 4, 42)
        assertEquals(12, array2.size)
        array2[0, 0] = 100
        assertEquals(100, array2[0, 0])
    }

    @Test
    fun testTypeAliases() {
        val array1: MultiArray1<Int> = MultiArray.newWith(Shape1(5), 0)
        assertEquals(5, array1.size)

        val array2: MultiArray2<Int> = MultiArray.newWith(Shape2(3, 4), 0)
        assertEquals(12, array2.size)

        val array3: MultiArray3<Int> = MultiArray.newWith(Shape3(2, 3, 4), 0)
        assertEquals(24, array3.size)

        val array4: MultiArray4<Int> = MultiArray.newWith(Shape4(2, 2, 2, 2), 0)
        assertEquals(16, array4.size)

        val dynArray: DynMultiArray<Int> = MultiArray.newWith(DynShape(intArrayOf(3, 4)), 0)
        assertEquals(12, dynArray.size)
    }

    @Test
    fun testMutableTypeAliases() {
        val array1: MutableMultiArray1<Int> = MutableMultiArray.newWith(Shape1(5), 0)
        assertEquals(5, array1.size)

        val array2: MutableMultiArray2<Int> = MutableMultiArray.newWith(Shape2(3, 4), 0)
        assertEquals(12, array2.size)
    }

    @Test
    fun testMultiArrayZeroSize() {
        val array = MultiArray.newWith(Shape2(0, 5), 0)

        assertEquals(0, array.size)
        assertTrue(array.isEmpty())
    }

    @Test
    fun testMultiArrayLargeSize() {
        val array = MultiArray(Shape2(100, 100)) { i, _ -> i }

        assertEquals(10000, array.size)
        assertEquals(0, array[0, 0])
        assertEquals(9999, array[99, 99])
    }

    @Test
    fun testMultiArrayIndexOutOfBounds() {
        val array = MultiArray(Shape2(3, 4)) { i, _ -> i }

        assertFailsWith<OutOfShapeException> { array[3, 0] }
        assertFailsWith<OutOfShapeException> { array[0, 4] }
    }
}