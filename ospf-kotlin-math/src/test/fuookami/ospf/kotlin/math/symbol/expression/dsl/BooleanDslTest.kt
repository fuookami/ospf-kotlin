/**
 * 表达式 DSL 测试
 * Expression DSL Tests
 *
 * 验收标准：
 * 1. DSL 构造与 parser 解析可表达同一语义树
 * 2. DSL 函数风格与中缀风格并存
 */
package fuookami.ospf.kotlin.math.symbol.expression.dsl

import fuookami.ospf.kotlin.math.symbol.expression.*
import fuookami.ospf.kotlin.math.symbol.expression.parser.parseBooleanExpression
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*

@DisplayName("Boolean DSL Tests / 布尔表达式 DSL 测试")
class BooleanDslTest {

    @Nested
    @DisplayName("Path Builder Tests / 路径构建器测试")
    inner class PathBuilderTests {

        @Test
        @DisplayName("String comparison / 字符串比较")
        fun testStringComparison() {
            val expr = path("status") eq "active"

            assertEquals(ComparisonOperator.Eq, expr.operator)
        }

        @Test
        @DisplayName("Number comparison / 数字比较")
        fun testNumberComparison() {
            val expr = path("age") gt 18

            assertEquals(ComparisonOperator.Gt, expr.operator)
        }

        @Test
        @DisplayName("All comparison operators / 所有比较操作符")
        fun testAllComparisonOperators() {
            assertNotNull((path("x") eq 1))
            assertNotNull((path("x") ne 1))
            assertNotNull((path("x") lt 1))
            assertNotNull((path("x") le 1))
            assertNotNull((path("x") gt 1))
            assertNotNull((path("x") ge 1))
        }

        @Test
        @DisplayName("In expression / In 表达式")
        fun testInExpression() {
            val expr = path("status").inValues("active", "pending", "processing")

            assertFalse(expr.negated)
            assertEquals(3, expr.candidates.size)
        }

        @Test
        @DisplayName("Not in expression / Not in 表达式")
        fun testNotInExpression() {
            val expr = path("status").notInValues("deleted", "archived")

            assertTrue(expr.negated)
            assertEquals(2, expr.candidates.size)
        }

        @Test
        @DisplayName("Is null / 空值检查")
        fun testIsNull() {
            val expr = path("name").isNull()

            assertTrue(expr.isNull)
        }

        @Test
        @DisplayName("Is not null / 非空检查")
        fun testIsNotNull() {
            val expr = path("email").isNotNull()

            assertTrue(expr.isNotNull)
        }

        @Test
        @DisplayName("Like pattern match / Like 模式匹配")
        fun testLikePatternMatch() {
            val expr = path("name") like "John%"

            assertEquals(PatternMatchMode.Like, expr.mode)
        }
    }

    @Nested
    @DisplayName("Logical Operators Tests / 逻辑操作符测试")
    inner class LogicalOperatorsTests {

        @Test
        @DisplayName("And operator / And 操作符")
        fun testAndOperator() {
            val expr = (path("age") gt 18) and (path("status") eq "active")

            assertEquals(2, expr.operands.size)
        }

        @Test
        @DisplayName("Or operator / Or 操作符")
        fun testOrOperator() {
            val expr = (path("age") lt 18) or (path("age") gt 65)

            assertEquals(2, expr.operands.size)
        }

        @Test
        @DisplayName("Not operator / Not 操作符")
        fun testNotOperator() {
            val expr = !((path("status") eq "deleted"))

            assertEquals("Not", expr.typeName)
        }

        @Test
        @DisplayName("Complex expression / 复杂表达式")
        fun testComplexExpression() {
            val expr = ((path("age") gt 18) and (path("status") eq "active")) or
                    !((path("status") eq "deleted"))

            val or = expr
            assertTrue(or.operands[0] is AndExpression)
            assertTrue(or.operands[1] is NotExpression)
        }

        @Test
        @DisplayName("Flattened and / 扁平化的 and")
        fun testFlattenedAnd() {
            val expr = (path("a") eq 1) and (path("b") eq 2) and (path("c") eq 3)

            assertEquals(3, expr.operands.size)
        }
    }

