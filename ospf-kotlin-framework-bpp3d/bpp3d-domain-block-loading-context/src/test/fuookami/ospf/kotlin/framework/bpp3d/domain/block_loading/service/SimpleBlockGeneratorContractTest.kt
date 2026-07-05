/**
 * 简单块生成器契约测试。
 * Simple block generator contract test.
 */
package fuookami.ospf.kotlin.framework.bpp3d.domain.block_loading.service

import kotlin.test.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.geometry.Axis3
import fuookami.ospf.kotlin.quantities.unit.*
import fuookami.ospf.kotlin.quantities.quantity.*
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.*
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.*

class SimpleBlockGeneratorContractTest {
    private fun defaultPackageAttribute(type: PackageType = PackageType.CartonContainer): PackageAttribute {
        return PackageAttribute(
            packageType = type,
            packageMaxLayer = UInt64(10),
            maxHeight = FltX(10.0) * Meter,
            maxDepth = FltX(10.0) * Meter,
            weightAttribute = WeightAttribute(),
            deformationAttribute = LinearDeformationAttribute(FltX.zero),
            hangingPolicy = AbsoluteHangingPolicy(FltX.zero),
            stackingOnPolicy = FilterStackingOnPolicy()
        )
    }

    private fun item(id: String): ActualItem {
        return ActualItem(
            id = itemIdOf(id),
            name = id,
            width = FltX(1.0) * Meter,
            height = FltX(1.0) * Meter,
            depth = FltX(1.0) * Meter,
            weight = FltX(1.0) * Kilogram,
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
        val radius = FltX(0.5) * Meter
        val diameter = assertNotNull(radius + radius)
        val height = FltX(1.0) * Meter
        val weight = FltX(1.0) * Kilogram
        return ActualItem(
            id = itemIdOf(id),
            name = id,
            width = diameter,
            height = height,
            depth = diameter,
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
                width = FltX(2.0) * Meter,
                height = FltX(2.0) * Meter,
                depth = FltX(2.0) * Meter
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
            val blocks = generator(
                items = mapOf(
                    cylinderItem(
                        id = "cylinder-axis-$axis",
                        axis = axis,
                        enabledOrientations = listOf(Orientation.Upright)
                    ) to UInt64(8)
                ),
                space = Container3Shape(
                    width = FltX(2.0) * Meter,
                    height = FltX(2.0) * Meter,
                    depth = FltX(2.0) * Meter
                ),
                patterns = emptyList(),
                restWeight = FltX.maximum
            )

            assertTrue(blocks.isEmpty())
        }
    }

    @Test
/** 验证简单块生成器拒绝侧面朝向的圆柱 / Verify simple block generator rejects cylinder with side orientation */
    fun simpleBlockGeneratorShouldRejectCylinderWithSideOrientation() = runBlocking {
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
                    id = "cylinder-side",
                    axis = Axis3.Y,
                    enabledOrientations = listOf(Orientation.Side)
                ) to UInt64(8)
            ),
            space = Container3Shape(
                width = FltX(2.0) * Meter,
                height = FltX(2.0) * Meter,
                depth = FltX(2.0) * Meter
            ),
            patterns = emptyList(),
            restWeight = FltX.maximum
        )

        assertTrue(blocks.isEmpty())
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
                width = FltX(2.0) * Meter,
                height = FltX(2.0) * Meter,
                depth = FltX(2.0) * Meter
            ),
            patterns = emptyList(),
            restWeight = FltX.maximum
        )

        assertTrue(blocks.isNotEmpty())
    }
}
