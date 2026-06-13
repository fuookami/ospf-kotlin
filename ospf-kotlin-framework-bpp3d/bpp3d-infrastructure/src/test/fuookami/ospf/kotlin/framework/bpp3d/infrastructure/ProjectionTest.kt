/**
 * 投影测试。
 * Projection test.
 */
package fuookami.ospf.kotlin.framework.bpp3d.infrastructure

import kotlin.test.*
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.quantities.unit.*
import fuookami.ospf.kotlin.quantities.quantity.*

class ProjectionTest {
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

    private data class FltXBox(
        override val width: Quantity<FltX>,
        override val height: Quantity<FltX>,
        override val depth: Quantity<FltX>,
        override val weight: Quantity<FltX>
    ) : AbstractCuboid<FltX>

    @Test
    fun bottomPlaneLengthShouldReturnQuantityFltX() {
        val box = Box(
            width = fltX(2.0) * Meter,
            height = fltX(3.0) * Meter,
            depth = fltX(4.0) * Meter,
            weight = fltX(1.0) * Kilogram
        )

        assertTrue(Bottom.length(box) eq (fltX(4.0) * Meter))
        assertTrue(Bottom.width(box) eq (fltX(2.0) * Meter))
        assertTrue(Bottom.height(box) eq (fltX(3.0) * Meter))
        assertTrue(Bottom.length(box, Orientation.UprightRotated) eq (fltX(2.0) * Meter))
    }

    @Test
    fun projectionShapeAreaShouldKeepLengthSquareDimension() {
        val shape = ProjectionShape.invoke(
            length = fltX(2.0) * Meter,
            width = fltX(4.0) * Meter
        )

        assertTrue(shape.length eq (fltX(4.0) * Meter))
        assertTrue(shape.width eq (fltX(2.0) * Meter))
        assertTrue(shape.area eq (fltX(8.0) * SquareMeter))
    }

    @Test
    fun pileAndMultiPileProjectionShouldAggregateCorrectly() {
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
        val viewA = boxA.view()!!
        val viewB = boxB.view(Orientation.Lie)!!

        val planeProjection = PlaneProjection(viewA, Bottom)
        val pile = PileProjection(planeProjection, layer = UInt64(3))

        assertTrue(pile.height eq (fltX(9.0) * Meter))
        assertTrue(pile.weight eq (fltX(6.0) * Kilogram))
        assertEquals(UInt64(3), pile.amount(boxA))

        val multi = MultiPileProjection(listOf(viewA, viewB), Bottom)
        assertTrue(multi.length eq (fltX(5.0) * Meter))
        assertTrue(multi.width eq (fltX(2.0) * Meter))
        assertTrue(multi.height eq (fltX(6.0) * Meter))
        assertTrue(multi.weight eq (fltX(6.0) * Kilogram))
        assertEquals(UInt64.one, multi.amount(boxA))
        assertEquals(UInt64.one, multi.amount(boxB))
    }

    @Test
    fun multiPileToPlacement3ShouldStackByEachViewDepth() {
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
        val viewA = boxA.view(Orientation.Upright)!!
        val viewB = boxB.view(Orientation.Upright)!!
        val multi = MultiPileProjection(listOf(viewA, viewB, viewA), Bottom)

        val placements = multi.toPlacement3At(
            position = QuantityPoint2(
                x = fltX(1.0) * Meter,
                y = fltX(2.0) * Meter
            )
        )

        assertEquals(3, placements.size)
        assertTrue(Bottom.distance(placements[0].position) eq (fltX(0.0) * Meter))
        assertTrue(Bottom.distance(placements[1].position) eq (fltX(4.0) * Meter))
        assertTrue(Bottom.distance(placements[2].position) eq (fltX(7.0) * Meter))
        assertTrue(Bottom.point2(placements[0].position).x eq (fltX(1.0) * Meter))
        assertTrue(Bottom.point2(placements[0].position).y eq (fltX(2.0) * Meter))
        assertTrue(Bottom.point2(placements[1].position).x eq (fltX(1.0) * Meter))
        assertTrue(Bottom.point2(placements[1].position).y eq (fltX(2.0) * Meter))
        assertTrue(placements[0].depth eq (fltX(4.0) * Meter))
        assertTrue(placements[1].depth eq (fltX(3.0) * Meter))
        assertTrue(placements[2].depth eq (fltX(4.0) * Meter))
    }

    @Test
    fun projectionShapeShouldSupportFltXPath() {
        val box = FltXBox(
            width = Quantity(FltX(2.0), Meter),
            height = Quantity(FltX(3.0), Meter),
            depth = Quantity(FltX(4.0), Meter),
            weight = Quantity(FltX(1.0), Kilogram)
        )

        val shape = Bottom.shape(box, Orientation.SideRotated)

        assertTrue(shape.length eq Quantity(FltX(4.0), Meter))
        assertTrue(shape.width eq Quantity(FltX(3.0), Meter))
        assertTrue(shape.area eq Quantity(FltX(12.0), SquareMeter))
    }
}
