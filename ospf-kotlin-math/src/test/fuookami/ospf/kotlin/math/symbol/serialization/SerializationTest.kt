package fuookami.ospf.kotlin.math.symbol.serialization

import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.Int32
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.inequality.CanonicalInequality
import fuookami.ospf.kotlin.math.symbol.inequality.Comparison
import fuookami.ospf.kotlin.math.symbol.inequality.LinearInequality
import fuookami.ospf.kotlin.math.symbol.monomial.CanonicalMonomial
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.monomial.QuadraticMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.CanonicalPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.QuadraticPolynomial
import fuookami.ospf.kotlin.math.symbol.operation.canonicalInequalityFromJson
import fuookami.ospf.kotlin.math.symbol.operation.canonicalPolynomialFromJson
import fuookami.ospf.kotlin.math.symbol.operation.linearInequalityFromJson
import fuookami.ospf.kotlin.math.symbol.operation.linearPolynomialFromJson
import fuookami.ospf.kotlin.math.symbol.operation.quadraticInequalityFromJson
import fuookami.ospf.kotlin.math.symbol.operation.quadraticPolynomialFromJson
import fuookami.ospf.kotlin.math.symbol.operation.toJsonString
import fuookami.ospf.kotlin.math.symbol.operation.combineTerms
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class SerializationTest {
    private data class TestSymbol(
        override val name: String,
        override val displayName: String? = null
    ) : Symbol

    private val x = TestSymbol("x")
    private val y = TestSymbol("y")
    private val z = TestSymbol("z")

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
    fun linearPolynomialRoundTrip() {
        val lp = LinearPolynomial<Flt64>(
            monomials = listOf(
                LinearMonomial(Flt64(3.0), x),
                LinearMonomial(Flt64(-1.0), y)
            ),
            constant = Flt64(5.0)
        )
        val json = lp.toJsonString()
        val restored = linearPolynomialFromJson(json)!!
        assertLinearPolynomialEquals(lp, restored)
    }

    @Test
    fun quadraticPolynomialRoundTrip() {
        val qp = QuadraticPolynomial<Flt64>(
            monomials = listOf(
                QuadraticMonomial(Flt64(2.0), x, x),
                QuadraticMonomial(Flt64(1.5), x, y)
            ),
            constant = Flt64(-3.0)
        )
        val json = qp.toJsonString()
        val restored = quadraticPolynomialFromJson(json)!!
        assertQuadraticPolynomialEquals(qp, restored)
    }

    @Test
    fun canonicalPolynomialRoundTrip() {
        val cp = CanonicalPolynomial<Flt64>(
            monomials = listOf(
                CanonicalMonomial(Flt64(2.0), mapOf(x to Int32(2))),
                CanonicalMonomial(Flt64(3.0), mapOf(x to Int32(1), y to Int32(1)))
            ),
            constant = Flt64(1.0)
        )
        val json = cp.toJsonString()
        val restored = canonicalPolynomialFromJson(json)
        assertCanonicalPolynomialEquals(cp, restored)
    }

    @Test
    fun linearInequalityRoundTrip() {
        val li = LinearInequality(
            lhs = LinearPolynomial(listOf(LinearMonomial(Flt64(1.0), x)), Flt64(0.0)),
            rhs = LinearPolynomial(listOf(LinearMonomial(Flt64(2.0), y)), Flt64(3.0)),
            comparison = Comparison.LE,
            name = "test_ineq",
            displayName = "Test Inequality"
        )
        val json = li.toJsonString()
        val restored = linearInequalityFromJson(json)
        assertLinearPolynomialEquals(li.lhs, restored.lhs)
        assertLinearPolynomialEquals(li.rhs, restored.rhs)
        assertEquals(li.comparison, restored.comparison)
        assertEquals(li.name, restored.name)
        assertEquals(li.displayName, restored.displayName)
    }

    @Test
    fun canonicalInequalityRoundTrip() {
        val ci = CanonicalInequality<Flt64>(
            lhs = CanonicalPolynomial(
                listOf(CanonicalMonomial(Flt64(1.0), mapOf(x to Int32(1)))),
                Flt64(0.0)
            ),
            rhs = CanonicalPolynomial(
                listOf(CanonicalMonomial(Flt64(2.0), mapOf(y to Int32(1)))),
                Flt64(5.0)
            ),
            comparison = Comparison.LE
        )
        val json = ci.toJsonString()
        val restored = canonicalInequalityFromJson(json)
        assertCanonicalPolynomialEquals(ci.lhs, restored.lhs)
        assertCanonicalPolynomialEquals(ci.rhs, restored.rhs)
        assertEquals(ci.comparison, restored.comparison)
    }

    @Test
    fun quadraticInequalityRoundTrip() {
        val qi = fuookami.ospf.kotlin.math.symbol.inequality.QuadraticInequalityOf(
            lhs = QuadraticPolynomial(
                listOf(QuadraticMonomial(Flt64(1.0), x, x)),
                Flt64(0.0)
            ),
            rhs = QuadraticPolynomial(
                listOf(QuadraticMonomial(Flt64(2.0), y, y)),
                Flt64(3.0)
            ),
            comparison = Comparison.LE,
            name = "test_qineq",
            displayName = "Test Quadratic Inequality"
        )
        val json = qi.toJsonString()
        val restored = quadraticInequalityFromJson(json)
        assertQuadraticPolynomialEquals(qi.lhs, restored.lhs)
        assertQuadraticPolynomialEquals(qi.rhs, restored.rhs)
        assertEquals(qi.comparison, restored.comparison)
        assertEquals(qi.name, restored.name)
        assertEquals(qi.displayName, restored.displayName)
    }
}