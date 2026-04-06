/**
 * 旧表达式桥接测试
 * Legacy Expression Bridge Tests
 *
 * 验收标准：
 * 1. 新旧表达式可在公共子集上互通
 */
package fuookami.ospf.kotlin.math.symbol.expression.adapter

import fuookami.ospf.kotlin.math.symbol.parser.Expr
import fuookami.ospf.kotlin.math.symbol.parser.BinaryOperator as LegacyBinaryOperator
import fuookami.ospf.kotlin.math.symbol.parser.ComparisonOperator as LegacyComparisonOperator
import fuookami.ospf.kotlin.math.symbol.expression.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*

@DisplayName("LegacyExprBridge Tests / 旧表达式桥接测试")
class LegacyExprBridgeTest {

    @Nested
    @DisplayName("Expr to ScalarExpression Tests / Expr 到 ScalarExpression 测试")
    inner class ExprToScalarTests {

        @Test
        @DisplayName("NumberLiteral to Constant / 数字字面量到常量")
        fun testNumberLiteralToConstant() {
            val legacy = Expr.NumberLiteral("42")
            val newExpr = legacy.toScalarExpressionOrNull()

            assertNotNull(newExpr)
            assertTrue(newExpr is ScalarConstant<*>)
            assertEquals(42.0, (newExpr as ScalarConstant<*>).value)
        }

        @Test
        @DisplayName("Identifier to Reference / 标识符到引用")
        fun testIdentifierToReference() {
            val legacy = Expr.Identifier("x")
            val newExpr = legacy.toScalarExpressionOrNull()

            assertNotNull(newExpr)
            assertTrue(newExpr is ScalarReference<*>)
            assertEquals("x", (newExpr as ScalarReference<*>).path.value)
        }

        @Test
        @DisplayName("UnaryMinus to Unary / 一元负号到一元表达式")
        fun testUnaryMinusToUnary() {
            val legacy = Expr.UnaryMinus(Expr.Identifier("x"))
            val newExpr = legacy.toScalarExpressionOrNull()

            assertNotNull(newExpr)
            assertTrue(newExpr is ScalarUnary<*>)
            assertEquals(UnaryOperator.Negate, (newExpr as ScalarUnary<*>).operator)
        }

        @Test
        @DisplayName("Binary Add to Binary / 二元加法到二元表达式")
        fun testBinaryAdd() {
            val legacy = Expr.Binary(Expr.Identifier("a"), LegacyBinaryOperator.Add, Expr.Identifier("b"))
            val newExpr = legacy.toScalarExpressionOrNull()

            assertNotNull(newExpr)
            assertTrue(newExpr is ScalarBinary<*>)
            assertEquals(BinaryOperator.Add, (newExpr as ScalarBinary<*>).operator)
        }

        @Test
        @DisplayName("Binary Subtract to Binary / 二元减法到二元表达式")
        fun testBinarySubtract() {
            val legacy = Expr.Binary(Expr.Identifier("a"), LegacyBinaryOperator.Subtract, Expr.NumberLiteral("1"))
            val newExpr = legacy.toScalarExpressionOrNull()

            assertNotNull(newExpr)
            assertTrue(newExpr is ScalarBinary<*>)
            assertEquals(BinaryOperator.Subtract, (newExpr as ScalarBinary<*>).operator)
        }

        @Test
        @DisplayName("Binary Multiply to Binary / 二元乘法到二元表达式")
        fun testBinaryMultiply() {
            val legacy = Expr.Binary(Expr.Identifier("x"), LegacyBinaryOperator.Multiply, Expr.NumberLiteral("2"))
            val newExpr = legacy.toScalarExpressionOrNull()

            assertNotNull(newExpr)
            assertTrue(newExpr is ScalarBinary<*>)
            assertEquals(BinaryOperator.Multiply, (newExpr as ScalarBinary<*>).operator)
        }

        @Test
        @DisplayName("Binary Power to Binary / 二元幂运算到二元表达式")
        fun testBinaryPower() {
            val legacy = Expr.Binary(Expr.Identifier("x"), LegacyBinaryOperator.Power, Expr.NumberLiteral("2"))
            val newExpr = legacy.toScalarExpressionOrNull()

            assertNotNull(newExpr)
            assertTrue(newExpr is ScalarBinary<*>)
            assertEquals(BinaryOperator.Power, (newExpr as ScalarBinary<*>).operator)
        }

        @Test
        @DisplayName("FunctionCall to Function / 函数调用到函数表达式")
        fun testFunctionCallToFunction() {
            val legacy = Expr.FunctionCall("sqrt", listOf(Expr.Identifier("x")))
            val newExpr = legacy.toScalarExpressionOrNull()

            assertNotNull(newExpr)
            assertTrue(newExpr is ScalarFunction<*>)
            assertEquals("sqrt", (newExpr as ScalarFunction<*>).name)
            assertEquals(1, newExpr.arguments.size)
        }

        @Test
        @DisplayName("Nested expression / 嵌套表达式")
        fun testNestedExpression() {
            val legacy = Expr.Binary(
                Expr.Binary(Expr.Identifier("a"), LegacyBinaryOperator.Add, Expr.Identifier("b")),
                LegacyBinaryOperator.Multiply,
                Expr.NumberLiteral("2")
            )
            val newExpr = legacy.toScalarExpressionOrNull()

            assertNotNull(newExpr)
            assertTrue(newExpr is ScalarBinary<*>)
            val binary = newExpr as ScalarBinary<*>
            assertTrue(binary.left is ScalarBinary<*>)
        }
    }

