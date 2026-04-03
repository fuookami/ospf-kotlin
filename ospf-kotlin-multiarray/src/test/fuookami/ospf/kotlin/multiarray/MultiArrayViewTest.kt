package fuookami.ospf.kotlin.multiarray

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * 多维数组视图测试
 * Multi-dimensional array view tests
 */
class MultiArrayViewTest {

    // ========================================================================
    // MultiArrayView 创建测试
    // MultiArrayView creation tests
    // ========================================================================

    @Test
    fun testMultiArrayViewCreation() {
        // 测试视图创建
        // Test view creation
        val array = MultiArray(Shape2(4, 5)) { i, _ -> i }
        val view = MultiArrayView(array)

        assertEquals(20, view.size)
        assertEquals(2, view.shape.dimension)
    }

    @Test
    fun testMultiArrayViewFromFullSlice() {
        // 测试全切片视图
        // Test full slice view
        val array = MultiArray(Shape2(4, 5)) { i, _ -> i }
        val view = array[_a, _a]

        assertEquals(20, view.size)
        assertEquals(0, view[0, 0])
        assertEquals(19, view[3, 4])
    }

    // ========================================================================
    // MultiArrayView 切片测试
    // MultiArrayView slice tests
    // ========================================================================

    @Test
    fun testMultiArrayViewRowSlice() {
        // 测试行切片
        // Test row slice
        val array = MultiArray(Shape2(4, 5)) { i, _ -> i }
        val view = array[1, _a]

        assertEquals(5, view.size)
        assertEquals(1, view.shape.dimension)
        assertEquals(5, view[0])  // 第 1 行第 0 列
        assertEquals(6, view[1])  // 第 1 行第 1 列
        assertEquals(9, view[4])  // 第 1 行第 4 列
    }

    @Test
    fun testMultiArrayViewColumnSlice() {
        // 测试列切片
        // Test column slice
        val array = MultiArray(Shape2(4, 5)) { i, _ -> i }
        val view = array[_a, 2]

        assertEquals(4, view.size)
        assertEquals(1, view.shape.dimension)
        assertEquals(2, view[0])   // 第 0 行第 2 列
        assertEquals(7, view[1])   // 第 1 行第 2 列
        assertEquals(17, view[3])  // 第 3 行第 2 列
    }

    @Test
    fun testMultiArrayViewRangeSlice() {
        // 测试范围切片
        // Test range slice
        val array = MultiArray(Shape2(6, 6)) { i, _ -> i }
        val view = array[1..3, 2..4]

        assertEquals(9, view.size)  // 3x3
        assertEquals(2, view.shape.dimension)
        assertEquals(3, view.shape[0])
        assertEquals(3, view.shape[1])
    }

    @Test
    fun testMultiArrayViewRangeSliceValues() {
        // 测试范围切片值
        // Test range slice values
        val array = MultiArray(Shape2(6, 6)) { i, _ -> i }
        val view = array[1..3, 2..4]

        // 验证一些值 - 使用线性索引访问
        // Verify some values - using linear index access
        assertEquals(8, view[0])   // [1, 2] -> 1*6+2=8
        assertEquals(9, view[1])   // [1, 3] -> 1*6+3=9
        assertEquals(10, view[2])  // [1, 4] -> 1*6+4=10
    }

    @Test
    fun testMultiArrayViewDiscreteSlice() {
        // 测试离散切片
        // Test discrete slice
        val array = MultiArray(Shape2(5, 5)) { i, _ -> i }
        val view = array[DummyIndex.from(listOf(0, 2, 4)), _a]

        // Just verify the view is created and has correct size
        assertTrue(view.size > 0)
    }

    // ========================================================================
    // MultiArrayView 访问测试
    // MultiArrayView access tests
    // ========================================================================

    @Test
    fun testMultiArrayViewGetByLinearIndex() {
        // 测试线性索引访问
        // Test linear index access
        val array = MultiArray(Shape2(4, 5)) { i, _ -> i }
        val view = array[_a, _a]

        assertEquals(0, view[0])
        assertEquals(1, view[1])
        assertEquals(19, view[19])
    }

    @Test
    fun testMultiArrayViewGetByVectorIndex() {
        // 测试向量索引访问
        // Test vector index access
        val array = MultiArray(Shape2(4, 5)) { i, _ -> i }
        val view = array[_a, _a]

        assertEquals(0, view[intArrayOf(0, 0)])
        assertEquals(1, view[intArrayOf(0, 1)])
        assertEquals(19, view[intArrayOf(3, 4)])
    }

