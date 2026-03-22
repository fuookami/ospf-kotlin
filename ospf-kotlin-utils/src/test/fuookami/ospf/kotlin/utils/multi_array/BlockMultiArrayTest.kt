package fuookami.ospf.kotlin.utils.multi_array

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * 分块多维数组测试
 * Block multi-dimensional array tests
 */
class BlockMultiArrayTest {

    // ========================================================================
    // BlockMultiArray 创建测试
    // BlockMultiArray creation tests
    // ========================================================================

    @Test
    fun testBlockMultiArrayCreation() {
        // 测试分块数组创建
        // Test block array creation
        val shape = Shape2(5, 5)
        val blockArray = BlockMultiArray<Int, Shape2>(shape)

        assertEquals(0, blockArray.size)
        assertTrue(blockArray.isEmpty())
    }

    @Test
    fun testBlockMultiArrayEmpty() {
        // 测试空分块数组
        // Test empty block array
        val shape = Shape2(3, 4)
        val blockArray = BlockMultiArray.empty<Int, Shape2>(shape)

        assertEquals(0, blockArray.size)
        assertTrue(blockArray.isEmpty())
    }

    // ========================================================================
    // BlockMultiArray 访问测试
    // BlockMultiArray access tests
    // ========================================================================

    @Test
    fun testBlockMultiArraySetAndGet() {
        // 测试设置和获取元素
        // Test set and get element
        val shape = Shape2(5, 5)
        val blockArray = BlockMultiArray<Int, Shape2>(shape)

        // 使用 set 方法设置值
        blockArray.set(intArrayOf(1, 2), 42)

        // 验证值已设置（通过 contains 检查）
        assertTrue(blockArray.contains(intArrayOf(1, 2)))
        assertEquals(1, blockArray.size)
    }

    @Test
    fun testBlockMultiArrayMultipleSet() {
        // 测试设置多个元素
        // Test set multiple elements
        val shape = Shape2(5, 5)
        val blockArray = BlockMultiArray<Int, Shape2>(shape)

        blockArray.set(intArrayOf(0, 0), 1)
        blockArray.set(intArrayOf(1, 1), 2)
        blockArray.set(intArrayOf(4, 4), 3)

        assertEquals(3, blockArray.size)
        assertTrue(blockArray.contains(intArrayOf(0, 0)))
        assertTrue(blockArray.contains(intArrayOf(1, 1)))
        assertTrue(blockArray.contains(intArrayOf(4, 4)))
    }

    @Test
    fun testBlockMultiArrayGetOrSet() {
        // 测试获取或设置默认值
        // Test get or set default value
        val shape = Shape2(5, 5)
        val blockArray = BlockMultiArray<Int, Shape2>(shape)

        val value1 = blockArray.getOrSet(intArrayOf(1, 1)) { 99 }
        assertEquals(99, value1)
        assertEquals(1, blockArray.size)

        val value2 = blockArray.getOrSet(intArrayOf(1, 1)) { 100 }
        assertEquals(99, value2)  // 应该返回已存在的值
        assertEquals(1, blockArray.size)  // 大小不变
    }

    @Test
    fun testBlockMultiArrayContains() {
        // 测试包含检查
        // Test contains check
        val shape = Shape2(5, 5)
        val blockArray = BlockMultiArray<Int, Shape2>(shape)

        blockArray.set(intArrayOf(1, 2), 42)

        assertTrue(blockArray.contains(intArrayOf(1, 2)))
        assertFalse(blockArray.contains(intArrayOf(0, 0)))
    }

    @Test
    fun testBlockMultiArrayRemove() {
        // 测试移除元素
        // Test remove element
        val shape = Shape2(5, 5)
        val blockArray = BlockMultiArray<Int, Shape2>(shape)

        blockArray.set(intArrayOf(1, 2), 42)
        blockArray.set(intArrayOf(2, 3), 100)

        assertEquals(2, blockArray.size)

        val removed = blockArray.remove(intArrayOf(1, 2))
        assertEquals(42, removed)
        assertEquals(1, blockArray.size)
        assertFalse(blockArray.contains(intArrayOf(1, 2)))
    }

    @Test
    fun testBlockMultiArrayClear() {
        // 测试清空
        // Test clear
        val shape = Shape2(5, 5)
        val blockArray = BlockMultiArray<Int, Shape2>(shape)

        blockArray.set(intArrayOf(0, 0), 1)
        blockArray.set(intArrayOf(1, 1), 2)
        blockArray.set(intArrayOf(2, 2), 3)

        assertEquals(3, blockArray.size)

        blockArray.clear()

        assertEquals(0, blockArray.size)
        assertTrue(blockArray.isEmpty())
    }

    // ========================================================================
    // BlockMultiArray 迭代测试
    // BlockMultiArray iteration tests
    // ========================================================================

