package fuookami.ospf.kotlin.utils.math.symbol.operation

import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Fatal
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.math.Flt64
import fuookami.ospf.kotlin.utils.math.symbol.Symbol
import fuookami.ospf.kotlin.utils.math.symbol.adapter.MissingValuePolicy
import fuookami.ospf.kotlin.utils.math.symbol.monomial.CanonicalMonomial
import fuookami.ospf.kotlin.utils.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.utils.math.symbol.monomial.QuadraticMonomial
import fuookami.ospf.kotlin.utils.math.symbol.polynomial.CanonicalPolynomial
import fuookami.ospf.kotlin.utils.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.utils.math.symbol.polynomial.QuadraticPolynomial
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CanonicalOperationTest {
    private data class TestSymbol(
        override val name: String,
        override val displayName: String? = null
    ) : Symbol

    @Test
    fun combineCanonicalMonomialsShouldNormalizeAndMergeEquivalentFactors() {
        val x = TestSymbol("x")
        val y = TestSymbol("y")
        val merged = listOf(
            CanonicalMonomial<Flt64>(Flt64.two, listOf(x, y)),
            CanonicalMonomial<Flt64>(Flt64(3.0), listOf(y, x)),
            CanonicalMonomial<Flt64>(Flt64.one, listOf(x)),
            CanonicalMonomial<Flt64>(-Flt64.one, listOf(x))
        ).combineCanonicalTerms()

        assertEquals(1, merged.size)
        val only = merged.first()
        assertEquals(Flt64(5.0), only.coefficient)
        assertEquals(listOf(x, y), only.factors)
    }

    @Test
    fun canonicalPolynomialEvaluateShouldSupportMissingPolicies() {
        val x = TestSymbol("x")
        val y = TestSymbol("y")
        val polynomial = CanonicalPolynomial<Flt64>(
            monomials = listOf(
                CanonicalMonomial<Flt64>(Flt64.two, listOf(x, y)),
                CanonicalMonomial<Flt64>(Flt64(3.0), listOf(x))
            ),
            constant = Flt64.one
        )

        val returnNull = polynomial.evaluate(
            values = mapOf<Symbol, Flt64>(x to Flt64(2.0)),
            policy = MissingValuePolicy.ReturnNull
        )
        val asZero = polynomial.evaluate(
            values = mapOf<Symbol, Flt64>(x to Flt64(2.0)),
            policy = MissingValuePolicy.AsZero
        )

        assertNull(returnNull)
        assertEquals(Flt64(7.0), asZero)
    }

    @Test
    fun linearAndQuadraticPolynomialShouldRoundTripThroughCanonical() {
        val x = TestSymbol("x")
        val y = TestSymbol("y")

        val linear = LinearPolynomial<Flt64>(
            monomials = listOf(
                LinearMonomial<Flt64>(Flt64.two, x),
                LinearMonomial<Flt64>(Flt64(-1.0), y)
            ),
            constant = Flt64(3.0)
        )
        val linearRoundTrip = linear
            .toCanonicalPolynomial()
            .toLinearPolynomialRet()
        val linearRoundTripValue = when (linearRoundTrip) {
            is Ok -> {
                linearRoundTrip.value
            }

            is Failed -> {
                error("linear round trip failed: ${linearRoundTrip.error}")
            }

            is Fatal -> {
                error("linear round trip fatal: ${linearRoundTrip.errors}")
            }
        }
        assertEquals(linear.combineTerms(), linearRoundTripValue)

        val quadratic = QuadraticPolynomial<Flt64>(
            monomials = listOf(
                QuadraticMonomial<Flt64>(Flt64.two, x, y),
                QuadraticMonomial<Flt64>(Flt64(3.0), y, x),
                QuadraticMonomial<Flt64>(Flt64.one, x, x)
            ),
            constant = Flt64(4.0)
        )
        val quadraticRoundTrip = quadratic
            .toCanonicalPolynomial()
            .toQuadraticPolynomialRet()
        val quadraticRoundTripValue = when (quadraticRoundTrip) {
            is Ok -> {
                quadraticRoundTrip.value
            }

            is Failed -> {
                error("quadratic round trip failed: ${quadraticRoundTrip.error}")
            }

            is Fatal -> {
                error("quadratic round trip fatal: ${quadraticRoundTrip.errors}")
            }
        }
        assertEquals(quadratic.combineTerms(), quadraticRoundTripValue)
    }

    @Test
    fun canonicalPolynomialWithHigherDegreeTermShouldNotConvertToQuadratic() {
        val x = TestSymbol("x")
        val y = TestSymbol("y")
        val z = TestSymbol("z")
        val polynomial = CanonicalPolynomial<Flt64>(
            monomials = listOf(
                CanonicalMonomial<Flt64>(Flt64.one, listOf(x, y, z))
            ),
            constant = Flt64.zero
        )

        assertNull(polynomial.toQuadraticPolynomialOrNull())
        val ret = polynomial.toQuadraticPolynomialRet()
        assertTrue(ret is Failed)
    }

    @Test
    fun canonicalDegreeZeroMonomialShouldFoldIntoPolynomialConstant() {
        val x = TestSymbol("x")
        val polynomial = CanonicalPolynomial<Flt64>(
            monomials = listOf(
                CanonicalMonomial<Flt64>(Flt64(2.0), emptyList()),
                CanonicalMonomial<Flt64>(Flt64(3.0), listOf(x))
            ),
            constant = Flt64.one
        )

        val linear = polynomial.toLinearPolynomialOrNull()
        assertTrue(linear != null)
        assertEquals(Flt64(3.0), linear.constant)
        assertEquals(1, linear.monomials.size)
        assertEquals(Flt64(3.0), linear.monomials.first().coefficient)
        assertEquals(x, linear.monomials.first().symbol)
    }
}
