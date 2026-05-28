package fuookami.ospf.kotlin.math.symbol.operation

import kotlin.test.*
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.math.symbol.*
import fuookami.ospf.kotlin.math.symbol.monomial.*
import fuookami.ospf.kotlin.math.symbol.operation.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.math.algebra.number.*

class CombineTermsTest {
    private data class TestSymbol(
        override val name: String,
        override val displayName: String? = null
    ) : Symbol

    private data class TestIdentifiedSymbol(
        override val name: String,
        override val symbolId: String,
        override val displayName: String? = null
    ) : Symbol, IdentifiedSymbol

    @Test
    fun combineLinearMonomialsShouldMergeSameSymbolAndDropZero() {
        val x = TestSymbol("x")
        val y = TestSymbol("y")
        val merged = listOf(
            LinearMonomial<Flt64>(Flt64.two, x),
            LinearMonomial<Flt64>(Flt64(3.0), x),
            LinearMonomial<Flt64>(-Flt64.one, y),
            LinearMonomial<Flt64>(Flt64.one, y)
        ).combineTerms()

        assertEquals(1, merged.size)
        assertEquals(x, merged[0].symbol)
        assertTrue(merged[0].coefficient == Flt64(5.0))
    }

    @Test
    fun combineLinearPolynomialShouldKeepConstant() {
        val x = TestSymbol("x")
        val polynomial = LinearPolynomial<Flt64>(
            monomials = listOf(
                LinearMonomial<Flt64>(Flt64.two, x),
                LinearMonomial<Flt64>(-Flt64.one, x)
            ),
            constant = Flt64(7.0)
        )

        val combined = polynomial.combineTerms()

        assertEquals(1, combined.monomials.size)
        assertTrue(combined.monomials[0].coefficient == Flt64.one)
        assertTrue(combined.constant == Flt64(7.0))
    }

    @Test
    fun combineQuadraticMonomialsShouldNormalizeSymmetricTerms() {
        val x = TestSymbol("x")
        val y = TestSymbol("y")
        val merged = listOf(
            QuadraticMonomial<Flt64>(Flt64.two, x, y),
            QuadraticMonomial<Flt64>(Flt64(3.0), y, x),
            QuadraticMonomial<Flt64>(Flt64.one, x, null),
            QuadraticMonomial<Flt64>(-Flt64.one, x, null)
        ).combineTerms()

        assertEquals(1, merged.size)
        val only = merged.first()
        assertTrue(only.symbol1 == x || only.symbol1 == y)
        assertTrue(only.symbol2 == x || only.symbol2 == y)
        assertTrue(only.coefficient == Flt64(5.0))
    }

    @Test
    fun combineLinearTermsShouldBeConsistentBetweenFlt64AndRtn64() {
        val x = TestSymbol("x")
        val y = TestSymbol("y")

        val fltResult = listOf(
            LinearMonomial(Flt64.two, x),
            LinearMonomial(Flt64(3.0), x),
            LinearMonomial(-Flt64.one, y),
            LinearMonomial(Flt64.one, y)
        ).combineTerms()

        val rtnResult = LinearPolynomial(
            monomials = listOf(
                LinearMonomial(Rtn64(Int64(2L), Int64.one), x),
                LinearMonomial(Rtn64(Int64(3L), Int64.one), x),
                LinearMonomial(Rtn64(Int64(-1L), Int64.one), y),
                LinearMonomial(Rtn64(Int64.one, Int64.one), y)
            ),
            constant = Rtn64.zero
        ).combineLinearTerms(zero = Rtn64.zero)

        assertEquals(1, fltResult.size)
        assertEquals(1, rtnResult.monomials.size)
        assertEquals(fltResult[0].symbol, rtnResult.monomials[0].symbol)
        assertTrue(fltResult[0].coefficient == Flt64(5.0))
        assertEquals(Rtn64(Int64(5L), Int64.one), rtnResult.monomials[0].coefficient)
    }

    @Test
    fun combineQuadraticTermsShouldBeConsistentBetweenFlt64AndRtn64() {
        val x = TestSymbol("x")
        val y = TestSymbol("y")

        val fltResult = listOf(
            QuadraticMonomial(Flt64.two, x, y),
            QuadraticMonomial(Flt64(3.0), y, x),
            QuadraticMonomial(Flt64.one, x, null),
            QuadraticMonomial(-Flt64.one, x, null)
        ).combineTerms()

        val rtnResult = QuadraticPolynomial(
            monomials = listOf(
                QuadraticMonomial(Rtn64(Int64(2L), Int64.one), x, y),
                QuadraticMonomial(Rtn64(Int64(3L), Int64.one), y, x),
                QuadraticMonomial(Rtn64(Int64.one, Int64.one), x, null),
                QuadraticMonomial(Rtn64(Int64(-1L), Int64.one), x, null)
            ),
            constant = Rtn64.zero
        ).combineQuadraticTerms(zero = Rtn64.zero)

        assertEquals(1, fltResult.size)
        assertEquals(1, rtnResult.monomials.size)
        assertEquals(setOf(fltResult[0].symbol1, fltResult[0].symbol2), setOf(rtnResult.monomials[0].symbol1, rtnResult.monomials[0].symbol2))
        assertTrue(fltResult[0].coefficient == Flt64(5.0))
        assertEquals(Rtn64(Int64(5L), Int64.one), rtnResult.monomials[0].coefficient)
    }

    @Test
    fun combineCanonicalTermsShouldUseUnifiedSymbolIdentityOrdering() {
        val x2 = TestIdentifiedSymbol("x", "02")
        val x1 = TestIdentifiedSymbol("x", "01")
        val merged = listOf(
            fuookami.ospf.kotlin.math.symbol.monomial.CanonicalMonomial(Flt64.one, listOf(x2, x1))
        ).combineCanonicalTerms(defaultSymbolComparator)

        assertEquals(1, merged.size)
        assertEquals(listOf(x1, x2), merged.first().factors)
    }
}
