package fuookami.ospf.kotlin.core.solver.value

import fuookami.ospf.kotlin.core.testing.GenericNumberCase
import fuookami.ospf.kotlin.core.testing.GenericNumberCases
import fuookami.ospf.kotlin.math.algebra.concept.NumberField
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GenericNumberConverterTest {
    @Test
    fun fourNumberConvertersShouldKeepFiniteRoundTripStable() {
        runFiniteRoundTripCase(GenericNumberCases.flt64)
        runFiniteRoundTripCase(GenericNumberCases.rtn64)
        runFiniteRoundTripCase(GenericNumberCases.fltX)
        runFiniteRoundTripCase(GenericNumberCases.rtnX)
    }

    @Test
    fun infinityRoundTripCapabilityShouldBeExplicitAcrossNumberTypes() {
        runInfinityCapabilityCase(GenericNumberCases.flt64)
        runInfinityCapabilityCase(GenericNumberCases.rtn64)
        runInfinityCapabilityCase(GenericNumberCases.fltX)
        runInfinityCapabilityCase(GenericNumberCases.rtnX)
    }

    private fun <V> runFiniteRoundTripCase(numberCase: GenericNumberCase<V>)
            where V : RealNumber<V>, V : NumberField<V> {
        val finiteSamples = listOf(
            Flt64(-100.25),
            Flt64(-1.0),
            Flt64.zero,
            Flt64.one,
            Flt64(2.5),
            Flt64(1000000.0)
        )

        for (sample in finiteSamples) {
            val intoV = numberCase.converter.intoValue(sample)
            val backToFlt64 = numberCase.converter.fromValue(intoV)
            assertEquals(sample, backToFlt64, "${numberCase.name}: Flt64 -> V -> Flt64 round-trip mismatch for $sample")

            val backToV = numberCase.converter.intoValue(backToFlt64)
            assertEquals(
                backToFlt64,
                numberCase.converter.fromValue(backToV),
                "${numberCase.name}: V -> Flt64 -> V -> Flt64 round-trip mismatch for $sample"
            )
        }

        assertEquals(Flt64.zero, numberCase.converter.fromValue(numberCase.converter.zero), "${numberCase.name}: converter.zero mismatch")
        assertEquals(Flt64.one, numberCase.converter.fromValue(numberCase.converter.one), "${numberCase.name}: converter.one mismatch")
    }

    private fun <V> runInfinityCapabilityCase(numberCase: GenericNumberCase<V>)
            where V : RealNumber<V>, V : NumberField<V> {
        val conversion = runCatching { numberCase.converter.intoValue(Flt64.infinity) }
        if (conversion.isSuccess) {
            val back = numberCase.converter.fromValue(conversion.getOrThrow())
            assertTrue(back.toDouble().isInfinite(), "${numberCase.name}: infinity should remain infinite after round-trip")
        } else {
            assertTrue(
                numberCase.name == "Rtn64" || numberCase.name == "RtnX" || numberCase.name == "FltX",
                "${numberCase.name}: only explicitly known types may reject infinity conversion"
            )
        }
    }
}