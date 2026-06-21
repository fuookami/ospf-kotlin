package fuookami.ospf.kotlin.multiarray

import kotlin.test.assertEquals
import org.junit.jupiter.api.Test

/**
 * MappedMultiArrayView 映射测试
 * MappedMultiArrayView mapping tests
 *
 * 测试内容：
 * Test contents:
 * - 单次映射测试
 *   Single mapping tests
 * - 转置测试
 *   Transpose tests
 */
class ChainedMappingViewTest {

    // ========================================================================
    // 单次映射测试（基础验证）
    // Single mapping tests (basic verification)
    // ========================================================================

    @Test
    fun testSingleTranspose() {
        // 测试单次转置映射（2x3 -> 3x2）
        // Test single transpose mapping (2x3 -> 3x2)
        val array = MultiArray.newBy(Shape2(2, 3)) { i, vec ->
            vec[0] * 10 + vec[1]  // row*10 + col
        }

        // 转置：维度 0->1, 维度 1->0
        // Transpose: dimension 0->1, dimension 1->0
        val transposed = MappedMultiArrayView(array, listOf(
            MapIndex.Map(1),  // new dim 0 from old dim 1
            MapIndex.Map(0)   // new dim 1 from old dim 0
        ))

        // 验证形状
        // Verify shape
        assertEquals(2, transposed.shape.dimension)
        assertEquals(3, transposed.shape[0])  // old dim 1 size
        assertEquals(2, transposed.shape[1])  // old dim 0 size

        // 验证数据访问
        // Verify data access
        // transposed[0,0] should equal array[0,0] (row=0,col=0)
        assertOkEquals(array[intArrayOf(0, 0)], transposed[intArrayOf(0, 0)])
        // transposed[1,0] should equal array[0,1] (row=0,col=1)
        assertOkEquals(array[intArrayOf(0, 1)], transposed[intArrayOf(1, 0)])
        // transposed[0,1] should equal array[1,0] (row=1,col=0)
        assertOkEquals(array[intArrayOf(1, 0)], transposed[intArrayOf(0, 1)])
    }

    @Test
    fun testDimensionReorder3D() {
        // 测试 3D 维度重排（2x3x4 -> 4x2x3）
        // Test 3D dimension reorder (2x3x4 -> 4x2x3)
        val array = MultiArray.newBy(Shape3(2, 3, 4)) { i, vec ->
            vec[0] * 100 + vec[1] * 10 + vec[2]
        }

        // 重排：dim0->2, dim1->0, dim2->1
        // Reorder: dim0->2, dim1->0, dim2->1
        val reordered = MappedMultiArrayView(array, listOf(
            MapIndex.Map(2),  // new dim 0 from old dim 2
            MapIndex.Map(0),  // new dim 1 from old dim 0
            MapIndex.Map(1)   // new dim 2 from old dim 1
        ))

        // 验证形状
        // Verify shape
        assertEquals(3, reordered.shape.dimension)
        assertEquals(4, reordered.shape[0])  // old dim 2 size
        assertEquals(2, reordered.shape[1])  // old dim 0 size
        assertEquals(3, reordered.shape[2])  // old dim 1 size

        // 验证数据映射
        // Verify data mapping
        // reordered[a,b,c] should equal array[b,c,a]
        for (a in 0 until 4) {
            for (b in 0 until 2) {
                for (c in 0 until 3) {
                    val expected = array[intArrayOf(b, c, a)]
                    assertOkEquals(expected, reordered[intArrayOf(a, b, c)])
                }
            }
        }
    }

    // ========================================================================
    // 向量访问一致性测试
    // Vector access consistency tests
    // ========================================================================

    @Test
    fun testMappedViewVectorAccessConsistency() {
        // 测试映射视图向量访问一致性
        // Test mapped view vector access consistency
        val array = MultiArray.newBy(Shape2(4, 5)) { i, vec ->
            vec[0] * 10 + vec[1]
        }

        val transposed = MappedMultiArrayView(array, listOf(
            MapIndex.Map(1),
            MapIndex.Map(0)
        ))

        // 验证所有向量索引访问
        // Verify all vector index access
        for (newRow in 0 until 5) {
            for (newCol in 0 until 4) {
                val oldRow = newCol
                val oldCol = newRow
                assertOkEquals(
                    array[intArrayOf(oldRow, oldCol)],
                    transposed[intArrayOf(newRow, newCol)]
                )
            }
        }
    }

    // ========================================================================
    // 边界映射测试
    // Boundary mapping tests
    // ========================================================================

    @Test
    fun testIdentityMapping() {
        // 测试恒等映射（所有维度保持不变）
        // Test identity mapping (all dimensions unchanged)
        val array = MultiArray.newBy(Shape3(2, 3, 4)) { i, vec ->
            vec[0] * 100 + vec[1] * 10 + vec[2]
        }

        val identity = MappedMultiArrayView(array, listOf(
            MapIndex.Map(0),
            MapIndex.Map(1),
            MapIndex.Map(2)
        ))

        // 恒等映射应该完全保持原数组
        // Identity mapping should preserve original array completely
        assertEquals(2, identity.shape[0])
        assertEquals(3, identity.shape[1])
        assertEquals(4, identity.shape[2])

        for (i in 0 until 2) {
            for (j in 0 until 3) {
                for (k in 0 until 4) {
                    assertOkEquals(array[intArrayOf(i, j, k)], identity[intArrayOf(i, j, k)])
                }
            }
        }
    }

    @Test
    fun testSingleDimensionArrayMapping() {
        // 测试单维度数组映射
        // Test single dimension array mapping
        val array = MultiArray.newBy(Shape1(5)) { i, _ -> i }

        // 单维度只有恒等映射
        // Single dimension only has identity mapping
        val mapped = MappedMultiArrayView(array, listOf(
            MapIndex.Map(0)
        ))

        assertEquals(5, mapped.shape.size)
        for (i in 0 until 5) {
            assertEquals(array[i], mapped[i])
        }
    }
}
