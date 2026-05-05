package fuookami.ospf.kotlin.quantities

import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.quantities.quantity.*
import fuookami.ospf.kotlin.quantities.unit.*
import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class QuantityLogicTest {
    @Test
    fun `quantityEqNeq_shouldBeLogicalComplementOnDimensionMismatch`() {
        // 1 meter vs 1 second - different dimensions
        val length: Quantity<Flt64> = Flt64.one * Meter
        val time: Quantity<Flt64> = Flt64.one * Second

        // eq should return false (different dimensions)
        assertFalse(length eq time)

        // neq should return true (logical complement of eq)
        assertTrue(length neq time)
    }

    @Test
    fun `quantityEqNeq_shouldBeConsistentForSameDimension`() {
        // 1 meter vs 100 centimeters - same dimension
        val oneMeter: Quantity<Flt64> = Flt64.one * Meter
        val hundredCm: Quantity<Flt64> = Flt64(100.0) * Centimeter

        // Should be equal
        assertTrue(oneMeter eq hundredCm)
        assertFalse(oneMeter neq hundredCm)

        // Different values
        val twoMeters: Quantity<Flt64> = Flt64(2.0) * Meter
        assertFalse(oneMeter eq twoMeters)
        assertTrue(oneMeter neq twoMeters)
    }
}