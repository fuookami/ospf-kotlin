package fuookami.ospf.kotlin.multiarray

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * BlockMultiArray 语义测试
 * BlockMultiArray semantic tests
 *
 * 测试内容：
 * Test contents:
 * - fromMultiArray(filter) 的包含/排除语义
 *   fromMultiArray(filter) include/exclude semantics
 * - get(vararg) 命中与 miss（null）行为
 *   get(vararg) hit and miss (null) behavior
 */
class BlockMultiArraySemanticTest {

    // ========================================================================
    // 基础创建和访问测试
    // Basic creation and access tests
    // ========================================================================

    @Test
    fun testEmptyBlockMultiArray() {
        // 测试空 BlockMultiArray 创建
        // Test empty BlockMultiArray creation
        val shape = Shape3(10, 10, 10)
        val blockArray = BlockMultiArray.empty<Int, Shape3>(shape)

        assertEquals(1000, blockArray.shape.size)
        assertTrue(blockArray.isEmpty())
    }

    @Test
    fun testBlockMultiArrayGetHit() {
        // 测试命中元素访问
        // Test hit element access
        val shape = Shape2(5, 5)
        val blockArray = BlockMultiArray.empty<Int, Shape2>(shape)

        // 设置元素
        // Set element
        blockArray[intArrayOf(2, 3)] = 42

        // 验证命中访问
        // Verify hit access
        assertEquals(42, blockArray[2, 3])
    }

    @Test
    fun testBlockMultiArrayGetMiss() {
        // 测试未命中元素访问（返回 null）
        // Test miss element access (returns null)
        val shape = Shape2(5, 5)
        val blockArray = BlockMultiArray.empty<Int, Shape2>(shape)

        // 访问未设置的元素
        // Access unset element
        assertNull(blockArray[1, 2])
    }

    @Test
    fun testBlockMultiArrayGetVarargSyntax() {
        // 测试 vararg 语法访问
        // Test vararg syntax access
        val shape = Shape3(4, 5, 6)
        val blockArray = BlockMultiArray.empty<Int, Shape3>(shape)

        // 使用 vararg 设置
        // Set using vararg
        blockArray[intArrayOf(1, 2, 3)] = 100

        // 使用 vararg 访问
        // Access using vararg
        assertEquals(100, blockArray[1, 2, 3])

        // 未命中返回 null
        // Miss returns null
        assertNull(blockArray[0, 0, 0])
    }

    // ========================================================================
    // fromMultiArray 包含语义测试
    // fromMultiArray include semantics tests
    // ========================================================================

    @Test
    fun testFromMultiArrayIncludeNonDefault() {
        // 测试 fromMultiArray 包含非默认值元素
        // Test fromMultiArray includes non-default value elements
        val shape = Shape2(3, 3)
        val array = MultiArray.newBy<Int, Shape2>(shape) { i, vec ->
            if (i % 2 == 0) i else 0  // 非偶数索引为 0
        }

        val blockArray = BlockMultiArray.fromMultiArray(array, filter = { it != 0 })

        // 验证只包含非零值
        // Verify only contains non-zero values
        // 3x3 = 9 elements, even indices: 0, 2, 4, 6, 8 (5 elements)
        // But 0 is zero, so we have: 2, 4, 6, 8 (4 elements)
        assertEquals(4, blockArray.size)
    }

    @Test
    fun testFromMultiArrayIncludeSpecificValues() {
        // 测试 fromMultiArray 包含特定值
        // Test fromMultiArray includes specific values
        val shape = Shape2(4, 4)
        val array = MultiArray.newBy<Int, Shape2>(shape) { i, vec ->
            vec[0] * 4 + vec[1]  // unique value per position
        }

        val blockArray = BlockMultiArray.fromMultiArray(array, filter = { it in listOf(0, 5, 10) })

        // 验证只包含特定值
        // Verify only contains specific values
        assertEquals(3, blockArray.size)
        assertEquals(0, blockArray[0, 0])
        assertEquals(5, blockArray[1, 1])
        assertEquals(10, blockArray[2, 2])
    }

    @Test
    fun testFromMultiArrayEmptyResult() {
        // 测试 fromMultiArray 过滤后为空
        // Test fromMultiArray empty result after filtering
        val shape = Shape2(2, 2)
        val array = MultiArray.newWith(shape, 0)

        val blockArray = BlockMultiArray.fromMultiArray(array, filter = { it > 0 })

        // 所有值都是 0，过滤后应该为空
        // All values are 0, should be empty after filtering
        assertTrue(blockArray.isEmpty())
    }

    // ========================================================================
    // toMultiArray 转换语义测试
    // toMultiArray conversion semantics tests
    // ========================================================================

    @Test
    fun testToMultiArrayWithDefault() {
        // 测试 toMultiArray 使用默认值填充缺失元素
        // Test toMultiArray fills missing elements with default value
        val shape = Shape2(3, 3)
        val blockArray = BlockMultiArray.empty<Int, Shape2>(shape)

        blockArray[intArrayOf(1, 1)] = 10
        blockArray[intArrayOf(2, 2)] = 20

        val denseArray = blockArray.toMultiArray(defaultValue = 0)

        // 验证设置值正确
        // Verify set values correct
        assertEquals(10, denseArray[intArrayOf(1, 1)])
        assertEquals(20, denseArray[intArrayOf(2, 2)])

        // 验证缺失值使用默认值
        // Verify missing values use default
        assertEquals(0, denseArray[intArrayOf(0, 0)])
        assertEquals(0, denseArray[intArrayOf(0, 1)])
        assertEquals(0, denseArray[intArrayOf(1, 0)])
    }

