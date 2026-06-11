package fuookami.ospf.kotlin.framework.bpp3d.infrastructure

import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.quantities.quantity.eq
import fuookami.ospf.kotlin.quantities.quantity.times
import fuookami.ospf.kotlin.quantities.unit.Kilogram
import fuookami.ospf.kotlin.quantities.unit.Meter
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class QuantityContainerCoreProofTest {
    private data class FltXBox(
        override val width: Quantity<FltX>,
        override val height: Quantity<FltX>,
        override val depth: Quantity<FltX>,
        override val weight: Quantity<FltX>,
        override val enabledOrientations: List<Orientation> = Orientation.entries
    ) : QuantityCuboid<FltXBox, FltX> {
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

    private data class FltXContainer2(
        override val shape: Container2Geometry<Bottom, FltX>,
        override val units: List<QuantityProjectionPlacement2<*, FltX, Bottom>>
    ) : QuantityContainer2<FltXContainer2, FltX, Bottom> {
        override fun copy(): FltXContainer2 {
            return FltXContainer2(shape = shape, units = units)
        }
    }

    private data class FltXContainer3(
        override val shape: Container3Geometry<FltX>,
        override val units: List<QuantityCuboidPlacement3<*, FltX>>
    ) : QuantityContainer3<FltXContainer3, FltX> {
        override fun copy(): FltXContainer3 {
            return FltXContainer3(shape = shape, units = units)
        }
    }

    @Test
    fun quantityContainerAndCuboidShouldSupportFltX() {
        val box = FltXBox(
            width = Quantity(FltX(1.0), Meter),
            height = Quantity(FltX(2.0), Meter),
            depth = Quantity(FltX(3.0), Meter),
            weight = Quantity(FltX(4.0), Kilogram)
        )
        val space = QuantityContainer3Shape(
            width = Quantity(FltX(4.0), Meter),
            height = Quantity(FltX(4.0), Meter),
            depth = Quantity(FltX(4.0), Meter)
        )

        assertTrue(space.enabled(box, Orientation.Upright))
        assertTrue(space.enabled(box, Orientation.UprightRotated))

        val rotated = box.view(Orientation.UprightRotated)
        assertTrue(rotated.width eq Quantity(FltX(3.0), Meter))
        assertTrue(rotated.height eq Quantity(FltX(2.0), Meter))
        assertTrue(rotated.depth eq Quantity(FltX(1.0), Meter))
    }

    @Test
    fun legacyAdapterShouldBridgeToQuantityContainerShape() {
        val box = LegacyBox(
            width = infraScalar(2.0) * Meter,
            height = infraScalar(3.0) * Meter,
            depth = infraScalar(4.0) * Meter,
            weight = infraScalar(5.0) * Kilogram
        )
        val quantityCuboid = box.asQuantityCuboid()
        val space = Container3Shape(
            width = infraScalar(4.0) * Meter,
            height = infraScalar(4.0) * Meter,
            depth = infraScalar(4.0) * Meter
        ).asQuantityContainer3Shape()
        val space2 = Container2Shape(
            length = infraScalar(5.0) * Meter,
            width = infraScalar(6.0) * Meter,
            plane = Bottom
        ).asQuantityContainer2Shape()

        assertTrue(quantityCuboid.enabledOrientationsAt(space).contains(Orientation.Upright))
        assertEquals(Orientation.entries.size, quantityCuboid.enabledOrientationsAt(space, withRotation = true).size)
        assertTrue(space2.length eq (infraScalar(5.0) * Meter))
        assertTrue(space2.width eq (infraScalar(6.0) * Meter))
    }

    @Test
    fun quantityContainer2ShouldCountPlacements() {
        val box = FltXBox(
            width = Quantity(FltX(1.0), Meter),
            height = Quantity(FltX(1.0), Meter),
            depth = Quantity(FltX(1.0), Meter),
            weight = Quantity(FltX(2.0), Kilogram)
        )
        val projection = QuantityPlaneProjection(box.view(Orientation.Upright), Bottom)
        val container = FltXContainer2(
            shape = QuantityContainer2Shape(
                length = Quantity(FltX(10.0), Meter),
                width = Quantity(FltX(10.0), Meter),
                plane = Bottom
            ),
            units = listOf(
                QuantityProjectionPlacement2(
                    projection = projection,
                    position = QuantityPoint2G(Quantity(FltX.zero, Meter), Quantity(FltX.zero, Meter))
                ),
                QuantityProjectionPlacement2(
                    projection = projection,
                    position = QuantityPoint2G(Quantity(FltX(2.0), Meter), Quantity(FltX.zero, Meter))
                )
            )
        )

        assertEquals(UInt64(2), container.amount(box))
        assertTrue(container.contains(box))
    }

    @Test
    fun quantityContainer3ShouldAggregateWeightAndVolume() {
        val box = FltXBox(
            width = Quantity(FltX(1.0), Meter),
            height = Quantity(FltX(2.0), Meter),
            depth = Quantity(FltX(3.0), Meter),
            weight = Quantity(FltX(4.0), Kilogram)
        )
        val container = FltXContainer3(
            shape = QuantityContainer3Shape(
                width = Quantity(FltX(10.0), Meter),
                height = Quantity(FltX(10.0), Meter),
                depth = Quantity(FltX(10.0), Meter)
            ),
            units = listOf(
                QuantityCuboidPlacement3(
                    view = box.view(Orientation.Upright),
                    position = QuantityPoint3G(
                        x = Quantity(FltX.zero, Meter),
                        y = Quantity(FltX.zero, Meter),
                        z = Quantity(FltX.zero, Meter)
                    )
                ),
                QuantityCuboidPlacement3(
                    view = box.view(Orientation.Upright),
                    position = QuantityPoint3G(
                        x = Quantity(FltX(3.0), Meter),
                        y = Quantity(FltX.zero, Meter),
                        z = Quantity(FltX.zero, Meter)
                    )
                )
            )
        )

        assertEquals(UInt64(2), container.amount(box))
        assertTrue(container.weight eq Quantity(FltX(8.0), Kilogram))
        assertTrue(container.actualVolume eq Quantity(FltX(12.0), container.actualVolume.unit))
    }
}

