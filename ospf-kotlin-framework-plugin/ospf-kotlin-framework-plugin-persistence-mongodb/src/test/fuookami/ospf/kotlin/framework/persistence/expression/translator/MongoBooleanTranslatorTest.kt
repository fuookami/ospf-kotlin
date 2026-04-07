/**
 * MongoDB 布尔表达式翻译器测试
 * MongoDB Boolean Expression Translator Tests
 *
 * 验收标准：
 * 1. BooleanExpression 可正确翻译为 MongoDB Bson
 */
package fuookami.ospf.kotlin.framework.persistence.expression.translator

import fuookami.ospf.kotlin.framework.persistence.expression.*
import fuookami.ospf.kotlin.math.symbol.expression.*
import fuookami.ospf.kotlin.math.Trivalent
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*

@DisplayName("MongoBooleanTranslator Tests / MongoDB 布尔翻译器测试")
class MongoBooleanTranslatorTest {

    @Nested
    @DisplayName("Expression Pattern Tests / 表达式模式测试")
    inner class ExpressionPatternTests {

        @Test
        @DisplayName("BooleanConstant pattern / 布尔常量模式")
        fun testConstantPattern() {
            val trueExpr = BooleanConstant(Trivalent.True)
            val falseExpr = BooleanConstant(Trivalent.False)
            val unknownExpr = BooleanConstant(Trivalent.Unknown)

            assertEquals(Trivalent.True, trueExpr.value)
            assertEquals(Trivalent.False, falseExpr.value)
            assertEquals(Trivalent.Unknown, unknownExpr.value)
        }

        @Test
        @DisplayName("Comparison pattern / 比较模式")
        fun testComparisonPattern() {
            val expr = Comparison(
                ComparisonOperator.Gt,
                ScalarReference(PropertyPath.parse("age")),
                ScalarConstant(18)
            )

            assertEquals(ComparisonOperator.Gt, expr.operator)
            assertTrue(expr.left is ScalarReference<*>)
            assertTrue(expr.right is ScalarConstant<*>)
        }

        @Test
        @DisplayName("And pattern / And 模式")
        fun testAndPattern() {
            val expr = AndExpression(listOf(
                Comparison(ComparisonOperator.Gt, ScalarReference(PropertyPath.parse("a")), ScalarConstant(1)),
                Comparison(ComparisonOperator.Gt, ScalarReference(PropertyPath.parse("b")), ScalarConstant(2))
            ))

            assertEquals(2, expr.operands.size)
        }

        @Test
        @DisplayName("Or pattern / Or 模式")
        fun testOrPattern() {
            val expr = OrExpression(listOf(
                Comparison(ComparisonOperator.Lt, ScalarReference(PropertyPath.parse("a")), ScalarConstant(1)),
                Comparison(ComparisonOperator.Gt, ScalarReference(PropertyPath.parse("a")), ScalarConstant(10))
            ))

            assertEquals(2, expr.operands.size)
        }

        @Test
        @DisplayName("Not pattern / Not 模式")
        fun testNotPattern() {
            val inner = Comparison(ComparisonOperator.Eq, ScalarReference(PropertyPath.parse("status")), ScalarConstant("deleted"))
            val expr = NotExpression(inner)

            assertTrue(expr.operand is Comparison<*>)
        }

        @Test
        @DisplayName("NullCheck pattern / 空值检查模式")
        fun testNullCheckPattern() {
            val isNull = NullCheck(PropertyPath.parse("email"), NullCheckType.IsNull)
            val isNotNull = NullCheck(PropertyPath.parse("phone"), NullCheckType.IsNotNull)

            assertTrue(isNull.isNull)
            assertFalse(isNotNull.isNull)
        }

        @Test
        @DisplayName("InExpression pattern / In 表达式模式")
        fun testInPattern() {
            val expr = InExpression(
                ScalarReference(PropertyPath.parse("status")),
                listOf(ScalarConstant("active"), ScalarConstant("pending")),
                negated = false
            )

            assertFalse(expr.negated)
            assertEquals(2, expr.candidates.size)
        }

        @Test
        @DisplayName("PatternMatch pattern / 模式匹配模式")
        fun testPatternMatchPattern() {
            val expr = PatternMatch(
                ScalarReference(PropertyPath.parse("name")),
                ScalarConstant("A%"),
                PatternMatchMode.Like,
                negated = false
            )

            assertEquals(PatternMatchMode.Like, expr.mode)
            assertFalse(expr.negated)
        }
    }

    @Nested
    @DisplayName("Complex Expression Tests / 复杂表达式测试")
    inner class ComplexExpressionTests {

        @Test
        @DisplayName("Nested and/or pattern / 嵌套 and/or 模式")
        fun testNestedAndOr() {
            // (A and B) or (C and D)
            val expr = OrExpression(listOf(
                AndExpression(listOf(
                    Comparison(ComparisonOperator.Gt, ScalarReference(PropertyPath.parse("a")), ScalarConstant(1)),
                    Comparison(ComparisonOperator.Gt, ScalarReference(PropertyPath.parse("b")), ScalarConstant(2))
                )),
                AndExpression(listOf(
                    Comparison(ComparisonOperator.Lt, ScalarReference(PropertyPath.parse("c")), ScalarConstant(3)),
                    Comparison(ComparisonOperator.Eq, ScalarReference(PropertyPath.parse("d")), ScalarConstant(4))
                ))
            ))

            assertTrue(expr is OrExpression)
            assertEquals(2, (expr as OrExpression).operands.size)
        }

        @Test
        @DisplayName("Not with and pattern / Not 与 and 组合模式")
        fun testNotWithAnd() {
            // not (A and B)
            val expr = NotExpression(
                AndExpression(listOf(
                    Comparison(ComparisonOperator.Eq, ScalarReference(PropertyPath.parse("a")), ScalarConstant(1)),
                    Comparison(ComparisonOperator.Eq, ScalarReference(PropertyPath.parse("b")), ScalarConstant(2))
                ))
            )

            assertTrue(expr.operand is AndExpression)
        }
    }

    @Nested
    @DisplayName("PatternMatch Mode Tests / 模式匹配模式测试")
    inner class PatternMatchModeTests {

        @Test
        @DisplayName("All pattern match modes / 所有模式匹配模式")
        fun testAllPatternMatchModes() {
            val modes = listOf(
                PatternMatchMode.Exact,
                PatternMatchMode.Prefix,
                PatternMatchMode.Suffix,
                PatternMatchMode.Contains,
                PatternMatchMode.Like,
                PatternMatchMode.Regex
            )

            assertEquals(6, modes.size)
            modes.forEach { mode ->
                val expr = PatternMatch(
                    ScalarReference(PropertyPath.parse("field")),
                    ScalarConstant("test"),
                    mode,
                    negated = false
                )
                assertEquals(mode, expr.mode)
            }
        }
    }
}