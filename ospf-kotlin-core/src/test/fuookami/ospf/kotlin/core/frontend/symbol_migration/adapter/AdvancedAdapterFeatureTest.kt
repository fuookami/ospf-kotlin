package fuookami.ospf.kotlin.core.frontend.symbol_migration.adapter

import fuookami.ospf.kotlin.core.frontend.expression.adapter.compileEval
import fuookami.ospf.kotlin.core.frontend.expression.adapter.compileGradient
import fuookami.ospf.kotlin.core.frontend.expression.adapter.parseCoreQuadraticPolynomialRet
import fuookami.ospf.kotlin.core.frontend.expression.adapter.quadraticCorePolynomialFromExprJsonRet
import fuookami.ospf.kotlin.core.frontend.expression.adapter.toUtilsPolynomial
import fuookami.ospf.kotlin.core.frontend.expression.adapter.toSymbolExprJson
import fuookami.ospf.kotlin.core.frontend.expression.monomial.QuadraticMonomial
import fuookami.ospf.kotlin.core.frontend.expression.monomial.times
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.QuadraticPolynomial
import fuookami.ospf.kotlin.core.frontend.inequality.adapter.linearCoreInequalityFromExprJsonRet
import fuookami.ospf.kotlin.core.frontend.inequality.adapter.parseCoreLinearInequalityRet
import fuookami.ospf.kotlin.core.frontend.inequality.adapter.toLatex
import fuookami.ospf.kotlin.core.frontend.inequality.adapter.toSymbolExprJson
import fuookami.ospf.kotlin.core.frontend.variable.RealVar
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Fatal
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.operation.evaluateOrdered
import fuookami.ospf.kotlin.math.symbol.operation.gradient
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AdvancedAdapterFeatureTest {
    @Test
    fun compileAdaptersShouldMatchCoreEvaluateAndUtilsGradient() {
        val x = RealVar("x")
        val y = RealVar("y")
        val polynomial = QuadraticPolynomial(
            monomials = listOf(
                x * x,
                x * y,
                QuadraticMonomial(2 * x)
            ),
            constant = Flt64.one
        )
        val order = listOf<Symbol>(x, y)
        val values = listOf(Flt64(2.0), Flt64(3.0))
        val valueMap = mapOf<Symbol, Flt64>(
            x to Flt64(2.0),
            y to Flt64(3.0)
        )

        val compiledEval = polynomial.compileEval(order)
        val compiledGradient = polynomial.compileGradient(order)
        val expectedEval = polynomial.evaluate(valueMap, tokenList = null, zeroIfNone = false)
        val expectedGradient = polynomial.toUtilsPolynomial()
            .gradient(order)
            .map { it.evaluateOrdered(order, values) }

        assertNotNull(expectedEval)
        assertEquals(expectedEval, compiledEval(values))
        assertEquals(expectedGradient, compiledGradient(values))
    }

    @Test
    fun parserAndJsonAdaptersShouldRoundTripQuadraticPolynomial() {
        val x = RealVar("x")
        val y = RealVar("y")
        val symbolByName = mapOf(
            "x" to x,
            "y" to y
        )
        val parsed = parseCoreQuadraticPolynomialRet("x*x + 2*x*y + 1") { name ->
            symbolByName[name] ?: error("Unknown symbol: $name")
        }
        val corePolynomial = when (parsed) {
            is Ok -> parsed.value
            is Failed -> error("parseCoreQuadraticPolynomialRet failed: ${parsed.error}")
            is Fatal -> error("parseCoreQuadraticPolynomialRet failed: ${parsed.errors}")
        }
        val json = corePolynomial.toSymbolExprJson()
        val restored = quadraticCorePolynomialFromExprJsonRet(json) { name ->
            symbolByName[name] ?: error("Unknown symbol: $name")
        }
        val restoredPolynomial = when (restored) {
            is Ok -> restored.value
            is Failed -> error("quadraticCorePolynomialFromExprJsonRet failed: ${restored.error}")
            is Fatal -> error("quadraticCorePolynomialFromExprJsonRet failed: ${restored.errors}")
        }
        val values = mapOf<Symbol, Flt64>(
            x to Flt64(2.0),
            y to Flt64(4.0)
        )

        val originalValue = corePolynomial.evaluate(values, tokenList = null, zeroIfNone = false)
        val restoredValue = restoredPolynomial.evaluate(values, tokenList = null, zeroIfNone = false)

        assertEquals(originalValue, restoredValue)
    }

    @Test
    fun inequalityAdaptersShouldSupportParserLatexAndJsonRoundTrip() {
        val x = RealVar("x")
        val y = RealVar("y")
        val symbolByName = mapOf(
            "x" to x,
            "y" to y
        )
        val parsed = parseCoreLinearInequalityRet("x + 1 <= y") { name ->
            symbolByName[name] ?: error("Unknown symbol: $name")
        }
        val coreInequality = when (parsed) {
            is Ok -> parsed.value
            is Failed -> error("parseCoreLinearInequalityRet failed: ${parsed.error}")
            is Fatal -> error("parseCoreLinearInequalityRet failed: ${parsed.errors}")
        }
        val latex = coreInequality.toLatex()
        val json = coreInequality.toSymbolExprJson()
        val restored = linearCoreInequalityFromExprJsonRet(json) { name ->
            symbolByName[name] ?: error("Unknown symbol: $name")
        }
        val restoredInequality = when (restored) {
            is Ok -> restored.value
            is Failed -> error("linearCoreInequalityFromExprJsonRet failed: ${restored.error}")
            is Fatal -> error("linearCoreInequalityFromExprJsonRet failed: ${restored.errors}")
        }
        val values = mapOf<Symbol, Flt64>(
            x to Flt64(2.0),
            y to Flt64(5.0)
        )

        assertTrue(latex.contains("\\le"))
        assertEquals(
            coreInequality.isTrue(values, tokenList = null, zeroIfNone = false),
            restoredInequality.isTrue(values, tokenList = null, zeroIfNone = false)
        )
    }
}




