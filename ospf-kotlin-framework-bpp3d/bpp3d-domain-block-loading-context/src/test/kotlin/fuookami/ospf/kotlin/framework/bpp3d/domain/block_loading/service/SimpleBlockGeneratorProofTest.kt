package fuookami.ospf.kotlin.framework.bpp3d.domain.block_loading.service

import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.AbsoluteHangingPolicy
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.ActualItem
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.FilterStackingOnPolicy
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.LinearDeformationAttribute
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.PackageAttribute
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.WeightAttribute
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.BatchNo
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.Container3Shape
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.Orientation
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.PackageType
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.quantities.quantity.times
import fuookami.ospf.kotlin.quantities.unit.Kilogram
import fuookami.ospf.kotlin.quantities.unit.Meter
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

class SimpleBlockGeneratorProofTest {
    private fun defaultPackageAttribute(type: PackageType = PackageType.CartonContainer): PackageAttribute {
        return PackageAttribute(
            packageType = type,
            weightAttribute = WeightAttribute(),
            deformationAttribute = LinearDeformationAttribute(Flt64.zero),
            hangingPolicy = AbsoluteHangingPolicy(Flt64.zero),
            stackingOnPolicy = FilterStackingOnPolicy()
        )
    }

    private fun item(id: String): ActualItem {
        return ActualItem(
            id = id,
            name = id,
            width = 1.0 * Meter,
            height = 1.0 * Meter,
            depth = 1.0 * Meter,
            weight = 1.0 * Kilogram,
            enabledOrientations = listOf(Orientation.Upright),
            batchNo = BatchNo("B-$id"),
            packageAttribute = defaultPackageAttribute()
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
                width = 2.0 * Meter,
                height = 2.0 * Meter,
                depth = 2.0 * Meter
            ),
            patterns = emptyList(),
            restWeight = Flt64.infinity
        )

        assertTrue(blocks.isNotEmpty())
    }
}

