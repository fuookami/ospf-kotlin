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
import fuookami.ospf.kotlin.framework.geometry2d.*
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
            // 圆网格必须按**圆心距**判定重叠：外接正方形对六角网格过严，斜向相邻的两圆
            // 其外接方块必然相交，但圆本身并不重叠。
            // Circle grids must be validated by **centre distance**: the enclosing square is too strict for a
            // hex grid because diagonally adjacent circles have intersecting bounding squares while the circles
            // themselves do not overlap.
            // 用平方距离比较，避免对泛型数值开方：圆心距² 必须不小于直径²。
            // Compare squared distances to avoid taking a root on a generic value: the squared centre distance
            // must be at least the squared diameter.
            val diameter = layout.placements.first().width.value
            val diameterSquared = diameter * diameter
            for (i in layout.placements.indices) {
                for (j in i + 1 until layout.placements.size) {
                    val dx = layout.placements[i].x.value - layout.placements[j].x.value
                    val dy = layout.placements[i].y.value - layout.placements[j].y.value
                    val distanceSquared = dx * dx + dy * dy
                    assertTrue(
                        distanceSquared >= diameterSquared,
                        "网格 ${layout.mode} 中第 $i 与第 $j 个圆重叠：圆心距² $distanceSquared < 直径² $diameterSquared"
                    )
                }
            }
        }
    }

    @Test
    fun hexRowPitchScalesWithDiameter() {
        // 行距必须是 26/30·d 而非常量 0.8667：用两种直径验证行距随之翻倍。
        // The row pitch must be 26/30·d rather than the constant 0.8667: two diameters must double it.
        val generator = CircleGridGenerator()
        val small = generator.invoke(circleItem("small", 0.5), sheet(10.0, 10.0))
            .first { it.mode == CircleGridMode.HexGrid }
        val large = generator.invoke(circleItem("large", 1.0), sheet(20.0, 20.0))
            .first { it.mode == CircleGridMode.HexGrid }

        val smallRowPitch = rowPitchOf(small)
        val largeRowPitch = rowPitchOf(large)
        assertTrue(
            kotlin.math.abs(largeRowPitch - smallRowPitch * 2.0) < 1e-6,
            "行距必须随直径缩放：small=$smallRowPitch, large=$largeRowPitch"
        )
        // 且行距略大于 √3/2·d（保守，保证斜向相邻不重叠）。
        // It must also slightly exceed √3/2·d (conservative, so diagonal neighbours never overlap).
        assertTrue(
            smallRowPitch > 0.866025 * 1.0 - 1e-9,
            "行距应不小于 √3/2·d：$smallRowPitch"
        )
    }

    /** 由六角布局反推行距。 / Derive the row pitch from a hex layout. */
    private fun rowPitchOf(layout: CircleGridLayout<FltX>): Double {
        val ys = layout.placements.map { it.y.value.toDouble() }.distinct().sorted()
        return if (ys.size >= 2) (ys[1] - ys[0]) else 0.0
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
                Placement2(
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