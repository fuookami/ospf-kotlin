/**
 * 表达式树变换测试 / Expression tree transformation tests
 */
package fuookami.ospf.kotlin.math.symbol.expression

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.math.Trivalent

@DisplayName("ExpressionTransform Tests / 表达式树变换测试")
class ExpressionTransformTest {
    @Test
    @DisplayName("should transform scalar nodes inside boolean nodes / 应变换布尔树中的标量节点")
    fun shouldTransformScalarNodesInsideBooleanNodes() {
        val original = Comparison(
            operator = ComparisonOperator.Eq,
            left = ScalarReference<Int>(PropertyPath.parse("age")),
            right = ScalarConstant(18)
        )
        val transformed = original.transformScalars { scalar ->
            when (scalar) {
                is ScalarConstant<*> -> ScalarConstant(21)
                else -> scalar
            }
        }

        val comparison = transformed as Comparison<*>
        assertEquals(21, (comparison.right as ScalarConstant<*>).value)
    }

    @Test
    @DisplayName("should invoke boolean transformer post order / 应按后序调用布尔变换器")
    fun shouldInvokeBooleanTransformerPostOrder() {
        val original = AndExpression(
            listOf(
                BooleanConstant(Trivalent.True),
                NotExpression(BooleanConstant(Trivalent.False))
            )
        )
        val visited = mutableListOf<String>()
        val transformed = original.transformBooleans {
            visited += it.typeName
            it
        }

        assertTrue(transformed is AndExpression)
        assertEquals(4, visited.size)
        assertTrue(visited.last().contains("And"))
    }

    @Test
    @DisplayName("should transform nested scalar and boolean branches / 应递归变换嵌套标量和布尔分支")
    fun shouldTransformNestedScalarAndBooleanBranches() {
        val original = ScalarConditional(
            condition = NotExpression(
                Comparison(
                    operator = ComparisonOperator.Eq,
                    left = ScalarReference<Int>(PropertyPath.parse("status")),
                    right = ScalarConstant(1)
                )
            ),
            thenBranch = ScalarBinary(
                operator = BinaryOperator.Add,
                left = ScalarConstant(2),
                right = ScalarConstant(3)
            ),
            elseBranch = ScalarConstant(0)
        )

        val transformed = original.transform { scalar ->
            when (scalar) {
                is ScalarConstant<*> -> ScalarConstant(
                    when (val value = scalar.value) {
                        is Int -> value + 10
                        else -> value
                    }
                )
                else -> scalar
            }
        }

        val conditional = transformed as ScalarConditional<*>
        val condition = conditional.condition as NotExpression
        val comparison = condition.operand as Comparison<*>
        assertEquals(11, (comparison.right as ScalarConstant<*>).value)
        assertEquals(12, (conditional.thenBranch as ScalarBinary<*>).left.let { (it as ScalarConstant<*>).value })
        assertEquals(13, (conditional.thenBranch as ScalarBinary<*>).right.let { (it as ScalarConstant<*>).value })
        assertEquals(10, (conditional.elseBranch as ScalarConstant<*>).value)
    }
}
