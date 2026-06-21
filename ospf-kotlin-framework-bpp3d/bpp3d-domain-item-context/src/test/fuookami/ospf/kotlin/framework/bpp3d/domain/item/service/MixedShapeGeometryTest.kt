/**
 * 混合形状几何测试。
 * Mixed shape geometry test.
 */
package fuookami.ospf.kotlin.framework.bpp3d.domain.item.service

import kotlin.math.PI
import kotlin.test.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.geometry.Axis3
import fuookami.ospf.kotlin.quantities.unit.*
import fuookami.ospf.kotlin.quantities.quantity.*
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.*
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.*

/** 混装长方体+竖直圆柱几何回归测试。Mixed cuboid + vertical cylinder geometry regression test. */
class MixedShapeGeometryTest {
    private fun packageAttribute(withWeight: Boolean = true): PackageAttribute {
        return PackageAttribute(
            packageType = PackageType.CartonContainer,
            packageMaxLayer = UInt64(10),
            maxHeight = FltX(10.0) * Meter,
            maxDepth = FltX(10.0) * Meter,
            weightAttribute = WeightAttribute(),
            deformationAttribute = LinearDeformationAttribute(FltX.zero),
            hangingPolicy = AbsoluteHangingPolicy(FltX.zero, withWeight = withWeight),
            stackingOnPolicy = FilterStackingOnPolicy()
        )
    }

    private fun cuboidItem(
        id: String,
        width: Double,
        height: Double,
        depth: Double,
        attribute: PackageAttribute = packageAttribute()
    ): ActualItem {
        return ActualItem(
            id = id,
            name = id,
            width = FltX(width) * Meter,
            height = FltX(height) * Meter,
            depth = FltX(depth) * Meter,
            weight = FltX(1.0) * Kilogram,
            enabledOrientations = listOf(Orientation.Upright),
            batchNo = BatchNo("B-$id"),
            packageAttribute = attribute
        )
    }

    private fun cylinderItem(
        id: String,
        radiusValue: Double,
        heightValue: Double,
        attribute: PackageAttribute = packageAttribute(),
        axis: Axis3 = Axis3.Y
    ): ActualItem {
        val radius = FltX(radiusValue) * Meter
        val length = FltX(heightValue) * Meter
        val diameter = assertNotNull(radius + radius)
        return ActualItem(
            id = id,
            name = id,
            width = when (axis) {
                Axis3.X -> length
                Axis3.Y, Axis3.Z -> diameter
            },
            height = when (axis) {
                Axis3.X, Axis3.Z -> diameter
                Axis3.Y -> length
            },
            depth = when (axis) {
                Axis3.Z -> length
                Axis3.X, Axis3.Y -> diameter
            },
            weight = FltX(1.0) * Kilogram,
            enabledOrientations = listOf(Orientation.Upright),
            batchNo = BatchNo("B-$id"),
            packageAttribute = attribute,
            shapeSpecOverride = PackageShapeSpec.VerticalCylinder(
                radius = radius,
                axis = axis
            )
        )
    }

    private fun placementOf(
        item: ActualItem,
        x: Double = 0.0,
        y: Double = 0.0,
        z: Double = 0.0
    ) = itemPlacement3Of(
        view = item.view(Orientation.Upright),
        position = point3(
            x = FltX(x) * Meter,
            y = FltX(y) * Meter,
            z = FltX(z) * Meter
        )
    )

