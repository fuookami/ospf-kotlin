package fuookami.ospf.kotlin.core.token

import fuookami.ospf.kotlin.core.testing.GenericNumberCase
import fuookami.ospf.kotlin.core.testing.GenericNumberCases
import fuookami.ospf.kotlin.core.variable.RealVar
import fuookami.ospf.kotlin.math.algebra.concept.NumberField
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class GenericTokenConversionTest {
    @Test
    fun tokenResultAndBoundsShouldConvertForFourNumberTypes() {
        runConversionCase(GenericNumberCases.flt64)
        runConversionCase(GenericNumberCases.rtn64)
        runConversionCase(GenericNumberCases.fltX)
        runConversionCase(GenericNumberCases.rtnX)
    }

    private fun <V> runConversionCase(numberCase: GenericNumberCase<V>)
            where V : RealNumber<V>, V : NumberField<V> {
        val x = RealVar("${numberCase.name.lowercase()}_token_conversion_x")
        x.range.geq(Flt64(-2.0))
        x.range.leq(Flt64(5.0))

        val token = Token(
            variable = x,
            solverIndex = 0,
            refreshCallbacks = mutableMapOf(),
            converter = numberCase.converter
        )

        token._result = Flt64(1.25)
        val convertedResult = assertNotNull(token.result, "${numberCase.name}: result should convert from Flt64 to V")
        assertEquals(Flt64(1.25), toFlt64(numberCase, convertedResult), "${numberCase.name}: result conversion mismatch")
        assertEquals(
            convertedResult,
            token.result(numberCase.converter),
            "${numberCase.name}: result(converter) should align with result view"
        )

        val newValue = value(numberCase, 3.5)
        token.setResult(newValue)
        assertEquals(Flt64(3.5), token.resultFlt64, "${numberCase.name}: setResult should update resultFlt64")
        assertEquals(Flt64(3.5), toFlt64(numberCase, assertNotNull(token.result)), "${numberCase.name}: setResult should update result")

        val lower = assertNotNull(token.lowerBound(numberCase.converter), "${numberCase.name}: lowerBound(converter) should exist")
        val upper = assertNotNull(token.upperBound(numberCase.converter), "${numberCase.name}: upperBound(converter) should exist")
        assertEquals(Flt64(-2.0), toFlt64(numberCase, lower), "${numberCase.name}: lowerBound(converter) mismatch")
        assertEquals(Flt64(5.0), toFlt64(numberCase, upper), "${numberCase.name}: upperBound(converter) mismatch")

        assertTrue(token.containsInBounds(value(numberCase, -1.0), numberCase.converter), "${numberCase.name}: in-bounds value should be accepted")
        assertTrue(!token.containsInBounds(value(numberCase, 5.5), numberCase.converter), "${numberCase.name}: out-of-bounds value should be rejected")
    }

    private fun <V> value(
        numberCase: GenericNumberCase<V>,
        raw: Double
    ): V where V : RealNumber<V>, V : NumberField<V> = numberCase.converter.intoValue(Flt64(raw))

    private fun <V> toFlt64(
        numberCase: GenericNumberCase<V>,
        value: V
    ): Flt64 where V : RealNumber<V>, V : NumberField<V> = numberCase.converter.fromValue(value)
}
