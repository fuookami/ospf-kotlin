package fuookami.ospf.kotlin.example

import fuookami.ospf.kotlin.example.test.flt64TestConverter
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial

import fuookami.ospf.kotlin.core.symbol.function.MaskingFunction
import fuookami.ospf.kotlin.core.symbol.function.MaxFunction
import fuookami.ospf.kotlin.core.variable.BinVar
import fuookami.ospf.kotlin.core.variable.RealVar

/** Smoke test for heuristic-oriented max and masking function evaluation. */
class HeuristicDemoTest {
    @Test
    fun smoke() {
        val x = RealVar("heuristic_demo_x")
        val y = RealVar("heuristic_demo_y")
        val z = BinVar("heuristic_demo_mask")

        val xPoly = LinearPolynomial(
            monomials = listOf(LinearMonomial(Flt64.one, x)),
            constant = Flt64.zero
        )
        val yPoly = LinearPolynomial(
            monomials = listOf(LinearMonomial(Flt64.one, y)),
            constant = Flt64.zero
        )

        val maxFunction = MaxFunction(
            polynomials = listOf(xPoly, yPoly),
            converter = flt64TestConverter,
            name = "heuristic_max"
        )
        val masking = MaskingFunction(
            input = xPoly,
            mask = z,
            converter = flt64TestConverter,
            name = "heuristic_mask"
        )

        val maxValue = maxFunction.evaluate(mapOf<Symbol, Flt64>(x to Flt64(2.0), y to Flt64(5.0)))
        val maskedValue = masking.evaluate(mapOf<Symbol, Flt64>(x to Flt64(2.0), z to Flt64.one))

        assertNotNull(maxValue)
        assertNotNull(maskedValue)
        assertEquals(Flt64(5.0), maxValue)
        assertEquals(Flt64(2.0), maskedValue)
    }
}
