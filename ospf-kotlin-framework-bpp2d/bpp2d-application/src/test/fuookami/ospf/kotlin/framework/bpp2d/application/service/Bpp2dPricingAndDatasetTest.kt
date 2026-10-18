package fuookami.ospf.kotlin.framework.bpp2d.application.service

import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.Ret
import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.quantities.quantity.times
import fuookami.ospf.kotlin.quantities.unit.Kilogram
import fuookami.ospf.kotlin.quantities.unit.Meter
import fuookami.ospf.kotlin.framework.bpp2d.domain.item.model.Layer
import fuookami.ospf.kotlin.framework.bpp2d.domain.item.model.Sheet
import fuookami.ospf.kotlin.framework.bpp2d.domain.item.model.SheetLayer
import fuookami.ospf.kotlin.framework.bpp2d.domain.item.model.SheetType
import fuookami.ospf.kotlin.framework.bpp2d.domain.item.model.SheetTypeId
import fuookami.ospf.kotlin.framework.bpp2d.domain.layer_assignment.model.Bpp2dAssignmentDemandEntry
import fuookami.ospf.kotlin.framework.bpp2d.domain.layer_assignment.model.Bpp2dAssignmentDemandMode
import fuookami.ospf.kotlin.framework.bpp2d.domain.layer_assignment.model.Bpp2dAssignmentDemandValue
import fuookami.ospf.kotlin.framework.bpp2d.domain.layer_assignment.model.Bpp2dDemandShadowPriceKey
import fuookami.ospf.kotlin.framework.bpp2d.domain.layer_assignment.model.isImprovingColumn
import fuookami.ospf.kotlin.framework.bpp2d.domain.layer_assignment.model.layerReducedCost
import fuookami.ospf.kotlin.framework.bpp2d.infrastructure.*
import fuookami.ospf.kotlin.framework.bpp2d.infrastructure.LayerPlacement2
import fuookami.ospf.kotlin.framework.bpp2d.infrastructure.Sheet2Shape

class TailSheetOptimizerTest {
    private fun layer(itemId: String, count: Int, width: Double = 1.0): Layer<FltX> {
        val shape = rectangleShape2(
            width = FltX(width) * Meter,
            height = FltX(1.0) * Meter
        ).valueOrFail()
        val units = (0 until count).map { index ->
            LayerPlacement2(
                shape = shape,
                position = QuantityPoint2(
                    x = FltX(width * index) * Meter,
                    y = FltX(0.0) * Meter
                ),
                itemId = itemId
            )
        }
        return Layer(
            iteration = 0L,
            from = Layer::class,
            sheet = null,
            shape = Sheet2Shape(
                width = FltX(width * count) * Meter,
                height = FltX(1.0) * Meter
            ),
            units = units
        )
    }

    private fun sheetOf(type: SheetType<FltX>, layer: Layer<FltX>): Sheet<FltX> {
        return Sheet(
            type = type,
            layers = listOf(
                SheetLayer(
                    type = type,
                    shape = Sheet2Shape(width = type.width, height = type.height),
                    layers = listOf(layer)
                )
            )
        )
    }

    private fun sheetType(): SheetType<FltX> {
        return SheetType(
            width = FltX(20.0) * Meter,
            height = FltX(12.0) * Meter,
            maxLoad = FltX(1000.0) * Kilogram,
            typeCode = SheetTypeId("S"),
            isMain = true
        )
    }

    @Test
    fun tailSheetSwitchesToBetterFittingColumn() {
        val type = sheetType()
        // 原列覆盖 6 件，但剩余需求只有 3 件；候选列恰好覆盖 3 件。
        // The original column covers 6 pieces while the residual demand is only 3; a candidate covers exactly 3.
        val original = layer("R1", 6)
        val fitting = layer("R1", 3)
        val optimizer = TailSheetOptimizer(candidates = listOf(fitting))

        val result = optimizer.optimize(
            sheets = listOf(sheetOf(type, original)),
            demands = mapOf("R1" to 3uL)
        )

        assertEquals(1, result.size)
        val produced = result.first().layers.first().layers.first().units.size
        assertEquals(3, produced)
    }

    @Test
    fun keepsOriginalWhenItFitsBest() {
        val type = sheetType()
        val original = layer("R1", 3)
        val worse = layer("R1", 6)
        val optimizer = TailSheetOptimizer(candidates = listOf(worse))

        val result = optimizer.optimize(
            sheets = listOf(sheetOf(type, original)),
            demands = mapOf("R1" to 3uL)
        )

        assertEquals(1, result.size)
        assertEquals(3, result.first().layers.first().layers.first().units.size)
    }

