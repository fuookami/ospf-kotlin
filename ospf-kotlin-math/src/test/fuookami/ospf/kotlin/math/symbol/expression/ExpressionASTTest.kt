/**
 * 表达式 AST 完整性测试
 * Expression AST Completeness Tests
 *
 * 验收标准：
 * 1. AST 能完整表达比较 + 逻辑组合
 * 2. 所有表达式类型可正确构造和遍历
 */
package fuookami.ospf.kotlin.math.symbol.expression

import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*

@DisplayName("Expression AST Completeness Tests / 表达式 AST 完整性测试")
class ExpressionASTTest {

    @Nested
    @DisplayName("ScalarExpression Tests / 标量表达式测试")
    inner class ScalarExpressionTests {

        @Test
        @DisplayName("Constant expression / 常量表达式")
        fun testConstant() {
            val constant: ScalarExpression<Int> = ScalarConstant(42)

            assertEquals("Constant", constant.typeName)
            assertTrue(constant.isConstant())
            assertFalse(constant.containsReference())
            assertEquals(0, constant.children.size)
        }

        @Test
        @DisplayName("Reference expression / 引用表达式")
        fun testReference() {
            val path = PropertyPath.parse("user.age")
            val ref: ScalarExpression<Int> = ScalarReference(path)

            assertEquals("Reference", ref.typeName)
            assertFalse(ref.isConstant())
            assertTrue(ref.containsReference())
            assertEquals(path, ref.collectReferences().first())
        }

        @Test
        @DisplayName("Unary expression / 一元操作表达式")
        fun testUnary() {
            val inner = ScalarConstant(5.0)
            val unary: ScalarExpression<Double> = ScalarUnary(UnaryOperator.Negate, inner)

            assertEquals("Unary", unary.typeName)
            assertTrue(unary.isConstant())
            assertEquals(1, unary.children.size)
            assertEquals(inner, unary.children.first())
        }

        @Test
        @DisplayName("Binary expression / 二元操作表达式")
        fun testBinary() {
            val left = ScalarConstant(10.0)
            val right = ScalarConstant(5.0)
            val binary: ScalarExpression<Double> = ScalarBinary(BinaryOperator.Add, left, right)

            assertEquals("Binary", binary.typeName)
            assertTrue(binary.isConstant())
            assertEquals(2, binary.children.size)
        }

        @Test
        @DisplayName("Function expression / 函数表达式")
        fun testFunction() {
            val arg1 = ScalarConstant(10.0)
            val arg2 = ScalarConstant(20.0)
            val func: ScalarExpression<Double> = ScalarFunction("max", listOf(arg1, arg2))

            assertEquals("Function", func.typeName)
            assertTrue(func.isConstant())
            assertEquals(2, func.children.size)
        }

        @Test
        @DisplayName("Standard abs function keeps symbolic boundary / 标准 abs 函数保持符号边界")
        fun testStandardAbsFunctionBoundary() {
            val x = ScalarReference<Int>(PropertyPath.parse("x"))
            val constantAbs = ScalarFunction(ScalarFunctionNames.Abs, listOf(ScalarConstant(-3)))
            val referenceAbs = ScalarFunction(ScalarFunctionNames.Abs, listOf(x))

            assertEquals("Function", referenceAbs.typeName)
            assertTrue(constantAbs.isConstant())
            assertFalse(referenceAbs.isConstant())
            assertTrue(referenceAbs.containsReference())
            assertEquals(setOf(PropertyPath.parse("x")), referenceAbs.collectReferences())
        }

        @Test
        @DisplayName("Mixed expression with references / 包含引用的混合表达式")
        fun testMixedExpression() {
            val ref1 = ScalarReference<Double>(PropertyPath.parse("x"))
            val ref2 = ScalarReference<Double>(PropertyPath.parse("y"))
            val constant = ScalarConstant(1.0)

            val add = ScalarBinary(BinaryOperator.Add, ref1, ref2)
            val expr = ScalarBinary(BinaryOperator.Subtract, add, constant)

            assertFalse(expr.isConstant())
            assertTrue(expr.containsReference())

            val refs = expr.collectReferences()
            assertEquals(2, refs.size)
            assertTrue(refs.contains(PropertyPath.parse("x")))
            assertTrue(refs.contains(PropertyPath.parse("y")))
        }

        @Test
        @DisplayName("Factory methods / 工厂方法测试")
        fun testFactoryMethods() {
            val expr1 = ScalarExpressionFactory.constant(10)
            val expr2 = ScalarExpressionFactory.reference<Int>("x")
            val expr3 = ScalarExpressionFactory.add(expr1, expr2)

            assertTrue(expr1.isConstant())
            assertFalse(expr2.isConstant())
            assertFalse(expr3.isConstant())
        }
    }

