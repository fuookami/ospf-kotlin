/**
 * 复合块生成器契约测试。
 * Complex block generator contract test.
 */
package fuookami.ospf.kotlin.framework.bpp3d.domain.block_loading.service

import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.quantities.unit.*
import fuookami.ospf.kotlin.quantities.quantity.times
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.*
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.*

class ComplexBlockGeneratorContractTest {
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

    @Test
    fun complexBlockGeneratorShouldMergeSimpleBlocks() = runBlocking {
        val actualItem = item("item-1")
        val simpleBlocks = SimpleBlockGenerator(
            config = SimpleBlockGenerator.Config(
                mergeAsPatternBlock = false,
                withRotation = false,
                withRemainder = false
            )
        ).invoke(
            items = mapOf(actualItem to UInt64(2)),
            space = Container3Shape(
                width = fltX(2.0) * Meter,
                height = fltX(1.0) * Meter,
                depth = fltX(1.0) * Meter
            ),
            patterns = emptyList(),
            restWeight = FltX.maximum
        )
        val complexBlocks = ComplexBlockGenerator(
            config = ComplexBlockGenerator.Config(
                withX = true,
                withY = false,
                withZ = false
            )
        ).invoke(
            items = mapOf(actualItem to UInt64(2)),
            space = Container3Shape(
                width = fltX(2.0) * Meter,
                height = fltX(1.0) * Meter,
                depth = fltX(1.0) * Meter
            ),
            simpleBlocks = simpleBlocks,
            restWeight = FltX.maximum
        )

        assertTrue(complexBlocks.isNotEmpty())
    }

    @Test
    fun complexBlockGeneratorShouldRespectRestWeight() = runBlocking {
        val actualItem = item("item-2")
        val simpleBlocks = SimpleBlockGenerator(
            config = SimpleBlockGenerator.Config(
                mergeAsPatternBlock = false,
                withRotation = false,
                withRemainder = false
            )
        ).invoke(
            items = mapOf(actualItem to UInt64(2)),
            space = Container3Shape(
                width = fltX(2.0) * Meter,
                height = fltX(1.0) * Meter,
                depth = fltX(1.0) * Meter
            ),
            patterns = emptyList(),
            restWeight = FltX.maximum
        )
        val complexBlocks = ComplexBlockGenerator(
            config = ComplexBlockGenerator.Config(
                withX = true,
                withY = false,
                withZ = false
            )
        ).invoke(
            items = mapOf(actualItem to UInt64(2)),
            space = Container3Shape(
                width = fltX(2.0) * Meter,
                height = fltX(1.0) * Meter,
                depth = fltX(1.0) * Meter
            ),
            simpleBlocks = simpleBlocks,
            restWeight = FltX.one
        )

        assertTrue(complexBlocks.isEmpty())
    }
}
