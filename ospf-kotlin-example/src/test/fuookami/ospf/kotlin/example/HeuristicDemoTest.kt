package fuookami.ospf.kotlin.example

import fuookami.ospf.kotlin.core.intermediate_symbol.function.MaskingFunction
import fuookami.ospf.kotlin.core.intermediate_symbol.function.MaxFunction
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.variable.BinVar
import fuookami.ospf.kotlin.core.variable.RealVar
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class HeuristicDemoTest {
    private val flt64Converter = object : IntoValue<Flt64> {
        override fun intoValue(value: Flt64) = value
        override val zero get() = Flt64.zero
        override val one get() = Flt64.one
        override fun fromValue(value: Flt64) = value
    }

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
            converter = flt64Converter,
            name = "heuristic_max"
        )
        val masking = MaskingFunction(
            input = xPoly,
            mask = z,
            converter = flt64Converter,
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