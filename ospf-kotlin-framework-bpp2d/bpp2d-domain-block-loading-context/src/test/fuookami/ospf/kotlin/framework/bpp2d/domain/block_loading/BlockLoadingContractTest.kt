package fuookami.ospf.kotlin.framework.bpp2d.domain.block_loading

import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.Ret
import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.quantities.quantity.times
import fuookami.ospf.kotlin.quantities.unit.Kilogram
import fuookami.ospf.kotlin.quantities.unit.Meter
import fuookami.ospf.kotlin.framework.geometry2d.*
import fuookami.ospf.kotlin.framework.bpp2d.domain.block_loading.model.Space2
import fuookami.ospf.kotlin.framework.bpp2d.domain.block_loading.service.*
import fuookami.ospf.kotlin.framework.bpp2d.domain.item.model.ActualItem
import fuookami.ospf.kotlin.framework.bpp2d.domain.item.model.ItemId
import fuookami.ospf.kotlin.framework.bpp2d.infrastructure.*

class Space2ContractTest {
    @Test
    fun splitsAlongBothAxes() {
        val space = Space2(
            x = FltX(0.0) * Meter,
            y = FltX(0.0) * Meter,
            width = FltX(10.0) * Meter,
            height = FltX(6.0) * Meter
        )
        val (left, right) = space.splitX(FltX(4.0) * Meter)
        assertTrue(left.width.value.toDouble() == 4.0)
        assertTrue(right.x.value.toDouble() == 4.0)
        assertTrue(right.width.value.toDouble() == 6.0)

        val (bottom, top) = space.splitY(FltX(2.0) * Meter)
        assertTrue(bottom.height.value.toDouble() == 2.0)
        assertTrue(top.y.value.toDouble() == 2.0)
        assertTrue(top.height.value.toDouble() == 4.0)
    }

    @Test
    fun detectsFitAndDegeneration() {
        val space = Space2(
            x = FltX(0.0) * Meter,
            y = FltX(0.0) * Meter,
            width = FltX(4.0) * Meter,
            height = FltX(2.0) * Meter
        )
        assertTrue(space.enabled(FltX(4.0) * Meter, FltX(2.0) * Meter))
        assertTrue(!space.enabled(FltX(5.0) * Meter, FltX(2.0) * Meter))

        val degenerate = space.splitY(FltX(2.0) * Meter).second
        assertTrue(degenerate.isDegenerate())
    }
}

class BlockGeneratorContractTest {
    private fun rectangleItem(
        id: String,
        width: Double,
        height: Double,
        allowRotate: Boolean = false
    ): ActualItem<FltX> {
        return ActualItem(
            id = ItemId(id),
            name = id,
            shape = rectangleShape2(
                width = FltX(width) * Meter,
                height = FltX(height) * Meter
            ).valueOrFail(),
            allowRotate = allowRotate,
            weight = FltX(1.0) * Kilogram
        )
    }

    private fun circleItem(id: String, radius: Double): ActualItem<FltX> {
        return ActualItem(
            id = ItemId(id),
            name = id,
            shape = circleShape2(FltX(radius) * Meter).valueOrFail(),
            allowRotate = true,
            weight = FltX(1.0) * Kilogram
        )
    }

    private fun space(width: Double, height: Double): Space2<FltX> {
        return Space2(
            x = FltX(0.0) * Meter,
            y = FltX(0.0) * Meter,
            width = FltX(width) * Meter,
            height = FltX(height) * Meter
        )
    }

    @Test
    fun simpleBlockFillsWholeRows() {
        val generator = SimpleBlockGenerator()
        val item = rectangleItem("a", 2.0, 1.0)
        val blocks = generator.invoke(item, space(7.0, 3.0)).valueOrFail()

        assertTrue(blocks.isNotEmpty())
        val block = blocks.first()
        // 7/2 = 3 列，3/1 = 3 行。
        assertEquals(9, block.amount)
        assertEquals("simple-block", block.source)
        for (placement in block.placements) {
            assertTrue(quantityLessOrEqual(placement.x, FltX(7.0) * Meter))
        }
    }

    @Test
    fun simpleBlockGeneratesRotatedVariantWhenAllowed() {
        val generator = SimpleBlockGenerator()
        val item = rectangleItem("a", 4.0, 1.0, allowRotate = true)
        val blocks = generator.invoke(item, space(5.0, 4.0)).valueOrFail()
        val sources = blocks.map { it.source }.toSet()
        assertTrue(sources.contains("simple-block"))
        assertTrue(sources.contains("simple-block-rotated"))
    }

    @Test
    fun simpleBlockSkipsCircleItems() {
        val generator = SimpleBlockGenerator()
        val circle = circleItem("c", 1.0)
        val blocks = generator.invoke(circle, space(10.0, 10.0)).valueOrFail()
        assertTrue(blocks.isEmpty())
    }

    @Test
    fun complexBlockStitchesMultipleItems() {
        val generator = ComplexBlockGenerator()
        val items = listOf(
            rectangleItem("big", 4.0, 2.0),
            rectangleItem("small", 2.0, 1.0)
        )
        val blocks = generator.invoke(items, space(4.0, 3.0)).valueOrFail()
        assertEquals(1, blocks.size)
        val block = blocks.first()
        val ids = block.itemAmounts.keys
        assertTrue(ids.contains("big"))
        // 第 1 行放 1 件 big（宽 4 占满），第 2 行放 2 件 small（各宽 2）。
        // Row 1 holds one big item (width 4 fills the row); row 2 holds two small items (width 2 each).
        assertEquals(3, block.amount)
    }

