package fuookami.ospf.kotlin.math.symbol.roundtrip

import kotlin.test.assertEquals
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.math.symbol.monomial.*
import fuookami.ospf.kotlin.math.symbol.operation.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.math.symbol.Symbol

class SymbolRoundTripTest {
    private data class TestSymbol(
        override val name: String,
        override val displayName: String? = null
    ) : Symbol

    private val x = TestSymbol("x")
    private val y = TestSymbol("y")

    private fun assertLinearPolynomialEquals(expected: LinearPolynomial<Flt64>, actual: LinearPolynomial<Flt64>) {
        assertEquals(expected.monomials.size, actual.monomials.size)
        for (i in expected.monomials.indices) {
            assertEquals(expected.monomials[i].coefficient, actual.monomials[i].coefficient)
            assertEquals(expected.monomials[i].symbol.name, actual.monomials[i].symbol.name)
        }
        assertEquals(expected.constant, actual.constant)
    }

    private fun assertQuadraticPolynomialEquals(expected: QuadraticPolynomial<Flt64>, actual: QuadraticPolynomial<Flt64>) {
        assertEquals(expected.monomials.size, actual.monomials.size)
        for (i in expected.monomials.indices) {
            assertEquals(expected.monomials[i].coefficient, actual.monomials[i].coefficient)
            assertEquals(expected.monomials[i].symbol1.name, actual.monomials[i].symbol1.name)
            assertEquals(expected.monomials[i].symbol2?.name, actual.monomials[i].symbol2?.name)
        }
        assertEquals(expected.constant, actual.constant)
    }

    private fun assertCanonicalPolynomialEquals(expected: CanonicalPolynomial<Flt64>, actual: CanonicalPolynomial<Flt64>) {
        val e = expected.combineTerms()
        val a = actual.combineTerms()
        assertEquals(e.monomials.size, a.monomials.size)
        for (i in e.monomials.indices) {
            assertEquals(e.monomials[i].coefficient, a.monomials[i].coefficient)
            assertEquals(e.monomials[i].powers.mapKeys { it.key.name }, a.monomials[i].powers.mapKeys { it.key.name })
        }
        assertEquals(e.constant, a.constant)
    }

    @Test
    fun linearPolynomialJsonRoundTrip() {
        val original = LinearPolynomial<Flt64>(
            monomials = listOf(
                LinearMonomial(Flt64(2.5), x),
                LinearMonomial(Flt64(-1.0), y)
            ),
            constant = Flt64(10.0)
        )
        val json = original.toJsonString()
        val restored = linearPolynomialFromJson(json)!!
        assertLinearPolynomialEquals(original, restored)
    }

    @Test
    fun quadraticPolynomialJsonRoundTrip() {
        val original = QuadraticPolynomial<Flt64>(
            monomials = listOf(
                QuadraticMonomial(Flt64(1.0), x, x),
                QuadraticMonomial(Flt64(-0.5), x, y)
            ),
            constant = Flt64(0.0)
        )
        val json = original.toJsonString()
        val restored = quadraticPolynomialFromJson(json)!!
        assertQuadraticPolynomialEquals(original, restored)
    }

    @Test
    fun canonicalPolynomialJsonRoundTrip() {
        val original = CanonicalPolynomial<Flt64>(
            monomials = listOf(
                CanonicalMonomial(Flt64(3.0), mapOf(x to Int32(2), y to Int32(1)))
            ),
            constant = Flt64(-7.0)
        )
        val json = original.toJsonString()
        val restored = canonicalPolynomialFromJson(json)
        assertCanonicalPolynomialEquals(original, restored)
    }

    @Test
    fun linearInequalityJsonRoundTrip() {
        val original = LinearInequality(
            lhs = LinearPolynomial(listOf(LinearMonomial(Flt64(1.0), x)), Flt64(0.0)),
            rhs = LinearPolynomial(listOf(LinearMonomial(Flt64(1.0), y)), Flt64(5.0)),
            comparison = Comparison.LE
        )
        val json = original.toJsonString()
        val restored = linearInequalityFromJson(json).value!!
        assertLinearPolynomialEquals(original.lhs, restored.lhs)
        assertLinearPolynomialEquals(original.rhs, restored.rhs)
        assertEquals(original.comparison, restored.comparison)
    }

    @Test
    fun canonicalInequalityJsonRoundTrip() {
        val original = CanonicalInequality<Flt64>(
            lhs = CanonicalPolynomial(
                listOf(CanonicalMonomial(Flt64(1.0), mapOf(x to Int32(1)))),
                Flt64(0.0)
            ),
            rhs = CanonicalPolynomial(emptyList(), Flt64(10.0)),
            comparison = Comparison.GE
        )
        val json = original.toJsonString()
        val restored = canonicalInequalityFromJson(json).value!!
        assertCanonicalPolynomialEquals(original.lhs, restored.lhs)
        assertCanonicalPolynomialEquals(original.rhs, restored.rhs)
        assertEquals(original.comparison, restored.comparison)
    }

    @Test
    fun quadraticInequalityJsonRoundTrip() {
        val original = fuookami.ospf.kotlin.math.symbol.inequality.QuadraticInequalityOf(
            lhs = QuadraticPolynomial(
                listOf(QuadraticMonomial(Flt64(1.0), x, x)),
                Flt64(0.0)
            ),
            rhs = QuadraticPolynomial(emptyList(), Flt64(25.0)),
            comparison = Comparison.LE
        )
        val json = original.toJsonString()
        val restored = quadraticInequalityFromJson(json).value!!
        assertQuadraticPolynomialEquals(original.lhs, restored.lhs)
        assertQuadraticPolynomialEquals(original.rhs, restored.rhs)
        assertEquals(original.comparison, restored.comparison)
    }
}
