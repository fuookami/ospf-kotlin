package fuookami.ospf.kotlin.utils.math.symbol.polynomial

import org.junit.jupiter.api.*
import kotlin.test.assertEquals
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.math.symbol.*
import fuookami.ospf.kotlin.utils.math.symbol.monomial.*

class PolynomialTest {
    private data class TestSymbol(
        override val name: String,
        override val displayName: String? = null
    ) : Symbol

    @Test
    fun linearPolynomialCategoryShouldBeLinear() {
        val x = TestSymbol("x")
        val polynomial = LinearPolynomial(
            monomials = listOf(LinearMonomial(Flt64.two, x)),
            constant = Flt64.one
        )

        assertEquals(Linear, polynomial.category)
    }

    @Test
    fun quadraticPolynomialCategoryShouldFollowMonomials() {
        val x = TestSymbol("x")
        val y = TestSymbol("y")

        val linearOnly = QuadraticPolynomial(
            monomials = listOf(QuadraticMonomial(symbol1 = x)),
            constant = Flt64.zero
        )
        assertEquals(Linear, linearOnly.category)

        val hasQuadraticTerm = QuadraticPolynomial(
            monomials = listOf(
                QuadraticMonomial(symbol1 = x),
                QuadraticMonomial(symbol1 = x, symbol2 = y)
            ),
            constant = Flt64.zero
        )
        assertEquals(Quadratic, hasQuadraticTerm.category)
    }
}
