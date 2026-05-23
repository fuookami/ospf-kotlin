package fuookami.ospf.kotlin.framework.bpp3d.infrastructure

import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.quantities.quantity.*
import fuookami.ospf.kotlin.quantities.unit.Kilogram
import fuookami.ospf.kotlin.quantities.unit.Meter
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PlacementTest {
    private data class Box(
        override val width: QuantityFlt64,
        override val height: QuantityFlt64,
        override val depth: QuantityFlt64,
        override val weight: QuantityFlt64,
        override val enabledOrientations: List<Orientation> = Orientation.entries
    ) : Cuboid<Box>

    @Test
    fun placement3CoordinatesAndMaxShouldUseQuantity() {
        val box = Box(
            width = 2.0 * Meter,
            height = 3.0 * Meter,
            depth = 4.0 * Meter,
            weight = 1.0 * Kilogram
        )
        val placement = Placement3(
            view = box.view()!!,
            position = QuantityPoint3(
                x = 1.0 * Meter,
                y = 2.0 * Meter,
                z = 3.0 * Meter
            )
        )

        assertTrue(placement.x eq (1.0 * Meter))
        assertTrue(placement.y eq (2.0 * Meter))
        assertTrue(placement.z eq (3.0 * Meter))
        assertTrue(placement.maxX eq (3.0 * Meter))
        assertTrue(placement.maxY eq (5.0 * Meter))
        assertTrue(placement.maxZ eq (7.0 * Meter))
    }

    @Test
    fun toPlacement3FromPileShouldExpandLayers() {
        val box = Box(
            width = 2.0 * Meter,
            height = 1.0 * Meter,
            depth = 2.0 * Meter,
            weight = 1.0 * Kilogram
        )
        val projection = PileProjection(
            plane = PlaneProjection(box.view()!!, Bottom),
            layer = UInt64(3)
        )
        val placement2 = Placement2(
            projection = projection,
            position = QuantityPoint2(
                x = 0.0 * Meter,
                y = 0.0 * Meter
            )
        )

        val expanded = placement2.toPlacement3()

        assertEquals(3, expanded.size)
        assertTrue(expanded[0].y eq (0.0 * Meter))
        assertTrue(expanded[1].y eq (2.0 * Meter))
        assertTrue(expanded[2].y eq (4.0 * Meter))
    }

    @Test
    fun topAndBottomPlacementsShouldSelectExpectedUnits() {
        val box = Box(
            width = 2.0 * Meter,
            height = 1.0 * Meter,
            depth = 2.0 * Meter,
            weight = 1.0 * Kilogram
        )
        val view = box.view()!!
        val p0 = Placement3(view, QuantityPoint3(0.0 * Meter, 0.0 * Meter, 0.0 * Meter))
        val p1 = Placement3(view, QuantityPoint3(0.0 * Meter, 1.0 * Meter, 0.0 * Meter))
        val p2 = Placement3(view, QuantityPoint3(0.0 * Meter, 2.0 * Meter, 0.0 * Meter))
        val isolated = Placement3(view, QuantityPoint3(10.0 * Meter, 0.0 * Meter, 0.0 * Meter))

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
}
