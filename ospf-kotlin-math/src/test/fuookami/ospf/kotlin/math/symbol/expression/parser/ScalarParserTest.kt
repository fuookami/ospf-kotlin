/**
 * 标量表达式解析器与求值器测试
 * Scalar Expression Parser and Evaluator Tests
 *
 * 验收标准：
 * 1. 解析+求值覆盖全量 Aviator 语法
 * 2. -x^2 = -9（一元负号在加减层）
 * 3. math.round 返回 Long
 * 4. if 条件允许 && 和 ||
 * 5. 布尔参与算术返回 Failed
 * 6. 现有布尔解析器/求值器测试全绿
 */
package fuookami.ospf.kotlin.math.symbol.expression.parser

import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import fuookami.ospf.kotlin.math.symbol.expression.*
import fuookami.ospf.kotlin.math.symbol.expression.operation.*
import fuookami.ospf.kotlin.math.Trivalent
import fuookami.ospf.kotlin.utils.functional.*

@DisplayName("Scalar Parser Tests / 标量表达式解析器测试")
class ScalarParserTest {

    private val context = MapEvaluationContext.fromStringMap(
        mapOf("x" to 3.0, "y" to 4.0, "a" to 3.0, "b" to 4.0, "w" to 800.0)
    )

    // ========== 解析测试 / Parse Tests ==========

    @Nested
    @DisplayName("Arithmetic Tests / 算术测试")
    inner class ArithmeticTests {

        @Test
        @DisplayName("Parse and evaluate x + y")
        fun testAddition() {
            val expr = parseScalarExpression("x + y").value!!
            val result = evaluateScalar(expr, context).value!!
            assertEquals(7.0, result)
        }

        @Test
        @DisplayName("Parse and evaluate x * y")
        fun testMultiplication() {
            val expr = parseScalarExpression("x * y").value!!
            val result = evaluateScalar(expr, context).value!!
            assertEquals(12.0, result)
        }

        @Test
        @DisplayName("Parse and evaluate x * 2")
        fun testMultiplyByConstant() {
            val expr = parseScalarExpression("x * 2").value!!
            val result = evaluateScalar(expr, context).value!!
            assertEquals(6.0, result)
        }

        @Test
        @DisplayName("Parse and evaluate x / y")
        fun testDivision() {
            val expr = parseScalarExpression("x / y").value!!
            val result = evaluateScalar(expr, context).value!!
            assertEquals(0.75, result)
        }

        @Test
        @DisplayName("Parse and evaluate x % y")
        fun testModulo() {
            val expr = parseScalarExpression("x % y").value!!
            val result = evaluateScalar(expr, context).value!!
            assertEquals(3.0, result)
        }

        @Test
        @DisplayName("Parse and evaluate x ^ 2")
        fun testPowerCaret() {
            val expr = parseScalarExpression("x ^ 2").value!!
            val result = evaluateScalar(expr, context).value!!
            assertEquals(9.0, result)
        }

        @Test
        @DisplayName("Parse and evaluate x ** 2")
        fun testPowerDoubleStar() {
            val expr = parseScalarExpression("x ** 2").value!!
            val result = evaluateScalar(expr, context).value!!
            assertEquals(9.0, result)
        }

        @Test
        @DisplayName("Parse and evaluate -x")
        fun testUnaryNegate() {
            val expr = parseScalarExpression("-x").value!!
            val result = evaluateScalar(expr, context).value!!
            assertEquals(-3.0, result)
        }

        @Test
        @DisplayName("Parse and evaluate -x^2 = -(x^2) = -9")
        fun testUnaryMinusPrecedence() {
            val expr = parseScalarExpression("-x ^ 2").value!!
            val result = evaluateScalar(expr, context).value!!
            assertEquals(-9.0, result)
        }

        @Test
        @DisplayName("Parse and evaluate -x^2+1 = -(x^2)+1 = -8 (A4 regression)")
        fun testUnaryMinusWithTrailingAdd() {
            val expr = parseScalarExpression("-x ^ 2 + 1").value!!
            // 期望 -(x^2)+1 = -9+1 = -8，而非 -(x^2+1) = -10
            val result = evaluateScalar(expr, context).value!!
            assertEquals(-8.0, result)
        }

        @Test
        @DisplayName("Parse and evaluate -2-3 = (-2)-3 = -5 (A4 regression)")
        fun testUnaryMinusWithTrailingSubtract() {
            val ctx = MapEvaluationContext.fromStringMap(emptyMap())
            val expr = parseScalarExpression("-2 - 3").value!!
            // 期望 (-2)-3 = -5，而非 -(2-3) = 1
            val result = evaluateScalar(expr, ctx).value!!
            assertEquals(-5.0, result)
        }

        @Test
        @DisplayName("Parse and evaluate -2+3 = (-2)+3 = 1 (A4 regression)")
        fun testUnaryMinusWithTrailingAddNumber() {
            val ctx = MapEvaluationContext.fromStringMap(emptyMap())
            val expr = parseScalarExpression("-2 + 3").value!!
            val result = evaluateScalar(expr, ctx).value!!
            assertEquals(1.0, result)
        }

        @Test
        @DisplayName("Parse and evaluate --x = -(-x) = x (consecutive minus)")
        fun testConsecutiveUnaryMinus() {
            val expr = parseScalarExpression("--x").value!!
            val result = evaluateScalar(expr, context).value!!
            assertEquals(3.0, result)
        }

        @Test
        @DisplayName("Parse and evaluate - -x = -(-x) = x (consecutive minus with space)")
        fun testConsecutiveUnaryMinusWithSpace() {
            val expr = parseScalarExpression("- -x").value!!
            val result = evaluateScalar(expr, context).value!!
            assertEquals(3.0, result)
        }

        @Test
        @DisplayName("Parse and evaluate 2 + 3 * 4 (precedence)")
        fun testArithmeticPrecedence() {
            val ctx = MapEvaluationContext.fromStringMap(emptyMap())
            val expr = parseScalarExpression("2 + 3 * 4").value!!
            val result = evaluateScalar(expr, ctx).value!!
            assertEquals(14.0, result)
        }

        @Test
        @DisplayName("Parse and evaluate (2 + 3) * 4 (parentheses)")
        fun testParentheses() {
            val ctx = MapEvaluationContext.fromStringMap(emptyMap())
            val expr = parseScalarExpression("(2 + 3) * 4").value!!
            val result = evaluateScalar(expr, ctx).value!!
            assertEquals(20.0, result)
        }
    }

