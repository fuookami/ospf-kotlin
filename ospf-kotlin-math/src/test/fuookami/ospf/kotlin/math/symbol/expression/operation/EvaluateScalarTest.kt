/**
 * 标量表达式求值器测试
 * Scalar Expression Evaluator Tests
 *
 * 覆盖 spec Section 6 中的求值器专属用例：
 * 1. 布尔参与算术返回 Failed
 * 2. ScalarSymbolReference 求值返回 Failed
 * 3. ScalarCustom 求值返回 Failed
 * 4. 函数参数类型不匹配返回 Failed
 * 5. Unary ! 逻辑非行为
 */
package fuookami.ospf.kotlin.math.symbol.expression.operation

import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import fuookami.ospf.kotlin.math.symbol.expression.*
import fuookami.ospf.kotlin.math.symbol.expression.parser.*
import fuookami.ospf.kotlin.math.symbol.serde.symbolOfSerializedIdentifier
import fuookami.ospf.kotlin.math.Trivalent
import fuookami.ospf.kotlin.utils.functional.*

@DisplayName("EvaluateScalar Tests / 标量求值器测试")
class EvaluateScalarTest {

    private val context = MapEvaluationContext.fromStringMap(
        mapOf("x" to 3.0, "y" to 4.0, "a" to 3.0, "b" to 4.0, "w" to 800.0)
    )

    @Nested
    @DisplayName("Boolean-in-Arithmetic / 布尔参与算术")
    inner class BooleanInArithmeticTests {

        @Test
        @DisplayName("(x > 0) + 1 returns Failed (boolean not coerced to number)")
        fun testBooleanInArithmeticFails() {
            val expr = parseScalarExpression("(x > 0) + 1").value!!
            assertTrue(expr is ScalarBinary<*>)
            val result = evaluateScalar(expr, context)
            assertTrue(result.failed)
        }

        @Test
        @DisplayName("(x > 0) * 2 returns Failed")
        fun testBooleanMultiplyFails() {
            val expr = parseScalarExpression("(x > 0) * 2").value!!
            val result = evaluateScalar(expr, context)
            assertTrue(result.failed)
        }
    }

    @Nested
    @DisplayName("Opaque Node Evaluation / 不透明节点求值")
    inner class OpaqueNodeTests {

        @Test
        @DisplayName("ScalarSymbolReference evaluation returns Failed")
        fun testScalarSymbolReferenceFails() {
            val symbol = symbolOfSerializedIdentifier("unbound")
            val expr: ScalarExpression<Double> = ScalarSymbolReference(symbol)
            val result = evaluateScalar(expr, context)
            assertTrue(result.failed)
        }

        @Test
        @DisplayName("ScalarCustom evaluation returns Failed")
        fun testScalarCustomFails() {
            val expr: ScalarExpression<Double> = ScalarCustom("custom value", "test custom")
            val result = evaluateScalar(expr, context)
            assertTrue(result.failed)
        }
    }

    @Nested
    @DisplayName("Function Argument Type Mismatch / 函数参数类型不匹配")
    inner class FunctionTypeErrorTests {

        @Test
        @DisplayName("math.sqrt('abc') returns Failed (string arg to numeric function)")
        fun testStringArgToMathFunction() {
            val ctx = MapEvaluationContext.fromStringMap(emptyMap())
            val expr = parseScalarExpression("math.sqrt('abc')").value!!
            val result = evaluateScalar(expr, ctx, MathFunctionEvaluator)
            // 字符串不是 Number，asDoubleOrNull 返回 null，函数返回 null -> Failed
            assertTrue(result.failed)
        }

        @Test
        @DisplayName("math.pow(2, 'a') returns Failed")
        fun testMixedTypeArgs() {
            val ctx = MapEvaluationContext.fromStringMap(emptyMap())
            val expr = parseScalarExpression("math.pow(2, 'a')").value!!
            val result = evaluateScalar(expr, ctx, MathFunctionEvaluator)
            assertTrue(result.failed)
        }

        @Test
        @DisplayName("math.sqrt() with wrong arity returns Failed")
        fun testWrongArity() {
            val ctx = MapEvaluationContext.fromStringMap(emptyMap())
            val expr = parseScalarExpression("math.sqrt(1, 2)").value!!
            val result = evaluateScalar(expr, ctx, MathFunctionEvaluator)
            assertTrue(result.failed)
        }
    }

