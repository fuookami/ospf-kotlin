package fuookami.ospf.kotlin.framework.bpp3d.infrastructure

import fuookami.ospf.kotlin.math.algebra.number.FltX
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
        override val width: Quantity<FltX>,
        override val height: Quantity<FltX>,
        override val depth: Quantity<FltX>,
        override val weight: Quantity<FltX>
    ) : AbstractCuboid<FltX>

    @Test
    fun volumeShouldKeepLengthCubeDimension() {
        val shape = Container3Shape(
            width = fltX(2.0) * Meter,
            height = fltX(3.0) * Meter,
            depth = fltX(4.0) * Meter
        )

        assertTrue(shape.volume eq (fltX(24.0) * CubicMeter))
    }

    @Test
    fun enabledAndMaxAmountShouldRespectOrientationAndLimits() {
        val shape = Container3Shape(
            width = fltX(10.0) * Meter,
            height = fltX(9.0) * Meter,
            depth = fltX(8.0) * Meter
        )
        val unit = StubCuboid(
            width = fltX(2.0) * Meter,
            height = fltX(3.0) * Meter,
            depth = fltX(4.0) * Meter,
            weight = fltX(1.0) * Kilogram
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
            width = fltX(3.0) * Meter,
            height = fltX(2.0) * Meter,
            depth = fltX(2.0) * Meter
        )
        val rotatedUnit = StubCuboid(
            width = fltX(2.0) * Meter,
            height = fltX(2.0) * Meter,
            depth = fltX(3.0) * Meter,
            weight = fltX(1.0) * Kilogram
        )

        assertFalse(rotatedOnlyShape.enabled(rotatedUnit, Orientation.Upright))
        assertTrue(rotatedOnlyShape.enabled(rotatedUnit, Orientation.UprightRotated))
    }

    @Test
    fun restSpaceShouldSubtractPointAndVectorOffsets() {
        val shape = Container3Shape(
            width = fltX(8.0) * Meter,
            height = fltX(6.0) * Meter,
            depth = fltX(5.0) * Meter
        )
        val pointOffset = QuantityPoint3(
            x = fltX(3.0) * Meter,
            y = fltX(2.0) * Meter,
            z = fltX(1.0) * Meter
        )
        val vectorOffset = QuantityVector3(
            x = fltX(1.0) * Meter,
            y = fltX(1.5) * Meter,
            z = fltX(2.0) * Meter
        )

        val byPoint = shape.restSpace(pointOffset)
        val byVector = shape.restSpace(vectorOffset)

        assertTrue(byPoint.width eq (fltX(5.0) * Meter))
        assertTrue(byPoint.height eq (fltX(4.0) * Meter))
        assertTrue(byPoint.depth eq (fltX(4.0) * Meter))

        assertTrue(byVector.width eq (fltX(7.0) * Meter))
        assertTrue(byVector.height eq (fltX(4.5) * Meter))
        assertTrue(byVector.depth eq (fltX(3.0) * Meter))
    }

    @Test
    fun container3ShapeFrom2DShouldFollowPlaneAxisRules() {
        val bottom2 = Container2Shape(
            length = fltX(4.0) * Meter,
            width = fltX(3.0) * Meter,
            plane = Bottom
        )
        val side2 = Container2Shape(
            length = fltX(4.0) * Meter,
            width = fltX(3.0) * Meter,
            plane = Side
        )
        val front2 = Container2Shape(
            length = fltX(4.0) * Meter,
            width = fltX(3.0) * Meter,
            plane = Front
        )

        val bottom3 = Container3Shape(bottom2)
        val side3 = Container3Shape(side2)
        val front3 = Container3Shape(front2)

        assertTrue(bottom3.width eq (fltX(3.0) * Meter))
        assertTrue(bottom3.depth eq (fltX(4.0) * Meter))

        assertTrue(side3.width eq (fltX(4.0) * Meter))
        assertTrue(side3.height eq (fltX(3.0) * Meter))

        assertTrue(front3.width eq (fltX(4.0) * Meter))
        assertTrue(front3.height eq (fltX(3.0) * Meter))
    }

    @Test
    fun enabledWithVerticalCylinderShapeShouldUseRadiusBoundary() {
        val shape = Container3Shape(
            width = fltX(10.0) * Meter,
            height = fltX(6.0) * Meter,
            depth = fltX(8.0) * Meter
        )
        val cylinder = object : AbstractCylinder<FltX> {
            override val radius = fltX(1.5) * Meter
            override val height = fltX(4.0) * Meter
            override val axis = Axis3.Y
            override val weight = fltX(2.0) * Kilogram
        }.asPackingShape3()

        assertTrue(
            shape.enabled(
                shape = cylinder,
                position = QuantityPoint3(
                    x = fltX(2.0) * Meter,
                    y = fltX(1.0) * Meter,
                    z = fltX(3.0) * Meter
                )
            )
        )

        assertFalse(
            shape.enabled(
                shape = cylinder,
                position = QuantityPoint3(
                    x = fltX(8.0) * Meter,
                    y = fltX(1.0) * Meter,
                    z = fltX(3.0) * Meter
                )
            )
        )

        assertFalse(
            shape.enabled(
                shape = cylinder,
                position = QuantityPoint3(
                    x = fltX(-0.1) * Meter,
                    y = fltX(1.0) * Meter,
                    z = fltX(3.0) * Meter
                )
            )
        )
    }

    @Test
    fun enabledWithHorizontalCylinderShapeShouldUseBoundingBoxBoundary() {
        val shape = Container3Shape(
            width = fltX(10.0) * Meter,
            height = fltX(6.0) * Meter,
            depth = fltX(8.0) * Meter
        )
        val cylinderX = object : AbstractCylinder<FltX> {
            override val radius = fltX(1.0) * Meter
            override val height = fltX(5.0) * Meter
            override val axis = Axis3.X
            override val weight = fltX(2.0) * Kilogram
        }.asPackingShape3()
        val cylinderZ = object : AbstractCylinder<FltX> {
            override val radius = fltX(1.0) * Meter
            override val height = fltX(5.0) * Meter
            override val axis = Axis3.Z
            override val weight = fltX(2.0) * Kilogram
        }.asPackingShape3()

        assertTrue(
            shape.enabled(
                shape = cylinderX,
                position = QuantityPoint3(
                    x = fltX(5.0) * Meter,
                    y = fltX(4.0) * Meter,
                    z = fltX(6.0) * Meter
                )
            )
        )

        assertFalse(
            shape.enabled(
                shape = cylinderX,
                position = QuantityPoint3(
                    x = fltX(6.0) * Meter,
                    y = fltX(4.0) * Meter,
                    z = fltX(6.0) * Meter
                )
            )
        )

        assertTrue(
            shape.enabled(
                shape = cylinderZ,
                position = QuantityPoint3(
                    x = fltX(8.0) * Meter,
                    y = fltX(4.0) * Meter,
                    z = fltX(3.0) * Meter
                )
            )
        )

        assertFalse(
            shape.enabled(
                shape = cylinderZ,
                position = QuantityPoint3(
                    x = fltX(8.0) * Meter,
                    y = fltX(4.0) * Meter,
                    z = fltX(4.0) * Meter
                )
            )
        )
    }
}

