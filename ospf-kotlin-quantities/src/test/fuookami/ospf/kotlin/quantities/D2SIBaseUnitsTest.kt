package fuookami.ospf.kotlin.quantities

import fuookami.ospf.kotlin.quantities.unit.*
import fuookami.ospf.kotlin.quantities.dimension.StandardFundamentalQuantityDimension
import org.junit.jupiter.api.Test
import kotlin.test.assertNotNull
import kotlin.test.assertSame

class D2SIBaseUnitsTest {
    @Test
    fun `siBaseUnits_shouldContainAllFundamentalQuantities`() {
        // Verify all 10 fundamental quantities have base units in SI
        val expectedDimensions = listOf(
            StandardFundamentalQuantityDimension.Length,
            StandardFundamentalQuantityDimension.Mass,
            StandardFundamentalQuantityDimension.Time,
            StandardFundamentalQuantityDimension.Current,
            StandardFundamentalQuantityDimension.Temperature,
            StandardFundamentalQuantityDimension.SubstanceAmount,
            StandardFundamentalQuantityDimension.LuminousIntensity,
            StandardFundamentalQuantityDimension.Information,
            StandardFundamentalQuantityDimension.PlaneAngle,
            StandardFundamentalQuantityDimension.SolidAngle
        )

        for (dimension in expectedDimensions) {
            val baseUnit = SI.baseUnits[dimension]
            assertNotNull(baseUnit, "SI should have base unit for $dimension")
        }
    }

    @Test
    fun `siBaseUnits_lengthShouldBeMeter`() {
        val baseUnit = SI.baseUnits[StandardFundamentalQuantityDimension.Length]
        assertNotNull(baseUnit)
        assertSame(Meter, baseUnit)
    }

    @Test
    fun `siBaseUnits_massShouldBeKilogram`() {
        val baseUnit = SI.baseUnits[StandardFundamentalQuantityDimension.Mass]
        assertNotNull(baseUnit)
        assertSame(Kilogram, baseUnit)
    }

    @Test
    fun `siBaseUnits_timeShouldBeSecond`() {
        val baseUnit = SI.baseUnits[StandardFundamentalQuantityDimension.Time]
        assertNotNull(baseUnit)
        assertSame(Second, baseUnit)
    }

    @Test
    fun `siBaseUnits_currentShouldBeAmpere`() {
        val baseUnit = SI.baseUnits[StandardFundamentalQuantityDimension.Current]
        assertNotNull(baseUnit)
        assertSame(Ampere, baseUnit)
    }

    @Test
    fun `siBaseUnits_temperatureShouldBeKelvin`() {
        val baseUnit = SI.baseUnits[StandardFundamentalQuantityDimension.Temperature]
        assertNotNull(baseUnit)
        assertSame(Kelvin, baseUnit)
    }

    @Test
    fun `siBaseUnits_substanceAmountShouldBeMole`() {
        val baseUnit = SI.baseUnits[StandardFundamentalQuantityDimension.SubstanceAmount]
        assertNotNull(baseUnit)
        assertSame(Mole, baseUnit)
    }

    @Test
    fun `siBaseUnits_luminousIntensityShouldBeCandela`() {
        val baseUnit = SI.baseUnits[StandardFundamentalQuantityDimension.LuminousIntensity]
        assertNotNull(baseUnit)
        assertSame(Candela, baseUnit)
    }

    @Test
    fun `siBaseUnits_planeAngleShouldBeRadian`() {
        val baseUnit = SI.baseUnits[StandardFundamentalQuantityDimension.PlaneAngle]
        assertNotNull(baseUnit)
        assertSame(Radian, baseUnit)
    }

    @Test
    fun `siBaseUnits_solidAngleShouldBeSteradian`() {
        val baseUnit = SI.baseUnits[StandardFundamentalQuantityDimension.SolidAngle]
        assertNotNull(baseUnit)
        assertSame(Steradian, baseUnit)
    }
}