/**
 * 表达式序列化测试
 * Expression Serialization Tests
 *
 * 验收标准：
 * 1. serde round-trip 结构等价
 */
package fuookami.ospf.kotlin.math.symbol.expression.serde

import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import fuookami.ospf.kotlin.math.symbol.*
import fuookami.ospf.kotlin.math.symbol.expression.*
import fuookami.ospf.kotlin.math.Trivalent

@DisplayName("Expression Serde Tests / 表达式序列化测试")
class ExpressionSerdeTest {

    @Nested
    @DisplayName("ScalarExpression Serde Tests / 标量表达式序列化测试")
    inner class ScalarExpressionSerdeTests {

        @Test
        @DisplayName("Constant round-trip / 常量往返测试")
        fun testConstantRoundTrip() {
            val original: ScalarExpression<Int> = ScalarConstant(42)

            val json = original.toJsonString()
            val restored = scalarExpressionFromJson(json)

            assertTrue(restored is ScalarConstant<*>)
            assertEquals(42, (restored as ScalarConstant<*>).value)
        }

        @Test
        @DisplayName("String constant round-trip / 字符串常量往返测试")
        fun testStringConstantRoundTrip() {
            val original: ScalarExpression<String> = ScalarConstant("hello")

            val json = original.toJsonString()
            val restored = scalarExpressionFromJson(json)

            assertTrue(restored is ScalarConstant<*>)
            assertEquals("hello", (restored as ScalarConstant<*>).value)
        }

        @Test
        @DisplayName("Reference round-trip / 引用往返测试")
        fun testReferenceRoundTrip() {
            val original: ScalarExpression<Int> = ScalarReference(PropertyPath.parse("user.age"))

            val json = original.toJsonString()
            val restored = scalarExpressionFromJson(json)

            assertTrue(restored is ScalarReference<*>)
            assertEquals("user.age", (restored as ScalarReference<*>).path.value)
        }

        @Test
        @DisplayName("Symbol reference round-trip / 符号引用往返测试")
        fun testSymbolReferenceRoundTrip() {
            val original: ScalarExpression<Int> = ScalarSymbolReference(
                OwnedSymbol(SymbolId("stable-x"), "x")
            )

            val json = original.toJsonString()
            val restored = scalarExpressionFromJson(json)

            assertTrue(restored is ScalarSymbolReference<*>)
            val restoredSymbol = (restored as ScalarSymbolReference<*>).symbol
            assertTrue(restoredSymbol is OwnedSymbolLike)
            assertEquals("stable-x", (restoredSymbol as OwnedSymbolLike).id.value)
        }

        @Test
        @DisplayName("Binary expression round-trip / 二元表达式往返测试")
        fun testBinaryRoundTrip() {
            val original: ScalarExpression<Double> = ScalarBinary(
                BinaryOperator.Add,
                ScalarConstant(10.0),
                ScalarConstant(5.0)
            )

            val json = original.toJsonString()
            val restored = scalarExpressionFromJson(json)

            assertTrue(restored is ScalarBinary<*>)
            assertEquals(BinaryOperator.Add, (restored as ScalarBinary<*>).operator)
        }

        @Test
        @DisplayName("Custom scalar round-trip keeps payload text / 自定义标量往返保留 payload 文本")
        fun testCustomScalarRoundTrip() {
            val original: ScalarExpression<Any> = ScalarCustom(mapOf("k" to 1), "meta")

            val json = original.toJsonString()
            val restored = scalarExpressionFromJson(json)

            assertTrue(restored is ScalarCustom<*>)
            val custom = restored as ScalarCustom<*>
            assertEquals("{k=1}", custom.value)
            assertEquals("meta", custom.description)
        }

        @Test
        @DisplayName("Function round-trip / 函数往返测试")
        fun testFunctionRoundTrip() {
            val original: ScalarExpression<Int> = ScalarFunction(
                ScalarFunctionNames.Abs,
                listOf(ScalarReference(PropertyPath.parse("x")))
            )

            val json = original.toJsonString()
            val restored = scalarExpressionFromJson(json)

            assertTrue(restored is ScalarFunction<*>)
            val function = restored as ScalarFunction<*>
            assertEquals(ScalarFunctionNames.Abs, function.name)
            assertEquals(1, function.arguments.size)
            assertTrue(function.arguments.first() is ScalarReference<*>)
        }
    }

