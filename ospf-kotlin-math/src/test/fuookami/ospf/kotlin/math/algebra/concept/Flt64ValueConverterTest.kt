package fuookami.ospf.kotlin.math.algebra.concept

import kotlin.test.*
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.math.algebra.number.*

class Flt64ValueConverterTest {
    @Test
    fun flt64CompanionImplementsFlt64ValueConverter() {
        val converter: Flt64ValueConverter<Flt64> = Flt64
        assertEquals(Flt64(3.14), converter.intoValue(Flt64(3.14)))
        assertEquals(Flt64(3.14), converter.fromValue(Flt64(3.14)))
        assertTrue(converter.zero eq Flt64.zero)
        assertTrue(converter.one eq Flt64.one)
    }

    @Test
    fun fltXCompanionImplementsFlt64ValueConverter() {
        val converter: Flt64ValueConverter<FltX> = FltX
        val original = Flt64(3.14)
        val converted = converter.intoValue(original)
        val back = converter.fromValue(converted)
        assertTrue(converter.zero eq FltX.zero)
        assertTrue(converter.one eq FltX.one)
        assertTrue(back eq original)
    }

    @Test
    fun rtn64CompanionImplementsFlt64ValueConverter() {
        val converter: Flt64ValueConverter<Rtn64> = Rtn64
        val original = Flt64(3.5)
        val converted = converter.intoValue(original)
        val back = converter.fromValue(converted)
        assertTrue(converter.zero eq Rtn64.zero)
        assertTrue(converter.one eq Rtn64.one)
        assertTrue(converted.num.toLong() == 7L)
        assertTrue(converted.den.toLong() == 2L)
        assertTrue(back eq original)
    }

    @Test
    fun rtnXCompanionImplementsFlt64ValueConverter() {
        val converter: Flt64ValueConverter<RtnX> = RtnX
        val original = Flt64(5.5)
        val converted = converter.intoValue(original)
        val back = converter.fromValue(converted)
        assertTrue(converter.zero eq RtnX.zero)
        assertTrue(converter.one eq RtnX.one)
        assertTrue(converted eq RtnX(11, 2))
        assertTrue(back eq original)
    }

    @Test
    fun resolvesConverterFromCompanionObject() {
        val property = CompanionConstantProviderResolver.reflectionFallbackEnabledProperty
        val previous = System.getProperty(property)
        System.setProperty(property, "true")
        try {
            assertTrue(resolveFlt64ValueConverter<Flt64>("test") === Flt64)
            assertTrue(resolveFlt64ValueConverter<FltX>("test") === FltX)
            assertTrue(resolveFlt64ValueConverter<Rtn64>("test") === Rtn64)
            assertTrue(resolveFlt64ValueConverter<RtnX>("test") === RtnX)
        } finally {
            if (previous == null) {
                System.clearProperty(property)
            } else {
                System.setProperty(property, previous)
            }
        }
    }

    @Test
    fun flt64RoundTripPreservesIdentity() {
        val converter: Flt64ValueConverter<Flt64> = Flt64
        val values = listOf(Flt64.zero, Flt64.one, Flt64(-1.0), Flt64(42.0), Flt64(0.001))
        for (v in values) {
            assertTrue(converter.fromValue(converter.intoValue(v)) eq v)
        }
    }

    @Test
    fun fltXRoundTripPreservesApproximateValue() {
        val converter: Flt64ValueConverter<FltX> = FltX
        val values = listOf(Flt64.zero, Flt64.one, Flt64(-1.0), Flt64(42.0))
        for (v in values) {
            val converted = converter.intoValue(v)
            val back = converter.fromValue(converted)
            assertTrue(back eq v)
        }
    }

    @Test
    fun negativeAndZeroConversion() {
        val flt64Converter: Flt64ValueConverter<Flt64> = Flt64
        val fltXConverter: Flt64ValueConverter<FltX> = FltX
        val rtn64Converter: Flt64ValueConverter<Rtn64> = Rtn64

        assertTrue(flt64Converter.intoValue(Flt64.zero) eq Flt64.zero)
        assertTrue(flt64Converter.intoValue(Flt64(-0.5)) eq Flt64(-0.5))
        assertTrue(fltXConverter.intoValue(Flt64.zero) eq FltX.zero)
        assertTrue(rtn64Converter.intoValue(Flt64.zero) eq Rtn64.zero)
    }
}
