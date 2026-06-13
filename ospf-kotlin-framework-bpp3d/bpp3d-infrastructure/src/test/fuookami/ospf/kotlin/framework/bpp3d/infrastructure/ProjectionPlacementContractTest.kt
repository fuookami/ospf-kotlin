/**
 * 投影放置契约测试。
 * Projection placement contract test.
 */
package fuookami.ospf.kotlin.framework.bpp3d.infrastructure

import kotlin.test.*
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.quantities.unit.*
import fuookami.ospf.kotlin.quantities.quantity.*

class ProjectionPlacementContractTest {
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
    fun pileProjectionShouldStackByPlaneDistance() {
        val box = Box(
            width = Quantity(FltX(1.0), Meter),
            height = Quantity(FltX(1.0), Meter),
            depth = Quantity(FltX(2.0), Meter),
            weight = Quantity(FltX(3.0), Kilogram)
        )
        val planeProjection = PlaneProjection(
            view = box.view(Orientation.Upright)!!,
            plane = Bottom
        )
        val pile = PileProjection(
            plane = planeProjection,
            layer = UInt64(3)
        )

        val placements = pile.toPlacement3At(
            position = QuantityPoint2(
                x = Quantity(FltX.zero, Meter),
                y = Quantity(FltX.zero, Meter)
            )
        )

        assertEquals(3, placements.size)
        assertTrue(placements[0].position.z eq Quantity(FltX.zero, Meter))
        assertTrue(Bottom.distance(placements[0].position) eq Quantity(FltX.zero, Meter))
        assertTrue(Bottom.distance(placements[1].position) eq Quantity(FltX(2.0), Meter))
        assertTrue(Bottom.distance(placements[2].position) eq Quantity(FltX(4.0), Meter))
    }

    @Test
    fun projectivePlaneShouldReadContainerGeometry() {
        val shape = QuantityContainer3Shape(
            width = Quantity(FltX(3.0), Meter),
            height = Quantity(FltX(5.0), Meter),
            depth = Quantity(FltX(7.0), Meter)
        )

        assertTrue(Bottom.length(shape) eq Quantity(FltX(7.0), Meter))
        assertTrue(Bottom.width(shape) eq Quantity(FltX(3.0), Meter))
        assertTrue(Bottom.height(shape) eq Quantity(FltX(5.0), Meter))
        assertTrue(Side.length(shape) eq Quantity(FltX(3.0), Meter))
        assertTrue(Side.width(shape) eq Quantity(FltX(5.0), Meter))
        assertTrue(Side.height(shape) eq Quantity(FltX(7.0), Meter))
        assertTrue(Front.length(shape) eq Quantity(FltX(7.0), Meter))
        assertTrue(Front.width(shape) eq Quantity(FltX(5.0), Meter))
        assertTrue(Front.height(shape) eq Quantity(FltX(3.0), Meter))
    }

    @Test
    fun placementShouldExposeGeometryWithoutAdapter() {
        val box = Box(
            width = fltX(1.0) * Meter,
            height = fltX(2.0) * Meter,
            depth = fltX(3.0) * Meter,
            weight = fltX(4.0) * Kilogram
        )
        val placement = QuantityPlacement3(
            view = box.view(Orientation.Upright)!!,
            position = point3(
                x = fltX(5.0) * Meter,
                y = fltX(6.0) * Meter,
                z = fltX(7.0) * Meter
            )
        )

        assertTrue(placement.x eq (fltX(5.0) * Meter))
        assertTrue(placement.y eq (fltX(6.0) * Meter))
        assertTrue(placement.z eq (fltX(7.0) * Meter))
        assertTrue(placement.width eq box.width)
        assertTrue(placement.depth eq box.depth)
    }

