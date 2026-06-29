/**
 * 布尔表达式命名式 DSL 测试
 * Named Boolean Expression DSL tests
 */
package fuookami.ospf.kotlin.framework.persistence.expression

import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.symbol.expression.*

@DisplayName("Boolean Expression Top-Level DSL Tests / 布尔表达式顶层 DSL 测试")
class BooleanExpressionTopLevelDslTest {
    data class User(
        val age: Int,
        val status: String,
        val deletedAt: String?
    )

    @Test
    @DisplayName("Named eq avoids extension conflicts / 命名式 eq 避免扩展冲突")
    fun namedEqShouldAvoidExtensionConflicts() {
        val expr = eq(User::status, "active")

        assertEquals(ComparisonOperator.Eq, expr.operator)
        assertEquals("status", (expr.left as ScalarReference<*>).path.value)
        assertEquals("active", (expr.right as ScalarConstant<*>).value)
    }

    @Test
    @DisplayName("Lambda and collects child expressions / lambda and 收集子表达式")
    fun lambdaAndShouldCollectChildExpressions() {
        val expr = and {
            eq(User::status, "active")
            ge(User::age, 18)
            isNull(User::deletedAt)
        }

        assertTrue(expr is AndExpression)
        assertEquals(3, (expr as AndExpression).operands.size)
    }

    @Test
    @DisplayName("Nested vararg and does not duplicate collected children / 嵌套 vararg and 不重复收集子表达式")
    fun nestedVarargAndShouldNotDuplicateCollectedChildren() {
        val expr = and {
            and(eq(User::status, "active"), ge(User::age, 18))
        }

        assertTrue(expr is AndExpression)
        assertEquals(2, (expr as AndExpression).operands.size)
    }

    @Test
    @DisplayName("Empty combinations use boolean identity / 空组合使用布尔单位元")
    fun emptyCombinationsShouldUseBooleanIdentity() {
        val andExpr = and()
        val orExpr = or()

        assertEquals(BooleanConstant.true_(), andExpr)
        assertEquals(BooleanConstant.false_(), orExpr)
    }
}
