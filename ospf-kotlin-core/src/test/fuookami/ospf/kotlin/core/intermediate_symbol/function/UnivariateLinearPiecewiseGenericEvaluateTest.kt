package fuookami.ospf.kotlin.core.intermediate_symbol.function

import fuookami.ospf.kotlin.core.testing.GenericNumberCase
import fuookami.ospf.kotlin.core.testing.GenericNumberCases
import fuookami.ospf.kotlin.core.variable.RealVar
import fuookami.ospf.kotlin.math.algebra.concept.NumberField
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class UnivariateLinearPiecewiseGenericEvaluateTest {
    @Test
    fun evaluateShouldUseVTypedArithmeticForFourNumberTypes() {
        runCase(GenericNumberCases.flt64)
        runCase(GenericNumberCases.fltX)
        runCase(GenericNumberCases.rtn64)
        runCase(GenericNumberCases.rtnX)
    }

    private fun <V> runCase(numberCase: GenericNumberCase<V>)
            where V : RealNumber<V>, V : NumberField<V> {
        val x = RealVar("${numberCase.name.lowercase()}_piecewise_eval_x")
        val three = numberCase.two + numberCase.one
        val four = numberCase.two + numberCase.two

        val function = UnivariateLinearPiecewiseFunction(
            x = LinearPolynomial(listOf(LinearMonomial(numberCase.one, x)), numberCase.zero),
            breakpoints = listOf(numberCase.zero, numberCase.two, four),
            slopes = listOf(numberCase.one, numberCase.two),
            intercepts = listOf(numberCase.zero, -numberCase.two),
            converter = numberCase.converter,
            name = "piecewise_eval_${numberCase.name.lowercase()}"
        )

        val firstSegment = function.evaluate(mapOf<Symbol, V>(x to numberCase.one))
        assertNotNull(firstSegment, "${numberCase.name}: first segment evaluate should not be null")
        assertTrue(firstSegment eq numberCase.one, "${numberCase.name}: x=1 should map to 1")

        val secondSegment = function.evaluate(mapOf<Symbol, V>(x to three))
        assertNotNull(secondSegment, "${numberCase.name}: second segment evaluate should not be null")
        assertTrue(secondSegment eq four, "${numberCase.name}: x=3 should map to 4")

        val outOfRange = function.evaluate(mapOf<Symbol, V>(x to numberCase.five))
        assertNull(outOfRange, "${numberCase.name}: x outside breakpoints should return null")
    }
}