package fuookami.ospf.kotlin.math.algebra.concept

import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.math.algebra.number.Rtn64
import fuookami.ospf.kotlin.math.algebra.number.RtnX
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class Flt64BridgeTest {
    @Test
    fun flt64CompanionImplementsFlt64Bridge() {
        val bridge: Flt64Bridge<Flt64> = Flt64
        assertEquals(Flt64(3.14), bridge.intoValue(Flt64(3.14)))
        assertEquals(Flt64(3.14), bridge.fromValue(Flt64(3.14)))
        assertTrue(bridge.zero eq Flt64.zero)
        assertTrue(bridge.one eq Flt64.one)
    }

    @Test
    fun fltXCompanionImplementsFlt64Bridge() {
        val bridge: Flt64Bridge<FltX> = FltX
        val original = Flt64(3.14)
        val converted = bridge.intoValue(original)
        val back = bridge.fromValue(converted)
        assertTrue(bridge.zero eq FltX.zero)
        assertTrue(bridge.one eq FltX.one)
        assertTrue(back eq original)
    }

    @Test
    fun rtn64CompanionImplementsFlt64Bridge() {
        val bridge: Flt64Bridge<Rtn64> = Rtn64
        val original = Flt64(3.5)
        val converted = bridge.intoValue(original)
        val back = bridge.fromValue(converted)
        assertTrue(bridge.zero eq Rtn64.zero)
        assertTrue(bridge.one eq Rtn64.one)
        assertTrue(converted.num.toLong() == 7L)
        assertTrue(converted.den.toLong() == 2L)
        assertTrue(back eq original)
    }

    @Test
    fun rtnXCompanionImplementsFlt64Bridge() {
        val bridge: Flt64Bridge<RtnX> = RtnX
        val original = Flt64(5.5)
        val converted = bridge.intoValue(original)
        val back = bridge.fromValue(converted)
        assertTrue(bridge.zero eq RtnX.zero)
        assertTrue(bridge.one eq RtnX.one)
        assertTrue(converted eq RtnX(11, 2))
        assertTrue(back eq original)
    }

    @Test
    fun resolvesBridgeFromCompanionObject() {
        val property = CompanionConstantProviderResolver.reflectionFallbackEnabledProperty
        val previous = System.getProperty(property)
        System.setProperty(property, "true")
        try {
            assertTrue(resolveFlt64Bridge<Flt64>("test") === Flt64)
            assertTrue(resolveFlt64Bridge<FltX>("test") === FltX)
            assertTrue(resolveFlt64Bridge<Rtn64>("test") === Rtn64)
            assertTrue(resolveFlt64Bridge<RtnX>("test") === RtnX)
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
        val bridge: Flt64Bridge<Flt64> = Flt64
        val values = listOf(Flt64.zero, Flt64.one, Flt64(-1.0), Flt64(42.0), Flt64(0.001))
        for (v in values) {
            assertTrue(bridge.fromValue(bridge.intoValue(v)) eq v)
        }
    }

    @Test
    fun fltXRoundTripPreservesApproximateValue() {
        val bridge: Flt64Bridge<FltX> = FltX
        val values = listOf(Flt64.zero, Flt64.one, Flt64(-1.0), Flt64(42.0))
        for (v in values) {
            val converted = bridge.intoValue(v)
            val back = bridge.fromValue(converted)
            assertTrue(back eq v)
        }
    }

    @Test
    fun negativeAndZeroConversion() {
        val flt64Bridge: Flt64Bridge<Flt64> = Flt64
        val fltXBridge: Flt64Bridge<FltX> = FltX
        val rtn64Bridge: Flt64Bridge<Rtn64> = Rtn64

        assertTrue(flt64Bridge.intoValue(Flt64.zero) eq Flt64.zero)
        assertTrue(flt64Bridge.intoValue(Flt64(-0.5)) eq Flt64(-0.5))
        assertTrue(fltXBridge.intoValue(Flt64.zero) eq FltX.zero)
        assertTrue(rtn64Bridge.intoValue(Flt64.zero) eq Rtn64.zero)
    }
}