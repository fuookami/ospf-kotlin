package fuookami.ospf.kotlin.core.symbol.function

import kotlin.test.*
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.core.testing.*
import fuookami.ospf.kotlin.core.variable.RealVar

class SlackRangeFunctionGenericEvaluateTest {
    @Test
    fun evaluateShouldUseVTypedArithmeticForFourNumberTypes() {
        runCase(GenericNumberCases.flt64)
        runCase(GenericNumberCases.fltX)
        runCase(GenericNumberCases.rtn64)
        runCase(GenericNumberCases.rtnX)
    }

    private fun <V> runCase(numberCase: GenericNumberCase<V>)
            where V : RealNumber<V>, V : NumberField<V> {
        val x = RealVar("${numberCase.name.lowercase()}_slack_range_x")
        val function = SlackRangeFunction(
            x = LinearPolynomial(listOf(LinearMonomial(numberCase.one, x)), numberCase.zero),
            lb = LinearPolynomial(emptyList(), -numberCase.two),
            ub = LinearPolynomial(emptyList(), numberCase.two),
            converter = numberCase.converter,
            name = "slack_range_eval_${numberCase.name.lowercase()}"
        )

        val high = function.evaluate(mapOf<Symbol, V>(x to numberCase.five))
        assertNotNull(high, "${numberCase.name}: evaluate should return value when input is present")
        assertTrue(high eq (numberCase.five - numberCase.two),
            "${numberCase.name}: expected positive slack")

        val low = function.evaluate(mapOf<Symbol, V>(x to numberCase.one))
        assertNotNull(low, "${numberCase.name}: evaluate should return zero when below threshold")
        assertTrue(low eq numberCase.zero, "${numberCase.name}: expected zero slack")
    }
}
