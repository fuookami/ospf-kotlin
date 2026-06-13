package fuookami.ospf.kotlin.framework.bpp3d.domain.block_loading.service

import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.AbsoluteHangingPolicy
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.ActualItem
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.FilterStackingOnPolicy
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.LinearDeformationAttribute
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.PackageAttribute
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.PackageShapeSpec
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.WeightAttribute
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.BatchNo
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.Container3Shape
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.Orientation
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.PackageType
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.fltX
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.math.geometry.Axis3
import fuookami.ospf.kotlin.quantities.quantity.plus
import fuookami.ospf.kotlin.quantities.quantity.times
import fuookami.ospf.kotlin.quantities.unit.Kilogram
import fuookami.ospf.kotlin.quantities.unit.Meter
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class SimpleBlockGeneratorContractTest {
    private fun defaultPackageAttribute(type: PackageType = PackageType.CartonContainer): PackageAttribute {
        return PackageAttribute(
            packageType = type,
            packageMaxLayer = UInt64(10),
            maxHeight = fltX(10.0) * Meter,
            maxDepth = fltX(10.0) * Meter,
            weightAttribute = WeightAttribute(),
            deformationAttribute = LinearDeformationAttribute(FltX.zero),
            hangingPolicy = AbsoluteHangingPolicy(FltX.zero),
            stackingOnPolicy = FilterStackingOnPolicy()
        )
    }

    private fun item(id: String): ActualItem {
        return ActualItem(
            id = id,
            name = id,
            width = fltX(1.0) * Meter,
            height = fltX(1.0) * Meter,
            depth = fltX(1.0) * Meter,
            weight = fltX(1.0) * Kilogram,
            enabledOrientations = listOf(Orientation.Upright),
            batchNo = BatchNo("B-$id"),
            packageAttribute = defaultPackageAttribute()
        )
    }

    private fun cylinderItem(
        id: String,
        axis: Axis3,
        enabledOrientations: List<Orientation>
    ): ActualItem {
        val radius = fltX(0.5) * Meter
        val height = fltX(1.0) * Meter
        val weight = fltX(1.0) * Kilogram
        return ActualItem(
            id = id,
            name = id,
            width = radius + radius,
            height = height,
            depth = radius + radius,
            weight = weight,
            enabledOrientations = enabledOrientations,
            batchNo = BatchNo("B-$id"),
            packageAttribute = defaultPackageAttribute(),
            shapeSpecOverride = PackageShapeSpec.VerticalCylinder(
                radius = radius,
                axis = axis
            )
        )
    }

    @Test
    fun simpleBlockGeneratorShouldGenerateBlocks() = runBlocking {
        val generator = SimpleBlockGenerator(
            config = SimpleBlockGenerator.Config(
                mergeAsPatternBlock = false,
                withRotation = false,
                withRemainder = false
            )
        )
        val blocks = generator(
            items = mapOf(item("item-1") to UInt64(8)),
            space = Container3Shape(
                width = fltX(2.0) * Meter,
                height = fltX(2.0) * Meter,
                depth = fltX(2.0) * Meter
            ),
            patterns = emptyList(),
            restWeight = FltX.maximum
        )

        assertTrue(blocks.isNotEmpty())
    }

    @Test
    fun simpleBlockGeneratorShouldRejectHorizontalCylinderAxes() = runBlocking {
        val generator = SimpleBlockGenerator(
            config = SimpleBlockGenerator.Config(
                mergeAsPatternBlock = false,
                withRotation = false,
                withRemainder = false
            )
        )
        for (axis in listOf(Axis3.X, Axis3.Z)) {
            val error = assertFailsWith<IllegalArgumentException> {
                generator(
                    items = mapOf(
                        cylinderItem(
                            id = "cylinder-axis-$axis",
                            axis = axis,
                            enabledOrientations = listOf(Orientation.Upright)
                        ) to UInt64(8)
                    ),
                    space = Container3Shape(
                        width = fltX(2.0) * Meter,
                        height = fltX(2.0) * Meter,
                        depth = fltX(2.0) * Meter
                    ),
                    patterns = emptyList(),
                    restWeight = FltX.maximum
                )
            }

            assertTrue(error.message?.contains("only Axis3.Y is allowed") == true)
            assertTrue(error.message?.contains("got $axis") == true)
        }
    }

    @Test
    fun simpleBlockGeneratorShouldRejectCylinderWithSideOrientation() = runBlocking {
        val generator = SimpleBlockGenerator(
            config = SimpleBlockGenerator.Config(
                mergeAsPatternBlock = false,
                withRotation = false,
                withRemainder = false
            )
        )
        val error = assertFailsWith<IllegalArgumentException> {
            generator(
                items = mapOf(
                    cylinderItem(
                        id = "cylinder-side",
                        axis = Axis3.Y,
                        enabledOrientations = listOf(Orientation.Side)
                    ) to UInt64(8)
                ),
                space = Container3Shape(
                    width = fltX(2.0) * Meter,
                    height = fltX(2.0) * Meter,
                    depth = fltX(2.0) * Meter
                ),
                patterns = emptyList(),
                restWeight = FltX.maximum
            )
        }

        assertTrue(error.message?.contains("only upright orientations are allowed") == true)
    }

    @Test
    fun simpleBlockGeneratorShouldGenerateBlocksForYAxisUprightCylinder() = runBlocking {
        val generator = SimpleBlockGenerator(
            config = SimpleBlockGenerator.Config(
                mergeAsPatternBlock = false,
                withRotation = false,
                withRemainder = false
            )
        )
        val blocks = generator(
            items = mapOf(
                cylinderItem(
                    id = "cylinder-axis-y",
                    axis = Axis3.Y,
                    enabledOrientations = listOf(Orientation.Upright)
                ) to UInt64(8)
            ),
            space = Container3Shape(
                width = fltX(2.0) * Meter,
                height = fltX(2.0) * Meter,
                depth = fltX(2.0) * Meter
            ),
            patterns = emptyList(),
            restWeight = FltX.maximum
        )

        assertTrue(blocks.isNotEmpty())
    }
}
