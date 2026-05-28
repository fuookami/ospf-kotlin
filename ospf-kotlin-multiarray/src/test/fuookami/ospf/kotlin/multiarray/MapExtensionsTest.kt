/**
 * Map 扩展操作符测试
 * Map extension operator tests
 *
 * 测试内容：
 * Test contents:
 * - Map<K, T>.get(All) 获取所有值
 *   Get all values using All index
 */
package fuookami.ospf.kotlin.multiarray

import kotlin.test.*
import org.junit.jupiter.api.Test

/**
 * Map 扩展函数测试
 * Map extension function tests
 */
class MapExtensionsTest {

    // ========================================================================
    // Map<K, T> 基础操作测试
    // Basic Map<K, T> operation tests
    // ========================================================================

    @Test
    fun testMapGetAll() {
        val map = mapOf(
            "a" to 1,
            "b" to 2,
            "c" to 3
        )

        val allValues = map[DummyIndex.All].toList()
        assertEquals(3, allValues.size)
        assertTrue(allValues.contains(1))
        assertTrue(allValues.contains(2))
        assertTrue(allValues.contains(3))
    }

    @Test
    fun testEmptyMapGetAll() {
        val emptyMap = emptyMap<String, Int>()

        val allValues = emptyMap[DummyIndex.All].toList()
        assertTrue(allValues.isEmpty())
    }
}
