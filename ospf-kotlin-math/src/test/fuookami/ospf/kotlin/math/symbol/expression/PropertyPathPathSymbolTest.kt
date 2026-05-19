/**
 * 属性路径和路径符号测试
 * Property Path and Path Symbol Tests
 *
 * 验收标准：
 * 1. PropertyPath 分段解析正确
 * 2. PathSymbol 双向转换行为稳定（身份可重复计算且稳定）
 */
package fuookami.ospf.kotlin.math.symbol.expression

import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*

@DisplayName("PropertyPath and PathSymbol Tests / 属性路径和路径符号测试")
class PropertyPathPathSymbolTest {

    @Nested
    @DisplayName("PropertyPath Tests / 属性路径测试")
    inner class PropertyPathTests {

        @Test
        @DisplayName("Parse path from string / 从字符串解析路径")
        fun testParseFromString() {
            val path = PropertyPath.parse("user.address.city")

            assertEquals("user.address.city", path.value)
            assertEquals(3, path.depth)
            assertEquals(listOf("user", "address", "city"), path.segments)
            assertEquals("user", path.root)
            assertEquals("city", path.leaf)
        }

        @Test
        @DisplayName("Parse single segment / 解析单分段")
        fun testParseSingleSegment() {
            val path = PropertyPath.parse("name")

            assertEquals("name", path.value)
            assertEquals(1, path.depth)
            assertEquals(listOf("name"), path.segments)
            assertEquals("name", path.root)
            assertEquals("name", path.leaf)
        }

        @Test
        @DisplayName("Create path from segments / 从分段创建路径")
        fun testCreateFromSegments() {
            val path = PropertyPath.of("a", "b", "c")

            assertEquals("a.b.c", path.value)
            assertEquals(3, path.depth)
        }

        @Test
        @DisplayName("Empty path / 空路径")
        fun testEmptyPath() {
            val path = PropertyPath.empty

            assertTrue(path.isEmpty)
            assertEquals(0, path.depth)
            assertNull(path.root)
            assertNull(path.leaf)
        }

        @Test
        @DisplayName("Parent and child paths / 父路径和子路径")
        fun testParentAndChild() {
            val path = PropertyPath.parse("user.address.city")

            val parent = path.parent
            assertNotNull(parent)
            assertEquals("user.address", parent?.value)

            val child = path.child
            assertNotNull(child)
            assertEquals("address.city", child?.value)

            val rootParent = PropertyPath.parse("user").parent
            assertNull(rootParent)

            val rootChild = PropertyPath.parse("user").child
            assertNull(rootChild)
        }

        @Test
        @DisplayName("Concat paths / 拼接路径")
        fun testConcat() {
            val path1 = PropertyPath.parse("user")
            val path2 = PropertyPath.parse("address.city")

            val concatenated = path1.concat(path2)
            assertEquals("user.address.city", concatenated.value)

            val concatenated2 = path1.concat("name")
            assertEquals("user.name", concatenated2.value)
        }

        @Test
        @DisplayName("Sub-path check / 子路径检查")
        fun testSubPath() {
            val parent = PropertyPath.parse("user.address")
            val child = PropertyPath.parse("user.address.city")

            assertTrue(child.isSubPathOf(parent))
            assertTrue(parent.isParentPathOf(child))
            assertFalse(parent.isSubPathOf(child))
            assertFalse(child.isParentPathOf(parent))
        }

        @Test
        @DisplayName("Parse invalid path returns null / 解析无效路径返回 null")
        fun testParseInvalid() {
            val invalid = PropertyPath.parseOrNull("123.invalid")
            assertNull(invalid)

            val empty = PropertyPath.parseOrNull("")
            assertNull(empty)

            val valid = PropertyPath.parseOrNull("_valid_name")
            assertNotNull(valid)
            assertEquals("_valid_name", valid?.value)
        }
    }

