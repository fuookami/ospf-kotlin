package fuookami.ospf.kotlin.utils.math.symbol.polynomial

import fuookami.ospf.kotlin.utils.math.algebra.number.Flt64
import fuookami.ospf.kotlin.utils.math.symbol.Linear
import fuookami.ospf.kotlin.utils.math.symbol.Quadratic
import fuookami.ospf.kotlin.utils.math.symbol.Symbol
import fuookami.ospf.kotlin.utils.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.utils.math.symbol.monomial.QuadraticMonomial
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class PolynomialTest {
    private data class TestSymbol(
        override val name: String,
        override val displayName: String? = null
    ) : Symbol

    @Test
    fun linearPolynomialCategoryShouldBeLinear() {
        val x = TestSymbol("x")
        val polynomial = LinearPolynomial<Flt64>(
            monomials = listOf(LinearMonomial<Flt64>(Flt64.two, x)),
            constant = Flt64.one
        )

        assertEquals(Linear, polynomial.category)
    }

    @Test
    fun quadraticPolynomialCategoryShouldFollowMonomials() {
        val x = TestSymbol("x")
        val y = TestSymbol("y")

        val linearOnly = QuadraticPolynomial<Flt64>(
            monomials = listOf(QuadraticMonomial<Flt64>(coefficient = Flt64.one, symbol1 = x)),
            constant = Flt64.zero
        )
        assertEquals(Linear, linearOnly.category)

        val hasQuadraticTerm = QuadraticPolynomial<Flt64>(
            monomials = listOf(
                QuadraticMonomial<Flt64>(coefficient = Flt64.one, symbol1 = x),
                QuadraticMonomial<Flt64>(coefficient = Flt64.one, symbol1 = x, symbol2 = y)
            ),
            constant = Flt64.zero
        )
        assertEquals(Quadratic, hasQuadraticTerm.category)
    }
}