    @Nested
    @DisplayName("BooleanExpression Tests / 布尔表达式测试")
    inner class BooleanExpressionTests {

        @Test
        @DisplayName("Comparison expression / 比较表达式")
        fun testComparison() {
            val left = ScalarConstant(10)
            val right = ScalarConstant(5)
            val comparison = Comparison(ComparisonOperator.Gt, left, right)

            assertEquals("Comparison", comparison.typeName)
            assertTrue(comparison.isConstant())
            assertEquals(0, comparison.children.size)
        }

        @Test
        @DisplayName("Comparison with references / 包含引用的比较表达式")
        fun testComparisonWithReferences() {
            val left = ScalarReference<Int>(PropertyPath.parse("age"))
            val right = ScalarConstant(18)
            val comparison = Comparison(ComparisonOperator.Ge, left, right)

            assertFalse(comparison.isConstant())
            assertTrue(comparison.collectReferences().contains(PropertyPath.parse("age")))
        }

        @Test
        @DisplayName("In expression / In 表达式")
        fun testInExpression() {
            val value = ScalarReference<String>(PropertyPath.parse("status"))
            val candidates = listOf(
                ScalarConstant("active"),
                ScalarConstant("pending")
            )
            val inExpr = InExpression(value, candidates)

            assertEquals("In", inExpr.typeName)
            assertFalse(inExpr.isNegated)
            assertEquals(1, inExpr.collectReferences().size)
        }

        @Test
        @DisplayName("Not In expression / Not In 表达式")
        fun testNotInExpression() {
            val value = ScalarReference<String>(PropertyPath.parse("status"))
            val candidates = listOf(ScalarConstant("deleted"))
            val notIn = InExpression(value, candidates, negated = true)

            assertEquals("NotIn", notIn.typeName)
            assertTrue(notIn.isNegated)
        }

        @Test
        @DisplayName("Pattern match expression / 模式匹配表达式")
        fun testPatternMatch() {
            val value = ScalarReference<String>(PropertyPath.parse("name"))
            val pattern = ScalarConstant("John%")
            val match = PatternMatch(value, pattern, PatternMatchMode.Like)

            assertEquals("PatternMatch", match.typeName)
            assertFalse(match.negated)
        }

        @Test
        @DisplayName("Null check expression / 空值检查表达式")
        fun testNullCheck() {
            val path = PropertyPath.parse("user.email")
            val isNull = NullCheck(path, NullCheckType.IsNull)

            assertEquals("NullCheck", isNull.typeName)
            assertTrue(isNull.isNull)
            assertFalse(isNull.isNotNull)
            assertEquals(path, isNull.collectReferences().first())
        }

        @Test
        @DisplayName("And expression with comparisons / 包含比较的逻辑与表达式")
        fun testAndExpressionWithComparisons() {
            val a = Comparison(ComparisonOperator.Gt, ScalarReference<Int>(PropertyPath.parse("age")), ScalarConstant(18))
            val b = Comparison(ComparisonOperator.Eq, ScalarReference<String>(PropertyPath.parse("status")), ScalarConstant("active"))
            val and = AndExpression(listOf(a, b))

            assertEquals("And", and.typeName)
            assertEquals(2, and.operands.size)
            assertEquals(2, and.children.size)
            assertFalse(and.isConstant())

            val refs = and.collectReferences()
            assertEquals(2, refs.size)
        }

        @Test
        @DisplayName("Or expression with comparisons / 包含比较的逻辑或表达式")
        fun testOrExpressionWithComparisons() {
            val a = Comparison(ComparisonOperator.Lt, ScalarReference<Int>(PropertyPath.parse("age")), ScalarConstant(18))
            val b = Comparison(ComparisonOperator.Eq, ScalarReference<String>(PropertyPath.parse("status")), ScalarConstant("minor"))
            val or = OrExpression(listOf(a, b))

            assertEquals("Or", or.typeName)
            assertEquals(2, or.operands.size)
        }

        @Test
        @DisplayName("Not expression with comparison / 包含比较的逻辑非表达式")
        fun testNotExpressionWithComparison() {
            val inner = Comparison(ComparisonOperator.Eq, ScalarReference<String>(PropertyPath.parse("status")), ScalarConstant("deleted"))
            val not = NotExpression(inner)

            assertEquals("Not", not.typeName)
            assertEquals(1, not.children.size)
            assertFalse(not.isConstant())
        }

        @Test
        @DisplayName("Complex logical expression / 复杂逻辑表达式")
        fun testComplexLogicalExpression() {
            // (A and B) or not C
            // where A = age > 18, B = status in ('active'), C = name is null
            val A = Comparison(
                ComparisonOperator.Gt,
                ScalarReference<Int>(PropertyPath.parse("age")),
                ScalarConstant(18)
            )
            val B = InExpression(
                ScalarReference<String>(PropertyPath.parse("status")),
                listOf(ScalarConstant("active"))
            )
            val C = NullCheck(PropertyPath.parse("name"), NullCheckType.IsNull)

            val and = AndExpression(listOf(A, B))
            val not = NotExpression(C)
            val or = OrExpression(listOf(and, not))

            assertEquals(3, or.depth())
            assertEquals(5, or.logicalOperatorCount())  // and(2) + not(1) + or(2) = 5
            assertFalse(or.isConstant())

            val refs = or.collectReferences()
            assertEquals(3, refs.size)
            assertTrue(refs.contains(PropertyPath.parse("age")))
            assertTrue(refs.contains(PropertyPath.parse("status")))
            assertTrue(refs.contains(PropertyPath.parse("name")))
        }

        @Test
        @DisplayName("Comparison factory methods / 比较工厂方法测试")
        fun testComparisonFactoryMethods() {
            val left = ScalarConstant(10)
            val right = ScalarConstant(5)

            val eq = BooleanExpressionFactory.eq(left, right)
            val ne = BooleanExpressionFactory.ne(left, right)
            val lt = BooleanExpressionFactory.lt(left, right)
            val le = BooleanExpressionFactory.le(left, right)
            val gt = BooleanExpressionFactory.gt(left, right)
            val ge = BooleanExpressionFactory.ge(left, right)

            assertEquals(ComparisonOperator.Eq, eq.operator)
            assertEquals(ComparisonOperator.Ne, ne.operator)
            assertEquals(ComparisonOperator.Lt, lt.operator)
            assertEquals(ComparisonOperator.Le, le.operator)
            assertEquals(ComparisonOperator.Gt, gt.operator)
            assertEquals(ComparisonOperator.Ge, ge.operator)
        }
    }