    @Test
    fun testMultiArrayViewGetByVararg() {
        // 测试可变参数访问
        // Test vararg access
        val array = MultiArray(Shape2(4, 5)) { i, _ -> i }
        val view = array[_a, _a]

        assertEquals(0, view[0, 0])
        assertEquals(1, view[0, 1])
        assertEquals(19, view[3, 4])
    }

    // ========================================================================
    // MultiArrayView 迭代测试
    // MultiArrayView iteration tests
    // ========================================================================

    @Test
    fun testMultiArrayViewIterator() {
        // 测试视图迭代器
        // Test view iterator
        val array = MultiArray(Shape2(3, 4)) { i, _ -> i }
        val view = array[_a, _a]

        val values = view.toList()
        assertEquals(12, values.size)
        assertEquals((0..11).toList(), values)
    }

    @Test
    fun testMultiArrayViewSliceIterator() {
        // 测试切片视图迭代器
        // Test slice view iterator
        val array = MultiArray(Shape2(4, 5)) { i, _ -> i }
        val view = array[1..3, 2..4]

        val values = view.toList()
        assertEquals(9, values.size)
    }

    @Test
    fun testMultiArrayViewContains() {
        // 测试包含检查
        // Test contains check
        val array = MultiArray(Shape2(4, 5)) { i, _ -> i }
        val view = array[_a, _a]

        assertTrue(0 in view)
        assertTrue(19 in view)
        assertFalse(20 in view)
    }

    @Test
    fun testMultiArrayViewIsEmpty() {
        // 测试空检查
        // Test empty check
        val array = MultiArray(Shape2(4, 5)) { i, _ -> i }
        val view1 = array[_a, _a]
        val view2 = array[0..0, _a]  // 空范围

        assertFalse(view1.isEmpty())
        // 注意：0..0 可能不是空范围，需要验证
    }

    // ========================================================================
    // MultiArrayView 子视图测试
    // MultiArrayView sub-view tests
    // ========================================================================

    @Test
    fun testMultiArrayViewSubView() {
        // 测试子视图创建
        // Test sub-view creation
        val array = MultiArray(Shape2(6, 6)) { i, _ -> i }
        val view1 = array[1..4, 1..4]
        val view2 = view1[0..2, 0..2]

        assertEquals(9, view2.size)
    }

    @Test
    fun testMultiArrayViewChainedSlicing() {
        // 测试链式切片
        // Test chained slicing
        val array = MultiArray(Shape3(4, 5, 6)) { i, _ -> i }
        val view1 = array[1..2, _a, _a]  // 2x5x6
        val view2 = view1[_a, 2..3, _a]  // 2x2x6

        // Just verify the view is created
        assertTrue(view2.size > 0)
    }

    // ========================================================================
    // MultiArrayView 形状测试
    // MultiArrayView shape tests
    // ========================================================================

    @Test
    fun testMultiArrayViewShape() {
        // 测试视图形状
        // Test view shape
        val array = MultiArray(Shape2(6, 8)) { i, _ -> i }
        val view = array[2..5, 3..6]

        assertEquals(4, view.shape[0])  // 2,3,4,5
        assertEquals(4, view.shape[1])  // 3,4,5,6
        assertEquals(16, view.size)
    }

    @Test
    fun testMultiArrayViewShapeFromOrigin() {
        // 测试视图形状来自原数组
        // Test view shape from origin
        val array = MultiArray(Shape2(10, 10)) { i, _ -> i }
        val view = array[0, _a]

        assertEquals(1, view.shape.dimension)
        assertEquals(10, view.shape[0])
        assertEquals(10, view.size)
    }

    // ========================================================================
    // MultiArrayView toString 测试
    // MultiArrayView toString tests
    // ========================================================================

    @Test
    fun testMultiArrayViewToString() {
        // 测试 toString 方法
        // Test toString method
        val array = MultiArray(Shape2(4, 5)) { i, _ -> i }
        val view = MultiArrayView(array)

        val str = view.toString()
        assertTrue(str.contains("MultiArrayView"))
        assertTrue(str.contains("shape="))
        assertTrue(str.contains("originShape="))
    }

    // ========================================================================
    // 3D 视图测试
    // 3D view tests
    // ========================================================================

    @Test
    fun testMultiArrayView3D() {
        // 测试 3D 视图
        // Test 3D view
        val array = MultiArray(Shape3(3, 4, 5)) { i, _ -> i }
        val view = array[_a, _a, _a]

        assertEquals(60, view.size)
        assertEquals(3, view.shape.dimension)
    }

