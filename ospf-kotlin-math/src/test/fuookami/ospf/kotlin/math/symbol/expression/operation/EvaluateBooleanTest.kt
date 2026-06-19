/**
 * 布尔表达式求值测试
 * Boolean Expression Evaluation Tests
 *
 * 验收标准：
 * 1. evaluate 对比较、逻辑、空值检查、in、patternMatch 均可本地求值
 */
package fuookami.ospf.kotlin.math.symbol.expression.operation

import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import fuookami.ospf.kotlin.math.symbol.expression.*
import fuookami.ospf.kotlin.math.Trivalent

@DisplayName("EvaluateBoolean Tests / 布尔表达式求值测试")
class EvaluateBooleanTest {

    @Nested
    @DisplayName("Comparison Evaluation Tests / 比较求值测试")
    inner class ComparisonEvaluationTests {

        @Test
        @DisplayName("Evaluate equality / 相等比较求值")
        fun testEvaluateEquality() {
            val expr = Comparison(ComparisonOperator.Eq, ScalarReference(PropertyPath.parse("name")), ScalarConstant("Alice"))
            assertEquals(Trivalent.True, expr.evaluateWith(mapOf("name" to "Alice")))
        }

        @Test
        @DisplayName("Evaluate inequality / 不等比较求值")
        fun testEvaluateInequality() {
            val expr = Comparison(ComparisonOperator.Ne, ScalarReference(PropertyPath.parse("status")), ScalarConstant("active"))
            assertEquals(Trivalent.True, expr.evaluateWith(mapOf("status" to "inactive")))
        }

        @Test
        @DisplayName("Evaluate less than / 小于比较求值")
        fun testEvaluateLessThan() {
            val expr = Comparison(ComparisonOperator.Lt, ScalarReference(PropertyPath.parse("age")), ScalarConstant(18))
            assertEquals(Trivalent.True, expr.evaluateWith(mapOf("age" to 15)))
        }

        @Test
        @DisplayName("Evaluate greater than / 大于比较求值")
        fun testEvaluateGreaterThan() {
            val expr = Comparison(ComparisonOperator.Gt, ScalarReference(PropertyPath.parse("age")), ScalarConstant(18))
            assertEquals(Trivalent.True, expr.evaluateWith(mapOf("age" to 25)))
        }

        @Test
        @DisplayName("Large integer comparison keeps precision / 大整数比较保持精度")
        fun testEvaluateLargeIntegerPrecision() {
            val larger = 9007199254740993L
            val smaller = 9007199254740992L

            val eqExpr = Comparison(
                ComparisonOperator.Eq,
                ScalarReference<Long>(PropertyPath.parse("value")),
                ScalarConstant(smaller)
            )
            val gtExpr = Comparison(
                ComparisonOperator.Gt,
                ScalarReference<Long>(PropertyPath.parse("value")),
                ScalarConstant(smaller)
            )

            assertEquals(Trivalent.False, eqExpr.evaluateWith(mapOf("value" to larger)))
            assertEquals(Trivalent.True, gtExpr.evaluateWith(mapOf("value" to larger)))
        }

        @Test
        @DisplayName("Evaluate with path / 路径求值")
        fun testEvaluateWithPath() {
            val expr = Comparison(ComparisonOperator.Eq, ScalarReference(PropertyPath.parse("user.address.city")), ScalarConstant("Beijing"))
            assertEquals(Trivalent.True, expr.evaluateWith(mapOf("user.address.city" to "Beijing")))
        }

        @Test
        @DisplayName("Evaluate with missing value returns Unknown / 缺失值返回 Unknown")
        fun testEvaluateMissingValue() {
            val expr = Comparison(ComparisonOperator.Eq, ScalarReference(PropertyPath.parse("name")), ScalarConstant("Alice"))
            assertEquals(Trivalent.Unknown, evaluateBoolean(expr, EmptyEvaluationContext))
        }

        @Test
        @DisplayName("Incomparable values return Unknown / 不可比较值返回 Unknown")
        fun testEvaluateIncomparableValues() {
            val expr = Comparison(
                ComparisonOperator.Le,
                ScalarReference<Any>(PropertyPath.parse("payload")),
                ScalarConstant(mapOf("k" to "v"))
            )
            assertEquals(
                Trivalent.Unknown,
                expr.evaluateWith(mapOf("payload" to listOf(1, 2, 3)))
            )
        }
    }

