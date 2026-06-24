/**
 * 放置测试。
 * Placement Test.
 */
package fuookami.ospf.kotlin.framework.bpp3d.infrastructure

import kotlin.math.*
import kotlin.test.*
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.geometry.Axis3
import fuookami.ospf.kotlin.quantities.unit.*
import fuookami.ospf.kotlin.quantities.quantity.*

class PlacementTest {
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
    fun placement3CoordinatesAndMaxShouldUseQuantity() {
        val box = Box(
            width = FltX(2.0) * Meter,
            height = FltX(3.0) * Meter,
            depth = FltX(4.0) * Meter,
            weight = FltX(1.0) * Kilogram
        )
        val placement = QuantityPlacement3(
            view = box.view()!!,
            position = QuantityPoint3(
                x = FltX(1.0) * Meter,
                y = FltX(2.0) * Meter,
                z = FltX(3.0) * Meter
            )
        )

        assertTrue(placement.x eq (FltX(1.0) * Meter))
        assertTrue(placement.y eq (FltX(2.0) * Meter))
        assertTrue(placement.z eq (FltX(3.0) * Meter))
        assertTrue(placement.maxX eq (FltX(3.0) * Meter))
        assertTrue(placement.maxY eq (FltX(5.0) * Meter))
        assertTrue(placement.maxZ eq (FltX(7.0) * Meter))
    }

    @Test
    fun toPlacement3FromPileShouldExpandLayers() {
        val box = Box(
            width = FltX(2.0) * Meter,
            height = FltX(1.0) * Meter,
            depth = FltX(2.0) * Meter,
            weight = FltX(1.0) * Kilogram
        )
        val projection = PileProjection(
            plane = PlaneProjection(box.view()!!, Bottom),
            layer = UInt64(3)
        )
        val QuantityPlacement2 = QuantityPlacement2(
            projection = projection,
            position = QuantityPoint2(
                x = FltX(0.0) * Meter,
                y = FltX(0.0) * Meter
            )
        )

        val expanded = QuantityPlacement2.toPlacement3()

        assertEquals(3, expanded.size)
        assertTrue(expanded[0].y eq (FltX(0.0) * Meter))
        assertTrue(expanded[1].y eq (FltX(2.0) * Meter))
        assertTrue(expanded[2].y eq (FltX(4.0) * Meter))
    }

    @Test
    fun topAndBottomPlacementsShouldSelectExpectedUnits() {
        val box = Box(
            width = FltX(2.0) * Meter,
            height = FltX(1.0) * Meter,
            depth = FltX(2.0) * Meter,
            weight = FltX(1.0) * Kilogram
        )
        val view = box.view()!!
        val p0 = QuantityPlacement3(view, QuantityPoint3(FltX(0.0) * Meter, FltX(0.0) * Meter, FltX(0.0) * Meter))
        val p1 = QuantityPlacement3(view, QuantityPoint3(FltX(0.0) * Meter, FltX(1.0) * Meter, FltX(0.0) * Meter))
        val p2 = QuantityPlacement3(view, QuantityPoint3(FltX(0.0) * Meter, FltX(2.0) * Meter, FltX(0.0) * Meter))
        val isolated = QuantityPlacement3(view, QuantityPoint3(FltX(10.0) * Meter, FltX(0.0) * Meter, FltX(0.0) * Meter))

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
            radius = FltX(1.0) * Meter,
            height = FltX(2.0) * Meter,
            axis = Axis3.Y,
            weight = FltX(1.0) * Kilogram,
            enabledAxes = listOf(Axis3.Y)
        )
        val shape = column.asPackingShape3()
        val left = ShapePlacement3(
            shape = shape,
            position = QuantityPoint3(
                x = FltX(0.0) * Meter,
                y = FltX(0.0) * Meter,
                z = FltX(0.0) * Meter
            )
        )
        val tangent = ShapePlacement3(
            shape = shape,
            position = QuantityPoint3(
                x = FltX(2.0) * Meter,
                y = FltX(0.0) * Meter,
                z = FltX(0.0) * Meter
            )
        )
        val far = ShapePlacement3(
            shape = shape,
            position = QuantityPoint3(
                x = FltX(2.1) * Meter,
                y = FltX(0.0) * Meter,
                z = FltX(0.0) * Meter
            )
        )

