package fuookami.ospf.kotlin.core.frontend.symbol_migration.adapter

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.math.symbol.*
import fuookami.ospf.kotlin.utils.math.symbol.operation.*
import fuookami.ospf.kotlin.core.frontend.variable.*
import fuookami.ospf.kotlin.core.frontend.expression.monomial.*
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.*
import fuookami.ospf.kotlin.core.frontend.expression.adapter.*
import fuookami.ospf.kotlin.core.frontend.inequality.*
import fuookami.ospf.kotlin.core.frontend.inequality.adapter.*
import fuookami.ospf.kotlin.utils.math.symbol.monomial.LinearMonomial as UtilsLinearMonomial
import fuookami.ospf.kotlin.utils.math.symbol.monomial.QuadraticMonomial as UtilsQuadraticMonomial

class AdapterRoundTripTest {
    private data class DummySymbol(
        override val name: String
    ) : Symbol {
        override val displayName: String? = null
    }

    @Test
    fun linearMonomialAdapterShouldRoundTrip() {
        val x = RealVar("x")
        val coreMonomial = LinearMonomial(Flt64.two, x)

        val utilsMonomial = coreMonomial.toUtilsMonomial()
        val roundTrip = utilsMonomial.toCoreMonomialRet()

        val converted = when (roundTrip) {
            is Ok -> {
                roundTrip.value
            }

            is Failed -> {
                error("linearMonomialAdapterShouldRoundTrip failed: ${roundTrip.error}")
            }
        }
        assertEquals(coreMonomial.coefficient, converted.coefficient)
        assertEquals(coreMonomial.symbol.variable, converted.symbol.variable)
    }

    @Test
    fun quadraticPolynomialAdapterShouldKeepEvaluation() {
        val x = RealVar("x")
        val y = RealVar("y")
        val corePolynomial = QuadraticPolynomial(
            monomials = listOf(
                QuadraticMonomial(Flt64.two, x, y),
                QuadraticMonomial(Flt64(3.0), x)
            ),
            constant = Flt64.one
        )
        val values = mapOf<Symbol, Flt64>(x to Flt64(2.0), y to Flt64(4.0))

        val utilsPolynomial = corePolynomial.toUtilsPolynomial()
        val roundTrip = utilsPolynomial.toCorePolynomialRet()

        val converted = when (roundTrip) {
            is Ok -> {
                roundTrip.value
            }

            is Failed -> {
                error("quadraticPolynomialAdapterShouldKeepEvaluation failed: ${roundTrip.error}")
            }
        }
        val originalValue = corePolynomial.evaluate(values, tokenList = null, zeroIfNone = false)
        val utilsValue = utilsPolynomial.evaluate(values, policy = fuookami.ospf.kotlin.utils.math.symbol.adapter.MissingValuePolicy.ReturnNull)
        val convertedValue = converted.evaluate(values, tokenList = null, zeroIfNone = false)
        assertEquals(originalValue, utilsValue)
        assertEquals(originalValue, convertedValue)
    }

    @Test
    fun inequalityAdapterShouldMapComparisonCorrectly() {
        val x = RealVar("x")
        val coreInequality = LinearInequality(
            lhs = LinearPolynomial(x),
            rhs = LinearPolynomial(Flt64.one),
            sign = Sign.GreaterEqual
        )
        val values = mapOf<Symbol, Flt64>(x to Flt64(2.0))

        val utilsInequality = coreInequality.toUtilsInequality()
        val roundTrip = utilsInequality.toCoreInequalityRet()

        assertEquals(fuookami.ospf.kotlin.utils.math.symbol.inequality.Comparison.GE, utilsInequality.comparison)

        val converted = when (roundTrip) {
            is Ok -> {
                roundTrip.value
            }

            is Failed -> {
                error("inequalityAdapterShouldMapComparisonCorrectly failed: ${roundTrip.error}")
            }
        }
        val originalTruth = coreInequality.isTrue(values, tokenList = null, zeroIfNone = false)
        val convertedTruth = converted.isTrue(values, tokenList = null, zeroIfNone = false)
        assertEquals(originalTruth, convertedTruth)
    }

    @Test
    fun linearMonomialAdapterShouldRejectUnsupportedSymbol() {
        val unsupportedMonomial = UtilsLinearMonomial(
            coefficient = Flt64.one,
            symbol = DummySymbol("dummy_linear")
        )

        val converted = unsupportedMonomial.toCoreMonomialRet()

        assertTrue(converted is Failed)
    }

    @Test
    fun quadraticMonomialAdapterShouldRejectUnsupportedSymbol() {
        val unsupportedMonomial = UtilsQuadraticMonomial(
            coefficient = Flt64.one,
            symbol1 = DummySymbol("dummy_quadratic"),
            symbol2 = null
        )

        val converted = unsupportedMonomial.toCoreMonomialRet()

        assertTrue(converted is Failed)
    }
}
