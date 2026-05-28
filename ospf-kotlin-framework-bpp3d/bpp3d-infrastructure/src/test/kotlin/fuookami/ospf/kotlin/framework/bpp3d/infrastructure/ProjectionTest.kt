package fuookami.ospf.kotlin.framework.bpp3d.infrastructure

import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.quantities.quantity.*
import fuookami.ospf.kotlin.quantities.unit.Kilogram
import fuookami.ospf.kotlin.quantities.unit.Meter
import fuookami.ospf.kotlin.quantities.unit.SquareMeter
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ProjectionTest {
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

    private data class FltXBox(
        override val width: Quantity<FltX>,
        override val height: Quantity<FltX>,
        override val depth: Quantity<FltX>,
        override val weight: Quantity<FltX>
    ) : AbstractCuboid<FltX>

    @Test
    fun bottomPlaneLengthShouldReturnQuantityFlt64() {
        val box = Box(
            width = 2.0 * Meter,
            height = 3.0 * Meter,
            depth = 4.0 * Meter,
            weight = 1.0 * Kilogram
        )

        assertTrue(Bottom.length(box) eq (4.0 * Meter))
        assertTrue(Bottom.width(box) eq (2.0 * Meter))
        assertTrue(Bottom.height(box) eq (3.0 * Meter))
        assertTrue(Bottom.length(box, Orientation.UprightRotated) eq (2.0 * Meter))
    }

    @Test
    fun projectionShapeAreaShouldKeepLengthSquareDimension() {
        val shape = ProjectionShapeG.invoke(
            length = 2.0 * Meter,
            width = 4.0 * Meter
        )

        assertTrue(shape.length eq (4.0 * Meter))
        assertTrue(shape.width eq (2.0 * Meter))
        assertTrue(shape.area eq (8.0 * SquareMeter))
    }

    @Test
    fun pileAndMultiPileProjectionShouldAggregateCorrectly() {
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
        val viewA = boxA.view()!!
        val viewB = boxB.view(Orientation.Lie)!!

        val planeProjection = PlaneProjection(viewA, Bottom)
        val pile = PileProjection(planeProjection, layer = UInt64(3))

        assertTrue(pile.height eq (9.0 * Meter))
        assertTrue(pile.weight eq (6.0 * Kilogram))
        assertEquals(UInt64(3), pile.amount(boxA))

        val multi = MultiPileProjection(listOf(viewA, viewB), Bottom)
        assertTrue(multi.length eq (5.0 * Meter))
        assertTrue(multi.width eq (2.0 * Meter))
        assertTrue(multi.height eq (6.0 * Meter))
        assertTrue(multi.weight eq (6.0 * Kilogram))
        assertEquals(UInt64.one, multi.amount(boxA))
        assertEquals(UInt64.one, multi.amount(boxB))
    }

    @Test
    fun multiPileToPlacement3ShouldStackByEachViewDepth() {
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
        val viewA = boxA.view(Orientation.Upright)!!
        val viewB = boxB.view(Orientation.Upright)!!
        val multi = MultiPileProjection(listOf(viewA, viewB, viewA), Bottom)

        val placements = multi.toPlacement3At(
            position = QuantityPoint2(
                x = 1.0 * Meter,
                y = 2.0 * Meter
            )
        )

        assertEquals(3, placements.size)
        assertTrue(Bottom.distance(placements[0].position) eq (0.0 * Meter))
        assertTrue(Bottom.distance(placements[1].position) eq (4.0 * Meter))
        assertTrue(Bottom.distance(placements[2].position) eq (7.0 * Meter))
        assertTrue(Bottom.point2(placements[0].position).x eq (1.0 * Meter))
        assertTrue(Bottom.point2(placements[0].position).y eq (2.0 * Meter))
        assertTrue(Bottom.point2(placements[1].position).x eq (1.0 * Meter))
        assertTrue(Bottom.point2(placements[1].position).y eq (2.0 * Meter))
        assertTrue(placements[0].depth eq (4.0 * Meter))
        assertTrue(placements[1].depth eq (3.0 * Meter))
        assertTrue(placements[2].depth eq (4.0 * Meter))
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
