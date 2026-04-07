package fuookami.ospf.kotlin.quantities

import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.quantities.quantity.*
import fuookami.ospf.kotlin.quantities.unit.*
import fuookami.ospf.kotlin.quantities.unit.Second
import fuookami.ospf.kotlin.quantities.unit.Newton
import fuookami.ospf.kotlin.quantities.unit.PoundForce
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class P0UnitConstantsTest {
    @Test
    fun `unitArea_areShouldEqual100SquareMeter`() {
        val oneAre = Flt64.one * Are
        val inSquareMeter = oneAre.to(SquareMeter)
        assertNotNull(inSquareMeter)
        assertEquals(100.0, inSquareMeter.value.toDouble(), 1e-10)
    }

    @Test
    fun `unitVolume_cubicYardShouldEqual27CubicFoot`() {
        val oneCubicYard = Flt64.one * CubicYard
        val inCubicFoot = oneCubicYard.to(CubicFoot)
        assertNotNull(inCubicFoot)
        assertEquals(27.0, inCubicFoot.value.toDouble(), 1e-10)
    }

    @Test
    fun `unitVolume_ukFluidOunceShouldEqual28_4130625Milliliter`() {
        val oneUKFluidOunce = Flt64.one * UKFluidOunce
        val inMilliliter = oneUKFluidOunce.to(Milliliter)
        assertNotNull(inMilliliter)
        assertEquals(28.4130625, inMilliliter.value.toDouble(), 1e-6)
    }

    @Test
    fun `unitVolume_usFluidOunceShouldEqual29_5735295625Milliliter`() {
        val oneUSFluidOunce = Flt64.one * USFluidOunce
        val inMilliliter = oneUSFluidOunce.to(Milliliter)
        assertNotNull(inMilliliter)
        assertEquals(29.5735295625, inMilliliter.value.toDouble(), 1e-6)
    }

    @Test
    fun `unitVolume_ukGallonShouldEqual4_54609Liter`() {
        val oneUKGallon = Flt64.one * UKGallon
        val inLiter = oneUKGallon.to(Liter)
        assertNotNull(inLiter)
        assertEquals(4.54609, inLiter.value.toDouble(), 1e-6)
    }

    @Test
    fun `unitVolume_usGallonShouldEqual3_78541178Liter`() {
        val oneUSGallon = Flt64.one * USGallon
        val inLiter = oneUSGallon.to(Liter)
        assertNotNull(inLiter)
        assertEquals(3.78541178, inLiter.value.toDouble(), 1e-6)
    }

    @Test
    fun `unitMomentum_kilogramMeterPerSecondShouldEqualNewtonSecond`() {
        // 1 kg·m/s = 1 N·s (因为 1 N = 1 kg·m/s²)
        val oneKgMeterPerSec = Flt64.one * KilogramMeterPerSecond
        val oneNewtonSecond = Flt64.one * (Newton * Second)
        assert(oneKgMeterPerSec eq oneNewtonSecond)
    }

    @Test
    fun `unitForce_poundForceShouldEqual4_4482216152605Newton`() {
        val onePoundForce = Flt64.one * PoundForce
        val inNewton = onePoundForce.to(Newton)
        assertNotNull(inNewton)
        assertEquals(4.4482216152605, inNewton.value.toDouble(), 1e-10)
    }
}