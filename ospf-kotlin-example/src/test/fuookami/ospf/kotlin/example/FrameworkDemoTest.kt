package fuookami.ospf.kotlin.example

import fuookami.ospf.kotlin.example.test.flt64TestConverter
import fuookami.ospf.kotlin.core.symbol.function.IfFunction
import fuookami.ospf.kotlin.core.symbol.function.SlackRangeFunction
import fuookami.ospf.kotlin.core.variable.RealVar
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class FrameworkDemoTest {
    @Test
    fun smoke() {
        val x = RealVar("framework_demo_x")
        val xPoly = LinearPolynomial(
            monomials = listOf(LinearMonomial(Flt64.one, x)),
            constant = Flt64.zero
        )
        val ifFunction = IfFunction(
            condition = xPoly,
            converter = flt64TestConverter,
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
            converter = flt64TestConverter,
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