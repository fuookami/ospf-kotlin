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
import kotlin.test.assertTrue

class FirstFunctionGenericEvaluateTest {
    @Test
    fun evaluateShouldReturnFirstPositiveIndexOrNForFourNumberTypes() {
        runCase(GenericNumberCases.flt64)
        runCase(GenericNumberCases.fltX)
        runCase(GenericNumberCases.rtn64)
        runCase(GenericNumberCases.rtnX)
    }

    private fun <V> runCase(numberCase: GenericNumberCase<V>)
            where V : RealNumber<V>, V : NumberField<V> {
        val x0 = RealVar("${numberCase.name.lowercase()}_first_x0")
        val x1 = RealVar("${numberCase.name.lowercase()}_first_x1")
        val x2 = RealVar("${numberCase.name.lowercase()}_first_x2")
        val one = numberCase.one
        val zero = numberCase.zero

        val function = FirstFunction(
            polynomials = listOf(
                LinearPolynomial(listOf(LinearMonomial(one, x0)), zero),
                LinearPolynomial(listOf(LinearMonomial(one, x1)), zero),
                LinearPolynomial(listOf(LinearMonomial(one, x2)), zero)
            ),
            converter = numberCase.converter,
            name = "first_eval_${numberCase.name.lowercase()}"
        )

        val firstPositive = function.evaluate(
            mapOf<Symbol, V>(
                x0 to zero,
                x1 to numberCase.two,
                x2 to numberCase.five
            )
        )
        assertNotNull(firstPositive, "${numberCase.name}: firstPositive should not be null")
        assertTrue(firstPositive eq one, "${numberCase.name}: first positive index should be 1")

        val allZero = function.evaluate(
            mapOf<Symbol, V>(
                x0 to zero,
                x1 to zero,
                x2 to zero
            )
        )
        assertNotNull(allZero, "${numberCase.name}: allZero should not be null")
        val expectedN = repeatAdd(one, 3)
        assertTrue(allZero eq expectedN, "${numberCase.name}: all zero case should return n")
    }
}