package fuookami.ospf.kotlin.framework.bpp2d.domain.packing.service

import kotlin.math.PI
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.framework.geometry2d.*
import fuookami.ospf.kotlin.framework.bpp2d.infrastructure.dto.*
import fuookami.ospf.kotlin.framework.bpp2d.domain.item.model.*
import fuookami.ospf.kotlin.framework.bpp2d.domain.packing.*

class PackerContractTest {

    @Test
    fun packerExpandsSheetsAndAssignsLoadingOrder() {
        val type = sheetTypeOf(width = 4.0, height = 1.0)
        val sheet = sheetOf(
            type = type,
            units = listOf(
                rectanglePlacementOf(x = 0.0, y = 0.0, width = 2.0, height = 1.0, itemId = "r1"),
                rectanglePlacementOf(x = 2.0, y = 0.0, width = 2.0, height = 1.0, itemId = "r1")
            ),
            batch = "b1"
        )

        val result = Packer()(sheets = listOf(sheet)).valueOrFail()
        val packedSheet = result.aggregation.bins.single()

        assertEquals("sheet-1", packedSheet.name)
        assertEquals(type, packedSheet.type)
        assertEquals(BatchNo("b1"), packedSheet.batchNo)
        assertEquals(2, packedSheet.items.size)
        assertEquals("r1", packedSheet.items[0].itemId)
        assertEquals(UInt64(1), packedSheet.items[0].loadingOrder)
        assertEquals(UInt64(2), packedSheet.items[1].loadingOrder)
        assertEquals(4.0, packedSheet.actualArea.value.toDouble(), 1e-9)
        assertEquals(1.0, packedSheet.loadingRate.toDouble(), 1e-9)
    }

    @Test
    fun packerKeepsSeveralSheetsApart() {
        val type = sheetTypeOf(width = 2.0, height = 1.0)
        val first = sheetOf(
            type = type,
            units = listOf(rectanglePlacementOf(x = 0.0, y = 0.0, width = 2.0, height = 1.0, itemId = "r1"))
        )
        val second = sheetOf(
            type = type,
            units = listOf(rectanglePlacementOf(x = 0.0, y = 0.0, width = 1.0, height = 1.0, itemId = "r2"))
        )

        val result = Packer()(sheets = listOf(first, second)).valueOrFail()

        assertEquals(2, result.aggregation.bins.size)
        assertEquals("sheet-1", result.aggregation.bins[0].name)
        assertEquals("sheet-2", result.aggregation.bins[1].name)
        assertEquals(0.5, result.aggregation.bins[1].loadingRate.toDouble(), 1e-9)
    }

    @Test
    fun packerRejectsPlacementOutsideSheet() {
        val type = sheetTypeOf(width = 4.0, height = 1.0)
        val sheet = sheetOf(
            type = type,
            units = listOf(rectanglePlacementOf(x = 3.0, y = 0.0, width = 2.0, height = 1.0, itemId = "r1"))
        )

        val result = Packer()(sheets = listOf(sheet))
        assertTrue(result is Failed)
    }

    @Test
    fun packerRejectsOverlappingPlacements() {
        val type = sheetTypeOf(width = 4.0, height = 1.0)
        val sheet = sheetOf(
            type = type,
            units = listOf(
                rectanglePlacementOf(x = 0.0, y = 0.0, width = 2.0, height = 1.0, itemId = "r1"),
                rectanglePlacementOf(x = 1.0, y = 0.0, width = 2.0, height = 1.0, itemId = "r2")
            )
        )

        val result = Packer()(sheets = listOf(sheet))
        assertTrue(result is Failed)
    }

