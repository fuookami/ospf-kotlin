package fuookami.ospf.kotlin.framework.bpp3d.domain.item.service

import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.test.fail
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.math.algebra.value_range.Interval
import fuookami.ospf.kotlin.math.algebra.value_range.ValueRange
import fuookami.ospf.kotlin.math.geometry.Axis3
import fuookami.ospf.kotlin.quantities.quantity.plus
import fuookami.ospf.kotlin.quantities.quantity.times
import fuookami.ospf.kotlin.quantities.unit.Kilogram
import fuookami.ospf.kotlin.quantities.unit.Meter
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.BatchNo
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.Container3Shape
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.Front
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.InfraNumber
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.Orientation
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.PackageType
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.infraScalar
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.AbsoluteHangingPolicy
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.ActualItem
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.FilterStackingOnPolicy
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.LinearDeformationAttribute
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.PackageAttribute
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.PackageShapeSpec
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.Pattern
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.WeightAttribute
import fuookami.ospf.kotlin.utils.functional.Failed

class ItemMergerCylinderTest {
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

    private fun cylinderItem(id: String): ActualItem {
        val radius = infraScalar(0.5) * Meter
        return ActualItem(
            id = id,
            name = id,
            width = radius + radius,
            height = infraScalar(1.0) * Meter,
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
            width = infraScalar(4.0) * Meter,
            height = infraScalar(4.0) * Meter,
            depth = infraScalar(4.0) * Meter
        )
    }

    private fun pattern(): Pattern {
        val range = ValueRange(
            lb = InfraNumber.zero,
            ub = infraScalar(10.0),
            lbInterval = Interval.Closed,
            ubInterval = Interval.Closed,
            constants = InfraNumber
        ).value!!
        return object : Pattern() {
            override val bottomLengthRange = range
            override val bottomWidthRange = range
            override val patterns = listOf(
                listOf(
                    Step(
                        lengthOrientation = Front,
                        nextPointExtractor = null
                    )
                )
            )
        }
    }

    private fun assertCuboidOnlyError(error: IllegalArgumentException) {
        assertTrue(error.message?.contains("item merge paths are cuboid-only") == true)
    }

    @Test
    fun itemMergerShouldRejectCylinderInTopLevelMerge() = runBlocking {
        val error = assertFailsWith<IllegalArgumentException> {
            ItemMerger.merge(
                items = listOf(cylinderItem("cylinder-top-level")),
                space = space(),
                restWeight = InfraNumber.maximum,
                patterns = emptyList()
            )
        }

        assertCuboidOnlyError(error)
    }

    @Test
    fun itemMergerShouldRejectCylinderInPileMerge() {
        val error = assertFailsWith<IllegalArgumentException> {
            ItemMerger.mergePiles(
                items = listOf(cylinderItem("cylinder-pile")),
                space = space(),
                restWeight = InfraNumber.maximum
            )
        }

        assertCuboidOnlyError(error)
    }

    @Test
    fun itemMergerShouldRejectCylinderInBlockMerge() {
        val error = assertFailsWith<IllegalArgumentException> {
            ItemMerger.mergeBlocks(
                items = listOf(cylinderItem("cylinder-block")),
                space = space(),
                restWeight = InfraNumber.maximum
            )
        }

        assertCuboidOnlyError(error)
    }

    @Test
    fun itemMergerShouldRejectCylinderInPatternBlockMerge() = runBlocking {
        val error = assertFailsWith<IllegalArgumentException> {
            ItemMerger.mergePatternBlocks(
                items = listOf(cylinderItem("cylinder-pattern-block")),
                space = space(),
                patterns = listOf(pattern()),
                restWeight = InfraNumber.maximum
            )
        }

        assertCuboidOnlyError(error)
    }

    @Test
    fun itemMergerShouldRejectCylinderInHollowSquareMerge() {
        val error = assertFailsWith<IllegalArgumentException> {
            ItemMerger.mergeHollowSquareBlocks(
                items = mapOf(cylinderItem("cylinder-hollow") to UInt64(8)),
                space = space(),
                restWeight = InfraNumber.maximum
            )
        }

        assertCuboidOnlyError(error)
    }

    @Test
    fun patternShouldRejectCylinderInDirectInvocation() = runBlocking {
        val result = pattern()(
            originItems = mapOf(cylinderItem("cylinder-pattern-direct") to UInt64.one),
            space = space(),
            restWeight = InfraNumber.maximum
        )

        when (result) {
            is Failed -> {
                assertTrue(result.message.contains("pattern placement paths are cuboid-only"))
            }

            else -> {
                fail("Pattern should reject cylinder inputs.")
            }
        }
    }
}
