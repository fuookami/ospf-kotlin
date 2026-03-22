package fuookami.ospf.kotlin.utils.multi_array

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * 访问顺序和迭代器测试
 * Access order and iterator tests
 */
class AccessOrderTest {

    // ========================================================================
    // AccessOrder 枚举测试
    // AccessOrder enum tests
    // ========================================================================

    @Test
    fun testAccessOrderDefault() {
        // 测试默认访问顺序是 RowMajor
        // Test default access order is RowMajor
        assertEquals(AccessOrder.RowMajor, AccessOrder.Default)
    }

    @Test
    fun testAccessOrderValues() {
        // 测试 AccessOrder 枚举值
        // Test AccessOrder enum values
        val orders = AccessOrder.values()
        assertEquals(2, orders.size)
        assertTrue(AccessOrder.RowMajor in orders)
        assertTrue(AccessOrder.ColumnMajor in orders)
    }

    // ========================================================================
    // IteratorPosition 测试
    // IteratorPosition tests
    // ========================================================================

    @Test
    fun testIteratorPositionCreation() {
        // 测试迭代器位置创建
        // Test iterator position creation
        val position = IteratorPosition(intArrayOf(0, 1, 2), false)
        assertEquals(3, position.positions.size)
        assertEquals(0, position.positions[0])
        assertEquals(1, position.positions[1])
        assertEquals(2, position.positions[2])
        assertFalse(position.exhausted)
    }

    @Test
    fun testIteratorPositionEquality() {
        // 测试迭代器位置相等性
        // Test iterator position equality
        val pos1 = IteratorPosition(intArrayOf(1, 2, 3), false)
        val pos2 = IteratorPosition(intArrayOf(1, 2, 3), false)
        val pos3 = IteratorPosition(intArrayOf(1, 2, 4), false)

        assertEquals(pos1, pos2)
        assertEquals(pos1.hashCode(), pos2.hashCode())
        assertTrue(pos1 != pos3)
    }

    @Test
    fun testIteratorPositionExhausted() {
        // 测试 exhausted 标志
        // Test exhausted flag
        val position1 = IteratorPosition(intArrayOf(0, 0), false)
        val position2 = IteratorPosition(intArrayOf(0, 0), true)

        assertFalse(position1.exhausted)
        assertTrue(position2.exhausted)
        // exhausted 不参与 equals 比较，只比较 positions
        // exhausted is not part of equals, only positions are compared
        assertEquals(position1, position2)
    }
}