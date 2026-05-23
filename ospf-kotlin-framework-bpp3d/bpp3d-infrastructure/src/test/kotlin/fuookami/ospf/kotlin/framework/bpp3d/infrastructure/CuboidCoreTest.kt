package fuookami.ospf.kotlin.framework.bpp3d.infrastructure

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
        override val width: QuantityFlt64,
        override val height: QuantityFlt64,
        override val depth: QuantityFlt64,
        override val weight: QuantityFlt64,
        override val enabledOrientations: List<Orientation> = Orientation.entries
    ) : Cuboid<Box>

    @Test
    fun abstractCuboidVolumeAndLinearDensityShouldKeepDimensions() {
        val box = Box(
            width = 2.0 * Meter,
            height = 3.0 * Meter,
            depth = 4.0 * Meter,
            weight = 8.0 * Kilogram
        )

        assertTrue(box.volume eq (24.0 * CubicMeter))
        assertEquals((Kilogram / Meter).quantity.dimensionSymbol(), box.linearDensity.unit.quantity.dimensionSymbol())
        assertEquals(2.0, box.linearDensity.value.toDouble(), 1e-10)
    }

    @Test
    fun bottomSupportPlusShouldWorkWithQuantity() {
        val lhs = BottomSupport(
            area = 2.0 * SquareMeter,
            weight = 3.0 * Kilogram
        )
        val rhs = BottomSupport(
            area = 1.5 * SquareMeter,
            weight = 1.0 * Kilogram
        )
        val sum = lhs + rhs

        assertTrue(sum.area eq (3.5 * SquareMeter))
        assertTrue(sum.weight eq (4.0 * Kilogram))
    }

    @Test
    fun cuboidViewShouldExposeOrientationDimensionsAsQuantity() {
        val box = Box(
            width = 2.0 * Meter,
            height = 3.0 * Meter,
            depth = 4.0 * Meter,
            weight = 1.0 * Kilogram
        )

        val upright = box.view(Orientation.Upright)!!
        val rotated = box.view(Orientation.UprightRotated)!!

        assertTrue(upright.width eq (2.0 * Meter))
        assertTrue(upright.height eq (3.0 * Meter))
        assertTrue(upright.depth eq (4.0 * Meter))

        assertTrue(rotated.width eq (4.0 * Meter))
        assertTrue(rotated.height eq (3.0 * Meter))
        assertTrue(rotated.depth eq (2.0 * Meter))
    }
}