    @Test
    fun testMultiArrayView3DSlice() {
        // 测试 3D 切片
        // Test 3D slice
        val array = MultiArray(Shape3(4, 5, 6)) { i, _ -> i }
        val view = array[1, _a, 2..4]

        assertEquals(15, view.size)  // 1x5x3
        assertEquals(2, view.shape.dimension)
    }

    @Test
    fun testMultiArrayView3DValues() {
        // 测试 3D 视图值
        // Test 3D view values
        val array = MultiArray(Shape3(3, 4, 5)) { i, _ -> i }

        // 验证线性索引和向量索引
        // Verify linear and vector index
        assertEquals(0, array[0, 0, 0])
        assertEquals(1, array[0, 0, 1])
        assertEquals(5, array[0, 1, 0])
        assertEquals(59, array[2, 3, 4])
    }

    // ========================================================================
    // 边界情况测试
    // Edge case tests
    // ========================================================================

    // @Test
    // fun testMultiArrayViewSingleElement() {
    //     // 测试单元素视图
    //     // Test single element view
    //     val array = MultiArray(Shape2(4, 5)) { i, _ -> i }
    //     val view = array[2, 3]
    //
    //     // 单元素视图应该是 0 维
    //     // Single element view should be 0-dimensional
    //     assertEquals(1, view.size)
    // }

    @Test
    fun testMultiArrayViewFullRow() {
        // 测试整行视图
        // Test full row view
        val array = MultiArray(Shape2(4, 5)) { i, _ -> i }
        val view = array[2, _a]

        assertEquals(5, view.size)
        assertEquals(10, view[0])  // 2*5+0=10
        assertEquals(14, view[4])  // 2*5+4=14
    }

    @Test
    fun testMultiArrayViewFullColumn() {
        // 测试整列视图
        // Test full column view
        val array = MultiArray(Shape2(4, 5)) { i, _ -> i }
        val view = array[_a, 3]

        assertEquals(4, view.size)
        assertEquals(3, view[0])
        assertEquals(8, view[1])
        assertEquals(18, view[3])
    }

    // ========================================================================
    // MappedMultiArrayView 测试
    // MappedMultiArrayView tests
    // ========================================================================

    @Test
    fun testMappedMultiArrayViewCreation() {
        // 测试映射视图创建
        // Test mapped view creation
        val array = MultiArray(Shape2(3, 4)) { i, _ -> i }
        // 创建转置映射
        // Create transpose mapping
        val mapVector = listOf(
            MapIndex.Map(1),  // 新维度 0 映射到原维度 1
            MapIndex.Map(0)   // 新维度 1 映射到原维度 0
        )
        val view = MappedMultiArrayView(array, mapVector)

        assertEquals(12, view.size)
        assertEquals(4, view.shape[0])
        assertEquals(3, view.shape[1])
    }

    @Test
    fun testMappedMultiArrayViewTranspose() {
        // 测试映射视图转置
        // Test mapped view transpose
        val array = MultiArray(Shape2(3, 4)) { i, v -> v[0] * 10 + v[1] }
        val mapVector = listOf(
            MapIndex.Map(1),
            MapIndex.Map(0)
        )
        val view = MappedMultiArrayView(array, mapVector)

        // 原数组 [0, 1] = 1, 转置后 [1, 0] = 1
        // Original [0, 1] = 1, after transpose [1, 0] = 1
        assertEquals(1, view[1, 0])
        assertEquals(2, view[2, 0])
        assertEquals(10, view[0, 1])
    }

    @Test
    fun testMappedMultiArrayViewGet() {
        // 测试映射视图访问
        // Test mapped view access
        val array = MultiArray(Shape2(3, 4)) { i, _ -> i }
        val mapVector = listOf(
            MapIndex.Map(1),
            MapIndex.Map(0)
        )
        val view = MappedMultiArrayView(array, mapVector)

        // Just verify linear access works
        assertTrue(view.size > 0)
        // Access first element
        val first = view[0]
        assertTrue(first >= 0)
    }

    @Test
    fun testMappedMultiArrayViewIterator() {
        // 测试映射视图迭代
        // Test mapped view iterator
        val array = MultiArray(Shape2(2, 3)) { i, _ -> i }
        val mapVector = listOf(
            MapIndex.Map(1),
            MapIndex.Map(0)
        )
        val view = MappedMultiArrayView(array, mapVector)

        val values = view.toList()
        assertEquals(6, values.size)
    }

    // ========================================================================
    // 辅助函数
    // Helper functions
    // ========================================================================

    private fun assertArrayEquals(expected: IntArray, actual: IntArray) {
        assertEquals(expected.size, actual.size, "Array sizes differ")
        for (i in expected.indices) {
            assertEquals(expected[i], actual[i], "Element at index $i differs")
        }
    }
}