    @Nested
    @DisplayName("Logical Evaluation Tests / 逻辑求值测试")
    inner class LogicalEvaluationTests {

        @Test
        @DisplayName("Evaluate And / And 求值")
        fun testEvaluateAnd() {
            val expr = AndExpression(listOf(
                Comparison(ComparisonOperator.Gt, ScalarReference(PropertyPath.parse("age")), ScalarConstant(18)),
                Comparison(ComparisonOperator.Eq, ScalarReference(PropertyPath.parse("status")), ScalarConstant("active"))
            ))
            assertEquals(Trivalent.True, expr.evaluateWith(mapOf("age" to 25, "status" to "active")))
        }

        @Test
        @DisplayName("Evaluate And with false operand / 含 false 操作数的 And 求值")
        fun testEvaluateAndWithFalse() {
            val expr = AndExpression(listOf(
                Comparison(ComparisonOperator.Gt, ScalarReference(PropertyPath.parse("age")), ScalarConstant(18)),
                Comparison(ComparisonOperator.Eq, ScalarReference(PropertyPath.parse("status")), ScalarConstant("inactive"))
            ))
            assertEquals(Trivalent.False, expr.evaluateWith(mapOf("age" to 25, "status" to "active")))
        }

        @Test
        @DisplayName("Evaluate Or / Or 求值")
        fun testEvaluateOr() {
            val expr = OrExpression(listOf(
                Comparison(ComparisonOperator.Lt, ScalarReference(PropertyPath.parse("age")), ScalarConstant(18)),
                Comparison(ComparisonOperator.Gt, ScalarReference(PropertyPath.parse("age")), ScalarConstant(65))
            ))
            assertEquals(Trivalent.True, expr.evaluateWith(mapOf("age" to 15)))
        }

        @Test
        @DisplayName("Evaluate Or with all false / 全 false 操作数的 Or 求值")
        fun testEvaluateOrAllFalse() {
            val expr = OrExpression(listOf(
                Comparison(ComparisonOperator.Lt, ScalarReference(PropertyPath.parse("age")), ScalarConstant(18)),
                Comparison(ComparisonOperator.Gt, ScalarReference(PropertyPath.parse("age")), ScalarConstant(65))
            ))
            assertEquals(Trivalent.False, expr.evaluateWith(mapOf("age" to 30)))
        }

        @Test
        @DisplayName("Evaluate Not / Not 求值")
        fun testEvaluateNot() {
            val expr = NotExpression(
                Comparison(ComparisonOperator.Eq, ScalarReference(PropertyPath.parse("status")), ScalarConstant("deleted"))
            )
            assertEquals(Trivalent.True, expr.evaluateWith(mapOf("status" to "active")))
        }
    }

    @Nested
    @DisplayName("Null Check Evaluation Tests / 空值检查求值测试")
    inner class NullCheckEvaluationTests {

        @Test
        @DisplayName("Evaluate is null / is null 求值")
        fun testEvaluateIsNull() {
            val expr = NullCheck(PropertyPath.parse("email"), NullCheckType.IsNull)
            assertEquals(Trivalent.True, expr.evaluateWith(mapOf("email" to null)))
        }

        @Test
        @DisplayName("Evaluate is not null / is not null 求值")
        fun testEvaluateIsNotNull() {
            val expr = NullCheck(PropertyPath.parse("email"), NullCheckType.IsNotNull)
            assertEquals(Trivalent.True, expr.evaluateWith(mapOf("email" to "test@example.com")))
        }

        @Test
        @DisplayName("Evaluate is null with non-null value / 非空值的 is null 求值")
        fun testEvaluateIsNullWithNonNull() {
            val expr = NullCheck(PropertyPath.parse("name"), NullCheckType.IsNull)
            assertEquals(Trivalent.False, expr.evaluateWith(mapOf("name" to "Alice")))
        }

        @Test
        @DisplayName("Evaluate is null with missing path returns Unknown / 缺失路径的 is null 返回 Unknown")
        fun testEvaluateIsNullWithMissingPath() {
            val expr = NullCheck(PropertyPath.parse("email"), NullCheckType.IsNull)
            assertEquals(Trivalent.Unknown, expr.evaluateWith(mapOf("name" to "Alice")))
        }
    }