    @Test
    fun trimsAcrossMultipleSheetsAndStopsWhenDemandMet() {
        val type = sheetType()
        val full = layer("R1", 4)
        val optimizer = TailSheetOptimizer(candidates = emptyList())

        val result = optimizer.optimize(
            sheets = listOf(sheetOf(type, full), sheetOf(type, full), sheetOf(type, full)),
            demands = mapOf("R1" to 5uL)
        )

        // 第一块 4 件，第二块只需 1 件，第三块不再需要。
        // The first sheet yields 4, the second only 1, and the third is no longer needed.
        assertEquals(2, result.size)
        val produced = result.sumOf { it.layers.first().layers.first().units.size }
        assertEquals(5, produced)
    }

    @Test
    fun emptyDemandProducesNoSheet() {
        val type = sheetType()
        val optimizer = TailSheetOptimizer(candidates = emptyList())
        assertTrue(
            optimizer.optimize(
                sheets = listOf(sheetOf(type, layer("R1", 4))),
                demands = emptyMap()
            ).isEmpty()
        )
    }
}

class Bpp2dCsvDatasetLoaderTest {
    private fun groupedCsv(): String {
        return """
            group_index,layer_index,item_id,width,height_meter,shape_type,allow_rotate,amount
            0,0,RECT_L,4.0,3.0,rectangle,true,6
            0,0,RECT_S,2.0,2.0,rectangle,true,8
            0,1,CIRCLE_M,2.0,,circle,true,12
        """.trimIndent()
    }

    @Test
    fun parsesGroupedLayerDataset() {
        val dataset = Bpp2dCsvDatasetLoader().load(groupedCsv()).valueOrFail()
        assertEquals(Bpp2dCsvDatasetKind.GroupedLayer, dataset.kind)
        assertEquals(3, dataset.items.size)
        assertEquals(3, dataset.rows)

        val circle = dataset.items.first { it.first.id.value == "CIRCLE_M" }
        assertEquals(PackingShape2Type.Circle, circle.first.shapeType)
        assertEquals(UInt64(12), circle.second)

        val rectangle = dataset.items.first { it.first.id.value == "RECT_L" }
        assertEquals(PackingShape2Type.Rectangle, rectangle.first.shapeType)
    }

    @Test
    fun parsesMaterialWidthAmountDataset() {
        val text = """
            material,width,height_meter,shape_type,allow_rotate,amount
            PANEL_4x3,4.0,3.0,rectangle,true,6
            DISC_1R,2.0,,circle,true,10
        """.trimIndent()
        val dataset = Bpp2dCsvDatasetLoader().load(text).valueOrFail()
        assertEquals(Bpp2dCsvDatasetKind.MaterialWidthAmount, dataset.kind)
        assertEquals(2, dataset.items.size)
    }

    @Test
    fun accumulatesDuplicateItemRows() {
        val text = """
            material,width,height_meter,shape_type,allow_rotate,amount
            PANEL,4.0,3.0,rectangle,true,2
            PANEL,4.0,3.0,rectangle,true,3
        """.trimIndent()
        val dataset = Bpp2dCsvDatasetLoader().load(text).valueOrFail()
        assertEquals(1, dataset.items.size)
        assertEquals(UInt64(5), dataset.items.first().second)
    }

    @Test
    fun rejectsDuplicateColumns() {
        val text = """
            material,material,width,height_meter,amount
            A,A,4.0,3.0,1
        """.trimIndent()
        assertTrue(Bpp2dCsvDatasetLoader().load(text) is Failed)
    }

    @Test
    fun rejectsUnknownColumns() {
        val text = """
            material,width,height_meter,amount,unknown_column
            A,4.0,3.0,1,x
        """.trimIndent()
        assertTrue(Bpp2dCsvDatasetLoader().load(text) is Failed)
    }

    @Test
    fun rejectsMissingRequiredColumn() {
        val text = """
            material,height_meter,amount
            A,3.0,1
        """.trimIndent()
        assertTrue(Bpp2dCsvDatasetLoader().load(text) is Failed)
    }

    @Test
    fun rejectsInvalidShapeType() {
        val text = """
            material,width,height_meter,shape_type,amount
            A,4.0,3.0,triangle,1
        """.trimIndent()
        assertTrue(Bpp2dCsvDatasetLoader().load(text) is Failed)
    }

