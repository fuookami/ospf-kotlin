package fuookami.ospf.kotlin.framework.bpp3d.infrastructure

import kotlin.math.PI
import fuookami.ospf.kotlin.math.geometry.Axis3
import fuookami.ospf.kotlin.quantities.quantity.eq
import fuookami.ospf.kotlin.quantities.quantity.times
import fuookami.ospf.kotlin.quantities.unit.CubicMeter
import fuookami.ospf.kotlin.quantities.unit.Kilogram
import fuookami.ospf.kotlin.quantities.unit.Meter
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class PackingShapeTest {
    private data class Box(
        override val width: InfraQuantity,
        override val height: InfraQuantity,
        override val depth: InfraQuantity,
        override val weight: InfraQuantity,
        override val enabledOrientations: List<Orientation> = Orientation.entries
    ) : Cuboid<Box> {
        override val self: Box
            get() = this
    }

    private data class Column(
        override val radius: InfraQuantity,
        override val height: InfraQuantity,
        override val axis: Axis3,
        override val weight: InfraQuantity,
        override val enabledAxes: List<Axis3>
    ) : Cylinder<Column> {
        override val self: Column
            get() = this
    }

    @Test
    fun cuboidPackingShapeShouldKeepBoundingAndVolumeContract() {
        val box = Box(
            width = infraScalar(2.0) * Meter,
            height = infraScalar(3.0) * Meter,
            depth = infraScalar(4.0) * Meter,
            weight = infraScalar(5.0) * Kilogram
        )
        val shape = box.asPackingShape3()

        assertEquals(PackingShapeType.Cuboid, shape.shapeType)
        assertEquals(PackingAlgorithmShapeType.Cuboid, shape.algorithmShapeType)
        assertTrue(shape.boundingWidth eq (infraScalar(2.0) * Meter))
        assertTrue(shape.boundingHeight eq (infraScalar(3.0) * Meter))
        assertTrue(shape.boundingDepth eq (infraScalar(4.0) * Meter))
        assertTrue(shape.actualVolume eq (infraScalar(24.0) * CubicMeter))

        val footprint = shape.footprint()
        assertIs<ShapeFootprint2.Rectangle<InfraNumber>>(footprint)
        assertTrue(footprint.width eq (infraScalar(2.0) * Meter))
        assertTrue(footprint.depth eq (infraScalar(4.0) * Meter))
    }

    @Test
    fun verticalCylinderPackingShapeShouldExposeRealGeometry() {
        val cylinder = Column(
            radius = infraScalar(1.5) * Meter,
            height = infraScalar(5.0) * Meter,
            axis = Axis3.Y,
            weight = infraScalar(7.0) * Kilogram,
            enabledAxes = listOf(Axis3.Y)
        )
        val shape = cylinder.asPackingShape3()

        assertEquals(PackingShapeType.Cylinder, shape.shapeType)
        assertEquals(PackingAlgorithmShapeType.VerticalCylinder, shape.algorithmShapeType)
        assertEquals(PackingAxis3.Y, cylinder.axis.asPackingAxis3())
        assertTrue(shape.boundingWidth eq (infraScalar(3.0) * Meter))
        assertTrue(shape.boundingHeight eq (infraScalar(5.0) * Meter))
        assertTrue(shape.boundingDepth eq (infraScalar(3.0) * Meter))
        assertTrue(shape.actualVolume eq ((infraScalar(PI) * infraScalar(1.5) * infraScalar(1.5) * infraScalar(5.0)) * CubicMeter))

        val footprint = shape.footprint()
        assertIs<ShapeFootprint2.Circle<InfraNumber>>(footprint)
        assertTrue(footprint.radius eq (infraScalar(1.5) * Meter))
    }

    @Test
    fun horizontalCylinderXShouldExposeShapeMetadata() {
        val cylinder = Column(
            radius = infraScalar(2.0) * Meter,
            height = infraScalar(6.0) * Meter,
            axis = Axis3.X,
            weight = infraScalar(9.0) * Kilogram,
            enabledAxes = listOf(Axis3.X)
        )
        val shape = cylinder.asPackingShape3()

        assertEquals(PackingShapeType.Cylinder, shape.shapeType)
        assertEquals(PackingAlgorithmShapeType.HorizontalCylinderX, shape.algorithmShapeType)
        assertEquals(PackingAxis3.X, cylinder.axis.asPackingAxis3())
        assertTrue(shape.boundingWidth eq (infraScalar(6.0) * Meter))
        assertTrue(shape.boundingHeight eq (infraScalar(4.0) * Meter))
        assertTrue(shape.boundingDepth eq (infraScalar(4.0) * Meter))
        assertTrue(shape.actualVolume eq ((infraScalar(PI) * infraScalar(2.0) * infraScalar(2.0) * infraScalar(6.0)) * CubicMeter))

        val footprint = shape.footprint()
        assertIs<ShapeFootprint2.Rectangle<InfraNumber>>(footprint)
        assertTrue(footprint.width eq (infraScalar(6.0) * Meter))
        assertTrue(footprint.depth eq (infraScalar(4.0) * Meter))
    }

    @Test
    fun horizontalCylinderZShouldExposeShapeMetadata() {
        val cylinder = Column(
            radius = infraScalar(2.0) * Meter,
            height = infraScalar(6.0) * Meter,
            axis = Axis3.Z,
            weight = infraScalar(9.0) * Kilogram,
            enabledAxes = listOf(Axis3.Z)
        )
        val shape = cylinder.asPackingShape3()

        assertEquals(PackingShapeType.Cylinder, shape.shapeType)
        assertEquals(PackingAlgorithmShapeType.HorizontalCylinderZ, shape.algorithmShapeType)
        assertEquals(PackingAxis3.Z, cylinder.axis.asPackingAxis3())
        assertTrue(shape.boundingWidth eq (infraScalar(4.0) * Meter))
        assertTrue(shape.boundingHeight eq (infraScalar(4.0) * Meter))
        assertTrue(shape.boundingDepth eq (infraScalar(6.0) * Meter))
        assertTrue(shape.actualVolume eq ((infraScalar(PI) * infraScalar(2.0) * infraScalar(2.0) * infraScalar(6.0)) * CubicMeter))

        val footprint = shape.footprint()
        assertIs<ShapeFootprint2.Rectangle<InfraNumber>>(footprint)
        assertTrue(footprint.width eq (infraScalar(4.0) * Meter))
        assertTrue(footprint.depth eq (infraScalar(6.0) * Meter))
    }
}
