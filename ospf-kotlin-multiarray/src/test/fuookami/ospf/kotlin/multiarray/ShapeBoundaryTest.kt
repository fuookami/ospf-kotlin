package fuookami.ospf.kotlin.multiarray

import kotlin.test.*
import org.junit.jupiter.api.Test

/**
 * Shape 边界场景测试
 * Shape boundary scenario tests
 *
 * 测试内容：
 * Test contents:
 * - 0 维 shape（DynShape(intArrayOf())）下的 size/index/vector 与逆变换
 *   Zero-dimension shape (DynShape(intArrayOf())) size/index/vector and inverse transform
 * - DynShape 防御性拷贝验证：外部修改输入 IntArray 后，实例行为不变
 *   DynShape defensive copy validation: instance behavior unchanged after external IntArray modification
 */
class ShapeBoundaryTest {

    // ========================================================================
    // 0 维 Shape 测试
    // Zero-dimension Shape tests
    // ========================================================================

    @Test
    fun testDynShapeZeroDimension() {
        // 测试 0 维 DynShape 创建
        // Test zero-dimension DynShape creation
        val shape = DynShape(intArrayOf())
        assertEquals(0, shape.dimension)
        assertEquals(1, shape.size)  // Empty shape has size 1 (single element)
        assertTrue(shape.offsets.isEmpty())
    }

    @Test
    fun testDynShapeZeroDimensionIndexVector() {
        // 测试 0 维 shape 的 index/vector 逆变换
        // Test zero-dimension shape index/vector inverse transform
        val shape = DynShape(intArrayOf())
        // For 0-dim shape, index(emptyArray) should return 0
        assertEquals(0, shape.index(intArrayOf()))
        // vector(0) should return empty array
        val vec = shape.vector(0)
        assertTrue(vec.isEmpty())
    }

    @Test
    fun testDynShapeZeroDimensionRowMajor() {
        // 测试 0 维 shape 行主序
        // Test zero-dimension shape row-major order
        val shape = DynShape.withOrder(intArrayOf(), StorageOrder.RowMajor)
        assertEquals(0, shape.dimension)
        assertEquals(1, shape.size)
        assertEquals(StorageOrder.RowMajor, shape.storageOrder)
    }

    @Test
    fun testDynShapeZeroDimensionColumnMajor() {
        // 测试 0 维 shape 列主序
        // Test zero-dimension shape column-major order
        val shape = DynShape.withOrder(intArrayOf(), StorageOrder.ColumnMajor)
        assertEquals(0, shape.dimension)
        assertEquals(1, shape.size)
        assertEquals(StorageOrder.ColumnMajor, shape.storageOrder)
    }

    @Test
    fun testDynShapeZeroDimensionInverseTransform() {
        // 测试 0 维 shape 的 index(vector(i)) == i 恒成立
        // Test zero-dimension shape index(vector(i)) == i invariant
        val shapeRowMajor = DynShape.withOrder(intArrayOf(), StorageOrder.RowMajor)
        val shapeColMajor = DynShape.withOrder(intArrayOf(), StorageOrder.ColumnMajor)

        // Only valid index is 0
        for (shape in listOf(shapeRowMajor, shapeColMajor)) {
            val vec = shape.vector(0)
            val idx = shape.index(vec)
            assertEquals(0, idx)
        }
    }

    // ========================================================================
    // DynShape 防御性拷贝测试
    // DynShape defensive copy tests
    // ========================================================================

    @Test
    fun testDynShapeDefensiveCopyInvoke() {
        // 测试 DynShape.invoke 防御性拷贝
        // Test DynShape.invoke defensive copy
        val inputShape = intArrayOf(2, 3, 4)
        val shape = DynShape(inputShape)

        // 修改输入数组
        // Modify input array
        inputShape[0] = 100
        inputShape[1] = 200

        // shape 实例应该保持不变
        // shape instance should remain unchanged
        assertEquals(2, shape[0])
        assertEquals(3, shape[1])
        assertEquals(4, shape[2])
        assertEquals(24, shape.size)
    }

    @Test
    fun testDynShapeDefensiveCopyWithOrder() {
        // 测试 DynShape.withOrder 防御性拷贝
        // Test DynShape.withOrder defensive copy
        val inputShape = intArrayOf(2, 3, 4)
        val shape = DynShape.withOrder(inputShape, StorageOrder.ColumnMajor)

        // 修改输入数组
        // Modify input array
        inputShape[0] = 100
        inputShape[1] = 200

        // shape 实例应该保持不变
        // shape instance should remain unchanged
        assertEquals(2, shape[0])
        assertEquals(3, shape[1])
        assertEquals(4, shape[2])
        assertEquals(24, shape.size)
        assertEquals(StorageOrder.ColumnMajor, shape.storageOrder)
    }