    @Test
    fun rejectsNonPositiveAmount() {
        val text = """
            material,width,height_meter,amount
            A,4.0,3.0,0
        """.trimIndent()
        assertTrue(Bpp2dCsvDatasetLoader().load(text) is Failed)
    }

    @Test
    fun rejectsInconsistentGeometryForSameItem() {
        val text = """
            material,width,height_meter,amount
            A,4.0,3.0,1
            A,5.0,3.0,1
        """.trimIndent()
        assertTrue(Bpp2dCsvDatasetLoader().load(text) is Failed)
    }

    @Test
    fun rejectsCellCountMismatch() {
        val text = """
            material,width,height_meter,amount
            A,4.0,3.0
        """.trimIndent()
        assertTrue(Bpp2dCsvDatasetLoader().load(text) is Failed)
    }
}

class ReducedCostContractTest {
    private fun layerOf(itemId: String, count: Int, areaPerPiece: Double = 2.0): Layer<FltX> {
        val shape = rectangleShape2(
            width = FltX(areaPerPiece) * Meter,
            height = FltX(1.0) * Meter
        ).valueOrFail()
        val units = (0 until count).map { index ->
            LayerPlacement2(
                shape = shape,
                position = QuantityPoint2(
                    x = FltX(areaPerPiece * index) * Meter,
                    y = FltX(0.0) * Meter
                ),
                itemId = itemId
            )
        }
        return Layer(
            iteration = 0L,
            from = Layer::class,
            sheet = null,
            shape = Sheet2Shape(
                width = FltX(areaPerPiece * count) * Meter,
                height = FltX(1.0) * Meter
            ),
            units = units
        )
    }

    private fun entry(itemId: String, amount: ULong): Bpp2dAssignmentDemandEntry {
        return Bpp2dAssignmentDemandEntry(
            mode = Bpp2dAssignmentDemandMode.ItemAmount,
            itemId = fuookami.ospf.kotlin.framework.bpp2d.domain.item.model.ItemId(itemId),
            demand = Bpp2dAssignmentDemandValue.Amount(UInt64(amount))
        )
    }

    private fun price(itemId: String, value: Double): Map<Bpp2dDemandShadowPriceKey, FltX> {
        return mapOf(
            Bpp2dDemandShadowPriceKey(
                Bpp2dAssignmentDemandMode.ItemAmount,
                fuookami.ospf.kotlin.framework.bpp2d.domain.item.model.ItemId(itemId)
            ) to FltX(value)
        )
    }

    @Test
    fun highDualMakesColumnImproving() {
        val layer = layerOf("R1", count = 2, areaPerPiece = 2.0)
        // 面积 4；对偶 10 × 覆盖 2 件 = 20 → 检验数 4 − 20 = −16 < 0，是改进列。
        // Area 4; dual 10 times coverage 2 equals 20, so the reduced cost is 4 − 20 = −16 < 0: improving.
        val reducedCost = layerReducedCost(
            layer = layer,
            demandEntries = listOf(entry("R1", 2uL)),
            shadowPrices = price("R1", 10.0)
        )
        assertTrue(reducedCost < FltX.zero)
        assertTrue(
            isImprovingColumn(
                layer = layer,
                demandEntries = listOf(entry("R1", 2uL)),
                shadowPrices = price("R1", 10.0)
            )
        )
    }

    @Test
    fun lowDualMakesColumnUseless() {
        val layer = layerOf("R1", count = 2, areaPerPiece = 2.0)
        // 对偶 0.5 × 2 = 1 < 面积 4 → 检验数 +3，不是改进列。
        // Dual 0.5 times 2 equals 1, below the area 4, so the reduced cost is +3: not improving.
        assertTrue(
            !isImprovingColumn(
                layer = layer,
                demandEntries = listOf(entry("R1", 2uL)),
                shadowPrices = price("R1", 0.5)
            )
        )
    }

    @Test
    fun missingPriceContributesNoDualValue() {
        val layer = layerOf("R1", count = 2, areaPerPiece = 2.0)
        val reducedCost = layerReducedCost(
            layer = layer,
            demandEntries = listOf(entry("R1", 2uL)),
            shadowPrices = price("OTHER", 100.0)
        )
        assertEquals(4.0, reducedCost.toDouble(), 1e-9)
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
