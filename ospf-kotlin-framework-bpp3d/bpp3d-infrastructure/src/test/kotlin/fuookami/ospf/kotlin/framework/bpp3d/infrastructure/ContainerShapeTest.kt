package fuookami.ospf.kotlin.framework.bpp3d.infrastructure

import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.math.geometry.Axis3
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
    ) : AbstractCuboid<InfraNumber>

    @Test
    fun volumeShouldKeepLengthCubeDimension() {
        val shape = Container3Shape(
            width = infraScalar(2.0) * Meter,
            height = infraScalar(3.0) * Meter,
            depth = infraScalar(4.0) * Meter
        )

        assertTrue(shape.volume eq (infraScalar(24.0) * CubicMeter))
    }

    @Test
    fun enabledAndMaxAmountShouldRespectOrientationAndLimits() {
        val shape = Container3Shape(
            width = infraScalar(10.0) * Meter,
            height = infraScalar(9.0) * Meter,
            depth = infraScalar(8.0) * Meter
        )
        val unit = StubCuboid(
            width = infraScalar(2.0) * Meter,
            height = infraScalar(3.0) * Meter,
            depth = infraScalar(4.0) * Meter,
            weight = infraScalar(1.0) * Kilogram
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
            width = infraScalar(3.0) * Meter,
            height = infraScalar(2.0) * Meter,
            depth = infraScalar(2.0) * Meter
        )
        val rotatedUnit = StubCuboid(
            width = infraScalar(2.0) * Meter,
            height = infraScalar(2.0) * Meter,
            depth = infraScalar(3.0) * Meter,
            weight = infraScalar(1.0) * Kilogram
        )

        assertFalse(rotatedOnlyShape.enabled(rotatedUnit, Orientation.Upright))
        assertTrue(rotatedOnlyShape.enabled(rotatedUnit, Orientation.UprightRotated))
    }

    @Test
    fun restSpaceShouldSubtractPointAndVectorOffsets() {
        val shape = Container3Shape(
            width = infraScalar(8.0) * Meter,
            height = infraScalar(6.0) * Meter,
            depth = infraScalar(5.0) * Meter
        )
        val pointOffset = QuantityPoint3(
            x = infraScalar(3.0) * Meter,
            y = infraScalar(2.0) * Meter,
            z = infraScalar(1.0) * Meter
        )
        val vectorOffset = QuantityVector3(
            x = infraScalar(1.0) * Meter,
            y = infraScalar(1.5) * Meter,
            z = infraScalar(2.0) * Meter
        )

        val byPoint = shape.restSpace(pointOffset)
        val byVector = shape.restSpace(vectorOffset)

        assertTrue(byPoint.width eq (infraScalar(5.0) * Meter))
        assertTrue(byPoint.height eq (infraScalar(4.0) * Meter))
        assertTrue(byPoint.depth eq (infraScalar(4.0) * Meter))

        assertTrue(byVector.width eq (infraScalar(7.0) * Meter))
        assertTrue(byVector.height eq (infraScalar(4.5) * Meter))
        assertTrue(byVector.depth eq (infraScalar(3.0) * Meter))
    }

    @Test
    fun container3ShapeFrom2DShouldFollowPlaneAxisRules() {
        val bottom2 = Container2Shape(
            length = infraScalar(4.0) * Meter,
            width = infraScalar(3.0) * Meter,
            plane = Bottom
        )
        val side2 = Container2Shape(
            length = infraScalar(4.0) * Meter,
            width = infraScalar(3.0) * Meter,
            plane = Side
        )
        val front2 = Container2Shape(
            length = infraScalar(4.0) * Meter,
            width = infraScalar(3.0) * Meter,
            plane = Front
        )

        val bottom3 = Container3Shape(bottom2)
        val side3 = Container3Shape(side2)
        val front3 = Container3Shape(front2)

        assertTrue(bottom3.width eq (infraScalar(3.0) * Meter))
        assertTrue(bottom3.depth eq (infraScalar(4.0) * Meter))

        assertTrue(side3.width eq (infraScalar(4.0) * Meter))
        assertTrue(side3.height eq (infraScalar(3.0) * Meter))

        assertTrue(front3.width eq (infraScalar(4.0) * Meter))
        assertTrue(front3.height eq (infraScalar(3.0) * Meter))
    }

    @Test
    fun enabledWithVerticalCylinderShapeShouldUseRadiusBoundary() {
        val shape = Container3Shape(
            width = infraScalar(10.0) * Meter,
            height = infraScalar(6.0) * Meter,
            depth = infraScalar(8.0) * Meter
        )
        val cylinder = object : AbstractCylinder<InfraNumber> {
            override val radius = infraScalar(1.5) * Meter
            override val height = infraScalar(4.0) * Meter
            override val axis = Axis3.Y
            override val weight = infraScalar(2.0) * Kilogram
        }.asPackingShape3()

        assertTrue(
            shape.enabled(
                shape = cylinder,
                position = QuantityPoint3(
                    x = infraScalar(2.0) * Meter,
                    y = infraScalar(1.0) * Meter,
                    z = infraScalar(3.0) * Meter
                )
            )
        )

        assertFalse(
            shape.enabled(
                shape = cylinder,
                position = QuantityPoint3(
                    x = infraScalar(8.0) * Meter,
                    y = infraScalar(1.0) * Meter,
                    z = infraScalar(3.0) * Meter
                )
            )
        )

        assertFalse(
            shape.enabled(
                shape = cylinder,
                position = QuantityPoint3(
                    x = infraScalar(-0.1) * Meter,
                    y = infraScalar(1.0) * Meter,
                    z = infraScalar(3.0) * Meter
                )
            )
        )
    }
}

