/**
 * 装箱形状测试。
 * Packing shape test.
 */
package fuookami.ospf.kotlin.framework.bpp3d.infrastructure

import kotlin.math.PI
import kotlin.test.*
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.math.geometry.Axis3
import fuookami.ospf.kotlin.quantities.unit.*
import fuookami.ospf.kotlin.quantities.quantity.*

class PackingShapeTest {
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

    private data class Column(
        override val radius: Quantity<FltX>,
        override val height: Quantity<FltX>,
        override val axis: Axis3,
        override val weight: Quantity<FltX>,
        override val enabledAxes: List<Axis3>
    ) : Cylinder<Column> {
        override val self: Column
            get() = this
    }

    @Test
    fun cuboidPackingShapeShouldKeepBoundingAndVolumeContract() {
        val box = Box(
            width = fltX(2.0) * Meter,
            height = fltX(3.0) * Meter,
            depth = fltX(4.0) * Meter,
            weight = fltX(5.0) * Kilogram
        )
        val shape = box.asPackingShape3()

        assertEquals(PackingShapeType.Cuboid, shape.shapeType)
        assertEquals(PackingAlgorithmShapeType.Cuboid, shape.algorithmShapeType)
        assertTrue(shape.boundingWidth eq (fltX(2.0) * Meter))
        assertTrue(shape.boundingHeight eq (fltX(3.0) * Meter))
        assertTrue(shape.boundingDepth eq (fltX(4.0) * Meter))
        assertTrue(shape.actualVolume eq (fltX(24.0) * CubicMeter))

        val footprint = shape.footprint()
        assertIs<ShapeFootprint2.Rectangle<FltX>>(footprint)
        assertTrue(footprint.width eq (fltX(2.0) * Meter))
        assertTrue(footprint.depth eq (fltX(4.0) * Meter))
    }

    @Test
    fun verticalCylinderPackingShapeShouldExposeRealGeometry() {
        val cylinder = Column(
            radius = fltX(1.5) * Meter,
            height = fltX(5.0) * Meter,
            axis = Axis3.Y,
            weight = fltX(7.0) * Kilogram,
            enabledAxes = listOf(Axis3.Y)
        )
        val shape = cylinder.asPackingShape3()

        assertEquals(PackingShapeType.Cylinder, shape.shapeType)
        assertEquals(PackingAlgorithmShapeType.VerticalCylinder, shape.algorithmShapeType)
        assertEquals(PackingAxis3.Y, cylinder.axis.asPackingAxis3())
        assertTrue(shape.boundingWidth eq (fltX(3.0) * Meter))
        assertTrue(shape.boundingHeight eq (fltX(5.0) * Meter))
        assertTrue(shape.boundingDepth eq (fltX(3.0) * Meter))
        assertTrue(shape.actualVolume eq ((fltX(PI) * fltX(1.5) * fltX(1.5) * fltX(5.0)) * CubicMeter))

        val footprint = shape.footprint()
        assertIs<ShapeFootprint2.Circle<FltX>>(footprint)
        assertTrue(footprint.radius eq (fltX(1.5) * Meter))
    }

    @Test
    fun horizontalCylinderXShouldExposeShapeMetadata() {
        val cylinder = Column(
            radius = fltX(2.0) * Meter,
            height = fltX(6.0) * Meter,
            axis = Axis3.X,
            weight = fltX(9.0) * Kilogram,
            enabledAxes = listOf(Axis3.X)
        )
        val shape = cylinder.asPackingShape3()

        assertEquals(PackingShapeType.Cylinder, shape.shapeType)
        assertEquals(PackingAlgorithmShapeType.HorizontalCylinderX, shape.algorithmShapeType)
        assertEquals(PackingAxis3.X, cylinder.axis.asPackingAxis3())
        assertTrue(shape.boundingWidth eq (fltX(6.0) * Meter))
        assertTrue(shape.boundingHeight eq (fltX(4.0) * Meter))
        assertTrue(shape.boundingDepth eq (fltX(4.0) * Meter))
        assertTrue(shape.actualVolume eq ((fltX(PI) * fltX(2.0) * fltX(2.0) * fltX(6.0)) * CubicMeter))

        val footprint = shape.footprint()
        assertIs<ShapeFootprint2.Rectangle<FltX>>(footprint)
        assertTrue(footprint.width eq (fltX(6.0) * Meter))
        assertTrue(footprint.depth eq (fltX(4.0) * Meter))
    }

    @Test
    fun horizontalCylinderZShouldExposeShapeMetadata() {
        val cylinder = Column(
            radius = fltX(2.0) * Meter,
            height = fltX(6.0) * Meter,
            axis = Axis3.Z,
            weight = fltX(9.0) * Kilogram,
            enabledAxes = listOf(Axis3.Z)
        )
        val shape = cylinder.asPackingShape3()

        assertEquals(PackingShapeType.Cylinder, shape.shapeType)
        assertEquals(PackingAlgorithmShapeType.HorizontalCylinderZ, shape.algorithmShapeType)
        assertEquals(PackingAxis3.Z, cylinder.axis.asPackingAxis3())
        assertTrue(shape.boundingWidth eq (fltX(4.0) * Meter))
        assertTrue(shape.boundingHeight eq (fltX(4.0) * Meter))
        assertTrue(shape.boundingDepth eq (fltX(6.0) * Meter))
        assertTrue(shape.actualVolume eq ((fltX(PI) * fltX(2.0) * fltX(2.0) * fltX(6.0)) * CubicMeter))

        val footprint = shape.footprint()
        assertIs<ShapeFootprint2.Rectangle<FltX>>(footprint)
        assertTrue(footprint.width eq (fltX(4.0) * Meter))
        assertTrue(footprint.depth eq (fltX(6.0) * Meter))
    }
}