    @Nested
    @DisplayName("Expr to BooleanExpression Tests / Expr 到 BooleanExpression 测试")
    inner class ExprToBooleanTests {

        @Test
        @DisplayName("Comparison Less / 小于比较")
        fun testComparisonLess() {
            val legacy = Expr.Comparison(Expr.Identifier("x"), LegacyComparisonOperator.Less, Expr.NumberLiteral("10"))
            val newExpr = legacy.toBooleanExpressionOrNull()

            assertNotNull(newExpr)
            assertTrue(newExpr is Comparison<*>)
            assertEquals(ComparisonOperator.Lt, (newExpr as Comparison<*>).operator)
        }

        @Test
        @DisplayName("Comparison Equal / 等于比较")
        fun testComparisonEqual() {
            val legacy = Expr.Comparison(Expr.Identifier("status"), LegacyComparisonOperator.Equal, Expr.Identifier("active"))
            val newExpr = legacy.toBooleanExpressionOrNull()

            assertNotNull(newExpr)
            assertTrue(newExpr is Comparison<*>)
            assertEquals(ComparisonOperator.Eq, (newExpr as Comparison<*>).operator)
        }

        @Test
        @DisplayName("Comparison Greater / 大于比较")
        fun testComparisonGreater() {
            val legacy = Expr.Comparison(Expr.Identifier("age"), LegacyComparisonOperator.Greater, Expr.NumberLiteral("18"))
            val newExpr = legacy.toBooleanExpressionOrNull()

            assertNotNull(newExpr)
            assertTrue(newExpr is Comparison<*>)
            assertEquals(ComparisonOperator.Gt, (newExpr as Comparison<*>).operator)
        }

        @Test
        @DisplayName("Comparison LessEqual / 小于等于比较")
        fun testComparisonLessEqual() {
            val legacy = Expr.Comparison(Expr.Identifier("x"), LegacyComparisonOperator.LessEqual, Expr.NumberLiteral("5"))
            val newExpr = legacy.toBooleanExpressionOrNull()

            assertNotNull(newExpr)
            assertTrue(newExpr is Comparison<*>)
            assertEquals(ComparisonOperator.Le, (newExpr as Comparison<*>).operator)
        }

        @Test
        @DisplayName("Comparison NotEqual / 不等于比较")
        fun testComparisonNotEqual() {
            val legacy = Expr.Comparison(Expr.Identifier("x"), LegacyComparisonOperator.NotEqual, Expr.NumberLiteral("0"))
            val newExpr = legacy.toBooleanExpressionOrNull()

            assertNotNull(newExpr)
            assertTrue(newExpr is Comparison<*>)
            assertEquals(ComparisonOperator.Ne, (newExpr as Comparison<*>).operator)
        }

        @Test
        @DisplayName("Comparison GreaterEqual / 大于等于比较")
        fun testComparisonGreaterEqual() {
            val legacy = Expr.Comparison(Expr.Identifier("x"), LegacyComparisonOperator.GreaterEqual, Expr.NumberLiteral("0"))
            val newExpr = legacy.toBooleanExpressionOrNull()

            assertNotNull(newExpr)
            assertTrue(newExpr is Comparison<*>)
            assertEquals(ComparisonOperator.Ge, (newExpr as Comparison<*>).operator)
        }
    }