    @Nested
    @DisplayName("Logical NOT (!) Behavior / 逻辑非行为")
    inner class LogicalNotTests {

        @Test
        @DisplayName("!(x > 0) evaluates to false when x=3")
        fun testLogicalNotWithParens() {
            val expr = parseScalarExpression("!(x > 0)").value!!
            assertTrue(expr is ScalarBoolean<*>)
            val result = evaluateScalar(expr, context).value!!
            assertEquals(false, result)
        }

        @Test
        @DisplayName("!(x < 0) evaluates to true when x=3")
        fun testLogicalNotTrueCase() {
            val expr = parseScalarExpression("!(x < 0)").value!!
            val result = evaluateScalar(expr, context).value!!
            assertEquals(true, result)
        }

        @Test
        @DisplayName("not (x > 0) keyword form works")
        fun testLogicalNotKeyword() {
            val expr = parseScalarExpression("not (x > 0)").value!!
            val result = evaluateScalar(expr, context).value!!
            assertEquals(false, result)
        }
    }

    @Nested
    @DisplayName("Conditional Edge Cases / 条件边界用例")
    inner class ConditionalEdgeCaseTests {

        @Test
        @DisplayName("Nested ternary: x > 0 ? (y > 0 ? 1 : 2) : 3")
        fun testNestedTernary() {
            val expr = parseScalarExpression("x > 0 ? (y > 0 ? 1 : 2) : 3").value!!
            val result = evaluateScalar(expr, context).value!!
            assertEquals(1.0, result)
        }

        @Test
        @DisplayName("Ternary with else-only branch true: x > 0 ? 10 : 20 = 10")
        fun testTernaryTrueBranch() {
            val expr = parseScalarExpression("x > 0 ? 10 : 20").value!!
            val result = evaluateScalar(expr, context).value!!
            assertEquals(10.0, result)
        }

        @Test
        @DisplayName("Ternary with else branch: x > 10 ? 10 : 20 = 20")
        fun testTernaryFalseBranch() {
            val expr = parseScalarExpression("x > 10 ? 10 : 20").value!!
            val result = evaluateScalar(expr, context).value!!
            assertEquals(20.0, result)
        }

        @Test
        @DisplayName("if with || condition true branch")
        fun testIfOrTrueBranch() {
            val expr = parseScalarExpression("if x > 0 || y < 0 then 100 else 200 fi").value!!
            val result = evaluateScalar(expr, context).value!!
            assertEquals(100.0, result)
        }
    }

    @Nested
    @DisplayName("math.* Function Coverage / 数学函数覆盖")
    inner class MathFunctionCoverageTests {

        @Test
        @DisplayName("math.max(x, y) = 4")
        fun testMax() {
            val expr = parseScalarExpression("math.max(x, y)").value!!
            val result = evaluateScalar(expr, context, MathFunctionEvaluator).value!!
            assertEquals(4.0, result)
        }

        @Test
        @DisplayName("math.min(x, y) = 3")
        fun testMin() {
            val expr = parseScalarExpression("math.min(x, y)").value!!
            val result = evaluateScalar(expr, context, MathFunctionEvaluator).value!!
            assertEquals(3.0, result)
        }

        @Test
        @DisplayName("math.abs(-5) = 5")
        fun testAbs() {
            val ctx = MapEvaluationContext.fromStringMap(emptyMap())
            val expr = parseScalarExpression("math.abs(-5)").value!!
            val result = evaluateScalar(expr, ctx, MathFunctionEvaluator).value!!
            assertEquals(5.0, result)
        }

        @Test
        @DisplayName("math.floor(3.7) = 3.0")
        fun testFloor() {
            val ctx = MapEvaluationContext.fromStringMap(emptyMap())
            val expr = parseScalarExpression("math.floor(3.7)").value!!
            val result = evaluateScalar(expr, ctx, MathFunctionEvaluator).value!!
            assertEquals(3.0, result)
        }

        @Test
        @DisplayName("math.ceil(3.2) = 4.0")
        fun testCeil() {
            val ctx = MapEvaluationContext.fromStringMap(emptyMap())
            val expr = parseScalarExpression("math.ceil(3.2)").value!!
            val result = evaluateScalar(expr, ctx, MathFunctionEvaluator).value!!
            assertEquals(4.0, result)
        }

        @Test
        @DisplayName("math.exp(0) = 1.0")
        fun testExp() {
            val ctx = MapEvaluationContext.fromStringMap(emptyMap())
            val expr = parseScalarExpression("math.exp(0)").value!!
            val result = evaluateScalar(expr, ctx, MathFunctionEvaluator).value!! as Double
            assertEquals(1.0, result, 0.0001)
        }

        @Test
        @DisplayName("MathFunctionEvaluator.supportedFunctions contains all 17 functions")
        fun testSupportedFunctionsCount() {
            val expected = setOf(
                "sqrt", "pow", "log", "log10", "exp",
                "sin", "cos", "tan", "asin", "acos", "atan",
                "floor", "ceil", "round",
                "max", "min", "abs"
            )
            assertEquals(expected, MathFunctionEvaluator.supportedFunctions)
            assertTrue(MathFunctionEvaluator.supportedFunctions.size == 17)
        }
    }
}
