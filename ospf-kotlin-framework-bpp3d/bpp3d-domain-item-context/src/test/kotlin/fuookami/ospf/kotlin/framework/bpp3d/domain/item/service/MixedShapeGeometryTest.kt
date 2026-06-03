package fuookami.ospf.kotlin.framework.bpp3d.domain.item.service

import kotlin.math.PI
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.math.geometry.Axis3
import fuookami.ospf.kotlin.quantities.quantity.plus
import fuookami.ospf.kotlin.quantities.quantity.times
import fuookami.ospf.kotlin.quantities.unit.Kilogram
import fuookami.ospf.kotlin.quantities.unit.Meter
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.BatchNo
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.Container3Shape
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.CuboidPackingShape3
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.CylinderPackingShape3
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.InfraNumber
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.Orientation
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.PackageType
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.PackingAlgorithmShapeType
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.PackingShapeType
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.infraScalar
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.toDouble
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.AbsoluteHangingPolicy
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.ActualItem
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.FilterStackingOnPolicy
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.LinearDeformationAttribute
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.PackageAttribute
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.PackageShapeSpec
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.WeightAttribute

/** 混装长方体+竖直圆柱几何回归测试。Mixed cuboid + vertical cylinder geometry regression test. */
class MixedShapeGeometryTest {
    private fun packageAttribute(): PackageAttribute {
        return PackageAttribute(
            packageType = PackageType.CartonContainer,
            packageMaxLayer = UInt64(10),
            maxHeight = infraScalar(10.0) * Meter,
            maxDepth = infraScalar(10.0) * Meter,
            weightAttribute = WeightAttribute(),
            deformationAttribute = LinearDeformationAttribute(InfraNumber.zero),
            hangingPolicy = AbsoluteHangingPolicy(InfraNumber.zero),
            stackingOnPolicy = FilterStackingOnPolicy()
        )
    }

    private fun cuboidItem(
        id: String,
        width: Double,
        height: Double,
        depth: Double
    ): ActualItem {
        return ActualItem(
            id = id,
            name = id,
            width = infraScalar(width) * Meter,
            height = infraScalar(height) * Meter,
            depth = infraScalar(depth) * Meter,
            weight = infraScalar(1.0) * Kilogram,
            enabledOrientations = listOf(Orientation.Upright),
            batchNo = BatchNo("B-$id"),
            packageAttribute = packageAttribute()
        )
    }

    private fun cylinderItem(
        id: String,
        radiusValue: Double,
        heightValue: Double
    ): ActualItem {
        val radius = infraScalar(radiusValue) * Meter
        return ActualItem(
            id = id,
            name = id,
            width = radius + radius,
            height = infraScalar(heightValue) * Meter,
            depth = radius + radius,
            weight = infraScalar(1.0) * Kilogram,
            enabledOrientations = listOf(Orientation.Upright),
            batchNo = BatchNo("B-$id"),
            packageAttribute = packageAttribute(),
            shapeSpecOverride = PackageShapeSpec.VerticalCylinder(
                radius = radius,
                axis = Axis3.Y
            )
        )
    }

    private fun space(): Container3Shape {
        return Container3Shape(
            width = infraScalar(10.0) * Meter,
            height = infraScalar(10.0) * Meter,
            depth = infraScalar(10.0) * Meter
        )
    }

    @Test
    fun `mixed items have correct packing shape types`() {
        val cuboid = cuboidItem("box1", 1.0, 2.0, 3.0)
        val cylinder = cylinderItem("cyl1", 0.5, 1.5)

        assertTrue(cuboid.packingShape is CuboidPackingShape3)
        assertTrue(cylinder.packingShape is CylinderPackingShape3)
    }

    @Test
    fun `cylinder shapeVolume is real geometric volume not bounding box`() {
        val radius = 0.5
        val height = 1.5
        val cylinder = cylinderItem("cyl1", radius, height)

        val expectedVolume = PI * radius * radius * height
        val boundingBoxVolume = (radius * 2.0) * height * (radius * 2.0)

        assertEquals(expectedVolume, cylinder.shapeVolume.toDouble(), 1e-9)
        assertTrue(cylinder.shapeVolume.toDouble() < boundingBoxVolume)
    }

    @Test
    fun `cuboid shapeVolume equals bounding box volume`() {
        val cuboid = cuboidItem("box1", 1.0, 2.0, 3.0)
        val expectedVolume = 1.0 * 2.0 * 3.0

        assertEquals(expectedVolume, cuboid.shapeVolume.toDouble(), 1e-9)
    }

    @Test
    fun `mixed items shapeBoundingBox is derived from packingShape`() {
        val cuboid = cuboidItem("box1", 1.0, 2.0, 3.0)
        val cylinder = cylinderItem("cyl1", 0.5, 1.5)

        assertEquals(1.0, cuboid.shapeBoundingBox.width.toDouble(), 1e-9)
        assertEquals(2.0, cuboid.shapeBoundingBox.height.toDouble(), 1e-9)
        assertEquals(3.0, cuboid.shapeBoundingBox.depth.toDouble(), 1e-9)

        assertEquals(1.0, cylinder.shapeBoundingBox.width.toDouble(), 1e-9)
        assertEquals(1.5, cylinder.shapeBoundingBox.height.toDouble(), 1e-9)
        assertEquals(1.0, cylinder.shapeBoundingBox.depth.toDouble(), 1e-9)
    }

    @Test
    fun `mixed items can be distinguished by algorithm shape type`() {
        val cuboid = cuboidItem("box1", 1.0, 2.0, 3.0)
        val cylinder = cylinderItem("cyl1", 0.5, 1.5)

        assertEquals(PackingShapeType.Cuboid, cuboid.packingShape.shapeType)
        assertEquals(PackingShapeType.Cylinder, cylinder.packingShape.shapeType)
        assertEquals(PackingAlgorithmShapeType.Cuboid, cuboid.packingShape.algorithmShapeType)
        assertEquals(PackingAlgorithmShapeType.VerticalCylinder, cylinder.packingShape.algorithmShapeType)
    }

    @Test
    fun `cuboid-only item merge rejects mixed cylinder path`() = runBlocking {
        val cuboid = cuboidItem("box1", 1.0, 2.0, 3.0)
        val cylinder = cylinderItem("cyl1", 0.5, 1.5)

        val error = assertFailsWith<IllegalArgumentException> {
            ItemMerger.merge(
                items = listOf(cuboid, cylinder),
                space = space(),
                restWeight = InfraNumber.maximum,
                patterns = emptyList()
            )
        }

        assertTrue(error.message?.contains("item merge paths are cuboid-only") == true)
    }
}
