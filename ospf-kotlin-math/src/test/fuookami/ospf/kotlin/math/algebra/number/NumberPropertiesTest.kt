package fuookami.ospf.kotlin.math.algebra.number

import fuookami.ospf.kotlin.math.operator.eq
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

class NumberPropertiesTest {
    @Test
    fun additiveAndMultiplicativeCommutativityShouldHoldForCommonNumbers() {
        val intSamples = listOf(Int32(-3), Int32.zero, Int32.one, Int32.two, Int32(7))
        for (lhs in intSamples) {
            for (rhs in intSamples) {
                assertTrue((lhs + rhs) eq (rhs + lhs))
                assertTrue((lhs * rhs) eq (rhs * lhs))
            }
        }

        val fltSamples = listOf(Flt64(-3.5), Flt64.zero, Flt64.one, Flt64(2.25), Flt64(7.75))
        for (lhs in fltSamples) {
            for (rhs in fltSamples) {
                assertTrue((lhs + rhs) eq (rhs + lhs))
                assertTrue((lhs * rhs) eq (rhs * lhs))
            }
        }
    }

    @Test
    fun distributiveLawShouldHoldForInt32AndRtn64() {
        val intA = Int32(3)
        val intB = Int32(-2)
        val intC = Int32(5)
        assertTrue((intA * (intB + intC)) eq ((intA * intB) + (intA * intC)))

        val rtnA = Rtn64(Int64(3L), Int64(4L))
        val rtnB = Rtn64(Int64(2L), Int64(5L))
        val rtnC = Rtn64(Int64(7L), Int64(6L))
        assertTrue((rtnA * (rtnB + rtnC)) eq ((rtnA * rtnB) + (rtnA * rtnC)))
    }

    @Test
    fun crossTypeConversionsShouldPreserveSmallIntegerValues() {
        for (n in 0..8) {
            val int32 = Int32(n)
            val int64 = Int64(n.toLong())
            val rtn = Rtn64(int64, Int64.one)
            val flt = Flt64(n.toDouble())

            assertTrue(int32.toFlt64() eq flt)
            assertTrue(rtn.toFlt64() eq flt)
            assertTrue(rtn.toInt32() eq int32)
        }
    }

    @Test
    fun reciprocalOfUnitsShouldRoundTrip() {
        val positive = Int64.one
        val negative = Int64(-1L)

        assertTrue((positive * positive.reciprocal()) eq Int64.one)
        assertTrue((negative * negative.reciprocal()) eq Int64.one)
    }
}