    @Test
    fun projectionShouldExpandWithoutAdapter() {
        val box = Box(
            width = fltX(1.0) * Meter,
            height = fltX(2.0) * Meter,
            depth = fltX(3.0) * Meter,
            weight = fltX(4.0) * Kilogram
        )
        val projection = PileProjection(
            plane = PlaneProjection(box.view(Orientation.Upright)!!, Bottom),
            layer = UInt64(2)
        )
        val placements = projection.toPlacement3At(
            QuantityPoint2(
                x = fltX(0.0) * Meter,
                y = fltX(0.0) * Meter
            )
        )

        assertEquals(2, placements.size)
        assertTrue(projection.length eq (fltX(3.0) * Meter))
        assertTrue(projection.width eq (fltX(1.0) * Meter))
    }

    @Test
    fun placement2ShouldSupportContainsAndIntersection() {
        val box = Box(
            width = Quantity(FltX(2.0), Meter),
            height = Quantity(FltX(1.0), Meter),
            depth = Quantity(FltX(2.0), Meter),
            weight = Quantity(FltX(1.0), Kilogram)
        )
        val left = QuantityPlacement2(
            projection = PlaneProjection(box.view(Orientation.Upright)!!, Bottom),
            position = QuantityPoint2(
                x = Quantity(FltX.zero, Meter),
                y = Quantity(FltX.zero, Meter)
            )
        )
        val right = QuantityPlacement2(
            projection = PlaneProjection(box.view(Orientation.Upright)!!, Bottom),
            position = QuantityPoint2(
                x = Quantity(FltX(1.0), Meter),
                y = Quantity(FltX.zero, Meter)
            )
        )
        val inside = QuantityPoint2(
            x = Quantity(FltX(0.5), Meter),
            y = Quantity(FltX(0.5), Meter)
        )
        val intersect = left.intersect(right)

        assertTrue(left.contains(inside))
        assertTrue(left.overlapped(right))
        assertTrue(intersect != null)
        assertTrue(intersect!!.width eq Quantity(FltX(1.0), Meter))
        assertTrue(intersect.height eq Quantity(FltX(2.0), Meter))
    }

    @Test
    fun topAndBottomPlacementsShouldBeComputable() {
        val box = Box(
            width = Quantity(FltX(2.0), Meter),
            height = Quantity(FltX(1.0), Meter),
            depth = Quantity(FltX(2.0), Meter),
            weight = Quantity(FltX(1.0), Kilogram)
        )
        val pileProjection = PileProjection(
            plane = PlaneProjection(box.view(Orientation.Upright)!!, Bottom),
            layer = UInt64(2)
        )
        val placements: List<QuantityPlacement3<*, FltX>> = listOf(
            QuantityPlacement3(
                view = box.view(Orientation.Upright)!!,
                position = QuantityPoint3(
                    x = Quantity(FltX.zero, Meter),
                    y = Quantity(FltX.zero, Meter),
                    z = Quantity(FltX.zero, Meter)
                )
            ),
            QuantityPlacement3(
                view = box.view(Orientation.Upright)!!,
                position = QuantityPoint3(
                    x = Quantity(FltX.zero, Meter),
                    y = Quantity(FltX(0.5), Meter),
                    z = Quantity(FltX.zero, Meter)
                )
            ),
            QuantityPlacement3(
                view = box.view(Orientation.Upright)!!,
                position = QuantityPoint3(
                    x = Quantity(FltX(5.0), Meter),
                    y = Quantity(FltX.zero, Meter),
                    z = Quantity(FltX.zero, Meter)
                )
            )
        )

        val tops = topPlacements(placements)
        val bottoms = bottomPlacements(placements)
        val stackedLow = placements[0]
        val stackedHigh = placements[1]
        val isolated = placements[2]

        assertTrue(tops.contains(stackedHigh))
        assertTrue(tops.contains(isolated))
        assertTrue(!tops.contains(stackedLow))
        assertTrue(bottoms.contains(stackedLow))
        assertTrue(bottoms.contains(isolated))
        assertTrue(!bottoms.contains(stackedHigh))
        assertTrue((stackedHigh.y gr stackedLow.y) == true)
        assertTrue((stackedLow.y ls stackedHigh.y) == true)
        assertEquals(2, pileProjection.toPlacement3At(QuantityPoint2(Quantity(FltX.zero, Meter), Quantity(FltX.zero, Meter))).size)
    }
}
