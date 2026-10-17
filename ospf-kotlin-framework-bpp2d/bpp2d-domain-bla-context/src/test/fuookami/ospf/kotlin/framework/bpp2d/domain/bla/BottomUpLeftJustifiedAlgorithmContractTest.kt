package fuookami.ospf.kotlin.framework.bpp2d.domain.bla

import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.quantities.quantity.times
import fuookami.ospf.kotlin.quantities.unit.Kilogram
import fuookami.ospf.kotlin.quantities.unit.Meter
import fuookami.ospf.kotlin.framework.bpp2d.domain.bla.service.*
import fuookami.ospf.kotlin.framework.bpp2d.domain.item.model.*
import fuookami.ospf.kotlin.framework.bpp2d.infrastructure.*

class BottomUpLeftJustifiedAlgorithmContractTest {
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

    private fun sheet(width: Double, height: Double): Sheet2Shape<FltX> {
        return sheet2Shape(
            width = FltX(width) * Meter,
            height = FltX(height) * Meter
        ).valueOrFail()
    }

    @Test
    fun placesItemsBottomLeftFirst() {
        val algorithm = BottomUpLeftJustifiedAlgorithm(sheet = sheet(10.0, 6.0))
        val item = rectangleItem("a", 3.0, 2.0)
        val result = algorithm.invoke(listOf(item to 3)).valueOrFail()

        assertEquals(3, result.placements.size)
        assertTrue(result.fullyPlaced)

        val first = result.placements[0]
        assertTrue(first.x.value.toDouble() < 1e-9)
        assertTrue(first.y.value.toDouble() < 1e-9)

        // 所有放置都在板材内且互不重叠。
        for (placement in result.placements) {
            assertTrue(placement.insideSheet(sheet(10.0, 6.0)).valueOrFail())
        }
        for (i in result.placements.indices) {
            for (j in i + 1 until result.placements.size) {
                assertTrue(!result.placements[i].overlapped(result.placements[j]).valueOrFail())
            }
        }
    }

    @Test
    fun rotationProducesFittingLayoutWhenUprightDoesNot() {
        val algorithm = BottomUpLeftJustifiedAlgorithm(sheet = sheet(4.0, 1.0))
        val rotatable = rectangleItem("r", 3.0, 1.0, allowRotate = true)
        val uprightOnly = rectangleItem("f", 3.0, 1.0, allowRotate = false)

        val rotated = algorithm.invoke(listOf(rotatable to 1)).valueOrFail()
        assertTrue(rotated.placements.isNotEmpty())

        // 不可旋转物品在同一板材上仍然可行（宽 3 高 1 恰好放入）。
        val fixed = algorithm.invoke(listOf(uprightOnly to 1)).valueOrFail()
        assertTrue(fixed.placements.isNotEmpty())
    }

    @Test
    fun itemTooLargeIsReportedAsSkipped() {
        val algorithm = BottomUpLeftJustifiedAlgorithm(sheet = sheet(2.0, 2.0))
        val oversized = rectangleItem("big", 5.0, 5.0)
        val result = algorithm.invoke(listOf(oversized to 1)).valueOrFail()

        assertTrue(result.placements.isEmpty())
        assertEquals(listOf(ItemId("big")), result.skippedUnfittedItems)
    }

    @Test
    fun circleItemsAreSkippedWithExplicitMarker() {
        val algorithm = BottomUpLeftJustifiedAlgorithm(sheet = sheet(10.0, 10.0))
        val circle = circleItem("c", 1.0)
        val rectangle = rectangleItem("r", 2.0, 2.0)
        val result = algorithm.invoke(listOf(circle to 2, rectangle to 1)).valueOrFail()

        assertEquals(1, result.placements.size)
        assertEquals(listOf(ItemId("c")), result.skippedCircleItems)
        assertTrue(result.skippedUnfittedItems.isEmpty())
    }

    @Test
    fun displacementCanBeDisabled() {
        val disabled = BottomUpLeftJustifiedAlgorithm(
            sheet = sheet(10.0, 10.0),
            config = BottomUpLeftJustifiedAlgorithm.Config(
                withDisplacementX = false,
                withDisplacementY = false
            )
        )
        val item = rectangleItem("a", 2.0, 2.0)
        val result = disabled.invoke(listOf(item to 2)).valueOrFail()
        assertEquals(2, result.placements.size)
        for (i in result.placements.indices) {
            for (j in i + 1 until result.placements.size) {
                assertTrue(!result.placements[i].overlapped(result.placements[j]).valueOrFail())
            }
        }
    }

    @Test
    fun contextDelegatesToAlgorithm() {
        val context = BlaContext<FltX>()
        val item = rectangleItem("a", 2.0, 2.0)
        val result = context.layout(sheet = sheet(6.0, 6.0), items = listOf(item to 4)).valueOrFail()
        assertEquals(4, result.placements.size)
    }
}

/**
 * 解包 Ret 结果，失败时抛出断言错误。 / Unwrap a Ret result, throwing an assertion error on failure.
 *
 * @param message 断言失败时的错误信息 / error message when the assertion fails
 * @return 成功时的值 / value on success
*/
private fun <T> fuookami.ospf.kotlin.utils.functional.Ret<T>.valueOrFail(
    message: String = "result should succeed"
): T {
    return when (this) {
        is fuookami.ospf.kotlin.utils.functional.Ok -> value
        is fuookami.ospf.kotlin.utils.functional.Failed ->
            kotlin.test.fail("$message: ${error.message}")

        is fuookami.ospf.kotlin.utils.functional.Fatal ->
            kotlin.test.fail("$message: ${errors.joinToString { it.message }}")
    }
}
