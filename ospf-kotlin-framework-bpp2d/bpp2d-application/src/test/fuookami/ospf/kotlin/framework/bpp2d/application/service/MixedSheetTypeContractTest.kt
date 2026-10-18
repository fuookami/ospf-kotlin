package fuookami.ospf.kotlin.framework.bpp2d.application.service

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
import fuookami.ospf.kotlin.framework.bpp2d.domain.item.model.ActualItem
import fuookami.ospf.kotlin.framework.bpp2d.domain.item.model.ItemId
import fuookami.ospf.kotlin.framework.bpp2d.domain.item.model.Layer
import fuookami.ospf.kotlin.framework.bpp2d.domain.item.model.SheetType
import fuookami.ospf.kotlin.framework.bpp2d.domain.item.model.SheetTypeId
import fuookami.ospf.kotlin.framework.bpp2d.domain.layer_assignment.service.limits.layerWasteArea
import fuookami.ospf.kotlin.framework.bpp2d.domain.layer_assignment.service.limits.layerWasteRatio
import fuookami.ospf.kotlin.framework.bpp2d.domain.layer_assignment.service.limits.perTypeAmountCoefficient
import fuookami.ospf.kotlin.framework.bpp2d.domain.layer_assignment.service.limits.perTypeCost
import fuookami.ospf.kotlin.framework.bpp2d.domain.layer_generation.Bpp2dLayerGenerationRequest
import fuookami.ospf.kotlin.framework.bpp2d.domain.layer_generation.LayerGenerationContext
import fuookami.ospf.kotlin.framework.bpp2d.infrastructure.*

class MixedSheetTypeContractTest {
    private fun sheetType(id: String, width: Double, height: Double, isMain: Boolean = false): SheetType<FltX> {
        return SheetType(
            width = FltX(width) * Meter,
            height = FltX(height) * Meter,
            maxLoad = FltX(10000.0) * Kilogram,
            typeCode = SheetTypeId(id),
            isMain = isMain
        )
    }

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

    private fun column(type: SheetType<FltX>, itemId: String, count: Int): Layer<FltX> {
        val shape = rectangleShape2(
            width = FltX(2.0) * Meter,
            height = FltX(1.0) * Meter
        ).valueOrFail()
        val units = (0 until count).map { index ->
            Placement2(
                shape = shape,
                position = QuantityPoint2(
                    x = FltX(2.0 * index) * Meter,
                    y = FltX(0.0) * Meter
                ),
                itemId = itemId
            )
        }
        return Layer(
            iteration = 0L,
            from = Layer::class,
            sheet = type,
            shape = Sheet2Shape(
                width = FltX(2.0 * count) * Meter,
                height = FltX(1.0) * Meter
            ),
            units = units
        )
    }

    @Test
    fun wasteAreaIsSheetAreaMinusOccupiedArea() {
        // 板材 10×6 = 60 m²，占用 3 件 × 2 m² = 6 m²，余料 54 m²。
        // Sheet 10 by 6 equals 60 m², occupied is 3 pieces times 2 m², so the waste is 54 m².
        val type = sheetType("S", 10.0, 6.0)
        val target = column(type, "R", 3)
        assertEquals(54.0, layerWasteArea(target).toDouble(), 1e-9)
        assertEquals(0.9, layerWasteRatio(target).toDouble(), 1e-9)
    }

    @Test
    fun wasteAreaIsZeroWhenNoSheetType() {
        val untyped = Layer(
            iteration = 0L,
            from = Layer::class,
            sheet = null,
            shape = Sheet2Shape(width = FltX(2.0) * Meter, height = FltX(1.0) * Meter),
            units = emptyList()
        )
        assertEquals(0.0, layerWasteArea(untyped).toDouble(), 1e-9)
    }

    @Test
    fun columnsForDifferentSheetTypesAreNotDeduplicated() {
        // 同一排布放在两种板材上是两个不同的列；若按 units 去重会丢掉板材类型。
        // The same layout on two sheet types is two distinct columns; deduplicating by units alone would lose the
        // sheet type.
        val small = sheetType("SMALL", 10.0, 6.0)
        val big = sheetType("BIG", 20.0, 12.0, isMain = true)
        val onSmall = column(small, "R", 3)
        val onBig = column(big, "R", 3)
        assertTrue(onSmall != onBig, "不同板材上的同排布必须视为不同列")
    }

    @Test
    fun generationProducesColumnsForEverySheetType() {
        val small = sheetType("SMALL", 10.0, 6.0)
        val big = sheetType("BIG", 20.0, 12.0, isMain = true)
        val context = LayerGenerationContext<FltX>()
        val results = context.generate(
            Bpp2dLayerGenerationRequest(
                iteration = 0,
                sheet = big,
                sheets = listOf(small, big),
                items = listOf(rectangleItem("R", 2.0, 1.0) to 30)
            )
        )
        val types = results.mapNotNull { it.layer.sheet?.typeCode?.value }.toSet()
        assertTrue(types.contains("SMALL"), "应为小板材生成候选列")
        assertTrue(types.contains("BIG"), "应为大板材生成候选列")
    }

    @Test
    fun perTypeCostAndAmountHelpersFallBackToDefaults() {
        val small = sheetType("SMALL", 10.0, 6.0)
        val big = sheetType("BIG", 20.0, 12.0, isMain = true)
        val cost = perTypeCost(
            typeCost = mapOf(SheetTypeId("SMALL") to FltX(5.0)),
            defaultCost = FltX(9.0)
        )
        assertEquals(5.0, cost(column(small, "R", 1)).toDouble(), 1e-9)
        assertEquals(9.0, cost(column(big, "R", 1)).toDouble(), 1e-9)

        val coefficient = perTypeAmountCoefficient(
            typeCoefficient = mapOf(SheetTypeId("BIG") to FltX(2.0)),
            defaultCoefficient = FltX(1.0)
        )
        assertEquals(1.0, coefficient(column(small, "R", 1)).toDouble(), 1e-9)
        assertEquals(2.0, coefficient(column(big, "R", 1)).toDouble(), 1e-9)
    }
}

private fun <T> Ret<T>.valueOrFail(message: String = "result should succeed"): T {
    return when (this) {
        is Ok -> value
        is Failed -> kotlin.test.fail("$message: ${error.message}")
        is fuookami.ospf.kotlin.utils.functional.Fatal ->
            kotlin.test.fail("$message: ${errors.joinToString { it.message }}")
    }
}