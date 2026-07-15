package fuookami.ospf.kotlin.quantities

import kotlin.test.*
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.quantities.unit.*
import fuookami.ospf.kotlin.quantities.quantity.*

class IntegerConversionTest {
    @Test
    fun `quantityConvert_intShouldConvertWhenFactorIsInteger`() {
        // 1 meter = 100 centimeters - integer factor
        val oneMeter: Quantity<Int64> = Int64.one * Meter
        val inCm = oneMeter.to(Centimeter)
        assertNotNull(inCm)
        assertEquals(Int64(100L), inCm.value)
    }

    @Test
    fun `quantityConvert_intReturnsNullForNonIntegerFactors`() {
        // 100 cm to meter: factor is 1/100 = 0.01 (non-integer)
        // Integer conversion should return null for non-integer factors
        val hundredCm: Quantity<Int64> = Int64(100L) * Centimeter
        val inMeter = hundredCm.to(Meter)
        // With fixed implementation, non-integer factor returns null
        assertNull(inMeter)
    }

    @Test
    fun `quantityConvert_floatShouldHandleNonIntegerFactors`() {
        // Float conversion handles non-integer factors correctly
        val hundredCm: Quantity<Flt64> = Flt64(100.0) * Centimeter
        val inMeter = hundredCm.to(Meter)
        assertNotNull(inMeter)
        assertEquals(1.0, inMeter.value.toDouble(), 1e-10)
    }

    @Test
    fun `quantityConvert_intShouldWorkForLargeFactors`() {
        // 1 kilometer = 1000 meters
        val oneKm: Quantity<Int64> = Int64.one * Kilometer
        val inMeter = oneKm.to(Meter)
        assertNotNull(inMeter)
        assertEquals(Int64(1000L), inMeter.value)
    }

    @Test
    fun `quantityConvert_intToFloatShouldPreservePrecision`() {
        // Integer to float conversion should preserve value
        val oneMeter: Quantity<Int64> = Int64.one * Meter
        val asFlt64: Quantity<Flt64> = oneMeter.toFlt64()
        assertEquals(1.0, asFlt64.value.toDouble(), 1e-10)
    }

    @Test
    fun `quantityConvert_floatToIntTruncatesScale`() {
        // Float to int conversion applies the scale factor
        val oneMeterFlt: Quantity<Flt64> = Flt64.one * Meter
        val asInt: Quantity<Int64> = oneMeterFlt.toInt64()
        assertEquals(Int64.one, asInt.value)
    }

    @Test
    fun `quantityConvert_crossDimensionShouldReturnNull`() {
        // Cannot convert length to time
        val length: Quantity<Int64> = Int64.one * Meter
        val result = length.convertTo(Second)
        assertNull(result)
    }
}
