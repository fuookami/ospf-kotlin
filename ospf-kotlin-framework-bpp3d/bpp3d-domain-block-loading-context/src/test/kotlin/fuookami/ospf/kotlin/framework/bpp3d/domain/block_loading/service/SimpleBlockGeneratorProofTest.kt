package fuookami.ospf.kotlin.framework.bpp3d.domain.block_loading.service

import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.AbsoluteHangingPolicy
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.ActualItem
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.FilterStackingOnPolicy
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.LinearDeformationAttribute
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.PackageAttribute
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.WeightAttribute
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.AbstractCylinder
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.BatchNo
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.Container3Shape
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.CylinderPackingShape3
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.InfraNumber
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.Orientation
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.PackingShape3
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.PackageType
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.infraScalar
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

class SimpleBlockGeneratorProofTest {
    private fun defaultPackageAttribute(type: PackageType = PackageType.CartonContainer): PackageAttribute {
        return PackageAttribute(
            packageType = type,
            packageMaxLayer = UInt64(10),
            maxHeight = infraScalar(10.0) * Meter,
            maxDepth = infraScalar(10.0) * Meter,
            weightAttribute = WeightAttribute(),
            deformationAttribute = LinearDeformationAttribute(InfraNumber.zero),
            hangingPolicy = AbsoluteHangingPolicy(InfraNumber.zero),
            stackingOnPolicy = FilterStackingOnPolicy()
        )
    }

    private fun item(id: String): ActualItem {
        return ActualItem(
            id = id,
            name = id,
            width = infraScalar(1.0) * Meter,
            height = infraScalar(1.0) * Meter,
            depth = infraScalar(1.0) * Meter,
            weight = infraScalar(1.0) * Kilogram,
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
        val radius = infraScalar(0.5) * Meter
        val height = infraScalar(1.0) * Meter
        val weight = infraScalar(1.0) * Kilogram
        return object : ActualItem(
            id = id,
            name = id,
            width = radius + radius,
            height = height,
            depth = radius + radius,
            weight = weight,
            enabledOrientations = enabledOrientations,
            batchNo = BatchNo("B-$id"),
            packageAttribute = defaultPackageAttribute()
        ) {
            override val explicitPackingShape: PackingShape3<InfraNumber> by lazy {
                CylinderPackingShape3(
                    cylinder = object : AbstractCylinder<InfraNumber> {
                        override val radius = radius
                        override val height = height
                        override val axis = axis
                        override val weight = weight
                    }
                )
            }
        }
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
                width = infraScalar(2.0) * Meter,
                height = infraScalar(2.0) * Meter,
                depth = infraScalar(2.0) * Meter
            ),
            patterns = emptyList(),
            restWeight = InfraNumber.maximum
        )

        assertTrue(blocks.isNotEmpty())
    }

    @Test
    fun simpleBlockGeneratorShouldRejectCylinderWithAxisX() = runBlocking {
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
                        id = "cylinder-axis-x",
                        axis = Axis3.X,
                        enabledOrientations = listOf(Orientation.Upright)
                    ) to UInt64(8)
                ),
                space = Container3Shape(
                    width = infraScalar(2.0) * Meter,
                    height = infraScalar(2.0) * Meter,
                    depth = infraScalar(2.0) * Meter
                ),
                patterns = emptyList(),
                restWeight = InfraNumber.maximum
            )
        }

        assertTrue(error.message?.contains("only Axis3.Y is allowed") == true)
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
                    width = infraScalar(2.0) * Meter,
                    height = infraScalar(2.0) * Meter,
                    depth = infraScalar(2.0) * Meter
                ),
                patterns = emptyList(),
                restWeight = InfraNumber.maximum
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
                width = infraScalar(2.0) * Meter,
                height = infraScalar(2.0) * Meter,
                depth = infraScalar(2.0) * Meter
            ),
            patterns = emptyList(),
            restWeight = InfraNumber.maximum
        )

        assertTrue(blocks.isNotEmpty())
    }
}
