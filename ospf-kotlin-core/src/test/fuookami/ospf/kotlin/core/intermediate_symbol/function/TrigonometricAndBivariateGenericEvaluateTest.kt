package fuookami.ospf.kotlin.core.intermediate_symbol.function

import fuookami.ospf.kotlin.core.testing.GenericNumberCase
import fuookami.ospf.kotlin.core.testing.GenericNumberCases
import fuookami.ospf.kotlin.core.variable.RealVar
import fuookami.ospf.kotlin.math.algebra.concept.NumberField
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.geometry.Triangle
import fuookami.ospf.kotlin.math.geometry.Point
import fuookami.ospf.kotlin.math.geometry.Dim3
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import fuookami.ospf.kotlin.math.geometry.point3

class TrigonometricAndBivariateGenericEvaluateTest {
    @Test
    fun sinAndCosEvaluateShouldWorkForFourNumberTypes() {
        runSinCosCase(GenericNumberCases.flt64)
        runSinCosCase(GenericNumberCases.fltX)
        runSinCosCase(GenericNumberCases.rtn64)
        runSinCosCase(GenericNumberCases.rtnX)
    }

    @Test
    fun bivariatePiecewiseEvaluateShouldWorkForFourNumberTypes() {
        runBivariateCase(GenericNumberCases.flt64)
        runBivariateCase(GenericNumberCases.fltX)
        runBivariateCase(GenericNumberCases.rtn64)
        runBivariateCase(GenericNumberCases.rtnX)
    }

    private fun <V> runSinCosCase(numberCase: GenericNumberCase<V>)
            where V : RealNumber<V>, V : NumberField<V> {
        val x = RealVar("${numberCase.name.lowercase()}_trig_eval_x")
        val xPoly = LinearPolynomial(listOf(LinearMonomial(numberCase.one, x)), numberCase.zero)

        val sin = SinFunction(
            x = xPoly,
            converter = numberCase.converter,
            name = "sin_eval_${numberCase.name.lowercase()}"
        )
        val cos = CosFunction(
            x = xPoly,
            converter = numberCase.converter,
            name = "cos_eval_${numberCase.name.lowercase()}"
        )

        val zeroInput = numberCase.zero
        val sinAtZero = sin.evaluate(mapOf<Symbol, V>(x to zeroInput))
        assertNotNull(sinAtZero, "${numberCase.name}: sin(0) should not be null")
        assertTrue(sinAtZero eq numberCase.zero, "${numberCase.name}: sin(0) should be 0")

        val cosAtZero = cos.evaluate(mapOf<Symbol, V>(x to zeroInput))
        assertNotNull(cosAtZero, "${numberCase.name}: cos(0) should not be null")
        assertTrue(cosAtZero eq numberCase.one, "${numberCase.name}: cos(0) should be 1")
    }

    private fun <V> runBivariateCase(numberCase: GenericNumberCase<V>)
            where V : RealNumber<V>, V : NumberField<V> {
        val x = RealVar("${numberCase.name.lowercase()}_bivar_eval_x")
        val y = RealVar("${numberCase.name.lowercase()}_bivar_eval_y")
        val xPoly = LinearPolynomial(listOf(LinearMonomial(numberCase.one, x)), numberCase.zero)
        val yPoly = LinearPolynomial(listOf(LinearMonomial(numberCase.one, y)), numberCase.zero)

        val function = BivariateLinearPiecewiseFunction(
            x = xPoly,
            y = yPoly,
            triangles = listOf(
                Triangle(
                    point3(Flt64.zero, Flt64.zero, Flt64.zero),
                    point3(Flt64.one, Flt64.zero, Flt64.one),
                    point3(Flt64.zero, Flt64.one, Flt64.one)
                )
            ),
            converter = numberCase.converter,
            name = "bivar_eval_${numberCase.name.lowercase()}"
        )

        val inX = numberCase.converter.intoValue(Flt64(0.25))
        val inY = numberCase.converter.intoValue(Flt64(0.25))
        val expected = numberCase.converter.intoValue(Flt64(0.75))
        val inTriangle = function.evaluate(mapOf<Symbol, V>(x to inX, y to inY))
        assertNotNull(inTriangle, "${numberCase.name}: bivariate value inside triangle should not be null")
        assertTrue(inTriangle eq expected, "${numberCase.name}: expected interpolated z = 0.75")

        val outTriangle = function.evaluate(
            mapOf<Symbol, V>(
                x to numberCase.one,
                y to numberCase.one
            )
        )
        assertNull(outTriangle, "${numberCase.name}: outside triangle should return null")
    }
}