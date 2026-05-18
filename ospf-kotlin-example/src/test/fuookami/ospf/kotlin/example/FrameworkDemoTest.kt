package fuookami.ospf.kotlin.example

import fuookami.ospf.kotlin.core.intermediate_symbol.function.IfFunction
import fuookami.ospf.kotlin.core.intermediate_symbol.function.SlackRangeFunction
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.variable.RealVar
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class FrameworkDemoTest {
    private val flt64Converter = object : IntoValue<Flt64> {
        override fun intoValue(value: Flt64) = value
        override val zero get() = Flt64.zero
        override val one get() = Flt64.one
        override fun fromValue(value: Flt64) = value
    }

    @Test
    fun smoke() {
        val x = RealVar("framework_demo_x")
        val xPoly = LinearPolynomial(
            monomials = listOf(LinearMonomial(Flt64.one, x)),
            constant = Flt64.zero
        )
        val ifFunction = IfFunction(
            condition = xPoly,
            converter = flt64Converter,
            name = "framework_if"
        )
        val slackRange = SlackRangeFunction(
            x = xPoly,
            lb = LinearPolynomial(
                monomials = emptyList(),
                constant = -Flt64.two
            ),
            ub = LinearPolynomial(
                monomials = emptyList(),
                constant = Flt64.two
            ),
            converter = flt64Converter,
            name = "framework_slack_range"
        )

        val ifValue = ifFunction.evaluate(mapOf<Symbol, Flt64>(x to Flt64(0.1)))
        val slackValue = slackRange.evaluate(mapOf<Symbol, Flt64>(x to Flt64(5.0)))

        assertNotNull(ifValue)
        assertNotNull(slackValue)
        assertEquals(Flt64.one, ifValue)
        assertEquals(Flt64(3.0), slackValue)
    }
}
