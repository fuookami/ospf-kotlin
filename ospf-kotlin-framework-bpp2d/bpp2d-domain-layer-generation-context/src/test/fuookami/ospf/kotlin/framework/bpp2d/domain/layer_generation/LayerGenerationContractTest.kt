package fuookami.ospf.kotlin.framework.bpp2d.domain.layer_generation

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
import fuookami.ospf.kotlin.framework.bpp2d.domain.item.model.*
import fuookami.ospf.kotlin.framework.bpp2d.domain.layer_generation.service.*
import fuookami.ospf.kotlin.framework.bpp2d.infrastructure.*

class CircleGridGeneratorContractTest {
    private fun circleItem(id: String, radius: Double): ActualItem<FltX> {
        return ActualItem(
            id = ItemId(id),
            name = id,
            shape = circleShape2(FltX(radius) * Meter).valueOrFail(),
            allowRotate = true,
            weight = FltX(1.0) * Kilogram
        )
    }

    private fun sheet(width: Double, height: Double): SheetType<FltX> {
        return SheetType(
            width = FltX(width) * Meter,
            height = FltX(height) * Meter,
            maxLoad = FltX(1000.0) * Kilogram,
            typeCode = SheetTypeId("S")
        )
    }

    @Test
    fun squareGridPlacesOneCirclePerDiameterCell() {
        val generator = CircleGridGenerator()
        val layouts = generator.invoke(circleItem("c", 0.5), sheet(4.0, 3.0))
        val square = layouts.first { it.mode == CircleGridMode.SquareGrid }
        // 直径 1：4 列 × 3 行。
        assertEquals(12, square.placements.size)
    }

    @Test
    fun hexGridBeatsSquareGridOnLargeSheet() {
        val generator = CircleGridGenerator()
        val layouts = generator.invoke(circleItem("c", 0.5), sheet(10.0, 10.0))
        val square = layouts.first { it.mode == CircleGridMode.SquareGrid }
        val hex = layouts.first { it.mode == CircleGridMode.HexGrid }
        assertTrue(
            hex.placements.size > square.placements.size,
            "六角网格应比直角网格放得更多：hex=${hex.placements.size}, square=${square.placements.size}"
        )
    }

    @Test
    fun everyGridLayoutIsOverlapFreeAndInsideSheet() {
        val generator = CircleGridGenerator()
        val sheet = sheet(9.0, 7.0)
        val target = Sheet2Shape(width = sheet.width, height = sheet.height)
        for (layout in generator.invoke(circleItem("c", 0.5), sheet)) {
            for (placement in layout.placements) {
                assertTrue(placement.insideSheet(target).valueOrFail())
            }
            for (i in layout.placements.indices) {
                for (j in i + 1 until layout.placements.size) {
                    assertTrue(
                        !layout.placements[i].overlapped(layout.placements[j]).valueOrFail(),
                        "网格 ${layout.mode} 中第 $i 与第 $j 个圆重叠"
                    )
                }
            }
        }
    }

    @Test
    fun circleLargerThanSheetProducesNoLayout() {
        val generator = CircleGridGenerator()
        assertTrue(generator.invoke(circleItem("c", 5.0), sheet(4.0, 4.0)).isEmpty())
    }

    @Test
    fun circleGeneratorIgnoresRectangleItems() {
        val generator = CircleGridGenerator()
        val rectangle = ActualItem(
            id = ItemId("r"),
            name = "r",
            shape = rectangleShape2(
                width = FltX(2.0) * Meter,
                height = FltX(1.0) * Meter
            ).valueOrFail(),
            allowRotate = true,
            weight = FltX(1.0) * Kilogram
        )
        assertTrue(generator.invoke(rectangle, sheet(10.0, 10.0)).isEmpty())
    }
}

