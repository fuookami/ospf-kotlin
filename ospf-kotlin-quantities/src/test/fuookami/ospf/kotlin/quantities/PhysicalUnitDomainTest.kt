package fuookami.ospf.kotlin.quantities

import kotlin.test.*
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.math.Scale
import fuookami.ospf.kotlin.quantities.unit.*
import fuookami.ospf.kotlin.quantities.dimension.*

class PhysicalUnitDomainTest {
    @Test
    fun positivePowersPreserveDiscreteDomain() {
        for (index in listOf(1, 2, 3, 8)) {
            assertEquals(QuantityDomain.Discrete, Bit.pow(index).domain, "Bit^$index")
        }
    }

    @Test
    fun positivePowersPreserveContinuousDomain() {
        for (index in listOf(1, 2, 3, 8)) {
            assertEquals(QuantityDomain.Continuous, Meter.pow(index).domain, "Meter^$index")
        }
    }

    @Test
    fun multiplicationAndPowersRespectUnitDomainOverrides() {
        val discrete = AnonymousPhysicalUnit(
            quantity = Meter.quantity,
            conversionRule = Meter.conversionRule,
            domain = QuantityDomain.Discrete
        )
        val continuous = AnonymousPhysicalUnit(
            quantity = Bit.quantity,
            conversionRule = Bit.conversionRule,
            domain = QuantityDomain.Continuous
        )

        assertEquals(QuantityDomain.Discrete, (discrete * discrete).domain)
        assertEquals(QuantityDomain.Discrete, (discrete * Bit).domain)
        assertEquals(QuantityDomain.Discrete, (Bit * discrete).domain)
        assertEquals(QuantityDomain.Continuous, (continuous * Bit).domain)
        assertEquals(QuantityDomain.Continuous, (Bit * continuous).domain)
        assertEquals(QuantityDomain.Continuous, (discrete * continuous).domain)
        for (index in listOf(1, 2, 3)) {
            assertEquals(QuantityDomain.Discrete, discrete.pow(index).domain)
            assertEquals(QuantityDomain.Continuous, continuous.pow(index).domain)
        }
    }

    @Test
    fun zeroPowerRemainsDimensionlessAndContinuous() {
        assertSame(NoneUnit, Bit.pow(0))
        assertSame(NoneUnit, Meter.pow(0))
        assertEquals(QuantityDomain.Continuous, NoneUnit.domain)
    }

    @Test
    fun divisionAndNegativePowersRemainContinuous() {
        val discrete = AnonymousPhysicalUnit(
            quantity = Meter.quantity,
            conversionRule = Meter.conversionRule,
            domain = QuantityDomain.Discrete
        )

        assertEquals(QuantityDomain.Continuous, (Bit / Bit).domain)
        assertEquals(QuantityDomain.Continuous, (discrete / discrete).domain)
        assertEquals(QuantityDomain.Continuous, (Bit / discrete).domain)
        assertEquals(QuantityDomain.Continuous, discrete.reciprocal().domain)
        for (index in listOf(-1, -2, -3)) {
            assertEquals(QuantityDomain.Continuous, Bit.pow(index).domain)
            assertEquals(QuantityDomain.Continuous, discrete.pow(index).domain)
        }
    }

    @Test
    fun powersPreserveDimensionsAndConversionScales() {
        val discrete = AnonymousPhysicalUnit(
            quantity = Meter.quantity,
            conversionRule = UnitConversionRule.Linear(Scale(2) * Scale(3)),
            domain = QuantityDomain.Discrete
        )

        for (index in listOf(-3, -2, -1, 1, 2, 3)) {
            var expectedScale = Scale()
            repeat(kotlin.math.abs(index)) {
                expectedScale = if (index > 0) {
                    expectedScale * discrete.scale
                } else {
                    expectedScale / discrete.scale
                }
            }
            val powered = discrete.pow(index)
            assertEquals(discrete.quantity.pow(index), powered.quantity)
            assertEquals(expectedScale, powered.scale)
            assertEquals(index > 0, powered.domain == QuantityDomain.Discrete)
        }
    }

    @Test
    fun affineUnitsStillRejectMultiplicationDivisionAndPowers() {
        assertFailsWith<IllegalArgumentException> { Celsius * Bit }
        assertFailsWith<IllegalArgumentException> { Bit * Celsius }
        assertFailsWith<IllegalArgumentException> { Celsius / Bit }
        assertFailsWith<IllegalArgumentException> { Bit / Celsius }
        for (index in listOf(-1, 0, 1, 2)) {
            assertFailsWith<IllegalArgumentException> { Celsius.pow(index) }
        }
        assertFailsWith<IllegalArgumentException> { Celsius.reciprocal() }
    }
}
