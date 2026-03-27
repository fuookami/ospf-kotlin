package fuookami.ospf.kotlin.utils.math.symbol.operation

import fuookami.ospf.kotlin.utils.math.Flt64
import fuookami.ospf.kotlin.utils.math.symbol.Symbol
import fuookami.ospf.kotlin.utils.math.symbol.inequality.CanonicalInequality
import fuookami.ospf.kotlin.utils.math.symbol.inequality.Comparison
import fuookami.ospf.kotlin.utils.math.symbol.inequality.LinearInequality
import fuookami.ospf.kotlin.utils.math.symbol.inequality.QuadraticInequality
import fuookami.ospf.kotlin.utils.math.symbol.monomial.CanonicalMonomial
import fuookami.ospf.kotlin.utils.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.utils.math.symbol.monomial.QuadraticMonomial
import fuookami.ospf.kotlin.utils.math.symbol.polynomial.CanonicalPolynomial
import fuookami.ospf.kotlin.utils.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.utils.math.symbol.polynomial.QuadraticPolynomial
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class LatexTest {
    private data class TestSymbol(
        override val name: String,
        override val displayName: String? = null
    ) : Symbol

    @Test
    fun linearPolynomialLatexShouldHandleSignsAndDisplayName() {
        val x = TestSymbol("x", "x_{1}")
        val y = TestSymbol("y")
        val polynomial = LinearPolynomial<Flt64>(
            monomials = listOf(
                LinearMonomial<Flt64>(Flt64.one, x),
                LinearMonomial<Flt64>(-Flt64.one, y)
            ),
            constant = Flt64(2.0)
        )

        assertEquals("x_{1}-y+2", polynomial.toLatex())
        assertEquals(
            "1 \\cdot x_{1} - 1 \\cdot y + 2",
            polynomial.toLatex(LatexOptions(compact = false, showOnes = true, useCdot = true))
        )
    }

    @Test
    fun quadraticPolynomialLatexShouldHandleSquareAndCrossTerms() {
        val x = TestSymbol("x")
        val y = TestSymbol("y")
        val polynomial = QuadraticPolynomial<Flt64>(
            monomials = listOf(
                QuadraticMonomial<Flt64>(Flt64(3.0), x, x),
                QuadraticMonomial<Flt64>(Flt64(-2.0), x, y),
                QuadraticMonomial<Flt64>(Flt64.one, y, null)
            ),
            constant = Flt64(-4.0)
        )

        assertEquals("3x^{2}-2xy+y-4", polynomial.toLatex())
        assertEquals(
            "3\\cdotx^{2}-2\\cdotx\\cdoty+y-4",
            polynomial.toLatex(LatexOptions(useCdot = true))
        )
    }

    @Test
    fun canonicalLatexShouldHandleRepeatedFactorsAsPowers() {
        val x = TestSymbol("x")
        val y = TestSymbol("y")
        val monomial = CanonicalMonomial<Flt64>(
            coefficient = Flt64(2.0),
            factors = listOf(x, x, y, y, y)
        )
        val polynomial = CanonicalPolynomial<Flt64>(
            monomials = listOf(
                monomial,
                CanonicalMonomial<Flt64>(Flt64(-1.0), listOf(y))
            ),
            constant = Flt64.one
        )

        assertEquals("2x^{2}y^{3}-y+1", polynomial.toLatex())
    }

    @Test
    fun inequalityLatexShouldUseComparisonSymbols() {
        val x = TestSymbol("x")
        val y = TestSymbol("y")
        val linear = LinearInequality(
            lhs = LinearPolynomial<Flt64>(
                monomials = listOf(LinearMonomial<Flt64>(Flt64.one, x)),
                constant = Flt64.one
            ),
            rhs = LinearPolynomial<Flt64>(
                monomials = listOf(LinearMonomial<Flt64>(Flt64.one, y))
            ),
            comparison = Comparison.LE
        )
        val quadratic = QuadraticInequality(
            lhs = QuadraticPolynomial<Flt64>(
                monomials = listOf(QuadraticMonomial<Flt64>(Flt64.one, x, x))
            ),
            rhs = QuadraticPolynomial<Flt64>(constant = Flt64.zero),
            comparison = Comparison.GE
        )
        val canonical = CanonicalInequality(
            lhs = CanonicalPolynomial<Flt64>(
                monomials = listOf(CanonicalMonomial<Flt64>(Flt64.one, listOf(x, x)))
            ),
            rhs = CanonicalPolynomial<Flt64>(constant = Flt64.zero),
            comparison = Comparison.NE
        )

        assertEquals("x+1 \\le y", linear.toLatex())
        assertEquals("x^{2} \\ge 0", quadratic.toLatex())
        assertEquals("x^{2} \\ne 0", canonical.toLatex())
    }

    @Test
    fun emptyPolynomialLatexShouldBeZero() {
        assertEquals("0", LinearPolynomial<Flt64>().toLatex())
        assertEquals("0", QuadraticPolynomial<Flt64>().toLatex())
        assertEquals("0", CanonicalPolynomial<Flt64>().toLatex())
    }
}
