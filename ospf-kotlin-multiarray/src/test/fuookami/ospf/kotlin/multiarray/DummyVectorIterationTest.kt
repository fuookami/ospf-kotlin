package fuookami.ospf.kotlin.multiarray

import kotlin.test.*
import org.junit.jupiter.api.Test

/**
 * DummyVector 迭代专项测试
 * DummyVector iteration specific tests
 *
 * 测试内容：
 * Test contents:
 * - iterateWithOrder 在 RowMajor / ColumnMajor 下的顺序一致性
 *   iterateWithOrder order consistency under RowMajor/ColumnMajor
 * - 混合 Index/Range/IndexArray/All 索引类型
 *   Mixed Index/Range/IndexArray/All index types
 * - 边界场景：空 range、越界索引
 *   Boundary cases: empty range, out-of-bounds index
 */
class DummyVectorIterationTest {

    // ========================================================================
    // iterateWithOrder 顺序一致性测试
    // iterateWithOrder order consistency tests
    // ========================================================================

    @Test
    fun testIterateWithOrderRowMajor() {
        // 测试 RowMajor 顺序迭代
        // Test RowMajor order iteration
        val shape = Shape2(3, 4)
        val dummyVector: DummyVector = listOf(
            DummyIndex.All,      // dimension 0: all rows
            DummyIndex.Index(2)  // dimension 1: column 2
        )

        val indices = dummyVector.iterateWithOrder(shape, AccessOrder.RowMajor).toList()

        // RowMajor: 最后一维变化最快
        // RowMajor: last dimension varies fastest
        // 预期: (0,2), (1,2), (2,2)
        // Expected: (0,2), (1,2), (2,2)
        assertEquals(3, indices.size)
        assertArrayEquals(intArrayOf(0, 2), indices[0])
        assertArrayEquals(intArrayOf(1, 2), indices[1])
        assertArrayEquals(intArrayOf(2, 2), indices[2])
    }

    @Test
    fun testIterateWithOrderColumnMajor() {
        // 测试 ColumnMajor 顺序迭代
        // Test ColumnMajor order iteration
        val shape = Shape2(3, 4)
        val dummyVector: DummyVector = listOf(
            DummyIndex.All,      // dimension 0: all rows
            DummyIndex.Index(2)  // dimension 1: column 2
        )

        val indices = dummyVector.iterateWithOrder(shape, AccessOrder.ColumnMajor).toList()

        // ColumnMajor: 第一维变化最快
        // ColumnMajor: first dimension varies fastest
        // 预期: (0,2), (1,2), (2,2) - 同上因为只有一维在变化
        // Expected: (0,2), (1,2), (2,2) - same as above since only one dimension varies
        assertEquals(3, indices.size)
        assertArrayEquals(intArrayOf(0, 2), indices[0])
        assertArrayEquals(intArrayOf(1, 2), indices[1])
        assertArrayEquals(intArrayOf(2, 2), indices[2])
    }

    @Test
    fun testIterateWithOrderRowMajorMultiDim() {
        // 测试 RowMajor 多维迭代顺序
        // Test RowMajor multi-dimensional iteration order
        val shape = Shape3(2, 3, 2)
        val dummyVector: DummyVector = listOf(
            DummyIndex.All,  // dim 0: all
            DummyIndex.All,  // dim 1: all
            DummyIndex.All   // dim 2: all
        )

        val indices = dummyVector.iterateWithOrder(shape, AccessOrder.RowMajor).toList()

        // RowMajor: dim2 变化最快，dim0 最慢
        // RowMajor: dim2 varies fastest, dim0 slowest
        assertEquals(12, indices.size)  // 2*3*2 = 12

        // 验证前几个索引顺序
        // Verify first few indices order
        assertArrayEquals(intArrayOf(0, 0, 0), indices[0])
        assertArrayEquals(intArrayOf(0, 0, 1), indices[1])
        assertArrayEquals(intArrayOf(0, 1, 0), indices[2])
        assertArrayEquals(intArrayOf(0, 1, 1), indices[3])
    }