    @Nested
    @DisplayName("PathSymbol Tests / 路径符号测试")
    inner class PathSymbolTests {

        @Test
        @DisplayName("PathSymbol identity / 路径符号身份")
        fun testIdentity() {
            val path = PropertyPath.parse("user.address.city")
            val symbol = PathSymbol(path)

            assertEquals(path.value, symbol.name)
            assertEquals("path:${path.value}", symbol.symbolId)
            assertEquals("user.address.city", symbol.name)
        }

        @Test
        @DisplayName("PathSymbol equality / 路径符号相等性")
        fun testEquality() {
            val path1 = PropertyPath.parse("user.address")
            val path2 = PropertyPath.parse("user.address")

            val symbol1 = PathSymbol(path1)
            val symbol2 = PathSymbol(path2)

            assertEquals(symbol1, symbol2)
            assertEquals(symbol1.hashCode(), symbol2.hashCode())
        }

        @Test
        @DisplayName("PathSymbol equality keeps symmetry / PathSymbol 相等性保持对称")
        fun testPathSymbolEqualitySymmetry() {
            val pathSymbol = PathSymbol(PropertyPath.parse("user.address"))
            val foreignIdentified = object : fuookami.ospf.kotlin.math.symbol.IdentifiedSymbol {
                override val name: String = "user.address"
                override val displayName: String? = null
                override val symbolId: String = pathSymbol.symbolId
            }

            assertNotEquals(pathSymbol, foreignIdentified)
        }

        @Test
        @DisplayName("PathSymbol inequality / 路径符号不等性")
        fun testInequality() {
            val path1 = PropertyPath.parse("user.address")
            val path2 = PropertyPath.parse("user.name")

            val symbol1 = PathSymbol(path1)
            val symbol2 = PathSymbol(path2)

            assertNotEquals(symbol1, symbol2)
        }

        @Test
        @DisplayName("PathSymbol identity stability / 路径符号身份稳定性")
        fun testIdentityStability() {
            // 身份可重复计算且稳定
            // Identity can be recomputed and is stable
            val path = PropertyPath.parse("test.path")
            val symbol1 = PathSymbol.from(path)
            val symbol2 = PathSymbol.from(path.value)

            assertEquals(symbol1.symbolId, symbol2.symbolId)
            assertEquals(symbol1, symbol2)
        }

        @Test
        @DisplayName("PathSymbol from segments / 从分段创建路径符号")
        fun testFromSegments() {
            val symbol = PathSymbol.of("a", "b", "c")

            assertEquals("a.b.c", symbol.name)
            assertEquals("path:a.b.c", symbol.symbolId)
        }
    }

    @Nested
    @DisplayName("Bidirectional Conversion Tests / 双向转换测试")
    inner class BidirectionalConversionTests {

        @Test
        @DisplayName("PropertyPath to PathSymbol / 属性路径转路径符号")
        fun testPropertyPathToPathSymbol() {
            val path = PropertyPath.parse("user.id")
            val symbol = path.toPathSymbol()

            assertEquals(path, symbol.path)
            assertEquals(path.value, symbol.name)
        }

        @Test
        @DisplayName("String to PathSymbol / 字符串转路径符号")
        fun testStringToPathSymbol() {
            val symbol = "user.name".toPathSymbol()

            assertEquals("user.name", symbol.name)
            assertEquals("path:user.name", symbol.symbolId)
        }

        @Test
        @DisplayName("PathSymbol to PropertyPath / 路径符号转属性路径")
        fun testPathSymbolToPropertyPath() {
            val path = PropertyPath.parse("user.address")
            val symbol = PathSymbol(path)

            val convertedPath = symbol.toPropertyPathOrNull()
            assertNotNull(convertedPath)
            assertEquals(path, convertedPath)
        }

        @Test
        @DisplayName("Non-PathSymbol to PropertyPath returns null / 非路径符号转属性路径返回 null")
        fun testNonPathSymbolToPropertyPath() {
            // 假设有一个非 PathSymbol 的符号
            // Assume we have a non-PathSymbol symbol
            val nonPathSymbol = object : fuookami.ospf.kotlin.math.symbol.Symbol {
                override val name = "test"
                override val displayName: String? = null
            }

            val converted = nonPathSymbol.toPropertyPathOrNull()
            assertNull(converted)
        }

        @Test
        @DisplayName("IdentifiedSymbol with path: prefix / 带 path: 前缀的识别符号")
        fun testIdentifiedSymbolWithPathPrefix() {
            val pathSymbol = PathSymbol(PropertyPath.parse("test.value"))

            val converted = pathSymbol.toPropertyPathFromIdOrNull()
            assertNotNull(converted)
            assertEquals("test.value", converted?.value)
        }

        @Test
        @DisplayName("IdentifiedSymbol without path: prefix / 不带 path: 前缀的识别符号")
        fun testIdentifiedSymbolWithoutPathPrefix() {
            val nonPathIdentified = object : fuookami.ospf.kotlin.math.symbol.IdentifiedSymbol {
                override val name: String = "other.value"
                override val displayName: String? = null
                override val symbolId = "other:value"
            }

            val converted = nonPathIdentified.toPropertyPathFromIdOrNull()
            assertNull(converted)
        }
    }
}