    @Nested
    @DisplayName("BooleanExpression Serde Tests / 布尔表达式序列化测试")
    inner class BooleanExpressionSerdeTests {

        @Test
        @DisplayName("Boolean constant round-trip / 布尔常量往返测试")
        fun testBooleanConstantRoundTrip() {
            val original = BooleanConstant(Trivalent.True)

            val json = original.toJsonString()
            val restored = booleanExpressionFromJson(json)

            assertTrue(restored is BooleanConstant)
            assertEquals(Trivalent.True, (restored as BooleanConstant).value)
        }

        @Test
        @DisplayName("Comparison round-trip / 比较表达式往返测试")
        fun testComparisonRoundTrip() {
            val original = Comparison(
                ComparisonOperator.Gt,
                ScalarReference(PropertyPath.parse("age")),
                ScalarConstant(18)
            )

            val json = original.toJsonString()
            val restored = booleanExpressionFromJson(json)

            assertTrue(restored is Comparison<*>)
            assertEquals(ComparisonOperator.Gt, (restored as Comparison<*>).operator)
        }

        @Test
        @DisplayName("And expression round-trip / And 表达式往返测试")
        fun testAndRoundTrip() {
            val original = AndExpression(listOf(
                Comparison(ComparisonOperator.Gt, ScalarReference(PropertyPath.parse("age")), ScalarConstant(18)),
                Comparison(ComparisonOperator.Eq, ScalarReference(PropertyPath.parse("status")), ScalarConstant("active"))
            ))

            val json = original.toJsonString()
            val restored = booleanExpressionFromJson(json)

            assertTrue(restored is AndExpression)
            assertEquals(2, (restored as AndExpression).operands.size)
        }

        @Test
        @DisplayName("Or expression round-trip / Or 表达式往返测试")
        fun testOrRoundTrip() {
            val original = OrExpression(listOf(
                Comparison(ComparisonOperator.Lt, ScalarReference(PropertyPath.parse("age")), ScalarConstant(18)),
                Comparison(ComparisonOperator.Gt, ScalarReference(PropertyPath.parse("age")), ScalarConstant(65))
            ))

            val json = original.toJsonString()
            val restored = booleanExpressionFromJson(json)

            assertTrue(restored is OrExpression)
            assertEquals(2, (restored as OrExpression).operands.size)
        }

        @Test
        @DisplayName("Not expression round-trip / Not 表达式往返测试")
        fun testNotRoundTrip() {
            val original = NotExpression(
                Comparison(ComparisonOperator.Eq, ScalarReference(PropertyPath.parse("status")), ScalarConstant("deleted"))
            )

            val json = original.toJsonString()
            val restored = booleanExpressionFromJson(json)

            assertTrue(restored is NotExpression)
        }

        @Test
        @DisplayName("In expression round-trip / In 表达式往返测试")
        fun testInRoundTrip() {
            val original = InExpression(
                ScalarReference(PropertyPath.parse("status")),
                listOf(ScalarConstant("active"), ScalarConstant("pending"))
            )

            val json = original.toJsonString()
            val restored = booleanExpressionFromJson(json)

            assertTrue(restored is InExpression<*>)
            assertEquals(2, (restored as InExpression<*>).candidates.size)
            assertFalse(restored.negated)
        }

        @Test
        @DisplayName("Null check round-trip / 空值检查往返测试")
        fun testNullCheckRoundTrip() {
            val original = NullCheck(PropertyPath.parse("email"), NullCheckType.IsNotNull)

            val json = original.toJsonString()
            val restored = booleanExpressionFromJson(json)

            assertTrue(restored is NullCheck)
            assertEquals(NullCheckType.IsNotNull, (restored as NullCheck).type)
            assertEquals("email", restored.path.value)
        }

        @Test
        @DisplayName("Complex expression round-trip / 复杂表达式往返测试")
        fun testComplexRoundTrip() {
            val original = OrExpression(listOf(
                AndExpression(listOf(
                    Comparison(ComparisonOperator.Gt, ScalarReference(PropertyPath.parse("age")), ScalarConstant(18)),
                    Comparison(ComparisonOperator.Eq, ScalarReference(PropertyPath.parse("status")), ScalarConstant("active"))
                )),
                NotExpression(NullCheck(PropertyPath.parse("email"), NullCheckType.IsNull))
            ))

            val json = original.toJsonString()
            val restored = booleanExpressionFromJson(json)

            assertTrue(restored is OrExpression)
            val or = restored as OrExpression
            assertEquals(2, or.operands.size)
            assertTrue(or.operands[0] is AndExpression)
            assertTrue(or.operands[1] is NotExpression)
        }

        @Test
        @DisplayName("Custom boolean round-trip keeps payload text / 自定义布尔往返保留 payload 文本")
        fun testCustomBooleanRoundTrip() {
            val original: BooleanExpression = BooleanCustom(listOf("x", "y"), "tag")

            val json = original.toJsonString()
            val restored = booleanExpressionFromJson(json)

            assertTrue(restored is BooleanCustom)
            val custom = restored as BooleanCustom
            assertEquals("[x, y]", custom.value)
            assertEquals("tag", custom.description)
        }

        @Test
        @DisplayName("Path with dots round-trip / 点分隔路径往返测试")
        fun testPathWithDotsRoundTrip() {
            val original = Comparison(
                ComparisonOperator.Eq,
                ScalarReference(PropertyPath.parse("user.address.city")),
                ScalarConstant("Beijing")
            )

            val json = original.toJsonString()
            val restored = booleanExpressionFromJson(json)

            assertTrue(restored is Comparison<*>)
            val comp = restored as Comparison<*>
            val left = comp.left as? ScalarReference<*>
            assertEquals("user.address.city", left?.path?.value)
        }
    }

    @Nested
    @DisplayName("Error Handling Tests / 错误处理测试")
    inner class ErrorHandlingTests {

        @Test
        @DisplayName("Invalid JSON returns null / 无效 JSON 返回 null")
        fun testInvalidJson() {
            val restored = booleanExpressionFromJsonOrNull("invalid json")
            assertNull(restored)
        }
    }
}
