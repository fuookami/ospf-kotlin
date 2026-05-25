package fuookami.ospf.kotlin.framework.bpp3d.infrastructure

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
        override val width: QuantityFlt64,
        override val height: QuantityFlt64,
        override val depth: QuantityFlt64,
        override val weight: QuantityFlt64,
        override val enabledOrientations: List<Orientation> = Orientation.entries
    ) : Cuboid<Box> {
        override val self: Box
            get() = this
    }

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
        val shape = ProjectionShape.invoke(
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
}
