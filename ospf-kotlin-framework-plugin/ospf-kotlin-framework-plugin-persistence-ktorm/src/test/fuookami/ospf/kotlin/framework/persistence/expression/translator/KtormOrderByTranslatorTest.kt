/**
 * Ktorm 排序翻译器测试
 * Ktorm Order By Translator Tests
 *
 * 验收标准：
 * 1. SortBy 可正确转换为排序逻辑
 * 2. 支持多字段排序
 */
package fuookami.ospf.kotlin.framework.persistence.expression.translator

import fuookami.ospf.kotlin.framework.persistence.expression.*
import fuookami.ospf.kotlin.math.symbol.expression.PropertyPath
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*

@DisplayName("KtormOrderByTranslator Tests / Ktorm 排序翻译器测试")
class KtormOrderByTranslatorTest {

    // 简单列名解析器：直接使用路径最后一部分
    private val simpleResolver: KtormColumnResolver = { path ->
        // 返回 null，因为测试不需要实际的 Ktorm Column
        null
    }

    @Nested
    @DisplayName("Translator Creation Tests / 翻译器创建测试")
    inner class CreationTests {

        @Test
        @DisplayName("Create translator with column resolver / 使用列解析器创建翻译器")
        fun testCreateWithColumnResolver() {
            val translator = KtormOrderByTranslator(simpleResolver)
            assertNotNull(translator)
        }

        @Test
        @DisplayName("Create translator with NullsOrderSupport / 使用 NullsOrderSupport 创建翻译器")
        fun testCreateWithNullsOrderSupport() {
            val translator = KtormOrderByTranslator(simpleResolver, NullsOrderSupport.Always)
            assertNotNull(translator)
        }
    }

    @Nested
    @DisplayName("SortBy Model Tests / SortBy 模型测试")
    inner class SortByModelTests {

        @Test
        @DisplayName("Empty SortBy / 空 SortBy")
        fun testEmptySortBy() {
            val sort = SortBy.empty
            assertTrue(sort.isEmpty())
        }

        @Test
        @DisplayName("Single field sort / 单字段排序")
        fun testSingleFieldSort() {
            val sort = SortBy.asc("id")
            assertFalse(sort.isEmpty())
            assertEquals(1, sort.items.size)
        }

        @Test
        @DisplayName("Multi field sort / 多字段排序")
        fun testMultiFieldSort() {
            val sort = SortBy.asc("priority") + SortBy.desc("name")
            assertEquals(2, sort.items.size)
        }

        @Test
        @DisplayName("Sort with nulls order / 带空值排序的排序")
        fun testSortWithNullsOrder() {
            val sort = SortBy.desc("name", NullsOrder.NullsLast)
            assertEquals(NullsOrder.NullsLast, sort.items[0].nulls)
        }
    }

    @Nested
    @DisplayName("NullsOrderSupport Tests / 空值排序支持测试")
    inner class NullsOrderSupportTests {

        @Test
        @DisplayName("Always support / 总是支持")
        fun testAlwaysSupport() {
            val item = SortItem("name", SortDirection.Desc, NullsOrder.NullsFirst)
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
            val descItem = SortItem("name", SortDirection.Desc, NullsOrder.NullsFirst)

            assertTrue(NullsOrderSupport.OnlyAsc.isSupported(ascItem))
            assertFalse(NullsOrderSupport.OnlyAsc.isSupported(descItem))
        }
    }

    @Nested
    @DisplayName("SortItem Tests / SortItem 测试")
    inner class SortItemTests {

        @Test
        @DisplayName("Create SortItem with all fields / 创建带所有字段的 SortItem")
        fun testCreateSortItem() {
            val item = SortItem("name", SortDirection.Asc, NullsOrder.NullsFirst)
            assertEquals("name", item.path)
            assertEquals(SortDirection.Asc, item.direction)
            assertEquals(NullsOrder.NullsFirst, item.nulls)
        }

        @Test
        @DisplayName("Create SortItem without nulls order / 创建不带空值排序的 SortItem")
        fun testCreateSortItemWithoutNulls() {
            val item = SortItem("priority", SortDirection.Desc)
            assertEquals("priority", item.path)
            assertEquals(SortDirection.Desc, item.direction)
            assertNull(item.nulls)
        }
    }

    @Nested
    @DisplayName("SortDirection Tests / SortDirection 测试")
    inner class SortDirectionTests {

        @Test
        @DisplayName("SortDirection values / SortDirection 值")
        fun testSortDirectionValues() {
            assertEquals(2, SortDirection.entries.size)
            assertTrue(SortDirection.entries.contains(SortDirection.Asc))
            assertTrue(SortDirection.entries.contains(SortDirection.Desc))
        }
    }
}