    @Nested
    @DisplayName("Math Function Tests / 数学函数测试")
    inner class MathFunctionTests {

        @Test
        @DisplayName("Parse and evaluate math.sqrt(16)")
        fun testSqrt() {
            val ctx = MapEvaluationContext.fromStringMap(emptyMap())
            val expr = parseScalarExpression("math.sqrt(16)").value!!
            assertTrue(expr is ScalarFunction<*>)
            assertEquals("sqrt", (expr as ScalarFunction<*>).name)
            val result = evaluateScalar(expr, ctx, MathFunctionEvaluator).value!!
            assertEquals(4.0, result)
        }

        @Test
        @DisplayName("Parse and evaluate math.pow(x, 2) + math.pow(y, 2)")
        fun testPowSum() {
            val expr = parseScalarExpression("math.pow(x, 2) + math.pow(y, 2)").value!!
            val result = evaluateScalar(expr, context, MathFunctionEvaluator).value!!
            assertEquals(25.0, result)
        }

        @Test
        @DisplayName("Parse math.PI as constant")
        fun testMathPI() {
            val ctx = MapEvaluationContext.fromStringMap(emptyMap())
            val expr = parseScalarExpression("math.PI").value!!
            assertTrue(expr is ScalarConstant<*>)
            assertEquals(kotlin.math.PI, (expr as ScalarConstant<*>).value)
        }

        @Test
        @DisplayName("Parse math.E as constant")
        fun testMathE() {
            val ctx = MapEvaluationContext.fromStringMap(emptyMap())
            val expr = parseScalarExpression("math.E").value!!
            assertTrue(expr is ScalarConstant<*>)
            assertEquals(kotlin.math.E, (expr as ScalarConstant<*>).value)
        }

        @Test
        @DisplayName("math.round(3.7) returns Long 4")
        fun testRoundReturnsLong() {
            val ctx = MapEvaluationContext.fromStringMap(emptyMap())
            val expr = parseScalarExpression("math.round(3.7)").value!!
            val result = evaluateScalar(expr, ctx, MathFunctionEvaluator).value!!
            assertTrue(result is Long)
            assertEquals(4L, result)
        }
    }

    @Nested
    @DisplayName("Conditional Tests / 条件测试")
    inner class ConditionalTests {

        @Test
        @DisplayName("Parse and evaluate x > 0 ? x : y")
        fun testTernary() {
            val expr = parseScalarExpression("x > 0 ? x : y").value!!
            assertTrue(expr is ScalarConditional<*>)
            val result = evaluateScalar(expr, context).value!!
            assertEquals(3.0, result)
        }

        @Test
        @DisplayName("Parse and evaluate if (w > 787) then x else y fi")
        fun testIfThenElse() {
            val expr = parseScalarExpression("if (w > 787) then x else y fi").value!!
            assertTrue(expr is ScalarConditional<*>)
            val result = evaluateScalar(expr, context).value!!
            assertEquals(3.0, result)
        }

        @Test
        @DisplayName("if with && condition: if x > 0 && y > 0 then x else y fi")
        fun testIfWithAndCondition() {
            val expr = parseScalarExpression("if x > 0 && y > 0 then x else y fi").value!!
            assertTrue(expr is ScalarConditional<*>)
            val result = evaluateScalar(expr, context).value!!
            assertEquals(3.0, result)
        }

        @Test
        @DisplayName("if condition with || operator")
        fun testIfWithOrCondition() {
            val ctx = MapEvaluationContext.fromStringMap(mapOf("a" to -1.0, "b" to 5.0, "r" to 10.0))
            val expr = parseScalarExpression("if a > 0 || b > 0 then r else 0 fi").value!!
            val result = evaluateScalar(expr, ctx).value!!
            assertEquals(10.0, result)
        }
    }

