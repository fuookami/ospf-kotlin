package fuookami.ospf.kotlin.core.symbol.function

import kotlin.test.*
import kotlinx.coroutines.runBlocking
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.testing.*
import fuookami.ospf.kotlin.core.variable.RealVar

class MinMaxFunctionTest {
    @Test
    fun wrappersShouldEvaluateForFourNumberTypes() {
        runEvaluationCase(GenericNumberCases.flt64)
        runEvaluationCase(GenericNumberCases.fltX)
        runEvaluationCase(GenericNumberCases.rtn64)
        runEvaluationCase(GenericNumberCases.rtnX)
    }

    @Test
    fun emptyInputsShouldBeRejected() {
        assertFailsWith<IllegalArgumentException> {
            MinMaxFunction<Flt64>(
                polynomials = emptyList(),
                converter = GenericNumberCases.flt64.converter,
                name = "empty_minmax"
            )
        }
        assertFailsWith<IllegalArgumentException> {
            MaxMinFunction<Flt64>(
                polynomials = emptyList(),
                converter = GenericNumberCases.flt64.converter,
                name = "empty_maxmin"
            )
        }
    }

    @Test
    fun wrappersShouldDelegateAuxiliaryTokensAndConstraints() {
        val numberCase = GenericNumberCases.flt64
        val x = RealVar("minmax_registration_x")
        val y = RealVar("minmax_registration_y")
        x.range.geq(Flt64(-10.0))
        x.range.leq(Flt64(10.0))
        y.range.geq(Flt64(-10.0))
        y.range.leq(Flt64(10.0))
        val xPoly = variablePolynomial(x, numberCase)
        val yPoly = variablePolynomial(y, numberCase)
        val metaModel = LinearMetaModel<Flt64>(
            name = "minmax-wrapper-registration",
            converter = numberCase.converter
        )

        try {
            assertTrue(metaModel.add(listOf(x, y)) is Ok)
            val minMax = MinMaxFunction(
                polynomials = listOf(xPoly, yPoly),
                converter = numberCase.converter,
                name = "minmax_registration"
            )
            val maxMin = MaxMinFunction(
                polynomials = listOf(xPoly, yPoly),
                converter = numberCase.converter,
                name = "maxmin_registration"
            )

            assertEquals(2, minMax.selectorVars.size)
            assertEquals(3, minMax.helperVariables.size)
            assertEquals(2, maxMin.selectorVars.size)
            assertEquals(3, maxMin.helperVariables.size)
            assertTrue(minMax.registerAuxiliaryTokens(metaModel.tokens) is Ok)
            assertTrue(maxMin.registerAuxiliaryTokens(metaModel.tokens) is Ok)
            assertTrue(metaModel.minimize(xPoly) is Ok)

            val mechanismResult = runBlocking {
                LinearMechanismModel.invoke(metaModel = metaModel, concurrent = false)
            }
            assertTrue(mechanismResult is Ok)
            val mechanismModel = mechanismResult.value

            val beforeMinMax = mechanismModel.constraints.size
            assertTrue(minMax.registerConstraints(mechanismModel) is Ok)
            assertTrue(mechanismModel.constraints.size > beforeMinMax)

            val beforeMaxMin = mechanismModel.constraints.size
            assertTrue(maxMin.registerConstraints(mechanismModel) is Ok)
            assertTrue(mechanismModel.constraints.size > beforeMaxMin)
        } finally {
            metaModel.close()
        }
    }

    private fun <V> runEvaluationCase(numberCase: GenericNumberCase<V>)
            where V : RealNumber<V>, V : NumberField<V> {
        val x = RealVar("${numberCase.name.lowercase()}_minmax_x")
        val y = RealVar("${numberCase.name.lowercase()}_minmax_y")
        val z = RealVar("${numberCase.name.lowercase()}_minmax_z")
        val polynomials = listOf(
            variablePolynomial(x, numberCase),
            variablePolynomial(y, numberCase),
            variablePolynomial(z, numberCase)
        )
        val minMax = MinMaxFunction(
            polynomials = polynomials,
            bigM = numberCase.ten,
            converter = numberCase.converter,
            name = "minmax_${numberCase.name.lowercase()}"
        )
        val maxMin = MaxMinFunction(
            polynomials = polynomials,
            bigM = numberCase.ten,
            converter = numberCase.converter,
            name = "maxmin_${numberCase.name.lowercase()}"
        )
        val negativeFive = numberCase.converter.intoValue(Flt64(-5.0))
        val values = mapOf<Symbol, V>(
            x to negativeFive,
            y to numberCase.two,
            z to numberCase.five
        )

        val maximum = minMax.evaluate(values)
        assertNotNull(maximum, "${numberCase.name}: MinMax evaluation")
        assertTrue(maximum eq numberCase.five, "${numberCase.name}: MinMax delegates to max")

        val minimum = maxMin.evaluate(values)
        assertNotNull(minimum, "${numberCase.name}: MaxMin evaluation")
        assertTrue(minimum eq negativeFive, "${numberCase.name}: MaxMin delegates to min")
        assertEquals(3, minMax.selectorVars.size)
        assertEquals(4, minMax.helperVariables.size)
        assertEquals(3, maxMin.selectorVars.size)
        assertEquals(4, maxMin.helperVariables.size)
    }

    private fun <V> variablePolynomial(variable: RealVar, numberCase: GenericNumberCase<V>): LinearPolynomial<V>
            where V : RealNumber<V>, V : NumberField<V> = LinearPolynomial(
        monomials = listOf(LinearMonomial(numberCase.one, variable)),
        constant = numberCase.zero
    )
}
