package fuookami.ospf.kotlin.math.symbol.operation

import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.operation.toCanonicalInequality
import fuookami.ospf.kotlin.math.symbol.operation.toLinearInequalityOrNull
import fuookami.ospf.kotlin.math.symbol.operation.toLinearPolynomialRet
import fuookami.ospf.kotlin.math.symbol.operation.toQuadraticPolynomialRet
import fuookami.ospf.kotlin.math.symbol.operation.moveAllToLhs
import fuookami.ospf.kotlin.math.symbol.operation.normalizeToLessEqualForm
import fuookami.ospf.kotlin.math.symbol.inequality.Comparison
import fuookami.ospf.kotlin.math.symbol.inequality.CanonicalInequality
import fuookami.ospf.kotlin.math.symbol.inequality.LinearInequality
import fuookami.ospf.kotlin.math.symbol.monomial.CanonicalMonomial
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.monomial.QuadraticMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.CanonicalPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.QuadraticPolynomial
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ConvertTest {
    private data class TestSymbol(
        override val name: String,
        override val displayName: String? = null
    ) : Symbol

    @Test
    fun moveAllToLhsShouldGenerateZeroRightHandSide() {
        val x = TestSymbol("x")
        val y = TestSymbol("y")
        val inequality = LinearInequality(
            lhs = LinearPolynomial<Flt64>(
                monomials = listOf(LinearMonomial<Flt64>(Flt64.two, x)),
                constant = Flt64.one
            ),
            rhs = LinearPolynomial<Flt64>(
                monomials = listOf(LinearMonomial<Flt64>(Flt64.one, y)),
                constant = Flt64(-2.0)
            ),
            comparison = Comparison.LE
        )

        val moved = inequality.moveAllToLhs()
        val coefficients = moved.lhs.monomials.associate { it.symbol to it.coefficient }

        assertEquals(Comparison.LE, moved.comparison)
        assertTrue(moved.rhs.constant == Flt64.zero)
        assertTrue(coefficients[x] == Flt64.two)
        assertTrue(coefficients[y] == Flt64(-1.0))
        assertTrue(moved.lhs.constant == Flt64(3.0))
    }

    @Test
    fun normalizeToLessEqualFormShouldReverseGreaterSide() {
        val x = TestSymbol("x")
        val y = TestSymbol("y")
        val inequality = LinearInequality(
            lhs = LinearPolynomial<Flt64>(
                monomials = listOf(LinearMonomial<Flt64>(Flt64.two, x)),
                constant = Flt64.one
            ),
            rhs = LinearPolynomial<Flt64>(
                monomials = listOf(LinearMonomial<Flt64>(Flt64.one, y)),
                constant = Flt64(-2.0)
            ),
            comparison = Comparison.GE
        )

        val normalized = inequality.normalizeToLessEqualForm()
        val coefficients = normalized.lhs.monomials.associate { it.symbol to it.coefficient }

        assertEquals(Comparison.LE, normalized.comparison)
        assertTrue(normalized.rhs.constant == Flt64.zero)
        assertTrue(coefficients[x] == Flt64(-2.0))
        assertTrue(coefficients[y] == Flt64.one)
        assertTrue(normalized.lhs.constant == Flt64(-3.0))
    }

    @Test
    fun linearPolynomialShouldConvertToQuadraticAndBack() {
        val x = TestSymbol("x")
        val y = TestSymbol("y")
        val linear = LinearPolynomial<Flt64>(
            monomials = listOf(
                LinearMonomial<Flt64>(Flt64.two, x),
                LinearMonomial<Flt64>(Flt64(-1.0), y)
            ),
            constant = Flt64(5.0)
        )

        val quadratic = linear.toQuadraticPolynomial()
        val convertedBack = quadratic.toLinearPolynomialOrNull()

        assertNotNull(convertedBack)
        assertEquals(linear.constant, convertedBack.constant)
        assertEquals(linear.monomials.toSet(), convertedBack.monomials.toSet())
    }

    @Test
    fun quadraticPolynomialWithQuadraticTermShouldNotConvertToLinear() {
        val x = TestSymbol("x")
        val y = TestSymbol("y")
        val quadratic = QuadraticPolynomial<Flt64>(
            monomials = listOf(
                QuadraticMonomial<Flt64>(Flt64.one, x, y)
            ),
            constant = Flt64.zero
        )

        assertNull(quadratic.toLinearPolynomialOrNull())
        val ret = quadratic.toLinearPolynomialRet()
        assertTrue(ret is Failed)
    }

    @Test
    fun normalizeToLessEqualFormShouldKeepNeUnchanged() {
        val x = TestSymbol("x")
        val inequality = LinearInequality(
            lhs = LinearPolynomial<Flt64>(
                monomials = listOf(LinearMonomial<Flt64>(Flt64.one, x)),
                constant = Flt64.one
            ),
            rhs = LinearPolynomial<Flt64>(constant = Flt64.zero),
            comparison = Comparison.NE
        )

        val normalized = inequality.normalizeToLessEqualForm()

        assertEquals(inequality, normalized)
    }

    @Test
    fun canonicalInequalityShouldSupportConversionAndNormalization() {
        val x = TestSymbol("x")
        val y = TestSymbol("y")
        val linear = LinearInequality(
            lhs = LinearPolynomial<Flt64>(
                monomials = listOf(
                    LinearMonomial<Flt64>(Flt64.two, x),
                    LinearMonomial<Flt64>(Flt64.one, y)
                ),
                constant = Flt64.one
            ),
            rhs = LinearPolynomial<Flt64>(
                monomials = listOf(LinearMonomial<Flt64>(Flt64(3.0), x)),
                constant = Flt64(4.0)
            ),
            comparison = Comparison.GE
        )

        val canonical = linear.toCanonicalInequality()
        val normalized = canonical.normalizeToLessEqualForm()

        assertEquals(Comparison.LE, normalized.comparison)
        assertEquals(Flt64.zero, normalized.rhs.constant)
        val roundTripLinear = normalized.toLinearInequalityOrNull()
        assertNotNull(roundTripLinear)
        assertEquals(Comparison.LE, roundTripLinear.comparison)
    }

    @Test
    fun canonicalInequalityNeShouldKeepOriginalForm() {
        val x = TestSymbol("x")
        val inequality = CanonicalInequality<Flt64>(
            lhs = CanonicalPolynomial<Flt64>(
                monomials = listOf(CanonicalMonomial<Flt64>(Flt64.one, listOf(x))),
                constant = Flt64.one
            ),
            rhs = CanonicalPolynomial<Flt64>(constant = Flt64.zero),
            comparison = Comparison.NE
        )

        val normalized = inequality.normalizeToLessEqualForm()

        assertEquals(inequality, normalized)
    }
}


