package fuookami.ospf.kotlin.framework.bpp3d.infrastructure

import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.quantities.quantity.eq
import fuookami.ospf.kotlin.quantities.quantity.gr
import fuookami.ospf.kotlin.quantities.quantity.ls
import fuookami.ospf.kotlin.quantities.quantity.times
import fuookami.ospf.kotlin.quantities.unit.Kilogram
import fuookami.ospf.kotlin.quantities.unit.Meter
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GenericProjectionPlacementCoreProofTest {
    private data class FltXBox(
        override val width: Quantity<FltX>,
        override val height: Quantity<FltX>,
        override val depth: Quantity<FltX>,
        override val weight: Quantity<FltX>,
        override val enabledOrientations: List<Orientation> = Orientation.entries
    ) : GenericCuboid<FltXBox, FltX> {
        override val self: FltXBox
            get() = this
    }

    private data class LegacyBox(
        override val width: InfraQuantity,
        override val height: InfraQuantity,
        override val depth: InfraQuantity,
        override val weight: InfraQuantity,
        override val enabledOrientations: List<Orientation> = Orientation.entries
    ) : Cuboid<LegacyBox> {
        override val self: LegacyBox
            get() = this
    }

    @Test
    fun genericPileProjectionShouldStackByPlaneDistance() {
        val box = FltXBox(
            width = Quantity(FltX(1.0), Meter),
            height = Quantity(FltX(1.0), Meter),
            depth = Quantity(FltX(2.0), Meter),
            weight = Quantity(FltX(3.0), Kilogram)
        )
        val planeProjection = GenericPlaneProjection(
            view = box.view(Orientation.Upright),
            plane = Bottom
        )
        val pile = GenericPileProjection(
            planeProjection = planeProjection,
            layer = UInt64(3)
        )

        val placements = pile.toPlacement3At(
            position = QuantityPoint2G(
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
    fun projectivePlaneShouldReadGenericContainerShape() {
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
    fun legacyPlacementShouldBridgeToGenericPlacement() {
        val box = LegacyBox(
            width = 1.0 * Meter,
            height = 2.0 * Meter,
            depth = 3.0 * Meter,
            weight = 4.0 * Kilogram
        )
        val legacyPlacement = QuantityPlacement3(
            view = box.view(Orientation.Upright)!!,
            position = point3(
                x = 5.0 * Meter,
                y = 6.0 * Meter,
                z = 7.0 * Meter
            )
        )
        val genericPlacement = legacyPlacement.asGenericPlacement3()

        assertTrue(genericPlacement.x eq (5.0 * Meter))
        assertTrue(genericPlacement.y eq (6.0 * Meter))
        assertTrue(genericPlacement.z eq (7.0 * Meter))
        assertTrue(genericPlacement.width eq box.width)
        assertTrue(genericPlacement.depth eq box.depth)
    }

    @Test
    fun legacyProjectionShouldBridgeToGenericProjection() {
        val box = LegacyBox(
            width = 1.0 * Meter,
            height = 2.0 * Meter,
            depth = 3.0 * Meter,
            weight = 4.0 * Kilogram
        )
        val legacyProjection = PileProjection(
            plane = PlaneProjection(box.view(Orientation.Upright)!!, Bottom),
            layer = UInt64(2)
        )
        val genericProjection = legacyProjection.asGenericProjection()
        val placements = genericProjection.toPlacement3At(
            QuantityPoint2G(
                x = 0.0 * Meter,
                y = 0.0 * Meter
            )
        )

        assertEquals(2, placements.size)
        assertTrue(genericProjection.length eq legacyProjection.length)
        assertTrue(genericProjection.width eq legacyProjection.width)
    }

    @Test
    fun genericPlacementShouldSupportContainsAndIntersection() {
        val box = FltXBox(
            width = Quantity(FltX(2.0), Meter),
            height = Quantity(FltX(1.0), Meter),
            depth = Quantity(FltX(2.0), Meter),
            weight = Quantity(FltX(1.0), Kilogram)
        )
        val left = GenericQuantityPlacement2(
            projection = GenericPlaneProjection(box.view(Orientation.Upright), Bottom),
            position = QuantityPoint2G(
                x = Quantity(FltX.zero, Meter),
                y = Quantity(FltX.zero, Meter)
            )
        )
        val right = GenericQuantityPlacement2(
            projection = GenericPlaneProjection(box.view(Orientation.Upright), Bottom),
            position = QuantityPoint2G(
                x = Quantity(FltX(1.0), Meter),
                y = Quantity(FltX.zero, Meter)
            )
        )
        val inside = QuantityPoint2G(
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
    fun genericTopAndBottomPlacementsShouldBeComputable() {
        val box = FltXBox(
            width = Quantity(FltX(2.0), Meter),
            height = Quantity(FltX(1.0), Meter),
            depth = Quantity(FltX(2.0), Meter),
            weight = Quantity(FltX(1.0), Kilogram)
        )
        val pileProjection = GenericPileProjection(
            planeProjection = GenericPlaneProjection(box.view(Orientation.Upright), Bottom),
            layer = UInt64(2)
        )
        val placements = listOf(
            GenericQuantityPlacement3(
                view = box.view(Orientation.Upright),
                position = QuantityPoint3G(
                    x = Quantity(FltX.zero, Meter),
                    y = Quantity(FltX.zero, Meter),
                    z = Quantity(FltX.zero, Meter)
                )
            ),
            GenericQuantityPlacement3(
                view = box.view(Orientation.Upright),
                position = QuantityPoint3G(
                    x = Quantity(FltX.zero, Meter),
                    y = Quantity(FltX(0.5), Meter),
                    z = Quantity(FltX.zero, Meter)
                )
            ),
            GenericQuantityPlacement3(
                view = box.view(Orientation.Upright),
                position = QuantityPoint3G(
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
        assertEquals(2, pileProjection.toPlacement3At(QuantityPoint2G(Quantity(FltX.zero, Meter), Quantity(FltX.zero, Meter))).size)
    }
}
