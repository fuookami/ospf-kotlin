package fuookami.ospf.kotlin.core.symbol.function

import fuookami.ospf.kotlin.core.testing.*
import fuookami.ospf.kotlin.core.variable.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.symbol.Symbol
import kotlin.test.*

class MaxAndMaskingFunctionGenericEvaluateTest {
    @Test
    fun maxAndMinEvaluateShouldUseGenericComparisonForFourNumberTypes() {
        runMaxMinCase(GenericNumberCases.flt64)
        runMaxMinCase(GenericNumberCases.fltX)
        runMaxMinCase(GenericNumberCases.rtn64)
        runMaxMinCase(GenericNumberCases.rtnX)
    }

    @Test
    fun maskingFunctionsEvaluateShouldUseGenericArithmeticForFourNumberTypes() {
        runMaskingCase(GenericNumberCases.flt64)
        runMaskingCase(GenericNumberCases.fltX)
        runMaskingCase(GenericNumberCases.rtn64)
        runMaskingCase(GenericNumberCases.rtnX)
    }

    private fun <V> runMaxMinCase(numberCase: GenericNumberCase<V>)
            where V : RealNumber<V>, V : NumberField<V> {
        val x = RealVar("${numberCase.name.lowercase()}_max_eval_x")
        val y = RealVar("${numberCase.name.lowercase()}_max_eval_y")
        val z = RealVar("${numberCase.name.lowercase()}_max_eval_z")

        val px = LinearPolynomial(listOf(LinearMonomial(numberCase.one, x)), numberCase.zero)
        val py = LinearPolynomial(listOf(LinearMonomial(numberCase.one, y)), numberCase.zero)
        val pz = LinearPolynomial(listOf(LinearMonomial(numberCase.one, z)), numberCase.zero)

        val maxFunction = MaxFunction(
            polynomials = listOf(px, py, pz),
            converter = numberCase.converter,
            bigM = numberCase.ten,
            name = "max_eval_${numberCase.name.lowercase()}"
        )
        val minFunction = MinFunction(
            polynomials = listOf(px, py, pz),
            converter = numberCase.converter,
            bigM = numberCase.ten,
            name = "min_eval_${numberCase.name.lowercase()}"
        )

        val values = mapOf<Symbol, V>(
            x to numberCase.two,
            y to numberCase.five,
            z to numberCase.one
        )

        val maxValue = maxFunction.evaluate(values)
        assertNotNull(maxValue, "${numberCase.name}: max evaluate should not be null")
        assertTrue(maxValue eq numberCase.five, "${numberCase.name}: max should be five")

        val minValue = minFunction.evaluate(values)
        assertNotNull(minValue, "${numberCase.name}: min evaluate should not be null")
        assertTrue(minValue eq numberCase.one, "${numberCase.name}: min should be one")
    }

    private fun <V> runMaskingCase(numberCase: GenericNumberCase<V>)
            where V : RealNumber<V>, V : NumberField<V> {
        val x = RealVar("${numberCase.name.lowercase()}_mask_eval_x")
        val mask = BinVar("${numberCase.name.lowercase()}_mask_eval_z")
        val polyMask = BinVar("${numberCase.name.lowercase()}_poly_mask_eval_z")
        val rangeMask = BinVar("${numberCase.name.lowercase()}_range_mask_eval_z")

        val input = LinearPolynomial(
            monomials = listOf(LinearMonomial(numberCase.one, x)),
            constant = numberCase.one
        )
        val expectedOn = numberCase.five + numberCase.one

        val maskingFunction = MaskingFunction(
            input = input,
            mask = mask,
            converter = numberCase.converter,
            bigM = numberCase.ten,
            name = "masking_eval_${numberCase.name.lowercase()}"
        )

        val maskingOn = maskingFunction.evaluate(
            mapOf<Symbol, V>(x to numberCase.five, mask to numberCase.one)
        )
        assertNotNull(maskingOn, "${numberCase.name}: masking on should not be null")
        assertTrue(maskingOn eq expectedOn, "${numberCase.name}: masking on should keep input")

        val maskingOff = maskingFunction.evaluate(
            mapOf<Symbol, V>(x to numberCase.five, mask to numberCase.zero)
        )
        assertNotNull(maskingOff, "${numberCase.name}: masking off should not be null")
        assertTrue(maskingOff eq numberCase.zero, "${numberCase.name}: masking off should be zero")

        val polyMaskFunction = MaskingWithPolyMaskFunction(
            input = input,
            maskPoly = LinearPolynomial(listOf(LinearMonomial(numberCase.one, polyMask)), numberCase.zero),
            converter = numberCase.converter,
            bigM = numberCase.ten,
            name = "poly_masking_eval_${numberCase.name.lowercase()}"
        )

        val polyMaskOn = polyMaskFunction.evaluate(
            mapOf<Symbol, V>(x to numberCase.five, polyMask to numberCase.one)
        )
        assertNotNull(polyMaskOn, "${numberCase.name}: poly masking on should not be null")
        assertTrue(polyMaskOn eq expectedOn, "${numberCase.name}: poly masking on should keep input")

        val polyMaskOff = polyMaskFunction.evaluate(
            mapOf<Symbol, V>(x to numberCase.five, polyMask to numberCase.zero)
        )
        assertNotNull(polyMaskOff, "${numberCase.name}: poly masking off should not be null")
        assertTrue(polyMaskOff eq numberCase.zero, "${numberCase.name}: poly masking off should be zero")

        val rangeMaskFunction = MaskingRangeFunction(
            mask = LinearPolynomial(listOf(LinearMonomial(numberCase.one, rangeMask)), numberCase.zero),
            lower = numberCase.one,
            upper = numberCase.five,
            converter = numberCase.converter,
            name = "range_masking_eval_${numberCase.name.lowercase()}"
        )

        val rangeOff = rangeMaskFunction.evaluate(
            mapOf<Symbol, V>(
                rangeMask to numberCase.zero,
                rangeMaskFunction.resultVar to numberCase.ten
            )
        )
        assertNotNull(rangeOff, "${numberCase.name}: range masking off should not be null")
        assertTrue(rangeOff eq numberCase.zero, "${numberCase.name}: range masking off should be zero")

        val rangeHigh = rangeMaskFunction.evaluate(
            mapOf<Symbol, V>(
                rangeMask to numberCase.one,
                rangeMaskFunction.resultVar to numberCase.ten
            )
        )
        assertNotNull(rangeHigh, "${numberCase.name}: range masking high should not be null")
        assertTrue(rangeHigh eq numberCase.five, "${numberCase.name}: range masking high should clamp to upper")

        val rangeLow = rangeMaskFunction.evaluate(
            mapOf<Symbol, V>(
                rangeMask to numberCase.one,
                rangeMaskFunction.resultVar to numberCase.zero
            )
        )
        assertNotNull(rangeLow, "${numberCase.name}: range masking low should not be null")
        assertTrue(rangeLow eq numberCase.one, "${numberCase.name}: range masking low should clamp to lower")

        val rangeInside = rangeMaskFunction.evaluate(
            mapOf<Symbol, V>(
                rangeMask to numberCase.one,
                rangeMaskFunction.resultVar to numberCase.two
            )
        )
        assertNotNull(rangeInside, "${numberCase.name}: range masking inside should not be null")
        assertTrue(rangeInside eq numberCase.two, "${numberCase.name}: range masking inside should keep value")
    }
}
