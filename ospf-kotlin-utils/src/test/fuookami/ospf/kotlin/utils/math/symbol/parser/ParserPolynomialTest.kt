package fuookami.ospf.kotlin.utils.math.symbol.parser

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

private fun Expr.render(): String {
    return when (this) {
        is Expr.NumberLiteral -> text
        is Expr.Identifier -> name
        is Expr.UnaryMinus -> "(-${operand.render()})"
        is Expr.Binary -> {
            val operator = when (this.operator) {
                BinaryOperator.Add -> "+"
                BinaryOperator.Subtract -> "-"
                BinaryOperator.Multiply -> "*"
                BinaryOperator.Power -> "^"
            }
            "(${left.render()} $operator ${right.render()})"
        }

        is Expr.FunctionCall -> {
            "$name(${arguments.joinToString(", ") { it.render() }})"
        }

        is Expr.Comparison -> {
            val operator = when (this.operator) {
                ComparisonOperator.Less -> "<"
                ComparisonOperator.LessEqual -> "<="
                ComparisonOperator.Equal -> "="
                ComparisonOperator.NotEqual -> "!="
                ComparisonOperator.GreaterEqual -> ">="
                ComparisonOperator.Greater -> ">"
            }
            "(${left.render()} $operator ${right.render()})"
        }
    }
}

class ParserPolynomialTest {
    @Test
    fun parserShouldHandleLinearPolynomialExpression() {
        val expr = parseSymbolExpression("2*x + 3*y - 1")
        assertEquals("(((2 * x) + (3 * y)) - 1)", expr.render())
    }

    @Test
    fun parserShouldHandleQuadraticPolynomialExpression() {
        val expr = parseSymbolExpression("x*x + 2*x*y + 1")
        assertEquals("(((x * x) + ((2 * x) * y)) + 1)", expr.render())
    }

    @Test
    fun parserShouldHandleParenthesesAndUnaryMinus() {
        val expr = parseSymbolExpression("-(x + 2) * y")
        assertEquals("((-(x + 2)) * y)", expr.render())
    }

    @Test
    fun parserShouldHandleFunctionSymbolExpression() {
        val expr = parseSymbolExpression("max(x, y + 1)")
        assertEquals("max(x, (y + 1))", expr.render())
    }

    @Test
    fun parserShouldFailForUnexpectedToken() {
        assertFailsWith<ParseError> {
            parseSymbolExpression("x + )")
        }
    }
}
