package fuookami.ospf.kotlin.math.symbol.polynomial

import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.Linear
import fuookami.ospf.kotlin.math.symbol.Quadratic
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.monomial.QuadraticMonomial
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

    @Test
    fun linearPolynomialOperatorsShouldWork() {
        val x = TestSymbol("x")
        val y = TestSymbol("y")
        val p1 = LinearPolynomial(
            monomials = listOf(LinearMonomial<Flt64>(Flt64.two, x)),
            constant = Flt64.one
        )
        val p2 = LinearPolynomial(
            monomials = listOf(LinearMonomial<Flt64>(Flt64(3.0), y)),
            constant = Flt64(2.0)
        )

        val sum = p1 + p2
        assertEquals(2, sum.monomials.size)
        assertEquals(Flt64(3.0), sum.constant)

        val scaled = p1 * Flt64(2.0)
        assertEquals(1, scaled.monomials.size)
        assertEquals(Flt64(2.0), scaled.constant)
    }

    @Test
    fun quadraticPolynomialOperatorsShouldSupportLinearAndConstant() {
        val x = TestSymbol("x")
        val y = TestSymbol("y")
        val q = QuadraticPolynomial(
            monomials = listOf(QuadraticMonomial<Flt64>(Flt64.two, x, y)),
            constant = Flt64.one
        )
        val l = LinearPolynomial(
            monomials = listOf(LinearMonomial<Flt64>(Flt64(3.0), x)),
            constant = Flt64(2.0)
        )

        val sum = q + l
        assertEquals(2, sum.monomials.size)
        assertEquals(Flt64(3.0), sum.constant)

        val reversed = l + q
        assertEquals(2, reversed.monomials.size)
        assertEquals(Flt64(3.0), reversed.constant)

        val minusConst = q - Flt64.one
        assertEquals(Flt64.zero, minusConst.constant)
    }

    @Test
    fun sumAggregationShouldWork() {
        val x = TestSymbol("x")
        val y = TestSymbol("y")
        val monomials = listOf(LinearMonomial(Flt64.one, x), LinearMonomial(Flt64.two, y))
        val result = sum(monomials)
        assertEquals(2, result.monomials.size)
        assertEquals(Flt64.zero, result.constant)
    }

    @Test
    fun qsumAggregationShouldWork() {
        val x = TestSymbol("x")
        val y = TestSymbol("y")
        val monomials = listOf(
            QuadraticMonomial.quadratic(Flt64.one, x, y),
            QuadraticMonomial.linear(Flt64.two, x)
        )
        val result = qsum(monomials)
        assertEquals(2, result.monomials.size)
        assertEquals(Flt64.zero, result.constant)
    }
}


