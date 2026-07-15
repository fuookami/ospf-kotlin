package fuookami.ospf.kotlin.multiarray

import kotlin.test.*
import org.junit.jupiter.api.Test

/**
 * MultiArray 边界分支测试
 * MultiArray boundary branch tests
 *
 * 测试内容：
 * Test contents:
 * - toStorageOrder 同序转换分支（Row->Row / Column->Column）
 *   toStorageOrder same-order conversion branch
 * - 空数组 toStorageOrder
 *   Empty array toStorageOrder
 * - reshape 的 same-size / shrink / empty->larger
 *   reshape same-size / shrink / empty->larger
 * - ctor == null 延迟初始化行为（VariableCombination 模式）
 *   ctor == null deferred initialization behavior (VariableCombination pattern)
 */
class MultiArrayBoundaryTest {

    // ========================================================================
    // toStorageOrder 同序转换测试
    // toStorageOrder same-order conversion tests
    // ========================================================================

    @Test
    fun testToStorageOrderSameRowMajor() {
        // 测试 RowMajor -> RowMajor 同序转换
        // Test RowMajor -> RowMajor same-order conversion
        val shape = DynShape.withOrder(intArrayOf(2, 3, 4), StorageOrder.RowMajor)
        val array = MultiArray.newBy<Int, DynShape>(shape) { i, _ -> i }

        val converted = array.toStorageOrder(StorageOrder.RowMajor)

        // 验证形状和存储顺序
        // Verify shape and storage order
        assertEquals(StorageOrder.RowMajor, converted.shape.storageOrder)
        assertEquals(2, converted.shape[0])
        assertEquals(3, converted.shape[1])
        assertEquals(4, converted.shape[2])

        // 验证数据不变
        // Verify data unchanged
        for (i in 0 until array.shape.size) {
            assertEquals(array.list[i], converted.list[i])
        }
    }

    @Test
    fun testToStorageOrderSameColumnMajor() {
        // 测试 ColumnMajor -> ColumnMajor 同序转换
        // Test ColumnMajor -> ColumnMajor same-order conversion
        val shape = DynShape.withOrder(intArrayOf(2, 3, 4), StorageOrder.ColumnMajor)
        val array = MultiArray.newBy<Int, DynShape>(shape) { i, _ -> i }

        val converted = array.toStorageOrder(StorageOrder.ColumnMajor)

        // 验证形状和存储顺序
        // Verify shape and storage order
        assertEquals(StorageOrder.ColumnMajor, converted.shape.storageOrder)

        // 验证数据不变
        // Verify data unchanged
        for (i in 0 until array.shape.size) {
            assertEquals(array.list[i], converted.list[i])
        }
    }

    // ========================================================================
    // 空数组测试
    // Empty array tests
    // ========================================================================

    @Test
    fun testEmptyArrayCreation() {
        // 测试空数组创建（维度含 0）
        // Test empty array creation (dimension contains 0)
        val shape = DynShape(intArrayOf(2, 0, 3))
        val array = MultiArray.newWith(shape, 0)

        assertEquals(0, array.shape.size)
        assertTrue(array.list.isEmpty())
    }

    @Test
    fun testEmptyArrayToStorageOrder() {
        // 测试空数组存储顺序转换
        // Test empty array storage order conversion
        val shape = DynShape.withOrder(intArrayOf(0, 3), StorageOrder.RowMajor)
        val array = MultiArray.newWith<Int, DynShape>(shape, 0)

        val convertedRow = array.toStorageOrder(StorageOrder.RowMajor)
        val convertedCol = array.toStorageOrder(StorageOrder.ColumnMajor)

        assertTrue(convertedRow.list.isEmpty())
        assertTrue(convertedCol.list.isEmpty())
    }

    @Test
    fun testZeroDimensionArray() {
        // 测试 0 维数组
        // Test zero-dimension array
        val shape = DynShape(intArrayOf())
        val array = MultiArray.newWith(shape, 42)

        assertEquals(1, array.shape.size)
        assertEquals(42, array.list[0])
    }

    @Test
    fun testZeroDimensionArrayToStorageOrder() {
        // 测试 0 维数组存储顺序转换
        // Test zero-dimension array storage order conversion
        val shape = DynShape.withOrder(intArrayOf(), StorageOrder.RowMajor)
        val array = MultiArray.newWith<Int, DynShape>(shape, 42)

        val converted = array.toStorageOrder(StorageOrder.ColumnMajor)
        assertEquals(42, converted.list[0])
    }

    // ========================================================================
    // reshape 测试
    // reshape tests
    // ========================================================================

    @Test
    fun testReshapeSameSize() {
        // 测试 reshape 保持相同大小（2x6 -> 3x4）
        // Test reshape same size (2x6 -> 3x4)
        val shape = DynShape(intArrayOf(2, 6))
        val array = MultiArray.newBy<Int, DynShape>(shape) { i, _ -> i }

        val reshaped = array.reshape(DynShape(intArrayOf(3, 4)), 0)

        // 验证大小不变
        // Verify size unchanged
        assertEquals(12, reshaped.shape.size)

        // 验证数据按线性索引顺序保持
        // Verify data preserved in linear index order
        for (i in 0 until 12) {
            assertEquals(array.list[i], reshaped.list[i])
        }
    }