    @Nested
    @DisplayName("In Expression Evaluation Tests / In 表达式求值测试")
    inner class InEvaluationTests {

        @Test
        @DisplayName("Evaluate in / in 求值")
        fun testEvaluateIn() {
            val expr = InExpression(
                ScalarReference(PropertyPath.parse("status")),
                listOf(ScalarConstant("active"), ScalarConstant("pending"), ScalarConstant("processing"))
            )
            assertEquals(Trivalent.True, expr.evaluateWith(mapOf("status" to "pending")))
        }

        @Test
        @DisplayName("Evaluate not in / not in 求值")
        fun testEvaluateNotIn() {
            val expr = InExpression(
                ScalarReference(PropertyPath.parse("status")),
                listOf(ScalarConstant("deleted"), ScalarConstant("archived")),
                negated = true
            )
            assertEquals(Trivalent.True, expr.evaluateWith(mapOf("status" to "active")))
        }

        @Test
        @DisplayName("Evaluate in with no match / 无匹配的 in 求值")
        fun testEvaluateInNoMatch() {
            val expr = InExpression(
                ScalarReference(PropertyPath.parse("status")),
                listOf(ScalarConstant("active"), ScalarConstant("pending"))
            )
            assertEquals(Trivalent.False, expr.evaluateWith(mapOf("status" to "deleted")))
        }
    }

    @Nested
    @DisplayName("Pattern Match Evaluation Tests / 模式匹配求值测试")
    inner class PatternMatchEvaluationTests {

        @Test
        @DisplayName("Evaluate exact match / 精确匹配求值")
        fun testEvaluateExactMatch() {
            val expr = PatternMatch(
                ScalarReference(PropertyPath.parse("name")),
                ScalarConstant("Alice"),
                PatternMatchMode.Exact
            )
            assertEquals(Trivalent.True, expr.evaluateWith(mapOf("name" to "Alice")))
        }

        @Test
        @DisplayName("Evaluate prefix match / 前缀匹配求值")
        fun testEvaluatePrefixMatch() {
            val expr = PatternMatch(
                ScalarReference(PropertyPath.parse("name")),
                ScalarConstant("Al"),
                PatternMatchMode.Prefix
            )
            assertEquals(Trivalent.True, expr.evaluateWith(mapOf("name" to "Alice")))
        }

        @Test
        @DisplayName("Evaluate suffix match / 后缀匹配求值")
        fun testEvaluateSuffixMatch() {
            val expr = PatternMatch(
                ScalarReference(PropertyPath.parse("name")),
                ScalarConstant("ce"),
                PatternMatchMode.Suffix
            )
            assertEquals(Trivalent.True, expr.evaluateWith(mapOf("name" to "Alice")))
        }

        @Test
        @DisplayName("Evaluate contains match / 包含匹配求值")
        fun testEvaluateContainsMatch() {
            val expr = PatternMatch(
                ScalarReference(PropertyPath.parse("name")),
                ScalarConstant("lic"),
                PatternMatchMode.Contains
            )
            assertEquals(Trivalent.True, expr.evaluateWith(mapOf("name" to "Alice")))
        }

        @Test
        @DisplayName("Evaluate like match / Like 匹配求值")
        fun testEvaluateLikeMatch() {
            val expr = PatternMatch(
                ScalarReference(PropertyPath.parse("name")),
                ScalarConstant("A%e"),
                PatternMatchMode.Like
            )
            assertEquals(Trivalent.True, expr.evaluateWith(mapOf("name" to "Alice")))
        }
    }

    @Nested
    @DisplayName("Convenience Method Tests / 便捷方法测试")
    inner class ConvenienceMethodTests {

        @Test
        @DisplayName("Evaluate with Map extension / 使用 Map 扩展求值")
        fun testEvaluateWithMap() {
            val expr = Comparison(ComparisonOperator.Gt, ScalarReference(PropertyPath.parse("age")), ScalarConstant(18))
            assertEquals(Trivalent.True, expr.evaluateWith(mapOf("age" to 25)))
        }

        @Test
        @DisplayName("Evaluate with orNull extension / 使用 orNull 扩展求值")
        fun testEvaluateWithOrNull() {
            val expr = Comparison(ComparisonOperator.Gt, ScalarReference(PropertyPath.parse("age")), ScalarConstant(18))
            assertEquals(true, expr.evaluateWithOrNull(mapOf("age" to 25)))
        }
    }

