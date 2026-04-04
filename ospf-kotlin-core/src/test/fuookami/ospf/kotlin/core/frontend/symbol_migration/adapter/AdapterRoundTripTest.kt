package fuookami.ospf.kotlin.core.frontend.symbol_migration.adapter

import fuookami.ospf.kotlin.core.frontend.expression.adapter.toCoreMonomialRet
import fuookami.ospf.kotlin.core.frontend.expression.adapter.toCorePolynomialRet
import fuookami.ospf.kotlin.core.frontend.expression.adapter.toUtilsMonomial
import fuookami.ospf.kotlin.core.frontend.expression.adapter.toUtilsPolynomial
import fuookami.ospf.kotlin.core.frontend.expression.monomial.LinearMonomial
import fuookami.ospf.kotlin.core.frontend.expression.monomial.QuadraticMonomial
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.QuadraticPolynomial
import fuookami.ospf.kotlin.core.frontend.inequality.LinearInequality
import fuookami.ospf.kotlin.core.frontend.inequality.Sign
import fuookami.ospf.kotlin.core.frontend.inequality.adapter.toCoreInequalityRet
import fuookami.ospf.kotlin.core.frontend.inequality.adapter.toUtilsInequality
import fuookami.ospf.kotlin.core.frontend.variable.RealVar
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Fatal
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.operation.evaluate
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial as UtilsLinearMonomial
import fuookami.ospf.kotlin.math.symbol.monomial.QuadraticMonomial as UtilsQuadraticMonomial

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

            is Fatal -> {
                error("linearMonomialAdapterShouldRoundTrip failed: ${roundTrip.errors}")
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

            is Fatal -> {
                error("quadraticPolynomialAdapterShouldKeepEvaluation failed: ${roundTrip.errors}")
            }
        }
        val originalValue = corePolynomial.evaluate(values, tokenList = null, zeroIfNone = false)
        val utilsValue = utilsPolynomial.evaluate(values, policy = fuookami.ospf.kotlin.math.symbol.adapter.MissingValuePolicy.ReturnNull)
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

        assertEquals(fuookami.ospf.kotlin.math.symbol.inequality.Comparison.GE, utilsInequality.comparison)

        val converted = when (roundTrip) {
            is Ok -> {
                roundTrip.value
            }

            is Failed -> {
                error("inequalityAdapterShouldMapComparisonCorrectly failed: ${roundTrip.error}")
            }

            is Fatal -> {
                error("inequalityAdapterShouldMapComparisonCorrectly failed: ${roundTrip.errors}")
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




