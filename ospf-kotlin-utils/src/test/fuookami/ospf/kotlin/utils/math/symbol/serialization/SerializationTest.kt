package fuookami.ospf.kotlin.utils.math.symbol.serialization

import fuookami.ospf.kotlin.utils.math.algebra.number.Flt64
import fuookami.ospf.kotlin.utils.math.symbol.Symbol
import fuookami.ospf.kotlin.utils.math.symbol.inequality.CanonicalInequality
import fuookami.ospf.kotlin.utils.math.symbol.inequality.Comparison
import fuookami.ospf.kotlin.utils.math.symbol.monomial.CanonicalMonomial
import fuookami.ospf.kotlin.utils.math.symbol.operation.combineTerms
import fuookami.ospf.kotlin.utils.math.symbol.parser.Expr
import fuookami.ospf.kotlin.utils.math.symbol.parser.parseSymbolExpression
import fuookami.ospf.kotlin.utils.math.symbol.parser.parseSymbolInequality
import fuookami.ospf.kotlin.utils.math.symbol.polynomial.CanonicalPolynomial
import fuookami.ospf.kotlin.utils.math.symbol.serde.symbolExprFromJson
import fuookami.ospf.kotlin.utils.math.symbol.serde.toCanonicalInequality
import fuookami.ospf.kotlin.utils.math.symbol.serde.toCanonicalPolynomial
import fuookami.ospf.kotlin.utils.math.symbol.serde.toExpr
import fuookami.ospf.kotlin.utils.math.symbol.serde.toJsonString
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class SerializationTest {
    private data class TestSymbol(
        override val name: String,
        override val displayName: String? = null
    ) : Symbol

    @Test
    fun parserExprShouldRoundTripThroughJson() {
        val expression = parseSymbolExpression("x^2 * y + 2*x - 1")
        val json = expression.toJsonString()
        val restored = symbolExprFromJson(json)

        assertEquals(expression, restored)
    }

    @Test
    fun parserInequalityShouldRoundTripThroughJson() {
        val inequality = parseSymbolInequality("x^2 + y != z + 1")
        val json = inequality.toJsonString()
        val restored = symbolExprFromJson(json)

        assertEquals(inequality, restored)
    }

    @Test
    fun canonicalPolynomialShouldRoundTripThroughExprModel() {
        val x = TestSymbol("x")
        val y = TestSymbol("y")
        val z = TestSymbol("z")
        val symbolByName = mapOf(
            "x" to x,
            "y" to y,
            "z" to z
        )
        val polynomial = CanonicalPolynomial<Flt64>(
            monomials = listOf(
                CanonicalMonomial<Flt64>(Flt64(3.0), listOf(x, x, y)),
                CanonicalMonomial<Flt64>(Flt64(-2.0), listOf(z))
            ),
            constant = Flt64.one
        )

        val expr = polynomial.toExpr()
        val restored = expr.toCanonicalPolynomial { name ->
            symbolByName[name] ?: error("Unknown symbol: $name")
        }

        assertEquals(polynomial.combineTerms(), restored.combineTerms())
    }

    @Test
    fun canonicalInequalityShouldRoundTripThroughExprModel() {
        val x = TestSymbol("x")
        val y = TestSymbol("y")
        val symbolByName = mapOf(
            "x" to x,
            "y" to y
        )
        val inequality = CanonicalInequality(
            lhs = CanonicalPolynomial<Flt64>(
                monomials = listOf(
                    CanonicalMonomial<Flt64>(Flt64.one, listOf(x, x)),
                    CanonicalMonomial<Flt64>(Flt64(2.0), listOf(y))
                ),
                constant = Flt64(-1.0)
            ),
            rhs = CanonicalPolynomial<Flt64>(
                monomials = listOf(
                    CanonicalMonomial<Flt64>(Flt64(3.0), listOf(x))
                ),
                constant = Flt64.one
            ),
            comparison = Comparison.LE
        )

        val expr = inequality.toExpr()
        val restored = (expr as Expr.Comparison).toCanonicalInequality { name ->
            symbolByName[name] ?: error("Unknown symbol: $name")
        }

        assertEquals(
            inequality.copy(
                lhs = inequality.lhs.combineTerms(),
                rhs = inequality.rhs.combineTerms()
            ),
            restored
        )
    }
}



