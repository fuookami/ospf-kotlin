/**
 * 物品合并圆柱测试。
 * Item merger cylinder test.
 */
package fuookami.ospf.kotlin.framework.bpp3d.domain.item.service

import kotlin.test.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.utils.functional.*
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
            maxHeight = FltX(10.0) * Meter,
            maxDepth = FltX(10.0) * Meter,
            weightAttribute = WeightAttribute(),
            deformationAttribute = LinearDeformationAttribute(FltX.zero),
            hangingPolicy = AbsoluteHangingPolicy(FltX.zero),
            stackingOnPolicy = FilterStackingOnPolicy()
        )
    }

    private fun cylinderItem(id: String): ActualItem {
        val radius = FltX(0.5) * Meter
        val diameter = assertNotNull(radius + radius)
        return ActualItem(
            id = itemIdOf(id),
            name = id,
            width = diameter,
            height = FltX(1.0) * Meter,
            depth = diameter,
            weight = FltX(1.0) * Kilogram,
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
            width = FltX(4.0) * Meter,
            height = FltX(4.0) * Meter,
            depth = FltX(4.0) * Meter
        )
    }

    private fun pattern(): Pattern {
        val range = ValueRange(
            lb = FltX.zero,
            ub = FltX(10.0),
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

    /** 断言仅立方体错误 / Assert cuboid-only error */
    private fun assertCuboidOnlyError(result: Ret<*>) {
        assertTrue(result is Failed)
        assertTrue(result.message.contains("item merge paths are cuboid-only"))
    }

    @Test
    /** 验证顶层合并拒绝圆柱 / Verify top-level merge rejects cylinder */
    fun itemMergerShouldRejectCylinderInTopLevelMerge() = runBlocking {
        val result = ItemMerger.merge(
            items = listOf(cylinderItem("cylinder-top-level")),
            space = space(),
            restWeight = FltX.maximum,
            patterns = emptyList()
        )

        assertCuboidOnlyError(result)
    }

    @Test
    /** 验证堆叠合并拒绝圆柱 / Verify pile merge rejects cylinder */
    fun itemMergerShouldRejectCylinderInPileMerge() {
        val result = ItemMerger.mergePiles(
            items = listOf(cylinderItem("cylinder-pile")),
            space = space(),
            restWeight = FltX.maximum
        )

        assertCuboidOnlyError(result)
    }

    @Test
    /** 验证块合并拒绝圆柱 / Verify block merge rejects cylinder */
    fun itemMergerShouldRejectCylinderInBlockMerge() {
        val result = ItemMerger.mergeBlocks(
            items = listOf(cylinderItem("cylinder-block")),
            space = space(),
            restWeight = FltX.maximum
        )

        assertCuboidOnlyError(result)
    }

    @Test
    /** 验证模式块合并拒绝圆柱 / Verify pattern block merge rejects cylinder */
    fun itemMergerShouldRejectCylinderInPatternBlockMerge() = runBlocking {
        val result = ItemMerger.mergePatternBlocks(
            items = listOf(cylinderItem("cylinder-pattern-block")),
            space = space(),
            patterns = listOf(pattern()),
            restWeight = FltX.maximum
        )

        assertCuboidOnlyError(result)
    }

    @Test
    /** 验证空心方形合并拒绝圆柱 / Verify hollow square merge rejects cylinder */
    fun itemMergerShouldRejectCylinderInHollowSquareMerge() {
        val result = ItemMerger.mergeHollowSquareBlocks(
            items = mapOf(cylinderItem("cylinder-hollow") to UInt64(8)),
            space = space(),
            restWeight = FltX.maximum
        )

        assertCuboidOnlyError(result)
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
