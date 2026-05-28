package fuookami.ospf.kotlin.math.symbol.monomial

import kotlin.test.*
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.math.symbol.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64

class MonomialTest {
    private data class TestSymbol(
        override val name: String,
        override val displayName: String? = null
    ) : Symbol

    @Test
    fun linearMonomialShouldKeepArguments() {
        val x = TestSymbol("x")
        val monomial = LinearMonomial<Flt64>(coefficient = Flt64.one, symbol = x)

        assertEquals(x, monomial.symbol)
        assertTrue(monomial.coefficient == Flt64.one)
    }

    @Test
    fun quadraticMonomialFlagAndCategory() {
        val x = TestSymbol("x")
        val y = TestSymbol("y")

        val linearLike = QuadraticMonomial<Flt64>(coefficient = Flt64.one, symbol1 = x)
        assertFalse(linearLike.isQuadratic)
        assertEquals(Linear, linearLike.category)

        val quadratic = QuadraticMonomial<Flt64>(coefficient = Flt64.two, symbol1 = x, symbol2 = y)
        assertTrue(quadratic.isQuadratic)
        assertEquals(Quadratic, quadratic.category)
        assertTrue(quadratic.coefficient == Flt64.two)
    }

    @Test
    fun linearMonomialOperatorsShouldComposeToPolynomial() {
        val x = TestSymbol("x")
        val y = TestSymbol("y")
        val mx = LinearMonomial<Flt64>(Flt64.two, x)
        val my = LinearMonomial<Flt64>(Flt64.one, y)

        val linearSum = mx + my
        assertEquals(2, linearSum.monomials.size)
        assertEquals(Flt64.zero, linearSum.constant)

        val linearDiff = mx - my
        assertEquals(2, linearDiff.monomials.size)
        assertEquals(Flt64.zero, linearDiff.constant)

        val withConstant = mx + Flt64.one
        assertEquals(1, withConstant.monomials.size)
        assertEquals(Flt64.one, withConstant.constant)

        val quad = mx * my
        assertTrue(quad.isQuadratic)
        assertEquals(Flt64.two, quad.coefficient)
    }

    @Test
    fun quadraticMonomialOperatorsShouldSupportLinearAndConstant() {
        val x = TestSymbol("x")
        val y = TestSymbol("y")
        val q = QuadraticMonomial<Flt64>(Flt64(3.0), x, y)
        val l = LinearMonomial<Flt64>(Flt64.two, x)

        val poly1: QuadraticPolynomial<Flt64> = q + l
        assertEquals(2, poly1.monomials.size)
        assertEquals(Flt64.zero, poly1.constant)

        val poly2: QuadraticPolynomial<Flt64> = q + Flt64.one
        assertEquals(1, poly2.monomials.size)
        assertEquals(Flt64.one, poly2.constant)

        val lp = LinearPolynomial<Flt64>(listOf(l), Flt64.one)
        val poly3: QuadraticPolynomial<Flt64> = q - lp
        assertEquals(2, poly3.monomials.size)
        assertEquals(Flt64(-1.0), poly3.constant)
    }
}