    @Test
    fun packerAcceptsTangentCirclesAndMeasuresPiRSquaredArea() {
        val type = sheetTypeOf(width = 2.0, height = 1.0)
        val sheet = sheetOf(
            type = type,
            units = listOf(
                circlePlacementOf(x = 0.0, y = 0.0, radius = 0.5, itemId = "c1"),
                circlePlacementOf(x = 1.0, y = 0.0, radius = 0.5, itemId = "c2")
            )
        )

        val result = Packer()(sheets = listOf(sheet)).valueOrFail()
        val packedSheet = result.aggregation.bins.single()

        assertEquals(2, packedSheet.items.size)
        assertEquals(PI * 0.5, packedSheet.actualArea.value.toDouble(), 1e-9)
        assertEquals(PI * 0.25, packedSheet.loadingRate.toDouble(), 1e-9)
    }

    @Test
    fun packerRejectsIllegalRotationWithItemCatalog() {
        val type = sheetTypeOf(width = 10.0, height = 10.0)
        val sheet = sheetOf(
            type = type,
            units = listOf(
                rectanglePlacementOf(
                    x = 0.0,
                    y = 0.0,
                    width = 1.0,
                    height = 3.0,
                    rotated = true,
                    itemId = "r1"
                )
            )
        )

        val fixed = Packer(
            itemCatalog = mapOf(ItemId("r1") to rectangleItemOf(id = "r1", allowRotate = false))
        )
        assertTrue(fixed(sheets = listOf(sheet)) is Failed)

        val rotatable = Packer(
            itemCatalog = mapOf(ItemId("r1") to rectangleItemOf(id = "r1", allowRotate = true))
        )
        assertTrue(rotatable(sheets = listOf(sheet)) is Ok)
    }

    @Test
    fun packerSummarizesMaterialsFromItemCatalog() {
        val material = MaterialKey(
            no = MaterialNo("M1"),
            type = MaterialType.RawMaterial
        )
        val item = rectangleItemOf(
            id = "r1",
            allowRotate = false,
            width = 1.0,
            height = 1.0,
            materialAmounts = mapOf(material to UInt64(2))
        )
        val type = sheetTypeOf(width = 2.0, height = 1.0)
        val sheet = sheetOf(
            type = type,
            units = listOf(
                rectanglePlacementOf(x = 0.0, y = 0.0, width = 1.0, height = 1.0, itemId = "r1"),
                rectanglePlacementOf(x = 1.0, y = 0.0, width = 1.0, height = 1.0, itemId = "r1")
            )
        )

        val result = Packer(itemCatalog = mapOf(ItemId("r1") to item))(sheets = listOf(sheet)).valueOrFail()

        assertEquals(1, result.materialSummary.size)
        assertEquals(material, result.materialSummary.single().material)
        assertEquals(UInt64(4), result.materialSummary.single().amount)
    }

    @Test
    fun packerCarriesContextInfoAndIgnoresUnknownItems() {
        val type = sheetTypeOf(width = 2.0, height = 1.0)
        val sheet = sheetOf(
            type = type,
            units = listOf(rectanglePlacementOf(x = 0.0, y = 0.0, width = 2.0, height = 1.0, itemId = "missing"))
        )
        val context = PackingContext(
            restItems = mapOf(ItemId("missing") to UInt64(3)),
            info = mapOf("plant" to "P1")
        )

        val result = Packer()(sheets = listOf(sheet), context = context).valueOrFail()

        assertEquals("P1", result.info["plant"])
        assertTrue(result.materialSummary.isEmpty())
    }
}

class RendererAdapterContractTest {

    @Test
    fun rendererEmitsCircleRadiusDiameterAndPiRSquaredArea() {
        val type = sheetTypeOf(width = 2.0, height = 1.0, code = "S-CIRCLE")
        val sheet = sheetOf(
            type = type,
            units = listOf(circlePlacementOf(x = 0.0, y = 0.0, radius = 0.5, itemId = "c1"))
        )
        val result = Packer()(sheets = listOf(sheet)).valueOrFail()

        val schema = PackingRendererAdapter().toSchema(result).valueOrFail()
        val plan = schema.plans.single()
        val item = plan.items.single()

        assertEquals("sheet-1", plan.name)
        assertEquals("S-CIRCLE", plan.sheet.typeCode)
        assertEquals(2.0, plan.sheet.width, 1e-9)
        assertEquals(1.0, plan.sheet.height, 1e-9)
        assertEquals(2.0, plan.sheet.area, 1e-9)
        assertEquals(PI * 0.25 / 2.0, plan.sheet.loadingRate, 1e-9)
        assertEquals("c1", item.name)
        assertEquals(RenderShape2TypeDTO.Circle, item.shapeType)
        assertEquals(0.5, item.radius.orFail(), 1e-9)
        assertEquals(1.0, item.diameter.orFail(), 1e-9)
        assertEquals(PI * 0.25, item.actualArea, 1e-9)
        assertEquals(1.0, item.boundingArea, 1e-9)
        assertEquals(1L, item.loadingOrder)
        assertEquals("sheet-1", item.info["sheet"])
    }

