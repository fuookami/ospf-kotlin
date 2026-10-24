package fuookami.ospf.kotlin.multiarray

import kotlin.test.*
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.utils.functional.*

class AccessOrderLinearPathTest {
    @Test
    fun matchingAndOppositeOrdersRetainCoordinateSequenceAcrossBuiltInShapes() {
        val shapes: List<Shape> = listOf(
            Shape1(5),
            Shape2(2, 3),
            Shape3(2, 3, 4),
            Shape4(2, 1, 3, 2),
            DynShape(intArrayOf()),
            DynShape(intArrayOf(2, 0, 3)),
            DynShape(intArrayOf(2, 1, 3, 2, 1))
        )
        for (base in shapes) {
            for (storageOrder in listOf(StorageOrder.RowMajor, StorageOrder.ColumnMajor)) {
                val shape = when (base) {
                    is Shape1 -> base.withStorageOrder(storageOrder)
                    is Shape2 -> base.withStorageOrder(storageOrder)
                    is Shape3 -> base.withStorageOrder(storageOrder)
                    is Shape4 -> base.withStorageOrder(storageOrder)
                    else -> (base as DynShape).withStorageOrder(storageOrder)
                }
                val array = MultiArray.newBy(shape) { index, _ -> index }
                for (order in listOf(AccessOrder.RowMajor, AccessOrder.ColumnMajor)) {
                    val original = shape.iterate(order).map { array[it] }.toList()
                    assertEquals(original, array.iterWithOrder(order).toList())
                    assertEquals(original, array.flatten(order))
                }
            }
        }
    }

    @Test
    fun customShapeIndexingAndMutableLazyReadsRetainOriginalBehavior() {
        val base = Shape1(4)
        var calls = 0
        val custom = object : Shape by base {
            override fun index(vector: IntArray): Ret<Int> {
                ++calls
                return base.index(vector).map { 3 - it }
            }

            override fun indexOrNull(vector: IntArray): Int? = index(vector).value
        }
        val customArray = MultiArray.newBy(custom) { index, _ -> index }
        assertEquals(listOf(3, 2, 1, 0), customArray.flatten())
        assertEquals(4, calls)
        val mutable = MutableMultiArray.newBy(base) { index, _ -> index }
        val values = mutable.iterWithOrder().iterator()
        assertEquals(0, values.next())
        mutable[1] = 99
        assertEquals(99, values.next())
        assertEquals(listOf(2, 3), values.asSequence().toList())
    }

    @Test
    fun publiclyModifiedOffsetsRetainCoordinateIndexingFallback() {
        val shape = Shape2(2, 2)
        val array = MultiArray.newBy(shape) { index, _ -> index }
        shape.offsets[0] = 1
        val original = shape.iterate().map { array[it] }.toList()
        assertEquals(listOf(0, 1, 1, 2), original)
        assertEquals(original, array.flatten())
    }
}
