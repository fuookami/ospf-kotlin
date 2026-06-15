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
            width = FltX(2.0) * Meter,
            height = FltX(3.0) * Meter,
            depth = FltX(4.0) * Meter,
            weight = FltX(1.0) * Kilogram
        )

        assertTrue(Bottom.length(box) eq (FltX(4.0) * Meter))
        assertTrue(Bottom.width(box) eq (FltX(2.0) * Meter))
        assertTrue(Bottom.height(box) eq (FltX(3.0) * Meter))
        assertTrue(Bottom.length(box, Orientation.UprightRotated) eq (FltX(2.0) * Meter))
    }

    @Test
    fun projectionShapeAreaShouldKeepLengthSquareDimension() {
        val shape = ProjectionShape.invoke(
            length = FltX(2.0) * Meter,
            width = FltX(4.0) * Meter
        )

        assertTrue(shape.length eq (FltX(4.0) * Meter))
        assertTrue(shape.width eq (FltX(2.0) * Meter))
        assertTrue(shape.area eq (FltX(8.0) * SquareMeter))
    }

    @Test
    fun pileAndMultiPileProjectionShouldAggregateCorrectly() {
        val boxA = Box(
            width = FltX(2.0) * Meter,
            height = FltX(3.0) * Meter,
            depth = FltX(4.0) * Meter,
            weight = FltX(2.0) * Kilogram
        )
        val boxB = Box(
            width = FltX(1.0) * Meter,
            height = FltX(5.0) * Meter,
            depth = FltX(3.0) * Meter,
            weight = FltX(4.0) * Kilogram
        )
        val viewA = boxA.view()!!
        val viewB = boxB.view(Orientation.Lie)!!

        val planeProjection = PlaneProjection(viewA, Bottom)
        val pile = PileProjection(planeProjection, layer = UInt64(3))

        assertTrue(pile.height eq (FltX(9.0) * Meter))
        assertTrue(pile.weight eq (FltX(6.0) * Kilogram))
        assertEquals(UInt64(3), pile.amount(boxA))

        val multi = MultiPileProjection(listOf(viewA, viewB), Bottom)
        assertTrue(multi.length eq (FltX(5.0) * Meter))
        assertTrue(multi.width eq (FltX(2.0) * Meter))
        assertTrue(multi.height eq (FltX(6.0) * Meter))
        assertTrue(multi.weight eq (FltX(6.0) * Kilogram))
        assertEquals(UInt64.one, multi.amount(boxA))
        assertEquals(UInt64.one, multi.amount(boxB))
    }

    @Test
    fun multiPileToPlacement3ShouldStackByEachViewDepth() {
        val boxA = Box(
            width = FltX(2.0) * Meter,
            height = FltX(3.0) * Meter,
            depth = FltX(4.0) * Meter,
            weight = FltX(2.0) * Kilogram
        )
        val boxB = Box(
            width = FltX(1.0) * Meter,
            height = FltX(5.0) * Meter,
            depth = FltX(3.0) * Meter,
            weight = FltX(4.0) * Kilogram
        )
        val viewA = boxA.view(Orientation.Upright)!!
        val viewB = boxB.view(Orientation.Upright)!!
        val multi = MultiPileProjection(listOf(viewA, viewB, viewA), Bottom)

        val placements = multi.toPlacement3At(
            position = QuantityPoint2(
                x = FltX(1.0) * Meter,
                y = FltX(2.0) * Meter
            )
        )

        assertEquals(3, placements.size)
        assertTrue(Bottom.distance(placements[0].position) eq (FltX(0.0) * Meter))
        assertTrue(Bottom.distance(placements[1].position) eq (FltX(4.0) * Meter))
        assertTrue(Bottom.distance(placements[2].position) eq (FltX(7.0) * Meter))
        assertTrue(Bottom.point2(placements[0].position).x eq (FltX(1.0) * Meter))
        assertTrue(Bottom.point2(placements[0].position).y eq (FltX(2.0) * Meter))
        assertTrue(Bottom.point2(placements[1].position).x eq (FltX(1.0) * Meter))
        assertTrue(Bottom.point2(placements[1].position).y eq (FltX(2.0) * Meter))
        assertTrue(placements[0].depth eq (FltX(4.0) * Meter))
        assertTrue(placements[1].depth eq (FltX(3.0) * Meter))
        assertTrue(placements[2].depth eq (FltX(4.0) * Meter))
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
