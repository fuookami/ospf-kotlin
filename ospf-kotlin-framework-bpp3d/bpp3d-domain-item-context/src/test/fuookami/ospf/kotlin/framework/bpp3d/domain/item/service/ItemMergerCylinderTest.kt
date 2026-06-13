/**
 * 物品合并圆柱测试。
 * Item merger cylinder test.
 */
package fuookami.ospf.kotlin.framework.bpp3d.domain.item.service

import kotlin.test.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.value_range.*
import fuookami.ospf.kotlin.math.geometry.Axis3
import fuookami.ospf.kotlin.quantities.unit.*
import fuookami.ospf.kotlin.quantities.quantity.*
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.*
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.*

class ItemMergerCylinderTest {
    private fun packageAttribute(): PackageAttribute {
        return PackageAttribute(
            packageType = PackageType.CartonContainer,
            packageMaxLayer = UInt64(10),
            maxHeight = fltX(10.0) * Meter,
            maxDepth = fltX(10.0) * Meter,
            weightAttribute = WeightAttribute(),
            deformationAttribute = LinearDeformationAttribute(FltX.zero),
            hangingPolicy = AbsoluteHangingPolicy(FltX.zero),
            stackingOnPolicy = FilterStackingOnPolicy()
        )
    }

    private fun cylinderItem(id: String): ActualItem {
        val radius = fltX(0.5) * Meter
        return ActualItem(
            id = id,
            name = id,
            width = radius + radius,
            height = fltX(1.0) * Meter,
            depth = radius + radius,
            weight = fltX(1.0) * Kilogram,
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
            width = fltX(4.0) * Meter,
            height = fltX(4.0) * Meter,
            depth = fltX(4.0) * Meter
        )
    }

    private fun pattern(): Pattern {
        val range = ValueRange(
            lb = FltX.zero,
            ub = fltX(10.0),
            lbInterval = Interval.Closed,
            ubInterval = Interval.Closed,
            constants = FltX
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
                restWeight = FltX.maximum,
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
                restWeight = FltX.maximum
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
                restWeight = FltX.maximum
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
                restWeight = FltX.maximum
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
                restWeight = FltX.maximum
            )
        }

        assertCuboidOnlyError(error)
    }

    @Test
    fun patternShouldRejectCylinderInDirectInvocation() = runBlocking {
        val result = pattern()(
            originItems = mapOf(cylinderItem("cylinder-pattern-direct") to UInt64.one),
            space = space(),
            restWeight = FltX.maximum
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
