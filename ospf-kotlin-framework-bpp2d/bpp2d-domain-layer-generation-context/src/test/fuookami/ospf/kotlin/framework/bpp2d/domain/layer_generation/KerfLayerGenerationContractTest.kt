package fuookami.ospf.kotlin.framework.bpp2d.domain.layer_generation

import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.quantities.quantity.times
import fuookami.ospf.kotlin.quantities.unit.Kilogram
import fuookami.ospf.kotlin.quantities.unit.Meter
import fuookami.ospf.kotlin.framework.bpp2d.domain.item.model.ActualItem
import fuookami.ospf.kotlin.framework.bpp2d.domain.item.model.ItemId
import fuookami.ospf.kotlin.framework.bpp2d.domain.item.model.SheetType
import fuookami.ospf.kotlin.framework.bpp2d.domain.item.model.SheetTypeId
import fuookami.ospf.kotlin.framework.bpp2d.domain.layer_generation.service.CircleGridGenerator
import fuookami.ospf.kotlin.framework.bpp2d.infrastructure.*
import fuookami.ospf.kotlin.framework.bpp2d.infrastructure.Sheet2Shape

class KerfLayerGenerationContractTest {
    private fun rectangleItem(id: String, width: Double, height: Double): ActualItem<FltX> {
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

    private fun circleItem(id: String, radius: Double): ActualItem<FltX> {
        return ActualItem(
            id = ItemId(id),
            name = id,
            shape = circleShape2(FltX(radius) * Meter).valueOrFail(),
            allowRotate = true,
            weight = FltX(1.0) * Kilogram
        )
    }

    private fun sheetType(width: Double, height: Double): SheetType<FltX> {
        return SheetType(
            width = FltX(width) * Meter,
            height = FltX(height) * Meter,
            maxLoad = FltX(1000.0) * Kilogram,
            typeCode = SheetTypeId("S"),
            isMain = true
        )
    }

    @Test
    fun kerfReducesPatternColumnCount() {
        val context = LayerGenerationContext<FltX>()
        val item = rectangleItem("R", 2.0, 1.0)
        val sheet = sheetType(10.0, 5.0)
        val baseRequest = Bpp2dLayerGenerationRequest(
            iteration = 0,
            sheet = sheet,
            items = listOf(item to 20)
        )
        // 多种生成器可能产出同一层并被去重，因此按「最大件数」比较而不是按来源名。
        // Several generators may produce the same layer and be deduplicated, so compare by maximum piece count
        // rather than by source name.
        val withoutKerf = context.generate(baseRequest).maxByOrNull { it.layer.units.size }
        val withKerf = context.generate(
            baseRequest.copy(kerf = FltX(0.5) * Meter)
        ).maxByOrNull { it.layer.units.size }

        assertTrue(withoutKerf != null, "无刀缝时应产出图样层 / a pattern layer is expected without a kerf")
        assertTrue(withKerf != null, "有刀缝时应产出图样层 / a pattern layer is expected with a kerf")
        // 无 kerf：5 列 × 5 行 = 25；有 kerf（节距 2.5 × 1.5）：4 列 × 3 行 = 12。
        // Without a kerf: 5 columns times 5 rows. With a kerf (pitch 2.5 by 1.5): 4 columns times 3 rows.
        assertEquals(25, withoutKerf!!.layer.units.size)
        assertEquals(12, withKerf!!.layer.units.size)
    }

    @Test
    fun kerfKeepsRealGapBetweenNeighbours() {
        val context = LayerGenerationContext<FltX>()
        val item = rectangleItem("R", 2.0, 1.0)
        val kerf = FltX(0.5) * Meter
        val results = context.generate(
            Bpp2dLayerGenerationRequest(
                iteration = 0,
                sheet = sheetType(10.0, 5.0),
                items = listOf(item to 20),
                kerf = kerf
            )
        )
        val pattern = results.filter { it.source == "pattern" }
            .maxByOrNull { it.layer.units.size }
            ?: return

        val units = pattern.layer.units
        for (i in units.indices) {
            for (j in i + 1 until units.size) {
                val lhs = units[i]
                val rhs = units[j]
                // 相邻件在任一轴上至少相隔 kerf；用膨胀形状判定不应重叠。
                // Neighbours must be at least a kerf apart on some axis, so inflated shapes must not overlap.
                val inflated = lhs.copy(shape = lhs.shape.inflatedBy(kerf))
                assertTrue(
                    !inflated.overlapped(rhs).valueOrFail(),
                    "刀缝不足：第 $i 与第 $j 件 / insufficient kerf between pieces $i and $j"
                )
            }
        }
    }

    @Test
    fun kerfReducesCircleGridCount() {
        val generator = CircleGridGenerator()
        val item = circleItem("C", 0.5)
        val sheet = sheetType(10.0, 10.0)
        val withoutKerf = generator.invoke(item, sheet).first { it.mode.name == "SquareGrid" }
        val withKerf = generator.invoke(item, sheet, FltX(0.5) * Meter)
            .first { it.mode.name == "SquareGrid" }

        // 节距 1.0 → 100 件；节距 1.5 → 6 列 × 6 行 = 36 件。
        // Pitch 1.0 gives 100 pieces; pitch 1.5 gives 6 columns times 6 rows.
        assertEquals(100, withoutKerf.placements.size)
        assertEquals(36, withKerf.placements.size)
    }

    @Test
    fun kerfSpacingFallsBackToZero() {
        val request = Bpp2dLayerGenerationRequest<FltX>(
            iteration = 0,
            sheet = sheetType(10.0, 10.0),
            items = emptyList()
        )
        val sample: Quantity<FltX> = FltX(1.0) * Meter
        assertEquals(0.0, request.kerfSpacing(sample).value.toDouble(), 1e-9)
    }

    @Test
    fun layoutStaysInsideSheetWithKerf() {
        val context = LayerGenerationContext<FltX>()
        val kerf = FltX(0.5) * Meter
        val sheet = sheetType(9.0, 7.0)
        val results = context.generate(
            Bpp2dLayerGenerationRequest(
                iteration = 0,
                sheet = sheet,
                items = listOf(rectangleItem("R", 2.0, 1.0) to 20),
                kerf = kerf
            )
        )
        val target = Sheet2Shape(width = sheet.width, height = sheet.height)
        for (result in results) {
            for (unit in result.layer.units) {
                assertTrue(
                    unit.insideSheet(target).valueOrFail(),
                    "带刀缝的放置仍在板材内 / a kerf-aware placement stays inside the sheet"
                )
            }
        }
    }
}

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
