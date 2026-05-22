/**
 * MyBatis 布尔表达式翻译器测试
 * MyBatis Boolean Expression Translator Tests
 *
 * 验收标准：
 * 1. BooleanExpression 可正确翻译为 MyBatis-Plus 条件
 */
package fuookami.ospf.kotlin.framework.persistence.expression.translator

import fuookami.ospf.kotlin.framework.persistence.expression.*
import fuookami.ospf.kotlin.math.symbol.expression.*
import fuookami.ospf.kotlin.math.Trivalent
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*

@DisplayName("MybatisBooleanTranslator Tests / MyBatis 布尔翻译器测试")
class MybatisBooleanTranslatorTest {
    data class TestEntity(val id: Long, val name: String?)

    private val resolver: MybatisColumnNameResolver = { path ->
        when (path.substringAfterLast(".")) {
            "id", "age", "status", "name", "a", "b", "c", "d", "x", "price", "quantity" -> path.substringAfterLast(".")
            else -> null
        }
    }

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

            assertEquals(2, expr.operands.size)
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
    @DisplayName("Comparison Operator Tests / 比较操作符测试")
    inner class ComparisonOperatorTests {

        @Test
        @DisplayName("All comparison operators / 所有比较操作符")
        fun testAllComparisonOperators() {
            val operators = listOf(
                ComparisonOperator.Eq,
                ComparisonOperator.Ne,
                ComparisonOperator.Lt,
                ComparisonOperator.Le,
                ComparisonOperator.Gt,
                ComparisonOperator.Ge
            )

            assertEquals(6, operators.size)
            operators.forEach { op ->
                val expr = Comparison(op, ScalarReference(PropertyPath.parse("x")), ScalarConstant(1))
                assertEquals(op, expr.operator)
            }
        }
    }

    @Nested
    @DisplayName("Translator Behavior Tests / 翻译行为测试")
    inner class TranslatorBehaviorTests {
        private val translator = MybatisBooleanTranslator<TestEntity>(resolver)

        @Test
        @DisplayName("false and unknown should become impossible condition / false 与 unknown 应转不可能条件")
        fun falseAndUnknownShouldBecomeImpossibleCondition() {
            val falseWrapper = translator.translate(QueryWrapper(), BooleanConstant(Trivalent.False))
            val unknownWrapper = translator.translate(QueryWrapper(), BooleanConstant(Trivalent.Unknown))

            assertTrue(falseWrapper.customSqlSegment.contains("1 = 0"))
            assertTrue(unknownWrapper.customSqlSegment.contains("1 = 0"))
        }

        @Test
        @DisplayName("unresolved path should become impossible condition / 未解析路径应转不可能条件")
        fun unresolvedPathShouldBecomeImpossibleCondition() {
            val expr = Comparison(
                ComparisonOperator.Eq,
                ScalarReference(PropertyPath.parse("unknown")),
                ScalarConstant(1)
            )

            val wrapper = translator.translate(QueryWrapper(), expr)
            assertTrue(wrapper.customSqlSegment.contains("1 = 0"))
        }

        @Test
        @DisplayName("regex should become impossible condition / regex 应转不可能条件")
        fun regexShouldBecomeImpossibleCondition() {
            val expr = PatternMatch(
                ScalarReference(PropertyPath.parse("name")),
                ScalarConstant("a.*"),
                PatternMatchMode.Regex
            )

            val wrapper = translator.translate(QueryWrapper(), expr)
            assertTrue(wrapper.customSqlSegment.contains("1 = 0"))
        }

        @Test
        @DisplayName("should translate column-column comparison / 应翻译列-列比较")
        fun shouldTranslateColumnColumnComparison() {
            val expr = Comparison<Int>(
                ComparisonOperator.Gt,
                ScalarReference<Int>(PropertyPath.parse("age")),
                ScalarReference<Int>(PropertyPath.parse("id"))
            )

            val wrapper = translator.translate(QueryWrapper(), expr)
            assertTrue(wrapper.customSqlSegment.contains("age > id"))
        }

        @Test
        @DisplayName("should parameterize arithmetic scalar comparison / 算术标量比较应参数化常量")
        fun shouldParameterizeArithmeticScalarComparison() {
            val expr = Comparison<Int>(
                ComparisonOperator.Gt,
                ScalarBinary<Int>(
                    BinaryOperator.Multiply,
                    ScalarReference<Int>(PropertyPath.parse("price")),
                    ScalarReference<Int>(PropertyPath.parse("quantity"))
                ),
                ScalarConstant<Int>(100)
            )

            val wrapper = translator.translate(QueryWrapper(), expr)
            assertTrue(wrapper.customSqlSegment.contains("(price * quantity) >"))
            assertFalse(wrapper.customSqlSegment.contains("100"))
        }

        @Test
        @DisplayName("should translate function comparison / 应翻译函数比较")
        fun shouldTranslateFunctionComparison() {
            val expr = Comparison(
                ComparisonOperator.Gt,
                ScalarFunction(
                    ScalarFunctionNames.Abs,
                    listOf(ScalarReference<Int>(PropertyPath.parse("age")))
                ),
                ScalarConstant(10)
            )

            val wrapper = translator.translate(QueryWrapper(), expr)

            assertTrue(wrapper.customSqlSegment.contains("ABS(age) >"))
            assertFalse(wrapper.customSqlSegment.contains("10"))
        }

        @Test
        @DisplayName("fail fast should throw for unsupported predicate / FailFast 应对不支持谓词抛异常")
        fun failFastShouldThrowForUnsupportedPredicate() {
            val failFastTranslator = MybatisBooleanTranslator<TestEntity>(
                resolver,
                UnsupportedPredicatePolicy.FailFast
            )

            assertThrows(IllegalArgumentException::class.java) {
                failFastTranslator.translate(QueryWrapper(), BooleanCustom("x"))
            }
        }

        @Test
        @DisplayName("unsupported policy should apply to unresolved comparison path / 不支持策略应作用于未解析比较路径")
        fun unsupportedPolicyShouldApplyToUnresolvedComparisonPath() {
            val failFastTranslator = MybatisBooleanTranslator<TestEntity>(
                resolver,
                UnsupportedPredicatePolicy.FailFast
            )
            val clientFilterTranslator = MybatisBooleanTranslator<TestEntity>(
                resolver,
                UnsupportedPredicatePolicy.ClientFilter
            )
            val expr = Comparison(
                ComparisonOperator.Eq,
                ScalarReference(PropertyPath.parse("unknown")),
                ScalarConstant(1)
            )

            assertThrows(IllegalArgumentException::class.java) {
                failFastTranslator.translate(QueryWrapper(), expr)
            }
            assertThrows(IllegalArgumentException::class.java) {
                clientFilterTranslator.translate(QueryWrapper(), expr)
            }
        }
    }
}
