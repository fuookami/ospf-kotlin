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
        override val width: InfraQuantity,
        override val height: InfraQuantity,
        override val depth: InfraQuantity,
        override val weight: InfraQuantity,
        override val enabledOrientations: List<Orientation> = Orientation.entries
    ) : Cuboid<Box> {
        override val self: Box
            get() = this
    }

    @Test
    fun abstractCuboidVolumeAndLinearDensityShouldKeepDimensions() {
        val box = Box(
            width = infraScalar(2.0) * Meter,
            height = infraScalar(3.0) * Meter,
            depth = infraScalar(4.0) * Meter,
            weight = infraScalar(8.0) * Kilogram
        )

        assertTrue(box.volume eq (infraScalar(24.0) * CubicMeter))
        assertEquals((Kilogram / Meter).quantity.dimensionSymbol(), box.linearDensity.unit.quantity.dimensionSymbol())
        assertEquals(2.0, box.linearDensity.value.toDouble(), 1e-10)
    }

    @Test
    fun bottomSupportPlusShouldWorkWithQuantity() {
        val lhs = BottomSupport(
            area = infraScalar(2.0) * SquareMeter,
            weight = infraScalar(3.0) * Kilogram
        )
        val rhs = BottomSupport(
            area = infraScalar(1.5) * SquareMeter,
            weight = infraScalar(1.0) * Kilogram
        )
        val sum = lhs + rhs

        assertTrue(sum.area eq (infraScalar(3.5) * SquareMeter))
        assertTrue(sum.weight eq (infraScalar(4.0) * Kilogram))
    }

    @Test
    fun cuboidViewShouldExposeOrientationDimensionsAsQuantity() {
        val box = Box(
            width = infraScalar(2.0) * Meter,
            height = infraScalar(3.0) * Meter,
            depth = infraScalar(4.0) * Meter,
            weight = infraScalar(1.0) * Kilogram
        )

        val upright = box.view(Orientation.Upright)!!
        val rotated = box.view(Orientation.UprightRotated)!!

        assertTrue(upright.width eq (infraScalar(2.0) * Meter))
        assertTrue(upright.height eq (infraScalar(3.0) * Meter))
        assertTrue(upright.depth eq (infraScalar(4.0) * Meter))

        assertTrue(rotated.width eq (infraScalar(4.0) * Meter))
        assertTrue(rotated.height eq (infraScalar(3.0) * Meter))
        assertTrue(rotated.depth eq (infraScalar(2.0) * Meter))
    }
}

