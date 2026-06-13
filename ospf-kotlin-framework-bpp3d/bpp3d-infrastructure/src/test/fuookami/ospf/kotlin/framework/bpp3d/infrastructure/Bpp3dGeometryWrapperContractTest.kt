/**
 * Bpp3D 几何包装契约测试。
 * Bpp3D geometry wrapper contract test.
 */
package fuookami.ospf.kotlin.framework.bpp3d.infrastructure

import kotlin.test.*
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.geometry.*
import fuookami.ospf.kotlin.quantities.unit.*
import fuookami.ospf.kotlin.quantities.quantity.*

class Bpp3dGeometryWrapperContractTest {
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

    private class NestingContainer(
        override val shape: AbstractContainer3Shape,
        override val units: List<QuantityPlacement3<*, FltX>>,
        override val enabledOrientations: List<Orientation> = listOf(Orientation.Upright)
    ) : Container3CuboidUnit<NestingContainer, FltX> {
        override fun copy(): NestingContainer {
            return NestingContainer(shape = shape, units = units, enabledOrientations = enabledOrientations)
        }
    }

    private data class Column(
        override val radius: Quantity<FltX>,
        override val height: Quantity<FltX>,
        override val axis: Axis3,
        override val weight: Quantity<FltX>,
        override val enabledAxes: List<Axis3>
    ) : Cylinder<Column> {
        override val self: Column
            get() = this
    }

    @Test
    fun cuboidViewShouldKeepExistingOrientationBehavior() {
        val box = Box(
            width = fltX(2.0) * Meter,
            height = fltX(3.0) * Meter,
            depth = fltX(4.0) * Meter,
            weight = fltX(1.0) * Kilogram
        )

        val upright = box.view(Orientation.Upright)
        val uprightRotated = box.view(Orientation.UprightRotated)
        val side = box.view(Orientation.Side)
        assertNotNull(upright)
        assertNotNull(uprightRotated)
        assertNotNull(side)

        assertTrue(upright.width eq (fltX(2.0) * Meter))
        assertTrue(upright.height eq (fltX(3.0) * Meter))
        assertTrue(upright.depth eq (fltX(4.0) * Meter))
        assertTrue(uprightRotated.width eq (fltX(4.0) * Meter))
        assertTrue(uprightRotated.height eq (fltX(3.0) * Meter))
        assertTrue(uprightRotated.depth eq (fltX(2.0) * Meter))
        assertTrue(side.width eq (fltX(3.0) * Meter))
        assertTrue(side.height eq (fltX(2.0) * Meter))
        assertTrue(side.depth eq (fltX(4.0) * Meter))
        assertEquals(Orientation.UprightRotated, upright.rotation?.orientation)
    }

    @Test
    fun projectionShouldKeepWeightAndAmountAggregation() {
        val boxA = Box(
            width = fltX(2.0) * Meter,
            height = fltX(3.0) * Meter,
            depth = fltX(4.0) * Meter,
            weight = fltX(2.0) * Kilogram
        )
        val boxB = Box(
            width = fltX(1.0) * Meter,
            height = fltX(5.0) * Meter,
            depth = fltX(3.0) * Meter,
            weight = fltX(4.0) * Kilogram
        )
        val viewA = boxA.view(Orientation.Upright)
        val viewB = boxB.view(Orientation.Lie)
        assertNotNull(viewA)
        assertNotNull(viewB)

        val pile = PileProjection(
            plane = PlaneProjection(viewA, Bottom),
            layer = UInt64(3)
        )
        assertTrue(pile.weight eq (fltX(6.0) * Kilogram))
        assertEquals(UInt64(3), pile.amount(boxA))

        val multi = MultiPileProjection(listOf(viewA, viewB), Bottom)
        assertTrue(multi.weight eq (fltX(6.0) * Kilogram))
        assertEquals(UInt64.one, multi.amount(boxA))
        assertEquals(UInt64.one, multi.amount(boxB))
    }

    @Test
    fun placementShouldKeepParentAndSupportSemantics() {
        val childBox = Box(
            width = fltX(2.0) * Meter,
            height = fltX(1.0) * Meter,
            depth = fltX(2.0) * Meter,
            weight = fltX(3.0) * Kilogram
        )
        val childPlacement = QuantityPlacement3(
            view = childBox.view(Orientation.Upright)!!,
            position = QuantityPoint3(
                x = fltX(1.0) * Meter,
                y = fltX(2.0) * Meter,
                z = fltX(3.0) * Meter
            )
        )
        val container = NestingContainer(
            shape = Container3Shape(
                width = fltX(10.0) * Meter,
                height = fltX(10.0) * Meter,
                depth = fltX(10.0) * Meter
            ),
            units = listOf(childPlacement)
        )
        val parentPlacement = QuantityPlacement3(
            view = container.view(Orientation.Upright)!!,
            position = QuantityPoint3(
                x = fltX(5.0) * Meter,
                y = fltX(6.0) * Meter,
                z = fltX(7.0) * Meter
            )
        )

        assertEquals(parentPlacement, childPlacement.parent)
        assertTrue(childPlacement.absoluteX eq (fltX(6.0) * Meter))
        assertTrue(childPlacement.absoluteY eq (fltX(8.0) * Meter))
        assertTrue(childPlacement.absoluteZ eq (fltX(10.0) * Meter))

        val topPlacement = QuantityPlacement3(
            view = childBox.view(Orientation.Upright)!!,
            position = QuantityPoint3(
                x = fltX(0.0) * Meter,
                y = fltX(1.0) * Meter,
                z = fltX(0.0) * Meter
            )
        )
        val bottomPlacement = QuantityPlacement3(
            view = childBox.view(Orientation.Upright)!!,
            position = QuantityPoint3(
                x = fltX(0.0) * Meter,
                y = fltX(0.0) * Meter,
                z = fltX(0.0) * Meter
            )
        )
        val support = bottomSupport(topPlacement, listOf(bottomPlacement))

        assertTrue(support.area eq (fltX(4.0) * SquareMeter))
        assertTrue(support.weight eq (fltX(3.0) * Kilogram))
    }

    @Test
    fun cylinderAndOrientationBridgeShouldKeepGeometryContract() {
        val column = Column(
            radius = fltX(1.0) * Meter,
            height = fltX(5.0) * Meter,
            axis = Axis3.Z,
            weight = fltX(2.0) * Kilogram,
            enabledAxes = listOf(Axis3.X, Axis3.Z)
        )

        val defaultGeometry = column.geometry()
        val axisXGeometry = column.geometry(Axis3.X)
        assertTrue(defaultGeometry.radius eq (fltX(1.0) * Meter))
        assertTrue(defaultGeometry.height eq (fltX(5.0) * Meter))
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
