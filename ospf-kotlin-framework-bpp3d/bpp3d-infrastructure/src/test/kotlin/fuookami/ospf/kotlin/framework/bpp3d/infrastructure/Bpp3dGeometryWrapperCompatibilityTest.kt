package fuookami.ospf.kotlin.framework.bpp3d.infrastructure

import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.math.geometry.Axis3
import fuookami.ospf.kotlin.math.geometry.QuantityAxisPermutation3
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.quantities.quantity.eq
import fuookami.ospf.kotlin.quantities.quantity.times
import fuookami.ospf.kotlin.quantities.unit.Kilogram
import fuookami.ospf.kotlin.quantities.unit.Meter
import fuookami.ospf.kotlin.quantities.unit.SquareMeter
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class Bpp3dGeometryWrapperCompatibilityTest {
    private data class Box(
        override val width: Quantity<InfraNumber>,
        override val height: Quantity<InfraNumber>,
        override val depth: Quantity<InfraNumber>,
        override val weight: Quantity<InfraNumber>,
        override val enabledOrientations: List<Orientation> = Orientation.entries
    ) : Cuboid<Box> {
        override val self: Box
            get() = this
    }

    private class NestingContainer(
        override val shape: AbstractContainer3Shape,
        override val units: List<QuantityPlacement3<*>>,
        override val enabledOrientations: List<Orientation> = listOf(Orientation.Upright)
    ) : Container3CuboidUnit<NestingContainer> {
        override fun copy(): NestingContainer {
            return NestingContainer(shape = shape, units = units, enabledOrientations = enabledOrientations)
        }
    }

    private data class Column(
        override val radius: Quantity<InfraNumber>,
        override val height: Quantity<InfraNumber>,
        override val axis: Axis3,
        override val weight: Quantity<InfraNumber>,
        override val enabledAxes: List<Axis3>
    ) : Cylinder<Column> {
        override val self: Column
            get() = this
    }

    @Test
    fun cuboidViewShouldKeepExistingOrientationBehavior() {
        val box = Box(
            width = 2.0 * Meter,
            height = 3.0 * Meter,
            depth = 4.0 * Meter,
            weight = 1.0 * Kilogram
        )

        val upright = box.view(Orientation.Upright)
        val uprightRotated = box.view(Orientation.UprightRotated)
        val side = box.view(Orientation.Side)
        assertNotNull(upright)
        assertNotNull(uprightRotated)
        assertNotNull(side)

        assertTrue(upright.width eq (2.0 * Meter))
        assertTrue(upright.height eq (3.0 * Meter))
        assertTrue(upright.depth eq (4.0 * Meter))
        assertTrue(uprightRotated.width eq (4.0 * Meter))
        assertTrue(uprightRotated.height eq (3.0 * Meter))
        assertTrue(uprightRotated.depth eq (2.0 * Meter))
        assertTrue(side.width eq (3.0 * Meter))
        assertTrue(side.height eq (2.0 * Meter))
        assertTrue(side.depth eq (4.0 * Meter))
        assertEquals(Orientation.UprightRotated, upright.rotation?.orientation)
    }

    @Test
    fun projectionShouldKeepWeightAndAmountAggregation() {
        val boxA = Box(
            width = 2.0 * Meter,
            height = 3.0 * Meter,
            depth = 4.0 * Meter,
            weight = 2.0 * Kilogram
        )
        val boxB = Box(
            width = 1.0 * Meter,
            height = 5.0 * Meter,
            depth = 3.0 * Meter,
            weight = 4.0 * Kilogram
        )
        val viewA = boxA.view(Orientation.Upright)
        val viewB = boxB.view(Orientation.Lie)
        assertNotNull(viewA)
        assertNotNull(viewB)

        val pile = PileProjection(
            plane = PlaneProjection(viewA, Bottom),
            layer = UInt64(3)
        )
        assertTrue(pile.weight eq (6.0 * Kilogram))
        assertEquals(UInt64(3), pile.amount(boxA))

        val multi = MultiPileProjection(listOf(viewA, viewB), Bottom)
        assertTrue(multi.weight eq (6.0 * Kilogram))
        assertEquals(UInt64.one, multi.amount(boxA))
        assertEquals(UInt64.one, multi.amount(boxB))
    }

    @Test
    fun placementShouldKeepParentAndSupportSemantics() {
        val childBox = Box(
            width = 2.0 * Meter,
            height = 1.0 * Meter,
            depth = 2.0 * Meter,
            weight = 3.0 * Kilogram
        )
        val childPlacement = QuantityPlacement3(
            view = childBox.view(Orientation.Upright)!!,
            position = QuantityPoint3(
                x = 1.0 * Meter,
                y = 2.0 * Meter,
                z = 3.0 * Meter
            )
        )
        val container = NestingContainer(
            shape = Container3Shape(
                width = 10.0 * Meter,
                height = 10.0 * Meter,
                depth = 10.0 * Meter
            ),
            units = listOf(childPlacement)
        )
        val parentPlacement = QuantityPlacement3(
            view = container.view(Orientation.Upright)!!,
            position = QuantityPoint3(
                x = 5.0 * Meter,
                y = 6.0 * Meter,
                z = 7.0 * Meter
            )
        )

        assertEquals(parentPlacement, childPlacement.parent)
        assertTrue(childPlacement.absoluteX eq (6.0 * Meter))
        assertTrue(childPlacement.absoluteY eq (8.0 * Meter))
        assertTrue(childPlacement.absoluteZ eq (10.0 * Meter))

        val topPlacement = QuantityPlacement3(
            view = childBox.view(Orientation.Upright)!!,
            position = QuantityPoint3(
                x = 0.0 * Meter,
                y = 1.0 * Meter,
                z = 0.0 * Meter
            )
        )
        val bottomPlacement = QuantityPlacement3(
            view = childBox.view(Orientation.Upright)!!,
            position = QuantityPoint3(
                x = 0.0 * Meter,
                y = 0.0 * Meter,
                z = 0.0 * Meter
            )
        )
        val support = bottomSupport(topPlacement, listOf(bottomPlacement))

        assertTrue(support.area eq (4.0 * SquareMeter))
        assertTrue(support.weight eq (3.0 * Kilogram))
    }

    @Test
    fun cylinderAndOrientationBridgeShouldKeepGeometryContract() {
        val column = Column(
            radius = 1.0 * Meter,
            height = 5.0 * Meter,
            axis = Axis3.Z,
            weight = 2.0 * Kilogram,
            enabledAxes = listOf(Axis3.X, Axis3.Z)
        )

        val defaultGeometry = column.geometry()
        val axisXGeometry = column.geometry(Axis3.X)
        assertTrue(defaultGeometry.radius eq (1.0 * Meter))
        assertTrue(defaultGeometry.height eq (5.0 * Meter))
        assertEquals(Axis3.Z, defaultGeometry.axis)
        assertEquals(Axis3.X, axisXGeometry.axis)

        assertEquals(QuantityAxisPermutation3.XYZ, Orientation.Upright.toAxisPermutation3())
        assertEquals(QuantityAxisPermutation3.ZYX, Orientation.UprightRotated.toAxisPermutation3())
        assertEquals(QuantityAxisPermutation3.YXZ, Orientation.Side.toAxisPermutation3())
        assertEquals(QuantityAxisPermutation3.ZXY, Orientation.SideRotated.toAxisPermutation3())
        assertEquals(QuantityAxisPermutation3.XZY, Orientation.Lie.toAxisPermutation3())
        assertEquals(QuantityAxisPermutation3.YZX, Orientation.LieRotated.toAxisPermutation3())
    }
}