    @Test
    fun testBlockMultiArrayIterator() {
        // 测试迭代器
        // Test iterator
        val shape = Shape2(5, 5)
        val blockArray = BlockMultiArray<Int, Shape2>(shape)

        blockArray.set(intArrayOf(0, 0), 10)
        blockArray.set(intArrayOf(1, 1), 20)
        blockArray.set(intArrayOf(2, 2), 30)

        val values = blockArray.toList()
        assertEquals(3, values.size)
        assertTrue(10 in values)
        assertTrue(20 in values)
        assertTrue(30 in values)
    }

    @Test
    fun testBlockMultiArrayContainsAll() {
        // 测试包含所有
        // Test contains all
        val shape = Shape2(5, 5)
        val blockArray = BlockMultiArray<Int, Shape2>(shape)

        blockArray.set(intArrayOf(0, 0), 10)
        blockArray.set(intArrayOf(1, 1), 20)
        blockArray.set(intArrayOf(2, 2), 30)

        assertTrue(blockArray.containsAll(listOf(10, 20)))
        assertFalse(blockArray.containsAll(listOf(10, 40)))
    }

    @Test
    fun testBlockMultiArrayContainsElement() {
        // 测试包含元素
        // Test contains element
        val shape = Shape2(5, 5)
        val blockArray = BlockMultiArray<Int, Shape2>(shape)

        blockArray.set(intArrayOf(0, 0), 10)
        blockArray.set(intArrayOf(1, 1), 20)

        assertTrue(blockArray.contains(10))
        assertTrue(blockArray.contains(20))
        assertFalse(blockArray.contains(30))
    }

    // ========================================================================
    // 3D BlockMultiArray 测试
    // 3D BlockMultiArray tests
    // ========================================================================

    @Test
    fun testBlockMultiArray3D() {
        // 测试 3D 分块数组
        // Test 3D block array
        val shape = Shape3(4, 4, 4)
        val blockArray = BlockMultiArray<Int, Shape3>(shape)

        blockArray.set(intArrayOf(0, 0, 0), 1)
        blockArray.set(intArrayOf(1, 2, 3), 100)
        blockArray.set(intArrayOf(3, 3, 3), 999)

        assertEquals(3, blockArray.size)
        assertTrue(blockArray.contains(intArrayOf(0, 0, 0)))
        assertTrue(blockArray.contains(intArrayOf(1, 2, 3)))
        assertTrue(blockArray.contains(intArrayOf(3, 3, 3)))
    }

    @Test
    fun testBlockMultiArrayDynShape() {
        // 测试动态形状分块数组
        // Test dynamic shape block array
        val shape = DynShape(intArrayOf(3, 4, 5, 6))
        val blockArray = BlockMultiArray<Int, DynShape>(shape)

        blockArray.set(intArrayOf(0, 0, 0, 0), 1)
        blockArray.set(intArrayOf(2, 3, 4, 5), 999)

        assertEquals(2, blockArray.size)
    }

    // ========================================================================
    // 边界情况测试
    // Edge case tests
    // ========================================================================

    @Test
    fun testBlockMultiArraySparse() {
        // 测试稀疏数组
        // Test sparse array
        val shape = Shape2(100, 100)
        val blockArray = BlockMultiArray<Int, Shape2>(shape)

        // 只设置少量元素
        // Set only a few elements
        blockArray.set(intArrayOf(0, 0), 1)
        blockArray.set(intArrayOf(50, 50), 2)
        blockArray.set(intArrayOf(99, 99), 3)

        assertEquals(3, blockArray.size)
        assertTrue(blockArray.size < shape.size)  // 验证稀疏性
    }

    @Test
    fun testBlockMultiArrayOverwrite() {
        // 测试覆盖值
        // Test overwrite value
        val shape = Shape2(5, 5)
        val blockArray = BlockMultiArray<Int, Shape2>(shape)

        blockArray.set(intArrayOf(1, 1), 10)
        assertTrue(blockArray.contains(intArrayOf(1, 1)))

        blockArray.set(intArrayOf(1, 1), 20)
        assertTrue(blockArray.contains(intArrayOf(1, 1)))
        assertEquals(1, blockArray.size)  // 大小不变
    }

    // ========================================================================
    // 类型别名测试
    // Type alias tests
    // ========================================================================

    @Test
    fun testTypeAliases() {
        // 测试类型别名
        // Test type aliases
        val array1: BlockMultiArray1<Int> = BlockMultiArray.empty(Shape1(5))
        assertEquals(0, array1.size)

        val array2: BlockMultiArray2<Int> = BlockMultiArray.empty(Shape2(3, 4))
        assertEquals(0, array2.size)

        val array3: BlockMultiArray3<Int> = BlockMultiArray.empty(Shape3(2, 3, 4))
        assertEquals(0, array3.size)

        val array4: BlockMultiArray4<Int> = BlockMultiArray.empty(Shape4(2, 2, 2, 2))
        assertEquals(0, array4.size)

        val dynArray: BlockDynMultiArray<Int> = BlockMultiArray.empty(DynShape(intArrayOf(3, 4)))
        assertEquals(0, dynArray.size)
    }
}