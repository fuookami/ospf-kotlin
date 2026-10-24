package fuookami.ospf.kotlin.multiarray

import kotlin.test.*
import org.junit.jupiter.api.Test

/** 验证初始化快路径及生成器坐标的所有权。 / Verify initialization fast paths and generator coordinate ownership. */
class MultiArrayInitializationTest {
    @Test
    fun constantAndDefaultFactoriesDoNotDecodeCoordinates() {
        for (order in StorageOrder.entries) {
            val shape = CountingShape(DynShape.withOrder(intArrayOf(2, 3, 4), order))
            val value = Any()
            val immutable = MultiArray.newWith(shape, value)
            val mutable = MutableMultiArray.newWith(shape, value)
            assertTrue(immutable.all { it === value })
            assertTrue(mutable.all { it === value })
            assertTrue(MultiArray.newOrNull<Int, CountingShape>(shape)!!.all { it == 0 })
            assertTrue(MutableMultiArray.newOrNull<Int, CountingShape>(shape)!!.all { it == 0 })
            assertEquals(0, shape.coordinateReads)
            val replacement = Any()
            mutable[0] = replacement
            assertSame(value, immutable[0])
            assertSame(replacement, mutable[0])
            assertSame(value, mutable[1])
        }
    }

    @Test
    fun constantFactoriesPreserveEmptyAndScalarShapes() {
        for (dimensions in listOf(intArrayOf(2, 0, 3), intArrayOf())) {
            val shape = CountingShape(DynShape(dimensions))
            val expectedSize = if (dimensions.isEmpty()) 1 else 0
            assertEquals(List(expectedSize) { 7 }, MultiArray.newWith(shape, 7).toList())
            assertEquals(List(expectedSize) { 7 }, MutableMultiArray.newWith(shape, 7).toList())
            assertEquals(0, shape.coordinateReads)
        }
    }

    @Test
    fun generatorsStillReceiveIndependentCoordinatesInStorageOrder() {
        for (order in StorageOrder.entries) {
            val shape = DynShape.withOrder(intArrayOf(2, 3), order)
            val retained = mutableListOf<IntArray>()
            val array = MultiArray.newBy(shape) { index, vector ->
                retained.add(vector)
                index
            }
            assertEquals((0 until shape.size).toList(), array.toList())
            for (index in retained.indices) {
                assertContentEquals(shape.vectorUnchecked(index), retained[index])
            }
            retained[0][0] = 99
            assertFalse(retained[1][0] == 99)
        }
    }

    private class CountingShape(private val delegate: Shape) : Shape by delegate {
        var coordinateReads = 0
            private set

        override fun vectorOrNull(index: Int): IntArray? {
            coordinateReads++
            return delegate.vectorOrNull(index)
        }
    }
}
