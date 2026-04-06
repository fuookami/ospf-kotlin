/**
 * 排序模型测试
 * Sort Model Tests
 *
 * 验收标准：
 * 1. SortBy 支持多字段排序
 * 2. 支持空值排序配置
 */
package fuookami.ospf.kotlin.framework.persistence.expression

import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*

@DisplayName("SortBy Tests / 排序模型测试")
class SortByTest {

    @Nested
    @DisplayName("SortBy Creation Tests / SortBy 创建测试")
    inner class CreationTests {

        @Test
        @DisplayName("Create empty sort / 创建空排序")
        fun testEmptySort() {
            val sort = SortBy.empty
            assertTrue(sort.isEmpty())
            assertFalse(sort.isNotEmpty())
        }

        @Test
        @DisplayName("Create ascending sort / 创建升序排序")
        fun testAscSort() {
            val sort = SortBy.asc("name")
            assertFalse(sort.isEmpty())
            assertEquals(1, sort.items.size)
            assertEquals("name", sort.items[0].path)
            assertEquals(SortDirection.Asc, sort.items[0].direction)
            assertNull(sort.items[0].nulls)
        }

        @Test
        @DisplayName("Create descending sort / 创建降序排序")
        fun testDescSort() {
            val sort = SortBy.desc("createdAt")
            assertEquals(1, sort.items.size)
            assertEquals("createdAt", sort.items[0].path)
            assertEquals(SortDirection.Desc, sort.items[0].direction)
        }

        @Test
        @DisplayName("Create sort with nulls order / 创建带空值排序的排序")
        fun testSortWithNullsOrder() {
            val sort = SortBy.desc("updatedAt", NullsOrder.NullsLast)
            assertEquals(NullsOrder.NullsLast, sort.items[0].nulls)
        }

        @Test
        @DisplayName("Create multi-field sort / 创建多字段排序")
        fun testMultiFieldSort() {
            val sort = SortBy.asc("status", "name")
            assertEquals(2, sort.items.size)
            assertEquals("status", sort.items[0].path)
            assertEquals("name", sort.items[1].path)
        }
    }

    @Nested
    @DisplayName("SortBy Combination Tests / SortBy 组合测试")
    inner class CombinationTests {

        @Test
        @DisplayName("Combine sorts with plus / 使用 plus 组合排序")
        fun testCombineWithPlus() {
            val sort1 = SortBy.asc("name")
            val sort2 = SortBy.desc("createdAt")

            val combined = sort1 + sort2

            assertEquals(2, combined.items.size)
            assertEquals("name", combined.items[0].path)
            assertEquals(SortDirection.Asc, combined.items[0].direction)
            assertEquals("createdAt", combined.items[1].path)
            assertEquals(SortDirection.Desc, combined.items[1].direction)
        }

        @Test
        @DisplayName("Chain sorts with thenAsc/thenDesc / 使用 thenAsc/thenDesc 链式排序")
        fun testChainSorts() {
            val sort = SortBy.asc("status")
                .thenDesc("priority")
                .thenAsc("createdAt", NullsOrder.NullsLast)

            assertEquals(3, sort.items.size)
            assertEquals("status", sort.items[0].path)
            assertEquals(SortDirection.Asc, sort.items[0].direction)
            assertEquals("priority", sort.items[1].path)
            assertEquals(SortDirection.Desc, sort.items[1].direction)
            assertEquals("createdAt", sort.items[2].path)
            assertEquals(NullsOrder.NullsLast, sort.items[2].nulls)
        }
    }

    @Nested
    @DisplayName("NullsOrderSupport Tests / 空值排序支持测试")
    inner class NullsOrderSupportTests {

        @Test
        @DisplayName("Always support / 总是支持")
        fun testAlwaysSupport() {
            val item = SortItem("name", SortDirection.Asc, NullsOrder.NullsFirst)
            assertTrue(NullsOrderSupport.Always.isSupported(item))
        }

        @Test
        @DisplayName("Never support / 从不支持")
        fun testNeverSupport() {
            val item = SortItem("name", SortDirection.Asc, NullsOrder.NullsFirst)
            assertFalse(NullsOrderSupport.Never.isSupported(item))
        }

        @Test
        @DisplayName("OnlyAsc support / 仅升序支持")
        fun testOnlyAscSupport() {
            val ascItem = SortItem("name", SortDirection.Asc, NullsOrder.NullsFirst)
            val descItem = SortItem("name", SortDirection.Desc, NullsOrder.NullsLast)

            assertTrue(NullsOrderSupport.OnlyAsc.isSupported(ascItem))
            assertFalse(NullsOrderSupport.OnlyAsc.isSupported(descItem))
        }
    }
}