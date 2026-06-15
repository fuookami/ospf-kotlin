/**
 * 容器形状测试。
 * Container shape test.
 */
package fuookami.ospf.kotlin.framework.bpp3d.infrastructure

import kotlin.test.*
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.geometry.Axis3
import fuookami.ospf.kotlin.quantities.unit.*
import fuookami.ospf.kotlin.quantities.quantity.*

class ContainerShapeTest {
    private data class StubCuboid(
        override val width: Quantity<FltX>,
        override val height: Quantity<FltX>,
        override val depth: Quantity<FltX>,
        override val weight: Quantity<FltX>
    ) : AbstractCuboid<FltX>

    @Test
    fun volumeShouldKeepLengthCubeDimension() {
        val shape = Container3Shape(
            width = FltX(2.0) * Meter,
            height = FltX(3.0) * Meter,
            depth = FltX(4.0) * Meter
        )

        assertTrue(shape.volume eq (FltX(24.0) * CubicMeter))
    }

    @Test
    fun enabledAndMaxAmountShouldRespectOrientationAndLimits() {
        val shape = Container3Shape(
            width = FltX(10.0) * Meter,
            height = FltX(9.0) * Meter,
            depth = FltX(8.0) * Meter
        )
        val unit = StubCuboid(
            width = FltX(2.0) * Meter,
            height = FltX(3.0) * Meter,
            depth = FltX(4.0) * Meter,
            weight = FltX(1.0) * Kilogram
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
            width = FltX(3.0) * Meter,
            height = FltX(2.0) * Meter,
            depth = FltX(2.0) * Meter
        )
        val rotatedUnit = StubCuboid(
            width = FltX(2.0) * Meter,
            height = FltX(2.0) * Meter,
            depth = FltX(3.0) * Meter,
            weight = FltX(1.0) * Kilogram
        )

        assertFalse(rotatedOnlyShape.enabled(rotatedUnit, Orientation.Upright))
        assertTrue(rotatedOnlyShape.enabled(rotatedUnit, Orientation.UprightRotated))
    }

    @Test
    fun restSpaceShouldSubtractPointAndVectorOffsets() {
        val shape = Container3Shape(
            width = FltX(8.0) * Meter,
            height = FltX(6.0) * Meter,
            depth = FltX(5.0) * Meter
        )
        val pointOffset = QuantityPoint3(
            x = FltX(3.0) * Meter,
            y = FltX(2.0) * Meter,
            z = FltX(1.0) * Meter
        )
        val vectorOffset = QuantityVector3(
            x = FltX(1.0) * Meter,
            y = FltX(1.5) * Meter,
            z = FltX(2.0) * Meter
        )

        val byPoint = shape.restSpace(pointOffset)
        val byVector = shape.restSpace(vectorOffset)

        assertTrue(byPoint.width eq (FltX(5.0) * Meter))
        assertTrue(byPoint.height eq (FltX(4.0) * Meter))
        assertTrue(byPoint.depth eq (FltX(4.0) * Meter))

        assertTrue(byVector.width eq (FltX(7.0) * Meter))
        assertTrue(byVector.height eq (FltX(4.5) * Meter))
        assertTrue(byVector.depth eq (FltX(3.0) * Meter))
    }

    @Test
    fun container3ShapeFrom2DShouldFollowPlaneAxisRules() {
        val bottom2 = Container2Shape(
            length = FltX(4.0) * Meter,
            width = FltX(3.0) * Meter,
            plane = Bottom
        )
        val side2 = Container2Shape(
            length = FltX(4.0) * Meter,
            width = FltX(3.0) * Meter,
            plane = Side
        )
        val front2 = Container2Shape(
            length = FltX(4.0) * Meter,
            width = FltX(3.0) * Meter,
            plane = Front
        )

        val bottom3 = Container3Shape(bottom2)
        val side3 = Container3Shape(side2)
        val front3 = Container3Shape(front2)

        assertTrue(bottom3.width eq (FltX(3.0) * Meter))
        assertTrue(bottom3.depth eq (FltX(4.0) * Meter))

        assertTrue(side3.width eq (FltX(4.0) * Meter))
        assertTrue(side3.height eq (FltX(3.0) * Meter))

        assertTrue(front3.width eq (FltX(4.0) * Meter))
        assertTrue(front3.height eq (FltX(3.0) * Meter))
    }

    @Test
    fun enabledWithVerticalCylinderShapeShouldUseRadiusBoundary() {
        val shape = Container3Shape(
            width = FltX(10.0) * Meter,
            height = FltX(6.0) * Meter,
            depth = FltX(8.0) * Meter
        )
        val cylinder = object : AbstractCylinder<FltX> {
            override val radius = FltX(1.5) * Meter
            override val height = FltX(4.0) * Meter
            override val axis = Axis3.Y
            override val weight = FltX(2.0) * Kilogram
        }.asPackingShape3()

        assertTrue(
            shape.enabled(
                shape = cylinder,
                position = QuantityPoint3(
                    x = FltX(2.0) * Meter,
                    y = FltX(1.0) * Meter,
                    z = FltX(3.0) * Meter
                )
            )
        )

        assertFalse(
            shape.enabled(
                shape = cylinder,
                position = QuantityPoint3(
                    x = FltX(8.0) * Meter,
                    y = FltX(1.0) * Meter,
                    z = FltX(3.0) * Meter
                )
            )
        )

        assertFalse(
            shape.enabled(
                shape = cylinder,
                position = QuantityPoint3(
                    x = FltX(-0.1) * Meter,
                    y = FltX(1.0) * Meter,
                    z = FltX(3.0) * Meter
                )
            )
        )
    }

    @Test
    fun enabledWithHorizontalCylinderShapeShouldUseBoundingBoxBoundary() {
        val shape = Container3Shape(
            width = FltX(10.0) * Meter,
            height = FltX(6.0) * Meter,
            depth = FltX(8.0) * Meter
        )
        val cylinderX = object : AbstractCylinder<FltX> {
            override val radius = FltX(1.0) * Meter
            override val height = FltX(5.0) * Meter
            override val axis = Axis3.X
            override val weight = FltX(2.0) * Kilogram
        }.asPackingShape3()
        val cylinderZ = object : AbstractCylinder<FltX> {
            override val radius = FltX(1.0) * Meter
            override val height = FltX(5.0) * Meter
            override val axis = Axis3.Z
            override val weight = FltX(2.0) * Kilogram
        }.asPackingShape3()

        assertTrue(
            shape.enabled(
                shape = cylinderX,
                position = QuantityPoint3(
                    x = FltX(5.0) * Meter,
                    y = FltX(4.0) * Meter,
                    z = FltX(6.0) * Meter
                )
            )
        )

        assertFalse(
            shape.enabled(
                shape = cylinderX,
                position = QuantityPoint3(
                    x = FltX(6.0) * Meter,
                    y = FltX(4.0) * Meter,
                    z = FltX(6.0) * Meter
                )
            )
        )

        assertTrue(
            shape.enabled(
                shape = cylinderZ,
                position = QuantityPoint3(
                    x = FltX(8.0) * Meter,
                    y = FltX(4.0) * Meter,
                    z = FltX(3.0) * Meter
                )
            )
        )

        assertFalse(
            shape.enabled(
                shape = cylinderZ,
                position = QuantityPoint3(
                    x = FltX(8.0) * Meter,
                    y = FltX(4.0) * Meter,
                    z = FltX(4.0) * Meter
                )
            )
        )
    }
}