    @Nested
    @DisplayName("Expression Traversal Tests / 表达式遍历测试")
    inner class ExpressionTraversalTests {

        @Test
        @DisplayName("Depth calculation / 深度计算")
        fun testDepthCalculation() {
            val leaf = Comparison(ComparisonOperator.Eq, ScalarReference<Int>(PropertyPath.parse("x")), ScalarConstant(1))
            val level1 = NotExpression(leaf)
            val level2 = AndExpression(listOf(level1, leaf))
            val level3 = OrExpression(listOf(level2, leaf))

            assertEquals(1, leaf.depth())
            assertEquals(2, level1.depth())
            assertEquals(3, level2.depth())
            assertEquals(4, level3.depth())
        }

        @Test
        @DisplayName("Logical operator count / 逻辑操作符计数")
        fun testLogicalOperatorCount() {
            val a = Comparison(ComparisonOperator.Eq, ScalarReference<Int>(PropertyPath.parse("x")), ScalarConstant(1))
            val b = Comparison(ComparisonOperator.Eq, ScalarReference<Int>(PropertyPath.parse("y")), ScalarConstant(2))
            val c = Comparison(ComparisonOperator.Eq, ScalarReference<Int>(PropertyPath.parse("z")), ScalarConstant(3))

            val and = AndExpression(listOf(a, b, c))
            assertEquals(3, and.logicalOperatorCount())

            val not = NotExpression(a)
            assertEquals(1, not.logicalOperatorCount())

            val complex = OrExpression(listOf(and, NotExpression(c)))
            // and(3) + not(1) + or(2) = 6
            assertEquals(6, complex.logicalOperatorCount())
        }
    }
}