    @Test
    fun complexBlockIgnoresCircleItems() {
        val generator = ComplexBlockGenerator()
        val items = listOf(circleItem("c", 1.0), rectangleItem("r", 2.0, 2.0))
        val blocks = generator.invoke(items, space(6.0, 4.0)).valueOrFail()
        assertEquals(1, blocks.size)
        assertEquals(setOf("r"), blocks.first().itemAmounts.keys)
    }
}

class SpaceSplitAlgorithmContractTest {
    private fun item(id: String, width: Double, height: Double): ActualItem<FltX> {
        return ActualItem(
            id = ItemId(id),
            name = id,
            shape = rectangleShape2(
                width = FltX(width) * Meter,
                height = FltX(height) * Meter
            ).valueOrFail(),
            allowRotate = false,
            weight = FltX(1.0) * Kilogram
        )
    }

    private fun space(width: Double, height: Double): Space2<FltX> {
        return Space2(
            x = FltX(0.0) * Meter,
            y = FltX(0.0) * Meter,
            width = FltX(width) * Meter,
            height = FltX(height) * Meter
        )
    }

    @Test
    fun depthFirstPlacesWithinBoundsWithoutOverlap() {
        val algorithm = DepthFirstSearchAlgorithm2D()
        val items = listOf(item("a", 2.0, 2.0) to 2, item("b", 1.0, 1.0) to 3)
        val result = algorithm.invoke(space(5.0, 5.0), items).valueOrFail()

        assertTrue(result.placements.isNotEmpty())
        assertEquals("dfs", result.source)
        for (placement in result.placements) {
            assertTrue(placement.insideSheet(sheet(5.0, 5.0)).valueOrFail())
        }
        for (i in result.placements.indices) {
            for (j in i + 1 until result.placements.size) {
                assertTrue(!result.placements[i].overlapped(result.placements[j]).valueOrFail())
            }
        }
    }

    @Test
    fun multiRowPlacesRowsBottomUp() {
        val algorithm = MultiRowHeuristicAlgorithm2D()
        val items = listOf(item("a", 2.0, 1.0) to 6)
        val result = algorithm.invoke(space(6.0, 3.0), items).valueOrFail()

        assertEquals("multi-row", result.source)
        assertEquals(6, result.placements.size)
        assertEquals(6, result.placedAmounts["a"])
        assertTrue(result.restAmounts.isEmpty())
    }

    @Test
    fun contextExposesAllStrategies() {
        val context = BlockLoadingContext<FltX>()
        val items = listOf(item("a", 2.0, 1.0) to 4)
        val target = space(6.0, 4.0)

        assertTrue(context.localBlocks(items, target).valueOrFail().isNotEmpty())
        assertTrue(context.globalBlocks(items, target).valueOrFail().isNotEmpty())
        assertTrue(context.depthFirst(target, items).valueOrFail().placements.isNotEmpty())
        assertTrue(context.multiRow(target, items).valueOrFail().placements.isNotEmpty())
    }

    private fun sheet(width: Double, height: Double): Sheet2Shape<FltX> {
        return sheet2Shape(
            width = FltX(width) * Meter,
            height = FltX(height) * Meter
        ).valueOrFail()
    }
}

class RotationGuardContractTest {
    @Test
    fun rejectsRotationForNonRotatableRectangle() {
        val item = ActualItem(
            id = ItemId("r"),
            name = "r",
            shape = rectangleShape2(
                width = FltX(2.0) * Meter,
                height = FltX(1.0) * Meter
            ).valueOrFail(),
            allowRotate = false,
            weight = FltX(1.0) * Kilogram
        )
        assertTrue(requireRotationAllowed(item) is Failed)
    }

    @Test
    fun allowsRotationForRotatableAndCircleItems() {
        val rotatable = ActualItem(
            id = ItemId("r"),
            name = "r",
            shape = rectangleShape2(
                width = FltX(2.0) * Meter,
                height = FltX(1.0) * Meter
            ).valueOrFail(),
            allowRotate = true,
            weight = FltX(1.0) * Kilogram
        )
        assertTrue(requireRotationAllowed(rotatable) is Ok)

        val circle = ActualItem(
            id = ItemId("c"),
            name = "c",
            shape = circleShape2(FltX(1.0) * Meter).valueOrFail(),
            allowRotate = false,
            weight = FltX(1.0) * Kilogram
        )
        assertTrue(requireRotationAllowed(circle) is Ok)
    }
}

/**
 * 解包 Ret 结果，失败时抛出断言错误。 / Unwrap a Ret result, throwing an assertion error on failure.
 *
 * @param message 断言失败时的错误信息 / error message when the assertion fails
 * @return 成功时的值 / value on success
*/
private fun <T> Ret<T>.valueOrFail(message: String = "result should succeed"): T {
    return when (this) {
        is Ok -> value
        is Failed -> kotlin.test.fail("$message: ${error.message}")
        is fuookami.ospf.kotlin.utils.functional.Fatal ->
            kotlin.test.fail("$message: ${errors.joinToString { it.message }}")
    }
}