    @Test
    fun testIterateWithOrderColumnMajorMultiDim() {
        // 测试 ColumnMajor 多维迭代顺序
        // Test ColumnMajor multi-dimensional iteration order
        val shape = Shape3(2, 3, 2)
        val dummyVector: DummyVector = listOf(
            DummyIndex.All,  // dim 0: all
            DummyIndex.All,  // dim 1: all
            DummyIndex.All   // dim 2: all
        )

        val indices = dummyVector.iterateWithOrder(shape, AccessOrder.ColumnMajor).toList()

        // ColumnMajor: dim0 变化最快，dim2 最慢
        // ColumnMajor: dim0 varies fastest, dim2 slowest
        assertEquals(12, indices.size)

        // 验证前几个索引顺序
        // Verify first few indices order
        assertArrayEquals(intArrayOf(0, 0, 0), indices[0])
        assertArrayEquals(intArrayOf(1, 0, 0), indices[1])
        assertArrayEquals(intArrayOf(0, 1, 0), indices[2])
        assertArrayEquals(intArrayOf(1, 1, 0), indices[3])
    }

    // ========================================================================
    // 混合索引类型测试
    // Mixed index type tests
    // ========================================================================

    @Test
    fun testMixedAllAndIndex() {
        // 测试混合 All 和 Index
        // Test mixed All and Index
        val shape = Shape3(2, 3, 4)
        val dummyVector: DummyVector = listOf(
            DummyIndex.All,           // dim 0: all
            DummyIndex.Index(1),      // dim 1: index 1
            DummyIndex.All            // dim 2: all
        )

        val indices = dummyVector.iterateWithOrder(shape).toList()

        // 预期: 2 * 1 * 4 = 8 indices
        // Expected: 2 * 1 * 4 = 8 indices
        assertEquals(8, indices.size)

        // 所有索引的 dim1 应该是 1
        // All indices should have dim1 = 1
        assertTrue(indices.all { it[1] == 1 })
    }

    @Test
    fun testMixedRangeAndAll() {
        // 测试混合 Range 和 All
        // Test mixed Range and All
        val shape = Shape2(4, 5)
        val dummyVector: DummyVector = listOf(
            DummyIndex.from(1..2),  // dim 0: range [1,2]
            DummyIndex.All          // dim 1: all
        )

        val indices = dummyVector.iterateWithOrder(shape).toList()

        // 预期: 2 rows * 5 cols = 10 indices
        // Expected: 2 rows * 5 cols = 10 indices
        assertEquals(10, indices.size)

        // 所有行应该在 1..2 范围
        // All rows should be in range 1..2
        assertTrue(indices.all { it[0] in 1..2 })
    }

    @Test
    fun testMixedIndexArrayAndIndex() {
        // 测试混合 IndexArray 和 Index
        // Test mixed IndexArray and Index
        val shape = Shape2(5, 5)
        val dummyVector: DummyVector = listOf(
            DummyIndex.IndexArray(listOf(0, 2, 4)),  // dim 0: indices 0, 2, 4
            DummyIndex.Index(3)                       // dim 1: index 3
        )

        val indices = dummyVector.iterateWithOrder(shape).toList()

        // 预期: 3 rows * 1 col = 3 indices
        // Expected: 3 rows * 1 col = 3 indices
        assertEquals(3, indices.size)

        // 验证行索引
        // Verify row indices
        val rows = indices.map { it[0] }
        assertEquals(listOf(0, 2, 4), rows)

        // 所有列应该是 3
        // All columns should be 3
        assertTrue(indices.all { it[1] == 3 })
    }

    @Test
    fun testMixedAllTypes() {
        // 测试混合所有索引类型
        // Test mixed all index types
        val shape = Shape3(4, 5, 6)
        val dummyVector: DummyVector = listOf(
            DummyIndex.IndexArray(listOf(0, 3)),  // dim 0: indices 0, 3
            DummyIndex.from(1..3),                // dim 1: range [1,3]
            DummyIndex.Index(5)                   // dim 2: index 5
        )

        val indices = dummyVector.iterateWithOrder(shape).toList()

        // 预期: 2 * 3 * 1 = 6 indices
        // Expected: 2 * 3 * 1 = 6 indices
        assertEquals(6, indices.size)

        // 验证约束
        // Verify constraints
        assertTrue(indices.all { it[0] in listOf(0, 3) })
        assertTrue(indices.all { it[1] in 1..3 })
        assertTrue(indices.all { it[2] == 5 })
    }

