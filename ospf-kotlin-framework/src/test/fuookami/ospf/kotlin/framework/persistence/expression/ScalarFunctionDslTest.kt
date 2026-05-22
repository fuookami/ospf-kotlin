/**
 * 仓储标量函数 DSL 测试
 * Repository scalar function DSL tests
 */
package fuookami.ospf.kotlin.framework.persistence.expression

import fuookami.ospf.kotlin.math.symbol.expression.ComparisonOperator
import fuookami.ospf.kotlin.math.symbol.expression.ScalarFunction
import fuookami.ospf.kotlin.math.symbol.expression.ScalarFunctionNames
import fuookami.ospf.kotlin.math.symbol.expression.dsl.eq
import fuookami.ospf.kotlin.math.symbol.expression.dsl.gt
import fuookami.ospf.kotlin.math.symbol.expression.dsl.path
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Repository Scalar Function DSL Tests / 仓储标量函数 DSL 测试")
class ScalarFunctionDslTest {
    @Test
    @DisplayName("String functions build scalar functions / 字符串函数构造标量函数")
    fun stringFunctionsShouldBuildScalarFunctions() {
        val lowerExpr = lower(path("name"))
        val upperExpr = upper(path("name"))
        val trimExpr = trim(path("name"))
        val lengthComparison = length(path("name")) gt 3

        assertEquals(ScalarFunctionNames.Lower, lowerExpr.name)
        assertEquals(ScalarFunctionNames.Upper, upperExpr.name)
        assertEquals(ScalarFunctionNames.Trim, trimExpr.name)
        assertEquals(ComparisonOperator.Gt, lengthComparison.operator)
        assertTrue(lengthComparison.left is ScalarFunction<*>)
    }

    @Test
    @DisplayName("Coalesce builds scalar function / coalesce 构造标量函数")
    fun coalesceShouldBuildScalarFunction() {
        val expr = coalesce(path("nickname"), path("name")) eq "alice"

        assertEquals(ComparisonOperator.Eq, expr.operator)
        assertTrue(expr.left is ScalarFunction<*>)
        assertEquals(ScalarFunctionNames.Coalesce, (expr.left as ScalarFunction<*>).name)
    }
}
