/**
 * 长方体核心测试。
 * Cuboid core test.
 */
package fuookami.ospf.kotlin.framework.bpp3d.infrastructure

import kotlin.test.*
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.quantities.unit.*
import fuookami.ospf.kotlin.quantities.quantity.*

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
            width = FltX(2.0) * Meter,
            height = FltX(3.0) * Meter,
            depth = FltX(4.0) * Meter,
            weight = FltX(8.0) * Kilogram
        )

        assertTrue(box.volume eq (FltX(24.0) * CubicMeter))
        assertEquals((Kilogram / Meter).quantity.dimensionSymbol(), box.linearDensity.unit.quantity.dimensionSymbol())
        assertEquals(2.0, box.linearDensity.value.toDouble(), 1e-10)
    }

    @Test
    fun bottomSupportPlusShouldWorkWithQuantity() {
        val lhs = BottomSupport(
            area = FltX(2.0) * SquareMeter,
            weight = FltX(3.0) * Kilogram
        )
        val rhs = BottomSupport(
            area = FltX(1.5) * SquareMeter,
            weight = FltX(1.0) * Kilogram
        )
        val sum = lhs + rhs

        assertTrue(sum.area eq (FltX(3.5) * SquareMeter))
        assertTrue(sum.weight eq (FltX(4.0) * Kilogram))
    }

    @Test
    fun cuboidViewShouldExposeOrientationDimensionsAsQuantity() {
        val box = Box(
            width = FltX(2.0) * Meter,
            height = FltX(3.0) * Meter,
            depth = FltX(4.0) * Meter,
            weight = FltX(1.0) * Kilogram
        )

        val upright = box.view(Orientation.Upright)!!
        val rotated = box.view(Orientation.UprightRotated)!!

        assertTrue(upright.width eq (FltX(2.0) * Meter))
        assertTrue(upright.height eq (FltX(3.0) * Meter))
        assertTrue(upright.depth eq (FltX(4.0) * Meter))

        assertTrue(rotated.width eq (FltX(4.0) * Meter))
        assertTrue(rotated.height eq (FltX(3.0) * Meter))
        assertTrue(rotated.depth eq (FltX(2.0) * Meter))
    }
}
