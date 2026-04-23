package fuookami.ospf.kotlin.math.symbol.parser

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

private fun Expr.renderInequality(): String {
    return when (this) {
        is Expr.NumberLiteral -> text
        is Expr.Identifier -> name
        is Expr.UnaryMinus -> "(-${operand.renderInequality()})"
        is Expr.Binary -> {
            val operator = when (this.operator) {
                BinaryOperator.Add -> "+"
                BinaryOperator.Subtract -> "-"
                BinaryOperator.Multiply -> "*"
                BinaryOperator.Power -> "^"
            }
            "(${left.renderInequality()} $operator ${right.renderInequality()})"
        }

        is Expr.FunctionCall -> {
            "$name(${arguments.joinToString(", ") { it.renderInequality() }})"
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
            "(${left.renderInequality()} $operator ${right.renderInequality()})"
        }
    }
}

class ParserInequalityTest {
    @Test
    fun parserShouldHandleAllComparisonOperators() {
        assertEquals("((x + 1) <= y)", parseLegacySymbolInequality("x + 1 <= y").renderInequality())
        assertEquals("((x ^ 2) >= 0)", parseLegacySymbolInequality("x^2 >= 0").renderInequality())
        assertEquals("((x * y) = 1)", parseLegacySymbolInequality("x*y = 1").renderInequality())
        assertEquals("((x + y) != 0)", parseLegacySymbolInequality("x + y != 0").renderInequality())
    }

    @Test
    fun parserShouldHandleCanonicalHighOrderTermsAndMixedConstants() {
        val rendered = parseLegacySymbolInequality("x^2 * y^3 + 2*x - 1 != z^4 + 3").renderInequality()
        assertEquals("(((((x ^ 2) * (y ^ 3)) + (2 * x)) - 1) != ((z ^ 4) + 3))", rendered)
    }
}
