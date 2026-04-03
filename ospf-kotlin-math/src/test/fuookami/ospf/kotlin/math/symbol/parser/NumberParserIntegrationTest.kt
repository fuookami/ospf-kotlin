package fuookami.ospf.kotlin.math.symbol.parser

import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.Int32
import fuookami.ospf.kotlin.math.algebra.number.Int64
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.operation.combineTerms
import fuookami.ospf.kotlin.math.symbol.serde.toCanonicalPolynomial
import fuookami.ospf.kotlin.math.symbol.serde.toCanonicalPolynomialTyped
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class NumberParserIntegrationTest {
    private data class TestSymbol(
        override val name: String,
        override val displayName: String? = null
    ) : Symbol

    @Test
    fun int64NumberParserShouldBuildCanonicalPolynomialTyped() {
        val x = TestSymbol("x")
        val expr = parseSymbolExpression("2*x + 3")

        val polynomial = expr.toCanonicalPolynomialTyped(
            numberParser = Int64NumberParser,
            zero = Int64.zero,
            one = Int64.one,
            symbolOf = { symbolName ->
                when (symbolName) {
                    "x" -> x
                    else -> error("Unknown symbol: $symbolName")
                }
            }
        )

        assertEquals(Int64(3L), polynomial.constant)
        assertEquals(1, polynomial.monomials.size)
        assertEquals(Int64(2L), polynomial.monomials.first().coefficient)
        assertEquals(mapOf<Symbol, Int32>(x to Int32.one), polynomial.monomials.first().powers)
    }

    @Test
    fun int64NumberParserShouldRejectDecimalLiteral() {
        val expr = parseSymbolExpression("1.5*x + 2")

        assertFailsWith<IllegalArgumentException> {
            expr.toCanonicalPolynomialTyped(
                numberParser = Int64NumberParser,
                zero = Int64.zero,
                one = Int64.one
            )
        }
    }

    @Test
    fun flt64DefaultPathShouldMatchTypedNumberParserPath() {
        val x = TestSymbol("x")
        val y = TestSymbol("y")
        val expr = parseSymbolExpression("x^2 + 2*x*y - 3")
        val symbolOf: (String) -> Symbol = { symbolName ->
            when (symbolName) {
                "x" -> x
                "y" -> y
                else -> error("Unknown symbol: $symbolName")
            }
        }

        val direct = expr.toCanonicalPolynomial(symbolOf).combineTerms()
        val typed = expr.toCanonicalPolynomialTyped(
            numberParser = Flt64NumberParser,
            zero = Flt64.zero,
            one = Flt64.one,
            symbolOf = symbolOf
        ).combineTerms()

        assertEquals(direct, typed)
    }
}