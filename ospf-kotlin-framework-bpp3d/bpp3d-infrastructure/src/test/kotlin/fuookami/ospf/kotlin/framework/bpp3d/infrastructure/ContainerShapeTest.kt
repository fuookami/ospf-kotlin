package fuookami.ospf.kotlin.framework.bpp3d.infrastructure

import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.quantities.quantity.*
import fuookami.ospf.kotlin.quantities.unit.CubicMeter
import fuookami.ospf.kotlin.quantities.unit.Kilogram
import fuookami.ospf.kotlin.quantities.unit.Meter
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ContainerShapeTest {
    private data class StubCuboid(
        override val width: InfraQuantity,
        override val height: InfraQuantity,
        override val depth: InfraQuantity,
        override val weight: InfraQuantity
    ) : AbstractCuboid<Flt64>

    @Test
    fun volumeShouldKeepLengthCubeDimension() {
        val shape = Container3Shape(
            width = 2.0 * Meter,
            height = 3.0 * Meter,
            depth = 4.0 * Meter
        )

        assertTrue(shape.volume eq (24.0 * CubicMeter))
    }

    @Test
    fun enabledAndMaxAmountShouldRespectOrientationAndLimits() {
        val shape = Container3Shape(
            width = 10.0 * Meter,
            height = 9.0 * Meter,
            depth = 8.0 * Meter
        )
        val unit = StubCuboid(
            width = 2.0 * Meter,
            height = 3.0 * Meter,
            depth = 4.0 * Meter,
            weight = 1.0 * Kilogram
        )

        assertTrue(shape.enabled(unit, Orientation.Upright))
        assertEquals(UInt64(30), shape.maxAmount(unit, Orientation.Upright))
        assertEquals(
            UInt64(4),
            shape.maxAmount(
                unit = unit,
                orientation = Orientation.Upright,
                maxXAmount = UInt64(2),
                maxYAmount = UInt64(2),
                maxZAmount = UInt64(1)
            )
        )

        val rotatedOnlyShape = Container3Shape(
            width = 3.0 * Meter,
            height = 2.0 * Meter,
            depth = 2.0 * Meter
        )
        val rotatedUnit = StubCuboid(
            width = 2.0 * Meter,
            height = 2.0 * Meter,
            depth = 3.0 * Meter,
            weight = 1.0 * Kilogram
        )

        assertFalse(rotatedOnlyShape.enabled(rotatedUnit, Orientation.Upright))
        assertTrue(rotatedOnlyShape.enabled(rotatedUnit, Orientation.UprightRotated))
    }

    @Test
    fun restSpaceShouldSubtractPointAndVectorOffsets() {
        val shape = Container3Shape(
            width = 8.0 * Meter,
            height = 6.0 * Meter,
            depth = 5.0 * Meter
        )
        val pointOffset = QuantityPoint3(
            x = 3.0 * Meter,
            y = 2.0 * Meter,
            z = 1.0 * Meter
        )
        val vectorOffset = QuantityVector3(
            x = 1.0 * Meter,
            y = 1.5 * Meter,
            z = 2.0 * Meter
        )

        val byPoint = shape.restSpace(pointOffset)
        val byVector = shape.restSpace(vectorOffset)

        assertTrue(byPoint.width eq (5.0 * Meter))
        assertTrue(byPoint.height eq (4.0 * Meter))
        assertTrue(byPoint.depth eq (4.0 * Meter))

        assertTrue(byVector.width eq (7.0 * Meter))
        assertTrue(byVector.height eq (4.5 * Meter))
        assertTrue(byVector.depth eq (3.0 * Meter))
    }

    @Test
    fun container3ShapeFrom2DShouldFollowPlaneAxisRules() {
        val bottom2 = Container2Shape(
            length = 4.0 * Meter,
            width = 3.0 * Meter,
            plane = Bottom
        )
        val side2 = Container2Shape(
            length = 4.0 * Meter,
            width = 3.0 * Meter,
            plane = Side
        )
        val front2 = Container2Shape(
            length = 4.0 * Meter,
            width = 3.0 * Meter,
            plane = Front
        )

        val bottom3 = Container3Shape(bottom2)
        val side3 = Container3Shape(side2)
        val front3 = Container3Shape(front2)

        assertTrue(bottom3.width eq (3.0 * Meter))
        assertTrue(bottom3.depth eq (4.0 * Meter))

        assertTrue(side3.width eq (4.0 * Meter))
        assertTrue(side3.height eq (3.0 * Meter))

        assertTrue(front3.width eq (4.0 * Meter))
        assertTrue(front3.height eq (3.0 * Meter))
    }
}