        assertTrue((left overlapped tangent).valueOrFail())
        assertFalse((left overlapped far).valueOrFail())
    }

    @Test
    fun shapePlacement3CircleRectangleOverlapShouldFollowFootprintGeometry() {
        val column = Column(
            radius = FltX(1.0) * Meter,
            height = FltX(2.0) * Meter,
            axis = Axis3.Y,
            weight = FltX(1.0) * Kilogram,
            enabledAxes = listOf(Axis3.Y)
        )
        val box = Box(
            width = FltX(1.0) * Meter,
            height = FltX(2.0) * Meter,
            depth = FltX(2.0) * Meter,
            weight = FltX(1.0) * Kilogram
        )
        val circle = ShapePlacement3(
            shape = column.asPackingShape3(),
            position = QuantityPoint3(
                x = FltX(0.0) * Meter,
                y = FltX(0.0) * Meter,
                z = FltX(0.0) * Meter
            )
        )
        val overlappedRect = ShapePlacement3(
            shape = box.asPackingShape3(),
            position = QuantityPoint3(
                x = FltX(1.5) * Meter,
                y = FltX(0.0) * Meter,
                z = FltX(0.0) * Meter
            )
        )
        val separatedRect = ShapePlacement3(
            shape = box.asPackingShape3(),
            position = QuantityPoint3(
                x = FltX(2.1) * Meter,
                y = FltX(0.0) * Meter,
                z = FltX(0.0) * Meter
            )
        )

        assertTrue((circle overlapped overlappedRect).valueOrFail())
        assertFalse((circle overlapped separatedRect).valueOrFail())
    }

    @Test
    fun shapePlacement3OverlapShouldRequirePositiveVerticalIntersection() {
        val box = Box(
            width = FltX(2.0) * Meter,
            height = FltX(2.0) * Meter,
            depth = FltX(2.0) * Meter,
            weight = FltX(1.0) * Kilogram
        )
        val base = ShapePlacement3(
            shape = box.asPackingShape3(),
            position = QuantityPoint3(
                x = FltX(0.0) * Meter,
                y = FltX(0.0) * Meter,
                z = FltX(0.0) * Meter
            )
        )
        val touchingTop = ShapePlacement3(
            shape = box.asPackingShape3(),
            position = QuantityPoint3(
                x = FltX(0.0) * Meter,
                y = FltX(2.0) * Meter,
                z = FltX(0.0) * Meter
            )
        )
        val crossingTop = ShapePlacement3(
            shape = box.asPackingShape3(),
            position = QuantityPoint3(
                x = FltX(0.0) * Meter,
                y = FltX(1.9) * Meter,
                z = FltX(0.0) * Meter
            )
        )

        assertFalse((base overlapped touchingTop).valueOrFail())
        assertTrue((base overlapped crossingTop).valueOrFail())
    }

    @Test
    fun quantityPlacement3ShouldConvertToShapePlacement3WithAbsolutePosition() {
        val box = Box(
            width = FltX(2.0) * Meter,
            height = FltX(3.0) * Meter,
            depth = FltX(4.0) * Meter,
            weight = FltX(1.0) * Kilogram
        )
        val placement = QuantityPlacement3(
            view = box.view()!!,
            position = QuantityPoint3(
                x = FltX(1.0) * Meter,
                y = FltX(2.0) * Meter,
                z = FltX(3.0) * Meter
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

    @Test
    fun bottomSupportShouldUseCircleFootprintOverlapAreaWhenResolverProvidesCylinderShape() {
        val box = Box(
            width = FltX(2.0) * Meter,
            height = FltX(1.0) * Meter,
            depth = FltX(2.0) * Meter,
            weight = FltX(4.0) * Kilogram
        )
        val topPlacement = QuantityPlacement3(
            view = box.view()!!,
            position = QuantityPoint3(
                x = FltX(0.0) * Meter,
                y = FltX(1.0) * Meter,
                z = FltX(0.0) * Meter
            )
        )
        val bottomPlacement = QuantityPlacement3(
            view = box.view()!!,
            position = QuantityPoint3(
                x = FltX(1.0) * Meter,
                y = FltX(0.0) * Meter,
                z = FltX(0.0) * Meter
            )
        )
        val cylinderShape = Column(
            radius = FltX(1.0) * Meter,
            height = FltX(1.0) * Meter,
            axis = Axis3.Y,
            weight = FltX(4.0) * Kilogram,
            enabledAxes = listOf(Axis3.Y)
        ).asPackingShape3()
        val support = bottomSupport(
            unit = topPlacement,
            bottomUnits = listOf(bottomPlacement),
            shapeResolver = { placement ->
                when (placement) {
                    topPlacement -> cylinderShape
                    bottomPlacement -> cylinderShape
                    else -> placement.view.asPackingShape3()
                }
            }
        )

        val expectedArea = 2.0 * acos(0.5) - 0.5 * sqrt(3.0)
        val expectedWeight = expectedArea / PI * 4.0
        assertTrue(abs(support.area.value.toDouble() - expectedArea) < 1e-6)
        assertTrue(abs(support.weight.value.toDouble() - expectedWeight) < 1e-6)
        assertTrue(support.area.value.toDouble() < 4.0)
    }
}
