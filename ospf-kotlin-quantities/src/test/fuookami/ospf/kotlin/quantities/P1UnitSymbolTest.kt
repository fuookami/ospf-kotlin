package fuookami.ospf.kotlin.quantities

import fuookami.ospf.kotlin.quantities.unit.*
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class P1UnitSymbolTest {
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
}