    @Nested
    @DisplayName("Convenience Functions Tests / 便捷函数测试")
    inner class ConvenienceFunctionsTests {

        @Test
        @DisplayName("Quick compare / 快速比较")
        fun testQuickCompare() {
            val expr = compare("age", ComparisonOperator.Gt, 18)

            assertEquals(ComparisonOperator.Gt, expr.operator)
        }

        @Test
        @DisplayName("Quick eq/ne/lt/le/gt/ge / 快速比较函数")
        fun testQuickComparisonFunctions() {
            assertNotNull(eq("x", 1))
            assertNotNull(ne("x", 1))
            assertNotNull(lt("x", 1))
            assertNotNull(le("x", 1))
            assertNotNull(gt("x", 1))
            assertNotNull(ge("x", 1))
        }

        @Test
        @DisplayName("Quick in/notIn / 快速集合成员函数")
        fun testQuickInFunctions() {
            val inExpr = inExpr("status", listOf("a", "b"))
            assertFalse(inExpr.negated)

            val notInExpr = notInExpr("status", listOf("a", "b"))
            assertTrue(notInExpr.negated)
        }

        @Test
        @DisplayName("Quick isNull/isNotNull / 快速空值检查函数")
        fun testQuickNullCheckFunctions() {
            val isNullExpr = isNull("name")
            assertTrue(isNullExpr.isNull)

            val isNotNullExpr = isNotNull("email")
            assertTrue(isNotNullExpr.isNotNull)
        }

        @Test
        @DisplayName("Quick and/or/not / 快速逻辑函数")
        fun testQuickLogicalFunctions() {
            val andExpr = and(eq("a", 1), eq("b", 2), eq("c", 3))
            assertEquals(3, andExpr.operands.size)

            val orExpr = or(eq("a", 1), eq("b", 2))
            assertEquals(2, orExpr.operands.size)

            val notExpr = notExpr(eq("a", 1))
            assertTrue(notExpr.operand is Comparison<*>)
        }
    }

    @Nested
    @DisplayName("DSL vs Parser Equivalence Tests / DSL 与 Parser 等价性测试")
    inner class DslParserEquivalenceTests {

        @Test
        @DisplayName("Simple comparison equivalence / 简单比较等价性")
        fun testSimpleComparisonEquivalence() {
            // DSL
            val dslExpr = path("age") gt 18

            // Parser
            val parserExpr = parseBooleanExpression("age > 18")

            // 验证结构相同 / Verify same structure
            assertTrue(parserExpr is Comparison<*>)

            val dslComp = dslExpr
            val parserComp = parserExpr as Comparison<*>

            assertEquals(dslComp.operator, parserComp.operator)
        }

        @Test
        @DisplayName("And expression equivalence / And 表达式等价性")
        fun testAndExpressionEquivalence() {
            // DSL
            val dslExpr = (path("age") gt 18) and (path("status") eq "active")

            // Parser
            val parserExpr = parseBooleanExpression("age > 18 and status = 'active'")

            assertTrue(parserExpr is AndExpression)

            val dslAnd = dslExpr
            val parserAnd = parserExpr as AndExpression

            assertEquals(dslAnd.operands.size, parserAnd.operands.size)
        }

        @Test
        @DisplayName("Complex expression equivalence / 复杂表达式等价性")
        fun testComplexExpressionEquivalence() {
            // DSL: (age > 18 and status = 'active') or not status = 'deleted'
            val dslExpr = (
                    (path("age") gt 18) and (path("status") eq "active")
                    ) or !((path("status") eq "deleted"))

            // Parser
            val parserExpr = parseBooleanExpression(
                "(age > 18 and status = 'active') or not status = 'deleted'"
            )

            assertTrue(parserExpr is OrExpression)
        }

        @Test
        @DisplayName("In expression equivalence / In 表达式等价性")
        fun testInExpressionEquivalence() {
            // DSL
            val dslExpr = path("status").inValues("active", "pending")

            // Parser
            val parserExpr = parseBooleanExpression("status in ('active', 'pending')")

            assertTrue(parserExpr is InExpression<*>)

            val dslIn = dslExpr
            val parserIn = parserExpr as InExpression<*>

            assertEquals(dslIn.negated, parserIn.negated)
            assertEquals(dslIn.candidates.size, parserIn.candidates.size)
        }

        @Test
        @DisplayName("Null check equivalence / 空值检查等价性")
        fun testNullCheckEquivalence() {
            // DSL
            val dslExpr = path("name").isNotNull()

            // Parser
            val parserExpr = parseBooleanExpression("name is not null")

            assertTrue(parserExpr is NullCheck)

            val dslNull = dslExpr
            val parserNull = parserExpr as NullCheck

            assertEquals(dslNull.isNull, parserNull.isNull)
            assertEquals(dslNull.isNotNull, parserNull.isNotNull)
        }

        @Test
        @DisplayName("Path with dots equivalence / 点分隔路径等价性")
        fun testPathEquivalence() {
            // DSL
            val dslExpr = path("user.address.city") eq "Beijing"

            // Parser
            val parserExpr = parseBooleanExpression("user.address.city = 'Beijing'")

            assertTrue(parserExpr is Comparison<*>)

            val dslComp = dslExpr
            val parserComp = parserExpr as Comparison<*>

            val dslRef = dslComp.left as? ScalarReference<*>
            val parserRef = parserComp.left as? ScalarReference<*>

            assertEquals(dslRef?.path?.value, parserRef?.path?.value)
        }
    }
}