    @Test
    fun rendererEmitsRectangleShapeWithoutRadius() {
        val type = sheetTypeOf(width = 2.0, height = 1.0)
        val sheet = sheetOf(
            type = type,
            units = listOf(rectanglePlacementOf(x = 0.0, y = 0.0, width = 2.0, height = 1.0, itemId = "r1"))
        )
        val result = Packer()(sheets = listOf(sheet)).valueOrFail()

        val schema = PackingRendererAdapter().toSchema(result).valueOrFail()
        val plan = schema.plans.single()
        val item = plan.items.single()

        assertEquals(RenderShape2TypeDTO.Rectangle, item.shapeType)
        assertNull(item.radius)
        assertNull(item.diameter)
        assertEquals(0.0, item.x, 1e-9)
        assertEquals(0.0, item.y, 1e-9)
        assertEquals(2.0, item.boundingWidth, 1e-9)
        assertEquals(1.0, item.boundingHeight, 1e-9)
        assertEquals(2.0, item.actualArea, 1e-9)
        assertEquals(2.0, item.boundingArea, 1e-9)
        assertEquals("1", plan.kpi["item_count"])
    }

    @Test
    fun rendererSummaryCarriesMaterialsAndContextInfo() {
        val material = MaterialKey(
            no = MaterialNo("M1"),
            type = MaterialType.RawMaterial
        )
        val item = rectangleItemOf(
            id = "r1",
            allowRotate = false,
            width = 1.0,
            height = 1.0,
            materialAmounts = mapOf(material to UInt64(2))
        )
        val type = sheetTypeOf(width = 1.0, height = 1.0)
        val sheet = sheetOf(
            type = type,
            units = listOf(rectanglePlacementOf(x = 0.0, y = 0.0, width = 1.0, height = 1.0, itemId = "r1"))
        )
        val context = PackingContext(info = mapOf("plant" to "P1"))
        val result = Packer(itemCatalog = mapOf(ItemId("r1") to item))(
            sheets = listOf(sheet),
            context = context
        ).valueOrFail()

        val schema = PackingRendererAdapter().toSchema(result).valueOrFail()

        assertEquals("1", schema.summary["sheet_count"])
        assertEquals("1", schema.summary["item_count"])
        assertEquals("1", schema.summary["material_count"])
        assertEquals("2", schema.summary["material:M1-RawMaterial"])
        assertEquals("P1", schema.summary["plant"])
    }

    @Test
    fun rendererRejectsResultWithOutOfSheetGeometry() {
        val result = PackingResult(
            aggregation = PackingAggregation(
                bins = listOf(
                    PackedSheet(
                        name = "sheet-1",
                        type = sheetTypeOf(width = 2.0, height = 2.0),
                        items = listOf(
                            PackedItem(
                                placement = rectanglePlacementOf(
                                    x = 1.5,
                                    y = 0.0,
                                    width = 2.0,
                                    height = 1.0,
                                    itemId = "r1"
                                ),
                                loadingOrder = UInt64(1),
                                itemId = "r1"
                            )
                        )
                    )
                )
            )
        )

        val schema = PackingRendererAdapter().toSchema(result)

        assertTrue(schema is Failed)
        assertTrue(schema.errorMessageOrFail().contains("type=outside_sheet"))
    }
}