    @Nested
    @DisplayName("ScalarExpression to Expr Tests / ScalarExpression 到 Expr 测试")
    inner class ScalarToExprTests {

        @Test
        @DisplayName("Constant to NumberLiteral / 常量到数字字面量")
        fun testConstantToNumberLiteral() {
            val newExpr: ScalarExpression<Double> = ScalarConstant(42.0)
            val legacy = newExpr.toLegacyExprOrNull()

            assertNotNull(legacy)
            assertTrue(legacy is Expr.NumberLiteral)
            assertEquals("42.0", (legacy as Expr.NumberLiteral).text)
        }

        @Test
        @DisplayName("String constant cannot convert to legacy / 字符串常量不能转换到旧 Expr")
        fun testStringConstantCannotConvert() {
            val newExpr: ScalarExpression<String> = ScalarConstant("active")
            val legacy = newExpr.toLegacyExprOrNull()
            assertNull(legacy)
        }

        @Test
        @DisplayName("Reference to Identifier / 引用到标识符")
        fun testReferenceToIdentifier() {
            val newExpr: ScalarExpression<Int> = ScalarReference(PropertyPath.parse("user.age"))
            val legacy = newExpr.toLegacyExprOrNull()

            assertNotNull(legacy)
            assertTrue(legacy is Expr.Identifier)
            assertEquals("user.age", (legacy as Expr.Identifier).name)
        }

        @Test
        @DisplayName("Unary Negate to UnaryMinus / 一元负号表达式")
        fun testUnaryNegateToUnaryMinus() {
            val newExpr = ScalarUnary(UnaryOperator.Negate, ScalarReference<Any>(PropertyPath.parse("x")))
            val legacy = newExpr.toLegacyExprOrNull()

            assertNotNull(legacy)
            assertTrue(legacy is Expr.UnaryMinus)
        }

        @Test
        @DisplayName("Binary Add to Binary / 二元加法表达式")
        fun testBinaryAddToBinary() {
            val newExpr = ScalarBinary(BinaryOperator.Add, ScalarConstant(1.0), ScalarConstant(2.0))
            val legacy = newExpr.toLegacyExprOrNull()

            assertNotNull(legacy)
            assertTrue(legacy is Expr.Binary)
            assertEquals(LegacyBinaryOperator.Add, (legacy as Expr.Binary).operator)
        }

        @Test
        @DisplayName("Binary Divide cannot convert / 二元除法无法转换")
        fun testBinaryDivideCannotConvert() {
            val newExpr = ScalarBinary(BinaryOperator.Divide, ScalarConstant(10.0), ScalarConstant(2.0))
            val legacy = newExpr.toLegacyExprOrNull()

            assertNull(legacy)  // Divide 不在旧 Expr 子集内 / Divide not in legacy subset
        }

        @Test
        @DisplayName("Function to FunctionCall / 函数表达式")
        fun testFunctionToFunctionCall() {
            val newExpr = ScalarFunction("sqrt", listOf(ScalarReference<Any>(PropertyPath.parse("x"))))
            val legacy = newExpr.toLegacyExprOrNull()

            assertNotNull(legacy)
            assertTrue(legacy is Expr.FunctionCall)
            assertEquals("sqrt", (legacy as Expr.FunctionCall).name)
        }
    }