    @Test
    fun testToMultiArrayFullBlock() {
        // 测试所有块都有值的 toMultiArray
        // Test toMultiArray with all blocks having values
        val shape = Shape2(2, 2)
        val blockArray = BlockMultiArray.empty<Int, Shape2>(shape)

        blockArray[intArrayOf(0, 0)] = 1
        blockArray[intArrayOf(0, 1)] = 2
        blockArray[intArrayOf(1, 0)] = 3
        blockArray[intArrayOf(1, 1)] = 4

        val denseArray = blockArray.toMultiArray(defaultValue = 0)

        // 验证所有值正确
        // Verify all values correct
        assertEquals(1, denseArray[intArrayOf(0, 0)])
        assertEquals(2, denseArray[intArrayOf(0, 1)])
        assertEquals(3, denseArray[intArrayOf(1, 0)])
        assertEquals(4, denseArray[intArrayOf(1, 1)])
    }

    @Test
    fun testToMultiArrayEmptyBlock() {
        // 测试空 BlockMultiArray 转换
        // Test empty BlockMultiArray conversion
        val shape = Shape2(3, 3)
        val blockArray = BlockMultiArray.empty<Int, Shape2>(shape)

        val denseArray = blockArray.toMultiArray(defaultValue = -1)

        // 所有值应该都是默认值
        // All values should be default
        for (i in 0 until 9) {
            assertEquals(-1, denseArray[i])
        }
    }

    // ========================================================================
    // 边界情况测试
    // Boundary case tests
    // ========================================================================

    @Test
    fun testBlockMultiArrayZeroDimension() {
        // 测试 0 维 BlockMultiArray
        // Test zero-dimension BlockMultiArray
        val shape = DynShape(intArrayOf())
        val blockArray = BlockMultiArray.empty<Int, DynShape>(shape)

        assertEquals(1, blockArray.shape.size)
    }

    @Test
    fun testBlockMultiArrayEmptyDimension() {
        // 测试维度含 0 的 BlockMultiArray
        // Test BlockMultiArray with dimension containing 0
        val shape = DynShape(intArrayOf(2, 0, 3))
        val blockArray = BlockMultiArray.empty<Int, DynShape>(shape)

        assertEquals(0, blockArray.shape.size)
        assertTrue(blockArray.isEmpty())
    }

    @Test
    fun testBlockMultiArrayLargeSparse() {
        // 测试大规模稀疏数组
        // Test large-scale sparse array
        val shape = Shape3(100, 100, 100)
        val blockArray = BlockMultiArray.empty<Int, Shape3>(shape)

        // 只设置少量值
        // Set only a few values
        blockArray[intArrayOf(0, 0, 0)] = 1
        blockArray[intArrayOf(50, 50, 50)] = 2
        blockArray[intArrayOf(99, 99, 99)] = 3

        assertEquals(3, blockArray.size)

        // 验证访问
        // Verify access
        assertEquals(1, blockArray[0, 0, 0])
        assertEquals(2, blockArray[50, 50, 50])
        assertEquals(3, blockArray[99, 99, 99])
        assertNull(blockArray[25, 25, 25])
    }

    @Test
    fun testBlockMultiArrayOverwrite() {
        // 测试覆盖已有值
        // Test overwriting existing value
        val shape = Shape2(3, 3)
        val blockArray = BlockMultiArray.empty<Int, Shape2>(shape)

        blockArray[intArrayOf(1, 1)] = 10
        assertEquals(10, blockArray[1, 1])

        // 覆盖
        // Overwrite
        blockArray[intArrayOf(1, 1)] = 20
        assertEquals(20, blockArray[1, 1])
        assertEquals(1, blockArray.size)  // still only 1 block
    }

    // ========================================================================
    // 迭代和遍历测试
    // Iteration and traversal tests
    // ========================================================================

    @Test
    fun testBlockMultiArrayIteration() {
        // 测试 BlockMultiArray 迭代
        // Test BlockMultiArray iteration
        val shape = Shape2(2, 2)
        val blockArray = BlockMultiArray.empty<Int, Shape2>(shape)

        blockArray[intArrayOf(0, 1)] = 5
        blockArray[intArrayOf(1, 0)] = 10

        val values = blockArray.toList()
        assertEquals(2, values.size)
        assertTrue(values.contains(5))
        assertTrue(values.contains(10))
    }

    @Test
    fun testBlockMultiArraySize() {
        // 测试 BlockMultiArray 块计数
        // Test BlockMultiArray block count
        val shape = Shape3(10, 10, 10)
        val blockArray = BlockMultiArray.empty<Int, Shape3>(shape)

        assertEquals(0, blockArray.size)

        blockArray[intArrayOf(1, 2, 3)] = 100
        assertEquals(1, blockArray.size)

        blockArray[intArrayOf(4, 5, 6)] = 200
        assertEquals(2, blockArray.size)
    }
}