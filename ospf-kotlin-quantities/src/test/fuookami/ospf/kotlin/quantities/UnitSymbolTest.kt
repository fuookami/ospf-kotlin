package fuookami.ospf.kotlin.quantities

import kotlin.test.assertEquals
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.quantities.unit.*

class UnitSymbolTest {
    @Test
    fun `unitSymbol_megahertzShouldBeMHz`() {
        assertEquals("MHz", Megahertz.symbol)
    }

    @Test
    fun `unitSymbol_gigahertzShouldBeGHz`() {
        assertEquals("GHz", Gigahertz.symbol)
    }

    @Test
    fun `unitSymbol_megapascalShouldBeMPa`() {
        assertEquals("MPa", Megapascal.symbol)
    }

    @Test
    fun `unitTorque_nameAndSymbolShouldBeCorrect`() {
        // Verify name and symbol are not swapped
        assertEquals("newton meter", NewtonMeter.name)
        assertEquals("N·m", NewtonMeter.symbol)
    }

    @Test
    fun `unitSymbol_accelerationSymbolsShouldBeCorrect`() {
        assertEquals("mps2", MeterPerSecondSquared.symbol)
        assertEquals("cmps2", CentimeterPerSecondSquared.symbol)
    }
}