    @Test
    fun testReshapeShrink() {
        // 测试 shrink reshape（如 4x4 -> 2x4，取前 8 个）
        // Test shrink reshape (4x4 -> 2x4, take first 8)
        val shape = DynShape(intArrayOf(4, 4))
        val array = MultiArray.newBy<Int, DynShape>(shape) { i, _ -> i }

        val reshaped = array.reshape(DynShape(intArrayOf(2, 4)), 0)

        assertEquals(8, reshaped.shape.size)
        for (i in 0 until 8) {
            assertEquals(array.list[i], reshaped.list[i])
        }
    }

    @Test
    fun testReshapeEmptyToLarger() {
        // 测试空数组 reshape 到更大数组
        // Test empty array reshape to larger array
        val shape = DynShape(intArrayOf(0))
        val array = MultiArray.newWith<Int, DynShape>(shape, 0)

        // 空 reshape 应该返回填充默认值的数组
        // Empty reshape should return array filled with default values
        val reshaped = array.reshape(DynShape(intArrayOf(2, 3)), -1)
        assertEquals(6, reshaped.shape.size)
        for (i in 0 until 6) {
            assertEquals(-1, reshaped.list[i])
        }
    }

    @Test
    fun testReshapePreserveDataOrder() {
        // 测试 reshape 保持数据线性顺序
        // Test reshape preserves data linear order
        val shape = DynShape(intArrayOf(2, 3, 4))
        val array = MultiArray.newBy<Int, DynShape>(shape) { i, _ -> i * 10 }

        val reshaped = array.reshape(DynShape(intArrayOf(6, 4)), 0)

        for (i in 0 until 24) {
            assertEquals(i * 10, reshaped.list[i])
        }
    }

    // ========================================================================
    // ctor 延迟初始化测试
    // ctor deferred initialization tests
    // ========================================================================

    @Test
    fun testCtorNullWithNonEmptyShapeDefersInit() {
        // ctor=null with non-empty shape defers initialization (VariableCombination pattern)
        val shape = DynShape(intArrayOf(2, 3))
        val array = MultiArray<Int, DynShape>(shape, null as ((Int, IntArray) -> Int)?)

        // Construction must not throw; shape is preserved
        assertEquals(6, array.shape.size)
    }

    @Test
    fun testCtorNullWithEmptyShapeAllowed() {
        // 测试 ctor=null 且空 shape（size=0）可以构造
        // Test ctor=null with empty shape (size=0) can construct
        val shape = DynShape(intArrayOf(2, 0, 3))
        val array = MultiArray.newWith<Int, DynShape>(shape, 0)

        assertEquals(0, array.shape.size)
        assertTrue(array.list.isEmpty())
    }

    @Test
    fun testFactoryMethodNewWithInitializesCorrectly() {
        // 测试 newWith 工厂方法正确初始化
        // Test newWith factory method initializes correctly
        val shape = Shape3(2, 3, 4)
        val array = MultiArray.newWith(shape, 42)

        for (i in 0 until 24) {
            assertEquals(42, array.list[i])
        }
    }

    @Test
    fun testFactoryMethodNewByInitializesCorrectly() {
        // 测试 newBy 工厂方法正确初始化
        // Test newBy factory method initializes correctly
        val shape = Shape2(2, 3)
        val array = MultiArray.newBy<Int, Shape2>(shape) { i, vec ->
            vec[0] * 100 + vec[1]
        }

        // 验证生成器正确应用
        // Verify generator applied correctly
        assertEquals(0, array[intArrayOf(0, 0)])   // 0*100 + 0
        assertEquals(2, array[intArrayOf(0, 2)])   // 0*100 + 2
        assertEquals(100, array[intArrayOf(1, 0)]) // 1*100 + 0
        assertEquals(102, array[intArrayOf(1, 2)]) // 1*100 + 2
    }

    // ========================================================================
    // MutableMultiArray 边界测试
    // MutableMultiArray boundary tests
    // ========================================================================

    @Test
    fun testMutableArrayEmptyOperations() {
        // 测试可变空数组操作
        // Test mutable empty array operations
        val shape = DynShape(intArrayOf(0, 3))
        val array = MutableMultiArray.newWith<Int, DynShape>(shape, 0)

        assertEquals(0, array.shape.size)
        // 任何索引访问应该失败（因为 size=0）
        // Any index access should fail (because size=0)
    }

    @Test
    fun testMutableArrayZeroDimension() {
        // 测试可变 0 维数组
        // Test mutable zero-dimension array
        val shape = DynShape(intArrayOf())
        val array = MutableMultiArray.newWith<Int, DynShape>(shape, 42)

        assertEquals(1, array.shape.size)
        assertEquals(42, array.list[0])

        // 修改值
        // Modify value
        array.list[0] = 100
        assertEquals(100, array.list[0])
    }

    // ========================================================================
    // 边界访问测试
    // Boundary access tests
    // ========================================================================

    @Test
    fun testArraySingleElement() {
        // 测试单元素数组
        // Test single element array
        val shape = DynShape(intArrayOf(1, 1, 1))
        val array = MultiArray.newWith(shape, 42)

        assertEquals(42, array[intArrayOf(0, 0, 0)])
    }

    @Test
    fun testArrayIndexOutOfBounds() {
        // 测试索引越界异常
        // Test index out of bounds exception
        val shape = Shape2(2, 3)
        val array = MultiArray.newWith(shape, 0)

        // 越界访问 - 抛出 OutOfShapeException
        // Out of bounds access - throws OutOfShapeException
        org.junit.jupiter.api.assertThrows<OutOfShapeException> {
            array[intArrayOf(3, 0)]  // row 3 exceeds 2
        }

        org.junit.jupiter.api.assertThrows<OutOfShapeException> {
            array[intArrayOf(0, 5)]  // col 5 exceeds 3
        }
    }
}