class LayerGeneratorContractTest {
    private fun rectangleItem(id: String, width: Double, height: Double, allowRotate: Boolean = false): ActualItem<FltX> {
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

    private fun sheet(width: Double, height: Double): SheetType<FltX> {
        return SheetType(
            width = FltX(width) * Meter,
            height = FltX(height) * Meter,
            maxLoad = FltX(1000.0) * Kilogram,
            typeCode = SheetTypeId("S")
        )
    }

    @Test
    fun rectangleRequestProducesCandidates() {
        val context = LayerGenerationContext<FltX>()
        val request = Bpp2dLayerGenerationRequest(
            iteration = 0,
            sheet = sheet(10.0, 6.0),
            items = listOf(rectangleItem("r", 2.0, 1.0, allowRotate = true) to 12)
        )
        val results = context.generate(request)
        assertTrue(results.isNotEmpty())
        assertTrue(results.all { it.layer.units.isNotEmpty() })
    }

    @Test
    fun pureCircleRequestOnlyUsesCircleGenerators() {
        val context = LayerGenerationContext<FltX>()
        val request = Bpp2dLayerGenerationRequest(
            iteration = 0,
            sheet = sheet(10.0, 10.0),
            items = listOf(circleItem("c", 0.5) to 20)
        )
        val results = context.generate(request)
        assertTrue(results.isNotEmpty())
        val sources = results.map { it.source }.toSet()
        assertTrue(sources.all { it.startsWith("circle-") })
        assertTrue(sources.contains("circle-rect-grid"))
        assertTrue(sources.contains("circle-hex-grid"))
    }

    @Test
    fun mixedRequestProducesMixedCandidates() {
        val context = LayerGenerationContext<FltX>()
        val request = Bpp2dLayerGenerationRequest(
            iteration = 0,
            sheet = sheet(12.0, 10.0),
            items = listOf(
                rectangleItem("r", 3.0, 2.0) to 4,
                circleItem("c", 0.5) to 20
            )
        )
        val results = context.generate(request)
        assertTrue(results.isNotEmpty())
        assertTrue(results.any { it.source == "mixed-shape" })
    }

    @Test
    fun maxCandidatesIsRespected() {
        val context = LayerGenerationContext<FltX>()
        val request = Bpp2dLayerGenerationRequest(
            iteration = 0,
            sheet = sheet(10.0, 10.0),
            items = listOf(rectangleItem("r", 2.0, 1.0, allowRotate = true) to 20),
            maxCandidates = 2
        )
        assertTrue(context.generate(request).size <= 2)
    }

    @Test
    fun historicalGeneratorReplaysExistingLayers() {
        val sheet = sheet(10.0, 10.0)
        val shape = rectangleShape2(
            width = FltX(2.0) * Meter,
            height = FltX(1.0) * Meter
        ).valueOrFail()
        val existing = Layer(
            iteration = 0L,
            from = Layer::class,
            sheet = sheet,
            shape = Sheet2Shape(width = FltX(2.0) * Meter, height = FltX(1.0) * Meter),
            units = listOf(
                LayerPlacement2(
                    shape = shape,
                    position = QuantityPoint2(x = FltX(0.0) * Meter, y = FltX(0.0) * Meter),
                    itemId = "r"
                )
            )
        )
        val context = LayerGenerationContext<FltX>(generators = listOf(HistoricalLayerGenerator()))
        val results = context.generate(
            Bpp2dLayerGenerationRequest(
                iteration = 0,
                sheet = sheet,
                items = emptyList(),
                existingLayers = listOf(existing)
            )
        )
        assertEquals(1, results.size)
        assertEquals("historical", results.first().source)
    }

    @Test
    fun shadowPriceScoreReordersCandidates() {
        val context = LayerGenerationContext<FltX>(generators = listOf(PatternLayerGenerator()))
        val request = Bpp2dLayerGenerationRequest(
            iteration = 0,
            sheet = sheet(10.0, 10.0),
            items = listOf(rectangleItem("r", 2.0, 1.0) to 20),
            scoreByShadowPrice = { layer, _ -> FuLlX(layer.amount) }
        )
        val results = context.generate(request)
        assertTrue(results.isNotEmpty())
        assertTrue(results.all { it.score != null })
        for (i in 1 until results.size) {
            assertTrue(results[i - 1].layer.units.size >= results[i].layer.units.size)
        }
    }

    /**
     * 把件数换算为可用于评分的 FltX 数值。 / Convert an amount into a FltX score value.
     *
     * @param amount 件数 / amount
     * @return 评分值 / score value
    */
    private fun FuLlX(amount: fuookami.ospf.kotlin.math.algebra.number.UInt64): FltX {
        return FltX(amount.toULong().toDouble())
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