    // ========================================================================
    // 边界场景测试
    // Boundary case tests
    // ========================================================================

    @Test
    fun testEmptyRangeIteration() {
        // 测试空范围迭代
        // Test empty range iteration
        val shape = Shape2(3, 4)
        val dummyVector: DummyVector = listOf(
            DummyIndex.from(2..1),  // empty range [2,1)
            DummyIndex.All
        )

        val indices = dummyVector.iterateWithOrder(shape).toList()

        // 空范围应该产生 0 个索引
        // Empty range should produce 0 indices
        assertTrue(indices.isEmpty())
    }

    @Test
    fun testEmptyIndexArray() {
        // 测试空索引数组
        // Test empty index array
        val shape = Shape2(3, 4)
        val dummyVector: DummyVector = listOf(
            DummyIndex.IndexArray(emptyList()),  // empty array
            DummyIndex.All
        )

        val indices = dummyVector.iterateWithOrder(shape).toList()

        // 空数组应该产生 0 个索引
        // Empty array should produce 0 indices
        assertTrue(indices.isEmpty())
    }

    @Test
    fun testSingleAllDimension() {
        // 测试单维度 All
        // Test single dimension All
        val shape = Shape1(5)
        val dummyVector: DummyVector = listOf(
            DummyIndex.All
        )

        val indices = dummyVector.iterateWithOrder(shape).toList()

        assertEquals(5, indices.size)
        for (i in 0 until 5) {
            assertEquals(1, indices[i].size)
            assertEquals(i, indices[i][0])
        }
    }

    @Test
    fun testFullCoverage() {
        // 测试完全覆盖（所有维度都是 All）
        // Test full coverage (all dimensions are All)
        val shape = Shape3(2, 3, 4)
        val dummyVector: DummyVector = listOf(
            DummyIndex.All,
            DummyIndex.All,
            DummyIndex.All
        )

        val indicesRowMajor = dummyVector.iterateWithOrder(shape, AccessOrder.RowMajor).toList()
        val indicesColMajor = dummyVector.iterateWithOrder(shape, AccessOrder.ColumnMajor).toList()

        // 两种顺序应该覆盖相同数量的元素
        // Both orders should cover same number of elements
        assertEquals(24, indicesRowMajor.size)
        assertEquals(24, indicesColMajor.size)

        // 验证唯一性
        // Verify uniqueness
        val uniqueRow = indicesRowMajor.map { it.toList() }.distinct()
        val uniqueCol = indicesColMajor.map { it.toList() }.distinct()
        assertEquals(24, uniqueRow.size)
        assertEquals(24, uniqueCol.size)
    }

    @Test
    fun testSingleIndexOnly() {
        // 测试仅使用 Single 索引
        // Test using only Single indices
        val shape = Shape3(2, 3, 4)
        val dummyVector: DummyVector = listOf(
            DummyIndex.Index(1),
            DummyIndex.Index(2),
            DummyIndex.Index(3)
        )

        val indices = dummyVector.iterateWithOrder(shape).toList()

        // 应该只有一个索引
        // Should have only one index
        assertEquals(1, indices.size)
        assertArrayEquals(intArrayOf(1, 2, 3), indices[0])
    }

    // ========================================================================
    // 迭代器快照独立性测试
    // Iterator snapshot independence tests
    // ========================================================================

    @Test
    fun testIteratorReturnsIndependentSnapshots() {
        // 测试迭代器返回独立快照
        // Test iterator returns independent snapshots
        val shape = Shape2(2, 3)
        val dummyVector: DummyVector = listOf(
            DummyIndex.All,
            DummyIndex.All
        )

        val indices = dummyVector.iterateWithOrder(shape).toList()

        // 所有索引应该是独立的对象
        // All indices should be independent objects
        for (i in indices.indices) {
            for (j in indices.indices) {
                if (i != j) {
                    val original = indices[j].copyOf()
                    indices[i][0] = 999
                    assertArrayEquals(original, indices[j])
                    indices[i][0] = 0  // restore
                }
            }
        }
    }

    // Helper function
    private fun assertArrayEquals(expected: IntArray, actual: IntArray) {
        assertEquals(expected.size, actual.size)
        for (i in expected.indices) {
            assertEquals(expected[i], actual[i])
        }
    }
}
