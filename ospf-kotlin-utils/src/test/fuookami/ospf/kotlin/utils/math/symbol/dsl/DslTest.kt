package fuookami.ospf.kotlin.utils.math.symbol.dsl

import fuookami.ospf.kotlin.utils.math.algebra.number.Flt64
import fuookami.ospf.kotlin.utils.math.symbol.Symbol
import fuookami.ospf.kotlin.utils.math.symbol.serde.toCanonicalInequality
import fuookami.ospf.kotlin.utils.math.symbol.serde.toCanonicalPolynomial
import fuookami.ospf.kotlin.utils.math.symbol.serde.toExpr
import fuookami.ospf.kotlin.utils.math.symbol.serde.toJsonString
import fuookami.ospf.kotlin.utils.math.symbol.serde.toLinearPolynomialOrNull
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class DslTest {
    private data class TestSymbol(
        override val name: String,
        override val displayName: String? = null
    ) : Symbol

    @Test
    fun dslShouldBuildLinearExpression() {
        val x = TestSymbol("x")
        val y = TestSymbol("y")
        val symbolByName = mapOf(
            "x" to x,
            "y" to y
        )

        val expression = symbolExpr {
            num(2) * symbol("x") + num(3) * symbol("y") - num(1)
        }
        val polynomial = expression.toLinearPolynomialOrNull { name ->
            symbolByName[name] ?: error("Unknown symbol: $name")
        }

        assertNotNull(polynomial)
        val coefficients = polynomial.monomials.associate { it.symbol to it.coefficient }
        assertEquals(Flt64(2.0), coefficients[x])
        assertEquals(Flt64(3.0), coefficients[y])
        assertEquals(Flt64(-1.0), polynomial.constant)
    }

    @Test
    fun dslShouldBuildCanonicalHighOrderExpression() {
        val x = TestSymbol("x")
        val y = TestSymbol("y")
        val symbolByName = mapOf(
            "x" to x,
            "y" to y
        )

        val expression = symbolExpr {
            (symbol("x") pow 2) * (symbol("y") pow 3) + num(2) * symbol("x") - num(1)
        }
        val polynomial = expression.toCanonicalPolynomial { name ->
            symbolByName[name] ?: error("Unknown symbol: $name")
        }
        val monomialByDegree = polynomial.monomials.associateBy { it.factors.size }

        assertEquals(Flt64(-1.0), polynomial.constant)
        assertEquals(Flt64(2.0), monomialByDegree[1]?.coefficient)
        assertEquals(listOf(x), monomialByDegree[1]?.factors)
        assertEquals(Flt64.one, monomialByDegree[5]?.coefficient)
        assertEquals(listOf(x, x, y, y, y), monomialByDegree[5]?.factors)
    }

    @Test
    fun dslShouldBuildInequalityExpression() {
        val x = TestSymbol("x")
        val y = TestSymbol("y")
        val symbolByName = mapOf(
            "x" to x,
            "y" to y
        )

        val expression = symbolExpr {
            (symbol("x") pow 2) + num(1) le (symbol("y") + num(3))
        }
        val inequality = (expression as fuookami.ospf.kotlin.utils.math.symbol.parser.Expr.Comparison)
            .toCanonicalInequality { name ->
                symbolByName[name] ?: error("Unknown symbol: $name")
            }

        assertEquals(
            inequality,
            inequality.toExpr().toCanonicalInequality { name ->
                symbolByName[name] ?: error("Unknown symbol: $name")
            }
        )
    }

    @Test
    fun dslExpressionShouldSupportJsonRoundTrip() {
        val expression = symbolExpr {
            call("max", symbol("x"), symbol("y") + num(1))
        }

        val restored = fuookami.ospf.kotlin.utils.math.symbol.serde.symbolExprFromJson(expression.toJsonString())

        assertEquals(expression, restored)
    }
}