    @Test
    fun testDynShapeDefensiveCopyIndexAfterMutation() {
        // 测试修改外部数组后索引计算仍然正确
        // Test index calculation remains correct after external array mutation
        val inputShape = intArrayOf(2, 3, 4)
        val shape = DynShape.withOrder(inputShape, StorageOrder.RowMajor)

        // 记录原始索引计算结果
        // Record original index calculation results
        val originalIndices = mutableMapOf<IntArray, Int>()
        for (i in 0 until shape.size) {
            val vec = shape.vector(i)
            originalIndices[vec.copyOf()] = shape.index(vec)
        }

        // 修改输入数组
        // Modify input array
        inputShape[0] = 100

        // 验证索引计算仍然正确
        // Verify index calculation remains correct
        for ((vec, expectedIdx) in originalIndices) {
            assertEquals(expectedIdx, shape.index(vec))
        }
    }

    @Test
    fun testDynShapeDefensiveCopyInverseTransformAfterMutation() {
        // 测试修改外部数组后逆变换仍然正确
        // Test inverse transform remains correct after external array mutation
        val inputShape = intArrayOf(2, 3, 4)
        val shape = DynShape.withOrder(inputShape, StorageOrder.ColumnMajor)

        // 修改输入数组
        // Modify input array
        inputShape[0] = 100
        inputShape[1] = 200

        // 验证逆变换仍然正确
        // Verify inverse transform remains correct
        for (i in 0 until shape.size) {
            val vec = shape.vector(i)
            assertEquals(i, shape.index(vec))
        }
    }

    // ========================================================================
    // DynShape 输入校验测试
    // DynShape input validation tests
    // ========================================================================

    @Test
    fun testDynShapeAcceptZeroDimension() {
        // 测试 DynShape 接受维度值为 0（如 2x0x3）
        // Test DynShape accepts dimension value 0 (e.g., 2x0x3)
        val shape = DynShape(intArrayOf(2, 0, 3))
        assertEquals(3, shape.dimension)
        assertEquals(0, shape.size)  // Any dimension is 0, total size is 0
        assertEquals(2, shape[0])
        assertEquals(0, shape[1])
        assertEquals(3, shape[2])
    }

    @Test
    fun testDynShapeNegativeDimensionBehavior() {
        // 测试 DynShape 负维度行为（当前实现可能接受或拒绝）
        // Test DynShape negative dimension behavior (current implementation may accept or reject)
        // 注：这是一个行为记录测试，不强制期望特定行为
        // Note: This is a behavior recording test, does not enforce specific behavior
        try {
            val shape = DynShape(intArrayOf(2, -1, 3))
            // 如果接受负维度，验证形状仍然有效
            // If negative dimension accepted, verify shape is still valid
            assertEquals(3, shape.dimension)
        } catch (e: IllegalArgumentException) {
            // 如果拒绝负维度，这是预期行为
            // If negative dimension rejected, this is expected behavior
        }
    }

    // ========================================================================
    // 边界形状测试
    // Boundary shape tests
    // ========================================================================

    @Test
    fun testDynShapeSingleElement() {
        // 测试单元素形状（1x1x1）
        // Test single element shape (1x1x1)
        val shape = DynShape(intArrayOf(1, 1, 1))
        assertEquals(3, shape.dimension)
        assertEquals(1, shape.size)

        // 验证唯一元素的索引
        // Verify single element index
        assertEquals(0, shape.index(intArrayOf(0, 0, 0)))
        assertArrayEquals(intArrayOf(0, 0, 0), shape.vector(0))
    }

    @Test
    fun testDynShapeLargeDimension() {
        // 测试大维度数（10 维）
        // Test large dimension count (10 dimensions)
        val dims = intArrayOf(2, 2, 2, 2, 2, 2, 2, 2, 2, 2)
        val shape = DynShape(dims)
        assertEquals(10, shape.dimension)
        assertEquals(1024, shape.size)

        // 验证逆变换
        // Verify inverse transform
        for (i in listOf(0, 512, 1023)) {
            val vec = shape.vector(i)
            assertEquals(i, shape.index(vec))
        }
    }

    // Helper function for array comparison
    private fun assertArrayEquals(expected: IntArray, actual: IntArray) {
        assertEquals(expected.size, actual.size)
        for (i in expected.indices) {
            assertEquals(expected[i], actual[i])
        }
    }
}