    @Nested
    @DisplayName("Scalar Function Evaluation Tests / 标量函数求值测试")
    inner class ScalarFunctionEvaluationTests {

        @Test
        @DisplayName("Evaluate numeric abs / 数值 abs 求值")
        fun testEvaluateAbs() {
            val constExpr = Comparison(
                ComparisonOperator.Eq,
                ScalarFunction(ScalarFunctionNames.Abs, listOf(ScalarConstant(-3))),
                ScalarConstant(3)
            )
            val refExpr = Comparison(
                ComparisonOperator.Eq,
                ScalarFunction(ScalarFunctionNames.Abs, listOf(ScalarReference(PropertyPath.parse("x")))),
                ScalarConstant(5)
            )

            assertEquals(Trivalent.True, constExpr.evaluateWith(emptyMap()))
            assertEquals(Trivalent.True, refExpr.evaluateWith(mapOf("x" to -5)))
        }

        @Test
        @DisplayName("Evaluate string functions / 字符串函数求值")
        fun testEvaluateStringFunctions() {
            val lowerExpr = Comparison(
                ComparisonOperator.Eq,
                ScalarFunction(ScalarFunctionNames.Lower, listOf(ScalarReference(PropertyPath.parse("name")))),
                ScalarConstant("alice")
            )
            val upperExpr = Comparison(
                ComparisonOperator.Eq,
                ScalarFunction(ScalarFunctionNames.Upper, listOf(ScalarReference(PropertyPath.parse("name")))),
                ScalarConstant("ALICE")
            )
            val lengthExpr = Comparison(
                ComparisonOperator.Eq,
                ScalarFunction(ScalarFunctionNames.Length, listOf(ScalarReference(PropertyPath.parse("name")))),
                ScalarConstant(5)
            )

            assertEquals(Trivalent.True, lowerExpr.evaluateWith(mapOf("name" to "Alice")))
            assertEquals(Trivalent.True, upperExpr.evaluateWith(mapOf("name" to "Alice")))
            assertEquals(Trivalent.True, lengthExpr.evaluateWith(mapOf("name" to "Alice")))
        }

        @Test
        @DisplayName("Evaluate coalesce / coalesce 求值")
        fun testEvaluateCoalesce() {
            val expr = Comparison(
                ComparisonOperator.Eq,
                ScalarFunction(
                    ScalarFunctionNames.Coalesce,
                    listOf(
                        ScalarReference(PropertyPath.parse("nickname")),
                        ScalarReference(PropertyPath.parse("name")),
                        ScalarConstant("fallback")
                    )
                ),
                ScalarConstant("Alice")
            )

            assertEquals(Trivalent.True, expr.evaluateWith(mapOf("nickname" to null, "name" to "Alice")))
        }

        @Test
        @DisplayName("Function with unknown argument returns unknown / 函数未知参数返回 Unknown")
        fun testFunctionWithUnknownArgumentReturnsUnknown() {
            val expr = Comparison(
                ComparisonOperator.Eq,
                ScalarFunction(ScalarFunctionNames.Abs, listOf(ScalarReference(PropertyPath.parse("x")))),
                ScalarConstant(1)
            )

            assertEquals(Trivalent.Unknown, expr.evaluateWith(emptyMap()))
        }

        @Test
        @DisplayName("Invalid function arguments return Unknown / 非法函数参数返回 Unknown")
        fun testInvalidFunctionArgumentsReturnUnknown() {
            val invalidAbs = Comparison(
                ComparisonOperator.Eq,
                ScalarFunction(ScalarFunctionNames.Abs, listOf(ScalarConstant("x"))),
                ScalarConstant(1)
            )
            val emptyCoalesce = Comparison(
                ComparisonOperator.Eq,
                ScalarFunction(ScalarFunctionNames.Coalesce, emptyList()),
                ScalarConstant(1)
            )
            val unknown = Comparison(
                ComparisonOperator.Eq,
                ScalarFunction("unknown", listOf(ScalarConstant(1))),
                ScalarConstant(1)
            )

            assertEquals(Trivalent.Unknown, invalidAbs.evaluateWith(emptyMap()))
            assertEquals(Trivalent.Unknown, emptyCoalesce.evaluateWith(emptyMap()))
            assertEquals(Trivalent.Unknown, unknown.evaluateWith(emptyMap()))
        }
    }
}