    private fun space(): Container3Shape {
        return Container3Shape(
            width = FltX(10.0) * Meter,
            height = FltX(10.0) * Meter,
            depth = FltX(10.0) * Meter
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
    fun `direct cylinder bottom support uses circular footprint area`() {
        val cylinder = cylinderItem("cyl1", 0.5, 1.0)
        val support = BottomSupport(
            area = (FltX(0.8) * Meter) * (FltX(1.0) * Meter),
            weight = FltX(10.0) * Kilogram
        )

        assertTrue(cylinder.enabledStackingOn(support))
    }

    @Test
    fun `cylinder on cuboid accepts full circular support`() = runBlocking {
        val attribute = packageAttribute(withWeight = false)
        val bottom = cuboidItem(
            id = "bottom-box",
            width = 2.0,
            height = 1.0,
            depth = 2.0,
            attribute = attribute
        )
        val top = cylinderItem(
            id = "top-cylinder",
            radiusValue = 0.5,
            heightValue = 1.0,
            attribute = attribute
        )

        val bottomPlacement = placementOf(item = bottom)
        val topPlacement = placementOf(
            item = top,
            x = 0.5,
            y = 1.0,
            z = 0.5
        )

        assertTrue(
            topPlacement.enabledStackingOn(
                bottomItems = listOf(bottomPlacement),
                space = space()
            )
        )
    }

    @Test
    fun `cuboid on cylinder rejects bounding-box-only support`() = runBlocking {
        val attribute = packageAttribute(withWeight = false)
        val bottom = cylinderItem(
            id = "bottom-cylinder",
            radiusValue = 0.5,
            heightValue = 1.0,
            attribute = attribute
        )
        val top = cuboidItem(
            id = "top-box",
            width = 1.0,
            height = 1.0,
            depth = 1.0,
            attribute = attribute
        )

        val bottomPlacement = placementOf(item = bottom)
        val topPlacement = placementOf(
            item = top,
            y = 1.0
        )

        assertFalse(
            topPlacement.enabledStackingOn(
                bottomItems = listOf(bottomPlacement),
                space = space()
            )
        )
    }

    @Test
    fun `cylinder on cylinder uses true circular overlap`() = runBlocking {
        val attribute = packageAttribute(withWeight = false)
        val bottom = cylinderItem(
            id = "bottom-cylinder",
            radiusValue = 0.5,
            heightValue = 1.0,
            attribute = attribute
        )
        val alignedTop = cylinderItem(
            id = "aligned-top-cylinder",
            radiusValue = 0.5,
            heightValue = 1.0,
            attribute = attribute
        )
        val shiftedTop = cylinderItem(
            id = "shifted-top-cylinder",
            radiusValue = 0.5,
            heightValue = 1.0,
            attribute = attribute
        )

        val bottomPlacement = placementOf(item = bottom)
        val alignedTopPlacement = placementOf(
            item = alignedTop,
            y = 1.0
        )
        val shiftedTopPlacement = placementOf(
            item = shiftedTop,
            x = 0.25,
            y = 1.0
        )

        assertTrue(
            alignedTopPlacement.enabledStackingOn(
                bottomItems = listOf(bottomPlacement),
                space = space()
            )
        )
        assertFalse(
            shiftedTopPlacement.enabledStackingOn(
                bottomItems = listOf(bottomPlacement),
                space = space()
            )
        )
    }

    @Test
    fun `horizontal cylinder direct hanging support remains unsupported without coordinates`() {
        val cylinder = cylinderItem(
            id = "horizontal-cylinder-direct-support",
            radiusValue = 0.5,
            heightValue = 1.0,
            axis = Axis3.X
        )
        val support = BottomSupport(
            area = (FltX(1.0) * Meter) * (FltX(1.0) * Meter),
            weight = FltX(10.0) * Kilogram
        )

        assertFalse(cylinder.enabledStackingOn(support))
    }

    @Test
    fun `horizontal cylinder accepts full length cuboid support in 3d stacking`() = runBlocking {
        val attribute = packageAttribute(withWeight = false)
        val bottom = cuboidItem(
            id = "bottom-full-support",
            width = 1.0,
            height = 1.0,
            depth = 1.0,
            attribute = attribute
        )
        val top = cylinderItem(
            id = "top-horizontal-cylinder",
            radiusValue = 0.5,
            heightValue = 1.0,
            attribute = attribute,
            axis = Axis3.X
        )

        val bottomPlacement = placementOf(item = bottom)
        val topPlacement = placementOf(
            item = top,
            y = 1.0
        )

        assertTrue(
            topPlacement.enabledStackingOn(
                bottomItems = listOf(bottomPlacement),
                space = space()
            )
        )
    }

    @Test
    fun `horizontal cylinder rejects partial cuboid support in 3d stacking`() = runBlocking {
        val attribute = packageAttribute(withWeight = false)
        val bottom = cuboidItem(
            id = "bottom-partial-support",
            width = 0.8,
            height = 1.0,
            depth = 1.0,
            attribute = attribute
        )
        val top = cylinderItem(
            id = "top-horizontal-cylinder-partial",
            radiusValue = 0.5,
            heightValue = 1.2,
            attribute = attribute,
            axis = Axis3.X
        )

        val bottomPlacement = placementOf(item = bottom)
        val topPlacement = placementOf(
            item = top,
            y = 1.0
        )

        assertFalse(
            topPlacement.enabledStackingOn(
                bottomItems = listOf(bottomPlacement),
                space = space()
            )
        )
    }

    @Test
    fun `cylinder hanging support rejects non-upright orientation`() {
        val cylinder = cylinderItem(
            id = "side-cylinder",
            radiusValue = 0.5,
            heightValue = 1.0
        )
        val support = BottomSupport(
            area = (FltX(1.0) * Meter) * (FltX(1.0) * Meter),
            weight = FltX(10.0) * Kilogram
        )

        assertFalse(cylinder.view(Orientation.Side).enabledStackingOn(support))
    }

    @Test
    fun `cuboid-only item merge rejects mixed cylinder path`() = runBlocking {
        val cuboid = cuboidItem("box1", 1.0, 2.0, 3.0)
        val cylinder = cylinderItem("cyl1", 0.5, 1.5)

        val result = ItemMerger.merge(
            items = listOf(cuboid, cylinder),
            space = space(),
            restWeight = FltX.maximum,
            patterns = emptyList()
        )

        assertTrue(result is Failed)
        assertTrue(result.message.contains("item merge paths are cuboid-only"))
    }
}