    @Nested
    @DisplayName("Boolean-as-Scalar Tests / 布尔标量测试")
    inner class BooleanAsScalarTests {

        @Test
        @DisplayName("Parse and evaluate x > 0 && y > 0 as ScalarBoolean")
        fun testBooleanAnd() {
            val expr = parseScalarExpression("x > 0 && y > 0").value!!
            assertTrue(expr is ScalarBoolean<*>)
            val result = evaluateScalar(expr, context).value!!
            assertEquals(true, result)
        }

        @Test
        @DisplayName("Parse and evaluate true as ScalarBoolean")
        fun testTrueConstant() {
            val ctx = MapEvaluationContext.fromStringMap(emptyMap())
            val expr = parseScalarExpression("true").value!!
            assertTrue(expr is ScalarBoolean<*>)
            val result = evaluateScalar(expr, ctx).value!!
            assertEquals(true, result)
        }

        @Test
        @DisplayName("Parse and evaluate false as ScalarBoolean")
        fun testFalseConstant() {
            val ctx = MapEvaluationContext.fromStringMap(emptyMap())
            val expr = parseScalarExpression("false").value!!
            assertTrue(expr is ScalarBoolean<*>)
            val result = evaluateScalar(expr, ctx).value!!
            assertEquals(false, result)
        }

        @Test
        @DisplayName("Parse and evaluate x > 0")
        fun testComparisonAsScalar() {
            val expr = parseScalarExpression("x > 0").value!!
            assertTrue(expr is ScalarBoolean<*>)
            val result = evaluateScalar(expr, context).value!!
            assertEquals(true, result)
        }

        @Test
        @DisplayName("Parse and evaluate x < 0")
        fun testComparisonFalseAsScalar() {
            val expr = parseScalarExpression("x < 0").value!!
            assertTrue(expr is ScalarBoolean<*>)
            val result = evaluateScalar(expr, context).value!!
            assertEquals(false, result)
        }
    }

    @Nested
    @DisplayName("Error Cases / 错误用例测试")
    inner class ErrorTests {

        @Test
        @DisplayName("Division by zero returns Failed")
        fun testDivisionByZero() {
            val ctx = MapEvaluationContext.fromStringMap(mapOf("a" to 1.0, "b" to 0.0))
            val expr = parseScalarExpression("a / b").value!!
            val result = evaluateScalar(expr, ctx)
            assertTrue(result.failed)
        }

        @Test
        @DisplayName("Unknown function returns Failed")
        fun testUnknownFunction() {
            val ctx = MapEvaluationContext.fromStringMap(emptyMap())
            val expr = parseScalarExpression("unknownFunc(1)").value!!
            val result = evaluateScalar(expr, ctx)
            assertTrue(result.failed)
        }

        @Test
        @DisplayName("Mismatched if/then/else/fi fails to parse")
        fun testMismatchedIf() {
            val result = parseScalarExpression("if x > 0 then x fi")
            assertTrue(result.failed)
        }

        @Test
        @DisplayName("Empty expression fails to parse")
        fun testEmptyExpression() {
            val result = parseScalarExpression("")
            assertTrue(result.failed)
        }
    }

    @Nested
    @DisplayName("AST Structure Tests / AST 结构测试")
    inner class ASTStructureTests {

        @Test
        @DisplayName("math.sqrt function name is 'sqrt' (not 'math.sqrt')")
        fun testFunctionNamespaceStripping() {
            val expr = parseScalarExpression("math.sqrt(16)").value!!
            assertTrue(expr is ScalarFunction<*>)
            assertEquals("sqrt", (expr as ScalarFunction<*>).name)
        }

        @Test
        @DisplayName("abs function preserves name (no math. prefix)")
        fun testAbsWithoutNamespace() {
            val expr = parseScalarExpression("abs(-5)").value!!
            assertTrue(expr is ScalarFunction<*>)
            assertEquals("abs", (expr as ScalarFunction<*>).name)
        }

        @Test
        @DisplayName("-x^2 has correct AST structure: Negate(Power(x, 2))")
        fun testUnaryMinusPowerAST() {
            val expr = parseScalarExpression("-x ^ 2").value!!
            assertTrue(expr is ScalarUnary<*>)
            val unary = expr as ScalarUnary<*>
            assertEquals(UnaryOperator.Negate, unary.operator)
            assertTrue(unary.operand is ScalarBinary<*>)
            val binary = unary.operand as ScalarBinary<*>
            assertEquals(BinaryOperator.Power, binary.operator)
        }

        @Test
        @DisplayName("ScalarConditional condition is BooleanExpression")
        fun testConditionalAST() {
            val expr = parseScalarExpression("if x > 0 then x else y fi").value!!
            assertTrue(expr is ScalarConditional<*>)
            val cond = expr as ScalarConditional<*>
            assertTrue(cond.condition is Comparison<*>)
        }
    }
}
