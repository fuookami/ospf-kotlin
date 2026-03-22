package fuookami.ospf.kotlin.utils.math.symbol.operation

import fuookami.ospf.kotlin.utils.math.Flt64
import fuookami.ospf.kotlin.utils.math.symbol.Symbol
import fuookami.ospf.kotlin.utils.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.utils.math.symbol.monomial.QuadraticMonomial
import fuookami.ospf.kotlin.utils.math.symbol.polynomial.LinearPolynomial
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CombineTermsTest {
    private data class TestSymbol(
        override val name: String,
        override val displayName: String? = null
    ) : Symbol

    @Test
    fun combineLinearMonomialsShouldMergeSameSymbolAndDropZero() {
        val x = TestSymbol("x")
        val y = TestSymbol("y")
        val merged = listOf(
            LinearMonomial(Flt64.two, x),
            LinearMonomial(Flt64(3.0), x),
            LinearMonomial(-Flt64.one, y),
            LinearMonomial(Flt64.one, y)
        ).combineTerms()

        assertEquals(1, merged.size)
        assertEquals(x, merged[0].symbol)
        assertTrue(merged[0].coefficient == Flt64(5.0))
    }

    @Test
    fun combineLinearPolynomialShouldKeepConstant() {
        val x = TestSymbol("x")
        val polynomial = LinearPolynomial(
            monomials = listOf(
                LinearMonomial(Flt64.two, x),
                LinearMonomial(-Flt64.one, x)
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
            QuadraticMonomial(Flt64.two, x, y),
            QuadraticMonomial(Flt64(3.0), y, x),
            QuadraticMonomial(Flt64.one, x, null),
            QuadraticMonomial(-Flt64.one, x, null)
        ).combineTerms()

        assertEquals(1, merged.size)
        val only = merged.first()
        assertTrue(only.symbol1 == x || only.symbol1 == y)
        assertTrue(only.symbol2 == x || only.symbol2 == y)
        assertTrue(only.coefficient == Flt64(5.0))
    }
}