    @Nested
    @DisplayName("BooleanExpression to Expr Tests / BooleanExpression 到 Expr 测试")
    inner class BooleanToExprTests {

        @Test
        @DisplayName("Comparison to Expr.Comparison / 比较表达式")
        fun testComparisonToExprComparison() {
            val newExpr = Comparison(ComparisonOperator.Gt, ScalarReference(PropertyPath.parse("age")), ScalarConstant(18))
            val legacy = newExpr.toLegacyExprOrNull()

            assertNotNull(legacy)
            assertTrue(legacy is Expr.Comparison)
            assertEquals(LegacyComparisonOperator.Greater, (legacy as Expr.Comparison).operator)
        }

        @Test
        @DisplayName("AndExpression cannot convert / And 表达式无法转换")
        fun testAndCannotConvert() {
            val newExpr = AndExpression(listOf(
                Comparison(ComparisonOperator.Gt, ScalarReference(PropertyPath.parse("a")), ScalarConstant(1)),
                Comparison(ComparisonOperator.Gt, ScalarReference(PropertyPath.parse("b")), ScalarConstant(2))
            ))
            val legacy = newExpr.toLegacyExprOrNull()

            assertNull(legacy)  // And 不在旧 Expr 子集内 / And not in legacy subset
        }

        @Test
        @DisplayName("OrExpression cannot convert / Or 表达式无法转换")
        fun testOrCannotConvert() {
            val newExpr = OrExpression(listOf(
                Comparison(ComparisonOperator.Lt, ScalarReference(PropertyPath.parse("a")), ScalarConstant(1)),
                Comparison(ComparisonOperator.Lt, ScalarReference(PropertyPath.parse("b")), ScalarConstant(2))
            ))
            val legacy = newExpr.toLegacyExprOrNull()

            assertNull(legacy)
        }

        @Test
        @DisplayName("InExpression cannot convert / In 表达式无法转换")
        fun testInCannotConvert() {
            val newExpr = InExpression(ScalarReference(PropertyPath.parse("status")), listOf(ScalarConstant("a")))
            val legacy = newExpr.toLegacyExprOrNull()

            assertNull(legacy)
        }

        @Test
        @DisplayName("NullCheck cannot convert / 空值检查无法转换")
        fun testNullCheckCannotConvert() {
            val newExpr = NullCheck(PropertyPath.parse("email"), NullCheckType.IsNull)
            val legacy = newExpr.toLegacyExprOrNull()

            assertNull(legacy)
        }
    }

    @Nested
    @DisplayName("Round-trip Tests / 往返测试")
    inner class RoundTripTests {

        @Test
        @DisplayName("ScalarExpression round-trip / ScalarExpression 往返")
        fun testScalarRoundTrip() {
            val legacy = Expr.Binary(Expr.Identifier("x"), LegacyBinaryOperator.Add, Expr.NumberLiteral("1"))
            val newExpr = legacy.toScalarExpressionOrNull()!!
            val backToLegacy = newExpr.toLegacyExprOrNull()

            assertNotNull(backToLegacy)
            assertTrue(backToLegacy is Expr.Binary)
            assertEquals(LegacyBinaryOperator.Add, (backToLegacy as Expr.Binary).operator)
        }

        @Test
        @DisplayName("BooleanExpression round-trip / BooleanExpression 往返")
        fun testBooleanRoundTrip() {
            val legacy = Expr.Comparison(Expr.Identifier("x"), LegacyComparisonOperator.Greater, Expr.NumberLiteral("0"))
            val newExpr = legacy.toBooleanExpressionOrNull()!!
            val backToLegacy = newExpr.toLegacyExprOrNull()

            assertNotNull(backToLegacy)
            assertTrue(backToLegacy is Expr.Comparison)
            assertEquals(LegacyComparisonOperator.Greater, (backToLegacy as Expr.Comparison).operator)
        }
    }
}
