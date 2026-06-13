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

class ContainerGeometryContractTest {
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

    private data class PlaneContainer(
        override val shape: Container2Geometry<Bottom, FltX>,
        override val units: List<QuantityPlacement2<*, FltX, Bottom>>
    ) : Container2<PlaneContainer, FltX, Bottom> {
        override fun copy(): PlaneContainer {
            return PlaneContainer(shape = shape, units = units)
        }
    }

    private data class SpaceContainer(
        override val shape: Container3Geometry<FltX>,
        override val units: List<QuantityPlacement3<*, FltX>>
    ) : Container3<SpaceContainer, FltX> {
        override fun copy(): SpaceContainer {
            return SpaceContainer(shape = shape, units = units)
        }
    }

    @Test
    fun containerGeometryShouldSupportFltX() {
        val box = Box(
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

        val rotated = box.view(Orientation.UprightRotated)!!
        assertTrue(rotated.width eq Quantity(FltX(3.0), Meter))
        assertTrue(rotated.height eq Quantity(FltX(2.0), Meter))
        assertTrue(rotated.depth eq Quantity(FltX(1.0), Meter))
    }

    @Test
    fun naturalCuboidShouldUseContainerGeometryDirectly() {
        val box = Box(
            width = fltX(2.0) * Meter,
            height = fltX(3.0) * Meter,
            depth = fltX(4.0) * Meter,
            weight = fltX(5.0) * Kilogram
        )
        val space = Container3Shape(
            width = fltX(4.0) * Meter,
            height = fltX(4.0) * Meter,
            depth = fltX(4.0) * Meter
        )
        val space2 = Container2Shape(
            length = fltX(5.0) * Meter,
            width = fltX(6.0) * Meter,
            plane = Bottom
        )

        assertTrue(box.enabledOrientationsAt(space).contains(Orientation.Upright))
        assertEquals(Orientation.entries.size, box.enabledOrientationsAt(space, withRotation = true).size)
        assertTrue(space2.length eq (fltX(5.0) * Meter))
        assertTrue(space2.width eq (fltX(6.0) * Meter))
    }

    @Test
    fun container2ShouldCountPlacements() {
        val box = Box(
            width = Quantity(FltX(1.0), Meter),
            height = Quantity(FltX(1.0), Meter),
            depth = Quantity(FltX(1.0), Meter),
            weight = Quantity(FltX(2.0), Kilogram)
        )
        val projection = PlaneProjection(box.view(Orientation.Upright)!!, Bottom)
        val container = PlaneContainer(
            shape = QuantityContainer2Shape(
                length = Quantity(FltX(10.0), Meter),
                width = Quantity(FltX(10.0), Meter),
                plane = Bottom
            ),
            units = listOf(
                QuantityPlacement2(
                    projection = projection,
                    position = QuantityPoint2(Quantity(FltX.zero, Meter), Quantity(FltX.zero, Meter))
                ),
                QuantityPlacement2(
                    projection = projection,
                    position = QuantityPoint2(Quantity(FltX(2.0), Meter), Quantity(FltX.zero, Meter))
                )
            )
        )

        assertEquals(UInt64(2), container.amount(box))
        assertTrue(container.contains(box))
    }

    @Test
    fun container3ShouldAggregateWeightAndVolume() {
        val box = Box(
            width = Quantity(FltX(1.0), Meter),
            height = Quantity(FltX(2.0), Meter),
            depth = Quantity(FltX(3.0), Meter),
            weight = Quantity(FltX(4.0), Kilogram)
        )
        val container = SpaceContainer(
            shape = QuantityContainer3Shape(
                width = Quantity(FltX(10.0), Meter),
                height = Quantity(FltX(10.0), Meter),
                depth = Quantity(FltX(10.0), Meter)
            ),
            units = listOf(
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
