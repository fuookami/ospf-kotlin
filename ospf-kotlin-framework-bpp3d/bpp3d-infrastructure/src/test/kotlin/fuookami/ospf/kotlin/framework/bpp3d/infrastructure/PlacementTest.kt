package fuookami.ospf.kotlin.framework.bpp3d.infrastructure

import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.math.geometry.Axis3
import fuookami.ospf.kotlin.quantities.quantity.*
import fuookami.ospf.kotlin.quantities.unit.Kilogram
import fuookami.ospf.kotlin.quantities.unit.Meter
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PlacementTest {
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

    private data class Column(
        override val radius: InfraQuantity,
        override val height: InfraQuantity,
        override val axis: Axis3,
        override val weight: InfraQuantity,
        override val enabledAxes: List<Axis3>
    ) : Cylinder<Column> {
        override val self: Column
            get() = this
    }

    @Test
    fun placement3CoordinatesAndMaxShouldUseQuantity() {
        val box = Box(
            width = infraScalar(2.0) * Meter,
            height = infraScalar(3.0) * Meter,
            depth = infraScalar(4.0) * Meter,
            weight = infraScalar(1.0) * Kilogram
        )
        val placement = QuantityPlacement3(
            view = box.view()!!,
            position = QuantityPoint3(
                x = infraScalar(1.0) * Meter,
                y = infraScalar(2.0) * Meter,
                z = infraScalar(3.0) * Meter
            )
        )

        assertTrue(placement.x eq (infraScalar(1.0) * Meter))
        assertTrue(placement.y eq (infraScalar(2.0) * Meter))
        assertTrue(placement.z eq (infraScalar(3.0) * Meter))
        assertTrue(placement.maxX eq (infraScalar(3.0) * Meter))
        assertTrue(placement.maxY eq (infraScalar(5.0) * Meter))
        assertTrue(placement.maxZ eq (infraScalar(7.0) * Meter))
    }

    @Test
    fun toPlacement3FromPileShouldExpandLayers() {
        val box = Box(
            width = infraScalar(2.0) * Meter,
            height = infraScalar(1.0) * Meter,
            depth = infraScalar(2.0) * Meter,
            weight = infraScalar(1.0) * Kilogram
        )
        val projection = PileProjection(
            plane = PlaneProjection(box.view()!!, Bottom),
            layer = UInt64(3)
        )
        val QuantityPlacement2 = QuantityPlacement2(
            projection = projection,
            position = QuantityPoint2(
                x = infraScalar(0.0) * Meter,
                y = infraScalar(0.0) * Meter
            )
        )

        val expanded = QuantityPlacement2.toPlacement3()

        assertEquals(3, expanded.size)
        assertTrue(expanded[0].y eq (infraScalar(0.0) * Meter))
        assertTrue(expanded[1].y eq (infraScalar(2.0) * Meter))
        assertTrue(expanded[2].y eq (infraScalar(4.0) * Meter))
    }

    @Test
    fun topAndBottomPlacementsShouldSelectExpectedUnits() {
        val box = Box(
            width = infraScalar(2.0) * Meter,
            height = infraScalar(1.0) * Meter,
            depth = infraScalar(2.0) * Meter,
            weight = infraScalar(1.0) * Kilogram
        )
        val view = box.view()!!
        val p0 = QuantityPlacement3(view, QuantityPoint3(infraScalar(0.0) * Meter, infraScalar(0.0) * Meter, infraScalar(0.0) * Meter))
        val p1 = QuantityPlacement3(view, QuantityPoint3(infraScalar(0.0) * Meter, infraScalar(1.0) * Meter, infraScalar(0.0) * Meter))
        val p2 = QuantityPlacement3(view, QuantityPoint3(infraScalar(0.0) * Meter, infraScalar(2.0) * Meter, infraScalar(0.0) * Meter))
        val isolated = QuantityPlacement3(view, QuantityPoint3(infraScalar(10.0) * Meter, infraScalar(0.0) * Meter, infraScalar(0.0) * Meter))

        val placements = listOf(p0, p1, p2, isolated)
        val top = topPlacements(placements)
        val bottom = bottomPlacements(placements)

        assertEquals(2, top.size)
        assertTrue(top.contains(p2))
        assertTrue(top.contains(isolated))

        assertEquals(2, bottom.size)
        assertTrue(bottom.contains(p0))
        assertTrue(bottom.contains(isolated))
    }

    @Test
    fun shapePlacement3CircleCircleOverlapShouldFollowFootprintGeometry() {
        val column = Column(
            radius = infraScalar(1.0) * Meter,
            height = infraScalar(2.0) * Meter,
            axis = Axis3.Y,
            weight = infraScalar(1.0) * Kilogram,
            enabledAxes = listOf(Axis3.Y)
        )
        val shape = column.asPackingShape3()
        val left = ShapePlacement3(
            shape = shape,
            position = QuantityPoint3(
                x = infraScalar(0.0) * Meter,
                y = infraScalar(0.0) * Meter,
                z = infraScalar(0.0) * Meter
            )
        )
        val tangent = ShapePlacement3(
            shape = shape,
            position = QuantityPoint3(
                x = infraScalar(2.0) * Meter,
                y = infraScalar(0.0) * Meter,
                z = infraScalar(0.0) * Meter
            )
        )
        val far = ShapePlacement3(
            shape = shape,
            position = QuantityPoint3(
                x = infraScalar(2.1) * Meter,
                y = infraScalar(0.0) * Meter,
                z = infraScalar(0.0) * Meter
            )
        )

        assertTrue(left overlapped tangent)
        assertFalse(left overlapped far)
    }

    @Test
    fun shapePlacement3CircleRectangleOverlapShouldFollowFootprintGeometry() {
        val column = Column(
            radius = infraScalar(1.0) * Meter,
            height = infraScalar(2.0) * Meter,
            axis = Axis3.Y,
            weight = infraScalar(1.0) * Kilogram,
            enabledAxes = listOf(Axis3.Y)
        )
        val box = Box(
            width = infraScalar(1.0) * Meter,
            height = infraScalar(2.0) * Meter,
            depth = infraScalar(2.0) * Meter,
            weight = infraScalar(1.0) * Kilogram
        )
        val circle = ShapePlacement3(
            shape = column.asPackingShape3(),
            position = QuantityPoint3(
                x = infraScalar(0.0) * Meter,
                y = infraScalar(0.0) * Meter,
                z = infraScalar(0.0) * Meter
            )
        )
        val overlappedRect = ShapePlacement3(
            shape = box.asPackingShape3(),
            position = QuantityPoint3(
                x = infraScalar(1.5) * Meter,
                y = infraScalar(0.0) * Meter,
                z = infraScalar(0.0) * Meter
            )
        )
        val separatedRect = ShapePlacement3(
            shape = box.asPackingShape3(),
            position = QuantityPoint3(
                x = infraScalar(2.1) * Meter,
                y = infraScalar(0.0) * Meter,
                z = infraScalar(0.0) * Meter
            )
        )

        assertTrue(circle overlapped overlappedRect)
        assertFalse(circle overlapped separatedRect)
    }

    @Test
    fun shapePlacement3OverlapShouldRequirePositiveVerticalIntersection() {
        val box = Box(
            width = infraScalar(2.0) * Meter,
            height = infraScalar(2.0) * Meter,
            depth = infraScalar(2.0) * Meter,
            weight = infraScalar(1.0) * Kilogram
        )
        val base = ShapePlacement3(
            shape = box.asPackingShape3(),
            position = QuantityPoint3(
                x = infraScalar(0.0) * Meter,
                y = infraScalar(0.0) * Meter,
                z = infraScalar(0.0) * Meter
            )
        )
        val touchingTop = ShapePlacement3(
            shape = box.asPackingShape3(),
            position = QuantityPoint3(
                x = infraScalar(0.0) * Meter,
                y = infraScalar(2.0) * Meter,
                z = infraScalar(0.0) * Meter
            )
        )
        val crossingTop = ShapePlacement3(
            shape = box.asPackingShape3(),
            position = QuantityPoint3(
                x = infraScalar(0.0) * Meter,
                y = infraScalar(1.9) * Meter,
                z = infraScalar(0.0) * Meter
            )
        )

        assertFalse(base overlapped touchingTop)
        assertTrue(base overlapped crossingTop)
    }

    @Test
    fun quantityPlacement3ShouldConvertToShapePlacement3WithAbsolutePosition() {
        val box = Box(
            width = infraScalar(2.0) * Meter,
            height = infraScalar(3.0) * Meter,
            depth = infraScalar(4.0) * Meter,
            weight = infraScalar(1.0) * Kilogram
        )
        val placement = QuantityPlacement3(
            view = box.view()!!,
            position = QuantityPoint3(
                x = infraScalar(1.0) * Meter,
                y = infraScalar(2.0) * Meter,
                z = infraScalar(3.0) * Meter
            )
        )

        val shapePlacement = placement.asShapePlacement3()

        assertTrue(shapePlacement.x eq placement.absoluteX)
        assertTrue(shapePlacement.y eq placement.absoluteY)
        assertTrue(shapePlacement.z eq placement.absoluteZ)
        assertTrue(shapePlacement.boundingWidth eq placement.width)
        assertTrue(shapePlacement.boundingHeight eq placement.height)
        assertTrue(shapePlacement.boundingDepth eq placement.depth)
    }
}

