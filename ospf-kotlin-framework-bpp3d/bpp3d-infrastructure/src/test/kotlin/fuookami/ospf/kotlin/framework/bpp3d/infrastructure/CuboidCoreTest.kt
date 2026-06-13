package fuookami.ospf.kotlin.framework.bpp3d.infrastructure

import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.quantities.quantity.*
import fuookami.ospf.kotlin.quantities.unit.CubicMeter
import fuookami.ospf.kotlin.quantities.unit.Kilogram
import fuookami.ospf.kotlin.quantities.unit.Meter
import fuookami.ospf.kotlin.quantities.unit.SquareMeter
import fuookami.ospf.kotlin.quantities.unit.div
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CuboidCoreTest {
    private data class Box(
        override val width: Quantity<FltX>,
        override val height: Quantity<FltX>,
        override val depth: Quantity<FltX>,
        override val weight: Quantity<FltX>,
        override val enabledOrientations: List<Orientation> = Orientation.entries
    ) : Cuboid<Box, FltX> {
        override val self: Box
            get() = this
    }

    @Test
    fun abstractCuboidVolumeAndLinearDensityShouldKeepDimensions() {
        val box = Box(
            width = fltX(2.0) * Meter,
            height = fltX(3.0) * Meter,
            depth = fltX(4.0) * Meter,
            weight = fltX(8.0) * Kilogram
        )

        assertTrue(box.volume eq (fltX(24.0) * CubicMeter))
        assertEquals((Kilogram / Meter).quantity.dimensionSymbol(), box.linearDensity.unit.quantity.dimensionSymbol())
        assertEquals(2.0, box.linearDensity.value.toDouble(), 1e-10)
    }

    @Test
    fun bottomSupportPlusShouldWorkWithQuantity() {
        val lhs = BottomSupport(
            area = fltX(2.0) * SquareMeter,
            weight = fltX(3.0) * Kilogram
        )
        val rhs = BottomSupport(
            area = fltX(1.5) * SquareMeter,
            weight = fltX(1.0) * Kilogram
        )
        val sum = lhs + rhs

        assertTrue(sum.area eq (fltX(3.5) * SquareMeter))
        assertTrue(sum.weight eq (fltX(4.0) * Kilogram))
    }

    @Test
    fun cuboidViewShouldExposeOrientationDimensionsAsQuantity() {
        val box = Box(
            width = fltX(2.0) * Meter,
            height = fltX(3.0) * Meter,
            depth = fltX(4.0) * Meter,
            weight = fltX(1.0) * Kilogram
        )

        val upright = box.view(Orientation.Upright)!!
        val rotated = box.view(Orientation.UprightRotated)!!

        assertTrue(upright.width eq (fltX(2.0) * Meter))
        assertTrue(upright.height eq (fltX(3.0) * Meter))
        assertTrue(upright.depth eq (fltX(4.0) * Meter))

        assertTrue(rotated.width eq (fltX(4.0) * Meter))
        assertTrue(rotated.height eq (fltX(3.0) * Meter))
        assertTrue(rotated.depth eq (fltX(2.0) * Meter))
    }
}

