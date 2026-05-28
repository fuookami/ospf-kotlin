/**
 * List 扩展操作符测试
 * List extension operator tests
 *
 * 测试内容：
 * Test contents:
 * - List2 的 All 索引访问
 *   All index access for List2
 * - List3 的 All 索引访问
 *   All index access for List3
 * - 边界场景：空列表、越界访问
 *   Boundary scenarios: empty list, out of bounds access
 */
package fuookami.ospf.kotlin.multiarray

import kotlin.test.*
import org.junit.jupiter.api.Test

/**
 * List 扩展函数测试
 * List extension function tests
 */
class ListExtensionsTest {

    // ========================================================================
    // List2 操作测试
    // List2 operation tests
    // ========================================================================

    @Test
    fun testList2TypeAlias() {
        val matrix: List2<Int> = listOf(
            listOf(1, 2, 3),
            listOf(4, 5, 6)
        )

        assertEquals(2, matrix.size)
        assertEquals(3, matrix[0].size)
    }

    @Test
    fun testList2GetAllByRow() {
        val matrix: List2<Int> = listOf(
            listOf(1, 2, 3),
            listOf(4, 5, 6)
        )

        val row0 = matrix[0, DummyIndex.All].toList()
        assertEquals(3, row0.size)
        assertEquals(listOf(1, 2, 3), row0)

        val row1 = matrix[1, DummyIndex.All].toList()
        assertEquals(3, row1.size)
        assertEquals(listOf(4, 5, 6), row1)
    }

    @Test
    fun testList2GetAllByColumn() {
        val matrix: List2<Int> = listOf(
            listOf(1, 2, 3),
            listOf(4, 5, 6)
        )

        val col0 = matrix[DummyIndex.All, 0].toList()
        assertEquals(2, col0.size)
        assertEquals(listOf(1, 4), col0)

        val col1 = matrix[DummyIndex.All, 1].toList()
        assertEquals(2, col1.size)
        assertEquals(listOf(2, 5), col1)
    }

    @Test
    fun testList2GetAllAll() {
        val matrix: List2<Int> = listOf(
            listOf(1, 2, 3),
            listOf(4, 5, 6)
        )

        val allElements = matrix[DummyIndex.All, DummyIndex.All].toList()
        assertEquals(6, allElements.size)
        assertTrue(allElements.containsAll(listOf(1, 2, 3, 4, 5, 6)))
    }

    @Test
    fun testList2DifferentRowLengths() {
        val jagged: List2<Int> = listOf(
            listOf(1, 2),
            listOf(3, 4, 5),
            listOf(6)
        )

        val col0 = jagged[DummyIndex.All, 0].toList()
        assertEquals(3, col0.size)
        assertEquals(listOf(1, 3, 6), col0)

        val col2 = jagged[DummyIndex.All, 2].toList()
        assertEquals(1, col2.size)
        assertEquals(5, col2[0])
    }

    // ========================================================================
    // List3 操作测试
    // List3 operation tests
    // ========================================================================

    @Test
    fun testList3TypeAlias() {
        val cube: List3<Int> = listOf(
            listOf(
                listOf(1, 2),
                listOf(3, 4)
            ),
            listOf(
                listOf(5, 6),
                listOf(7, 8)
            )
        )

        assertEquals(2, cube.size)
        assertEquals(2, cube[0].size)
        assertEquals(2, cube[0][0].size)
    }

    @Test
    fun testList3GetAllK() {
        val cube: List3<Int> = listOf(
            listOf(
                listOf(1, 2),
                listOf(3, 4)
            ),
            listOf(
                listOf(5, 6),
                listOf(7, 8)
            )
        )

        val result = cube[0, 0, DummyIndex.All].toList()
        assertEquals(2, result.size)
        assertEquals(listOf(1, 2), result)
    }

    @Test
    fun testList3GetAllAllAll() {
        val cube: List3<Int> = listOf(
            listOf(
                listOf(1, 2),
                listOf(3, 4)
            ),
            listOf(
                listOf(5, 6),
                listOf(7, 8)
            )
        )

        val allElements = cube[DummyIndex.All, DummyIndex.All, DummyIndex.All].toList()
        assertEquals(8, allElements.size)
        assertTrue(allElements.containsAll(listOf(1, 2, 3, 4, 5, 6, 7, 8)))
    }

    // ========================================================================
    // 边界场景测试
    // Boundary scenario tests
    // ========================================================================

    @Test
    fun testEmptyList2() {
        val empty: List2<Int> = emptyList()

        val allElements = empty[DummyIndex.All, DummyIndex.All].toList()
        assertTrue(allElements.isEmpty())

        val row = empty[0, DummyIndex.All].toList()
        assertTrue(row.isEmpty())
    }

    @Test
    fun testList2OutOfBoundRow() {
        val matrix: List2<Int> = listOf(
            listOf(1, 2, 3)
        )

        val row = matrix[1, DummyIndex.All].toList()
        assertTrue(row.isEmpty())
    }

    @Test
    fun testList2OutOfBoundColumn() {
        val matrix: List2<Int> = listOf(
            listOf(1, 2, 3)
        )

        val col = matrix[DummyIndex.All, 5].toList()
        assertTrue(col.isEmpty())
    }

    @Test
    fun testSingleElementList2() {
        val single: List2<Int> = listOf(
            listOf(42)
        )

        assertEquals(42, single[0, DummyIndex.All].first())
        assertEquals(42, single[DummyIndex.All, 0].first())
        assertEquals(42, single[DummyIndex.All, DummyIndex.All].first())
    }

    @Test
    fun testSingleElementList3() {
        val single: List3<Int> = listOf(
            listOf(
                listOf(42)
            )
        )

        assertEquals(42, single[0, 0, DummyIndex.All].first())
        assertEquals(42, single[DummyIndex.All, DummyIndex.All, DummyIndex.All].first())
    }
}
