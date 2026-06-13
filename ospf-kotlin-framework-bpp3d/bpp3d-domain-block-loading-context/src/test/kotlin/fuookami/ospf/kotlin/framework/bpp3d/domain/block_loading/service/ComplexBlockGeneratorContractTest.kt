package fuookami.ospf.kotlin.framework.bpp3d.domain.block_loading.service

import fuookami.ospf.kotlin.math.algebra.number.FltX
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
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.fltX
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.quantities.quantity.times
import fuookami.ospf.kotlin.quantities.unit.Kilogram
import fuookami.ospf.kotlin.quantities.unit.Meter
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

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
